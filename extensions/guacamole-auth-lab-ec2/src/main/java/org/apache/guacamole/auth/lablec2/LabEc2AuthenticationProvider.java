/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.guacamole.auth.lablec2;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.environment.LocalEnvironment;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.net.auth.*;
import org.apache.guacamole.net.auth.simple.SimpleConnection;
import org.apache.guacamole.net.auth.permission.ObjectPermission;
import org.apache.guacamole.net.auth.permission.ObjectPermissionSet;
import org.apache.guacamole.protocol.GuacamoleClientInformation;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authentication provider which decorates existing user contexts with a
 * per-user Illustrator-backed lab connection.
 */
public class LabEc2AuthenticationProvider extends AbstractAuthenticationProvider {

    /** Logger. */
    private static final Logger logger = LoggerFactory.getLogger(
            LabEc2AuthenticationProvider.class);

    /**
     * If the user has multiple data sources, this extension can inject the lab
     * connection into more than one data source, resulting in duplicate
     * "My Lab VM" entries. To mitigate this without requiring configuration,
     * we temporarily "pin" each user to a single underlying data source.
     */
    private static final long DECORATION_TARGET_TTL_MS = 10L * 60L * 1000L;
    private static final long BEARER_TOKEN_TTL_MS = 2L * 60L * 60L * 1000L;

    private static final class DecorationTarget {

        private final String authProviderIdentifier;
        private final long chosenAtMs;

        private DecorationTarget(String authProviderIdentifier, long chosenAtMs) {
            this.authProviderIdentifier = authProviderIdentifier;
            this.chosenAtMs = chosenAtMs;
        }

        private boolean isExpired(long nowMs) {
            return nowMs - chosenAtMs > DECORATION_TARGET_TTL_MS;
        }

    }

    private static final class CachedBearerToken {

        private final String token;
        private final long cachedAtMs;

        private CachedBearerToken(String token, long cachedAtMs) {
            this.token = token;
            this.cachedAtMs = cachedAtMs;
        }

        private boolean isExpired(long nowMs) {
            return nowMs - cachedAtMs > BEARER_TOKEN_TTL_MS;
        }

    }

    private final ObjectMapper objectMapper;

    private final String illustratorBaseUrl;
    private final String illustratorConnectionPath;

    private final java.util.concurrent.ConcurrentHashMap<String, DecorationTarget>
            decorationTargetsByUser = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, CachedBearerToken>
            bearerTokenByUser = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Creates the authentication provider, reading configuration from the
     * local environment.
     *
     * @throws GuacamoleException
     *                            if configuration cannot be read.
     */
    @SuppressWarnings("deprecation")
    public LabEc2AuthenticationProvider() throws GuacamoleException {
        Environment env = new LocalEnvironment();

        illustratorBaseUrl = env.getRequiredProperty(LabEc2Properties.LAB_EC2_ILLUSTRATOR_BASE_URL);
        illustratorConnectionPath = env.getProperty(
                LabEc2Properties.LAB_EC2_ILLUSTRATOR_CONNECTION_PATH,
                "/api/v1/guac/connection"
        );

        logger.info(
                "Initializing lab-ec2 extension: illustratorBaseUrl='{}' connectionPath='{}'",
                illustratorBaseUrl, illustratorConnectionPath);

        objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public String getIdentifier() {
        return "lab-ec2";
    }

    @Override
    public UserContext decorate(UserContext context,
            AuthenticatedUser authenticatedUser, Credentials credentials)
            throws GuacamoleException {

        if (context == null) {
            logger.debug("Skipping lab EC2 decoration because no base context was provided.");
            return null;
        }

        logger.debug("Evaluating lab EC2 decoration for user '{}'.",
                authenticatedUser.getIdentifier());

        String baseAuthProvider = context.getAuthenticationProvider() != null
                ? context.getAuthenticationProvider().getIdentifier()
                : null;

        if (baseAuthProvider != null) {
            long nowMs = System.currentTimeMillis();
            DecorationTarget target = decorationTargetsByUser.compute(authenticatedUser.getIdentifier(),
                    (key, existing) -> (existing == null || existing.isExpired(nowMs))
                            ? new DecorationTarget(baseAuthProvider, nowMs)
                            : existing);

            if (!baseAuthProvider.equals(target.authProviderIdentifier)) {
                logger.debug("Skipping lab-ec2 decoration for user '{}' in auth provider '{}' (already decorating via '{}' chosen {} ms ago).",
                        authenticatedUser.getIdentifier(), baseAuthProvider,
                        target.authProviderIdentifier, nowMs - target.chosenAtMs);
                return context;
            }
        }

        String owner = authenticatedUser.getIdentifier().toLowerCase(Locale.ROOT);

        logger.info("Resolving lab connection for user '{}'.",
                authenticatedUser.getIdentifier());

        String token = extractBearerToken(authenticatedUser, credentials);
        ConnectionResponse connection = resolveConnection(null, token);
        if (connection == null) {
            logger.info("No active lab mapping found for user '{}'; skipping lab-ec2 decoration.",
                    authenticatedUser.getIdentifier());
            return context;
        }
        String hostname = connection.host;
        if (hostname == null || hostname.isEmpty()) {
            throw new GuacamoleServerException("Lab VM has no reachable host yet.");
        }

        String connectionProtocol = normalizeProtocol(connection.protocol);
        if (connectionProtocol == null) {
            throw new GuacamoleServerException("Missing protocol from Illustrator response.");
        }
        String connectionPort = connection.port != null ? connection.port.toString() : null;
        if (connectionPort == null || connectionPort.isEmpty()) {
            throw new GuacamoleServerException("Missing port from Illustrator response.");
        }
        if (connection.username == null || connection.username.trim().isEmpty()) {
            throw new GuacamoleServerException("Missing username from Illustrator response.");
        }
        if (connection.password == null || connection.password.trim().isEmpty()) {
            throw new GuacamoleServerException("Missing password from Illustrator response.");
        }

        // Determine connection ID
        String connectionId = "lab-" + owner;
        String connectionName = "My Lab VM";
        if ("BUILDER".equalsIgnoreCase(connection.purpose)) {
            connectionId = "lab-build-" + owner;
            connectionName = "Lab Build VM";
        }

        // Check if connection already exists
        Directory<Connection> baseDir = context.getConnectionDirectory();
        Connection existingConnection = null;
        try {
            existingConnection = baseDir.get(connectionId);
        } catch (GuacamoleException e) {
            logger.debug("Failed to check for existing connection '{}': {}", connectionId, e.getMessage());
        }

        if (existingConnection != null) {
            logger.info("Connection '{}' already exists for user '{}'. Using existing connection.",
                    connectionId, authenticatedUser.getIdentifier());
            return context;
        }

        logger.info("Using lab host '{}' for user '{}' (protocol='{}' port='{}').",
                hostname, authenticatedUser.getIdentifier(), connectionProtocol, connectionPort);

        LabActiveConnectionDirectory mergedActiveConnections =
                new LabActiveConnectionDirectory(context.getActiveConnectionDirectory());

        Connection labConnection = buildLabConnection(connectionId, connectionName, owner, hostname,
                authenticatedUser.getIdentifier(),
                credentials.getRemoteHostname(),
                connectionProtocol, connectionPort, connection.username, connection.password,
                mergedActiveConnections);
        logger.debug("Prepared lab connection '{}' (name='{}') for user '{}' using protocol='{}' port='{}'.",
                labConnection.getIdentifier(), labConnection.getName(),
                authenticatedUser.getIdentifier(), connectionProtocol, connectionPort);

        final String rootId = context.getRootConnectionGroup().getIdentifier();
        labConnection.setParentIdentifier(rootId);

        Directory<Connection> mergedDir = new LabMergingConnectionDirectory(
                baseDir, labConnection);

        final ConnectionGroup mergedRoot = new DelegatingConnectionGroup(context.getRootConnectionGroup()) {
            @Override
            public Set<String> getConnectionIdentifiers() throws GuacamoleException {
                Set<String> ids = new HashSet<>(super.getConnectionIdentifiers());
                ids.add(labConnection.getIdentifier());
                return Collections.unmodifiableSet(ids);
            }
        };

        final User mergedSelf = new DelegatingUser(context.self()) {

            @Override
            public Permissions getEffectivePermissions() throws GuacamoleException {
                Permissions effective = super.getEffectivePermissions();
                return new LabPermissions(effective, labConnection.getIdentifier());
            }

            @Override
            public ObjectPermissionSet getConnectionPermissions() throws GuacamoleException {
                return new LabObjectPermissionSet(super.getConnectionPermissions(),
                        labConnection.getIdentifier());
            }

        };

        try {
            Permissions effective = mergedSelf.getEffectivePermissions();
            boolean hasRead = effective.getConnectionPermissions().hasPermission(
                    ObjectPermission.Type.READ, labConnection.getIdentifier());
            logger.debug("Effective permissions for user '{}' include READ on injected connection '{}': {}",
                    authenticatedUser.getIdentifier(), labConnection.getIdentifier(), hasRead);
        } catch (GuacamoleException e) {
            logger.warn("Unable to verify effective permissions for injected connection '{}' for user '{}': {}",
                    labConnection.getIdentifier(), authenticatedUser.getIdentifier(), e.getMessage());
        }

        try {
            logger.debug("Merged root connection identifiers for user '{}': {}",
                    authenticatedUser.getIdentifier(), mergedRoot.getConnectionIdentifiers());
        } catch (GuacamoleException e) {
            logger.debug("Unable to enumerate merged root connections for user '{}': {}",
                    authenticatedUser.getIdentifier(), e.getMessage());
        }

        return new DelegatingUserContext(context) {
            @Override
            public Directory<Connection> getConnectionDirectory() throws GuacamoleException {
                return mergedDir;
            }

            @Override
            public ConnectionGroup getRootConnectionGroup() throws GuacamoleException {
                return mergedRoot;
            }

            @Override
            public User self() {
                return mergedSelf;
            }

            @Override
            public Directory<ActiveConnection> getActiveConnectionDirectory() throws GuacamoleException {
                return mergedActiveConnections;
            }
        };
    }

    /**
     * Permissions wrapper that ensures the injected lab connection is actually
     * visible/usable (READ permission) within the decorated user context.
     */
    private static final class LabPermissions implements Permissions {

        private final Permissions delegate;
        private final String labConnectionId;

        private LabPermissions(Permissions delegate, String labConnectionId) {
            this.delegate = delegate;
            this.labConnectionId = labConnectionId;
        }

        @Override
        public ObjectPermissionSet getConnectionPermissions() throws GuacamoleException {
            return new LabObjectPermissionSet(delegate.getConnectionPermissions(), labConnectionId);
        }

        @Override
        public ObjectPermissionSet getActiveConnectionPermissions() throws GuacamoleException {
            return delegate.getActiveConnectionPermissions();
        }

        @Override
        public ObjectPermissionSet getConnectionGroupPermissions() throws GuacamoleException {
            return delegate.getConnectionGroupPermissions();
        }

        @Override
        public ObjectPermissionSet getSharingProfilePermissions() throws GuacamoleException {
            return delegate.getSharingProfilePermissions();
        }

        @Override
        public org.apache.guacamole.net.auth.permission.SystemPermissionSet getSystemPermissions()
                throws GuacamoleException {
            return delegate.getSystemPermissions();
        }

        @Override
        public ObjectPermissionSet getUserPermissions() throws GuacamoleException {
            return delegate.getUserPermissions();
        }

        @Override
        public ObjectPermissionSet getUserGroupPermissions() throws GuacamoleException {
            return delegate.getUserGroupPermissions();
        }

    }

    /**
     * ObjectPermissionSet wrapper that grants READ permission for the injected
     * lab connection identifier.
     */
    private static final class LabObjectPermissionSet implements ObjectPermissionSet {

        private final ObjectPermissionSet delegate;
        private final String labConnectionId;

        private LabObjectPermissionSet(ObjectPermissionSet delegate, String labConnectionId) {
            this.delegate = delegate;
            this.labConnectionId = labConnectionId;
        }

        @Override
        public boolean hasPermission(ObjectPermission.Type permission, String identifier)
                throws GuacamoleException {

            if (labConnectionId.equals(identifier) && permission == ObjectPermission.Type.READ)
                return true;

            return delegate.hasPermission(permission, identifier);
        }

        @Override
        public void addPermission(ObjectPermission.Type permission, String identifier)
                throws GuacamoleException {
            delegate.addPermission(permission, identifier);
        }

        @Override
        public void removePermission(ObjectPermission.Type permission, String identifier)
                throws GuacamoleException {
            delegate.removePermission(permission, identifier);
        }

        @Override
        public Collection<String> getAccessibleObjects(
                Collection<ObjectPermission.Type> permissions,
                Collection<String> identifiers) throws GuacamoleException {

            Collection<String> accessible = new ArrayList<>(
                    delegate.getAccessibleObjects(permissions, identifiers));

            if (identifiers.contains(labConnectionId)
                    && permissions.contains(ObjectPermission.Type.READ)
                    && !accessible.contains(labConnectionId))
                accessible.add(labConnectionId);

            return accessible;
        }

        @Override
        public Set<ObjectPermission> getPermissions() throws GuacamoleException {
            Set<ObjectPermission> merged = new HashSet<>(delegate.getPermissions());
            merged.add(new ObjectPermission(ObjectPermission.Type.READ, labConnectionId));
            return merged;
        }

        @Override
        public void addPermissions(Set<ObjectPermission> permissions) throws GuacamoleException {
            delegate.addPermissions(permissions);
        }

        @Override
        public void removePermissions(Set<ObjectPermission> permissions) throws GuacamoleException {
            delegate.removePermissions(permissions);
        }

    }

    /**
     * Directory wrapper that merges the active connections tracked by the
     * underlying UserContext with active connections created through the lab
     * connection. This is required because {@link SimpleConnection} does not
     * provide active connection tracking, and Guacamole expects active
     * connection metadata to be available for an active tunnel.
     */
    private static final class LabActiveConnectionDirectory implements Directory<ActiveConnection> {

        private final Directory<ActiveConnection> base;
        private final java.util.concurrent.ConcurrentHashMap<String, ActiveConnection> labActiveConnections =
                new java.util.concurrent.ConcurrentHashMap<>();

        private LabActiveConnectionDirectory(Directory<ActiveConnection> base) {
            this.base = base;
        }

        private void register(ActiveConnection activeConnection) {
            if (activeConnection != null && activeConnection.getIdentifier() != null)
                labActiveConnections.put(activeConnection.getIdentifier(), activeConnection);
        }

        private void unregister(String identifier) {
            if (identifier != null)
                labActiveConnections.remove(identifier);
        }

        @Override
        public ActiveConnection get(String identifier) throws GuacamoleException {
            ActiveConnection local = labActiveConnections.get(identifier);
            if (local != null)
                return local;
            return base.get(identifier);
        }

        @Override
        public Collection<ActiveConnection> getAll(Collection<String> identifiers) throws GuacamoleException {

            Set<String> baseIdentifiers = new HashSet<>(identifiers);
            List<ActiveConnection> all = new ArrayList<>();

            // Add local (lab) active connections first
            for (String identifier : identifiers) {
                ActiveConnection local = labActiveConnections.get(identifier);
                if (local != null) {
                    all.add(local);
                    baseIdentifiers.remove(identifier);
                }
            }

            // Add remaining from base directory
            if (!baseIdentifiers.isEmpty())
                all.addAll(base.getAll(baseIdentifiers));

            return all;
        }

        @Override
        public Set<String> getIdentifiers() throws GuacamoleException {
            Set<String> ids = new HashSet<>(base.getIdentifiers());
            ids.addAll(labActiveConnections.keySet());
            return ids;
        }

        @Override
        public void add(ActiveConnection object) throws GuacamoleException {
            base.add(object);
        }

        @Override
        public void update(ActiveConnection object) throws GuacamoleException {
            base.update(object);
        }

        @Override
        public void remove(String identifier) throws GuacamoleException {
            if (labActiveConnections.containsKey(identifier)) {
                labActiveConnections.remove(identifier);
                return;
            }
            base.remove(identifier);
        }

    }

    /**
     * Tunnel wrapper that unregisters its active connection entry when closed.
     */
    private static final class LabTrackingTunnel implements GuacamoleTunnel {

        private final GuacamoleTunnel delegate;
        private final Runnable onClose;

        private LabTrackingTunnel(GuacamoleTunnel delegate, Runnable onClose) {
            this.delegate = delegate;
            this.onClose = onClose;
        }

        @Override
        public org.apache.guacamole.io.GuacamoleReader acquireReader() {
            return delegate.acquireReader();
        }

        @Override
        public void releaseReader() {
            delegate.releaseReader();
        }

        @Override
        public boolean hasQueuedReaderThreads() {
            return delegate.hasQueuedReaderThreads();
        }

        @Override
        public org.apache.guacamole.io.GuacamoleWriter acquireWriter() {
            return delegate.acquireWriter();
        }

        @Override
        public void releaseWriter() {
            delegate.releaseWriter();
        }

        @Override
        public boolean hasQueuedWriterThreads() {
            return delegate.hasQueuedWriterThreads();
        }

        @Override
        public java.util.UUID getUUID() {
            return delegate.getUUID();
        }

        @Override
        public org.apache.guacamole.net.GuacamoleSocket getSocket() {
            return delegate.getSocket();
        }

        @Override
        public void close() throws GuacamoleException {
            try {
                delegate.close();
            }
            finally {
                try {
                    onClose.run();
                }
                catch (RuntimeException ignored) {
                }
            }
        }

        @Override
        public boolean isOpen() {
            return delegate.isOpen();
        }

    }

    /**
     * Connection that behaves like {@link SimpleConnection} but registers an
     * {@link ActiveConnection} entry so Guacamole can resolve active tunnel
     * metadata (ex: /activeConnection/connection/sharingProfiles).
     */
    private static final class LabTrackedConnection extends SimpleConnection {

        private final String username;
        private final String remoteHost;
        private final LabActiveConnectionDirectory activeConnections;

        private LabTrackedConnection(String name, String identifier, GuacamoleConfiguration config,
                String username, String remoteHost, LabActiveConnectionDirectory activeConnections) {
            super(name, identifier, config);
            this.username = username;
            this.remoteHost = remoteHost;
            this.activeConnections = activeConnections;
        }

        @Override
        public GuacamoleTunnel connect(GuacamoleClientInformation info, Map<String, String> tokens)
                throws GuacamoleException {

            GuacamoleTunnel tunnel = super.connect(info, tokens);

            // Track this tunnel as an active connection using the tunnel UUID
            String activeConnectionId = tunnel.getUUID().toString();

            ActiveConnection activeConnection = new AbstractActiveConnection() {
                @Override
                public org.apache.guacamole.net.auth.credentials.UserCredentials getSharingCredentials(String identifier)
                        throws GuacamoleException {
                    throw new org.apache.guacamole.GuacamoleSecurityException("Permission denied.");
                }
            };

            activeConnection.setIdentifier(activeConnectionId);
            activeConnection.setConnectionIdentifier(getIdentifier());
            activeConnection.setUsername(username);
            activeConnection.setRemoteHost(remoteHost);
            activeConnection.setStartDate(new Date());

            LabTrackingTunnel trackingTunnel = new LabTrackingTunnel(tunnel,
                    () -> activeConnections.unregister(activeConnectionId));
            activeConnection.setTunnel(trackingTunnel);

            activeConnections.register(activeConnection);

            return trackingTunnel;
        }

    }

    private static final class ConnectionResponse {
        public String purpose;
        public String protocol;
        public String host;
        public Integer port;
        public String username;
        public String password;
        public String vmStatus;
        public String note;
    }

    private String extractBearerToken(AuthenticatedUser authenticatedUser, Credentials requestCredentials)
            throws GuacamoleException {

        String userId = authenticatedUser != null ? authenticatedUser.getIdentifier() : null;
        long nowMs = System.currentTimeMillis();

        String token = firstNonBlank(
                tokenFromCredentials(requestCredentials),
                tokenFromCredentials(authenticatedUser != null ? authenticatedUser.getCredentials() : null)
        );

        if (token != null) {
            if (userId != null && !userId.isEmpty()) {
                bearerTokenByUser.put(userId, new CachedBearerToken(token, nowMs));
            }
            return token;
        }

        if (userId != null && !userId.isEmpty()) {
            CachedBearerToken cached = bearerTokenByUser.get(userId);
            if (cached != null) {
                if (!cached.isExpired(nowMs)) {
                    return cached.token;
                }
                bearerTokenByUser.remove(userId);
            }
        }

        throw new GuacamoleServerException("Missing access token for Illustrator request.");
    }

    private String tokenFromCredentials(Credentials credentials) {
        if (credentials == null) {
            return null;
        }

        String token = firstNonBlank(
                credentials.getParameter("access_token"),
                credentials.getParameter("id_token")
        );
        logger.debug(token);
        if (isLikelyJwt(token)) {
            return token;
        }

        String authorization = credentials.getHeader("Authorization");
        if (authorization != null) {
            String value = authorization.trim();
            if (value.regionMatches(true, 0, "Bearer ", 0, 7) && value.length() > 7) {
                String bearer = value.substring(7).trim();
                if (isLikelyJwt(bearer)) {
                    return bearer;
                }
            }
        }

        String tokenParam = credentials.getParameter("token");
        if (isLikelyJwt(tokenParam)) {
            return tokenParam;
        }

        return null;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null) {
                String trimmed = value.trim();
                if (!trimmed.isEmpty()) {
                    return trimmed;
                }
            }
        }
        return null;
    }

    private boolean isLikelyJwt(String value) {
        if (value == null) {
            return false;
        }

        String token = value.trim();
        if (token.isEmpty()) {
            return false;
        }

        int firstDot = token.indexOf('.');
        int secondDot = firstDot >= 0 ? token.indexOf('.', firstDot + 1) : -1;
        return firstDot > 0 && secondDot > firstDot + 1 && secondDot < token.length() - 1;
    }

    private ConnectionResponse resolveConnection(String purpose, String token) throws GuacamoleException {
        URI uri = buildConnectionUri(purpose);
        HttpURLConnection connection = null;
        try {
            URL url = uri.toURL();
            logger.debug("Calling Illustrator connection API at '{}'.", uri);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(600000);
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(false);

            int status = connection.getResponseCode();
            String body = readResponseBody(connection, status >= 200 && status < 300);
            logger.debug(body);
            if (status >= 200 && status < 300) {
                if (body == null || body.trim().isEmpty()) {
                    throw new GuacamoleServerException("Illustrator returned an empty connection response.");
                }
                try {
                    ConnectionResponse resolved = objectMapper.readValue(body, ConnectionResponse.class);
                    if (resolved.vmStatus != null && !"RUNNING".equalsIgnoreCase(resolved.vmStatus)) {
                        throw new GuacamoleServerException("Lab VM is still starting. Please retry shortly.");
                    }
                    return resolved;
                } catch (IOException e) {
                    throw new GuacamoleServerException("Unable to parse Illustrator response.", e);
                }
            }

            if (status == 404) {
                throw new GuacamoleServerException("No active lab VM mapping found for this user.");
            }

            if (status == 401 || status == 403) {
                throw new GuacamoleServerException("Not authorized to access the lab VM.");
            }

            throw new GuacamoleServerException("Illustrator returned HTTP " + status + ".");
        } catch (IOException e) {
            throw new GuacamoleServerException("Unable to contact Illustrator API.", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private URI buildConnectionUri(String purpose) throws GuacamoleException {
        String base = illustratorBaseUrl != null ? illustratorBaseUrl.trim() : "";
        if (base.isEmpty()) {
            throw new GuacamoleServerException("Illustrator base URL is not configured.");
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        String path = illustratorConnectionPath != null ? illustratorConnectionPath.trim() : "";
        if (path.isEmpty()) {
            path = "/api/v1/guac/connection";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        StringBuilder url = new StringBuilder(base).append(path);
        if (purpose != null && !purpose.trim().isEmpty()) {
            url.append("?purpose=").append(urlEncode(purpose));
        }

        return URI.create(url.toString());
    }

    private String normalizeProtocol(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Builds a connection targeting the given host for the specified user.
     */
    private Connection buildLabConnection(String connectionId, String connectionName, String owner,
            String hostname, String username,
            String remoteHost, String connectionProtocol, String connectionPort,
            String connectionUsername, String connectionPassword,
            LabActiveConnectionDirectory activeConnections) {

        GuacamoleConfiguration config = new GuacamoleConfiguration();
        config.setProtocol(connectionProtocol);
        config.setParameter("hostname", hostname);
        config.setParameter("port", connectionPort);
        String resolvedUsername = connectionUsername != null ? connectionUsername.trim() : "";
        config.setParameter("username", resolvedUsername);
        config.setParameter("ignore-cert", "true");

        String resolvedPassword = connectionPassword != null ? connectionPassword.trim() : "";
        if (!resolvedPassword.isEmpty())
            config.setParameter("password", resolvedPassword);

        logger.debug(
                "Building lab connection config for '{}' -> hostname='{}' port='{}' vmUsername='{}' passwordSet={}",
                connectionId, hostname, connectionPort, resolvedUsername,
                !resolvedPassword.isEmpty());

        return new LabTrackedConnection(connectionName, connectionId, config,
                username, remoteHost, activeConnections);
    }

    private String readResponseBody(HttpURLConnection connection, boolean success) throws IOException {
        InputStream stream = success ? connection.getInputStream() : connection.getErrorStream();
        if (stream == null) {
            return null;
        }
        try (InputStream input = stream; ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[4096];
            int read;
            while ((read = input.read(data)) != -1) {
                buffer.write(data, 0, read);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private String urlEncode(String value) throws GuacamoleException {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new GuacamoleServerException("UTF-8 encoding not supported.", e);
        }
    }

}
