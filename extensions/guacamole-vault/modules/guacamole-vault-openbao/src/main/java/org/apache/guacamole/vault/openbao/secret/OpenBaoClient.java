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

package org.apache.guacamole.vault.openbao.secret;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.net.event.listener.Listener;
import org.apache.guacamole.net.event.TunnelCloseEvent;
import org.apache.guacamole.net.event.TunnelConnectEvent;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.apache.guacamole.vault.openbao.conf.OpenBaoConfigurationService;
import org.apache.guacamole.vault.openbao.vault.FileTokenAuthentication;
import org.apache.guacamole.vault.openbao.vault.UsernamePasswordAuthentication;
import org.apache.guacamole.vault.openbao.vault.UsernamePasswordAuthenticationOptions;
import org.apache.guacamole.vault.openbao.vault.TtlAwareSessionManager;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultKeyValueOperations;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.VaultException;
import org.springframework.vault.support.VaultResponse;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class OpenBaoClient implements Listener {
    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(OpenBaoClient.class);

    /**
     * Service for retrieving OpenBao configuration.
     */
    private OpenBaoConfigurationService configService;

    /**
     * A singleton ObjectMapper for converting a Map to a JSON string when
     * returning a complex token.
     */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * The prefix of the Guacamole token to resolve on the vault server.
     */
    static final String VAULT_TOKEN_PREFIX = "vault://";

    /**
     * Cache of secrets recently fetched
     */
    private  Cache<String, Map<String, Object>> cache;

    /**
     * Vault template that will be used with all of the mount paths
     */
    private VaultTemplate vaultTemplate;

    /*
     * Ttl aware token manager for automatic token renewal
     */
    private TtlAwareSessionManager sessionManager;

    /**
     * Vault client authentication object
     */
    private ClientAuthentication authentication;

    /**
     * A HashMap of the checked out LDAP Sessions
     */
    private final Map<String, LDAPSessionInfo> ldapSessions = new HashMap<>();

    /**
     * Constructor allowing early injection of configuration and initialization
     * to start the token renewal process as early as possible
     */
    public OpenBaoClient(OpenBaoConfigurationService configService) {
      this.configService = configService;
    }

    /**
     * Complete the instantiation of the class on first use
     */
    public void init() throws GuacamoleException {
        try {
            VaultEndpoint endpoint = VaultEndpoint.from(configService.getVaultUri().resolve("v1"));

            if (configService.getVaultToken() != null) {
                if (isTokenReadableFile(configService.getVaultToken())) {
                    logger.info("File Token : {}", configService.getVaultToken());
                    this.authentication =
                            new FileTokenAuthentication(configService.getVaultToken(),
                                    endpoint, new RestTemplate());
                }
                else {
                    logger.info("Token : {}", configService.getVaultToken());
                    this.authentication = new TokenAuthentication(configService.getVaultToken());
                }
            }
            else if (configService.getVaultUsername() != null && configService.getVaultPassword() != null) {
                UsernamePasswordAuthenticationOptions options =
                    UsernamePasswordAuthenticationOptions.builder()
                        .username(configService.getVaultUsername())
                        .password(configService.getVaultPassword())
                        .build();

                this.authentication =
                    new UsernamePasswordAuthentication(options, endpoint, new RestTemplate());
            }
            else {
                throw new GuacamoleException("Either a vault token or Username/Password must be supplied");
            }

            // Create a task scheduler for our token renewal
            ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
            scheduler.setPoolSize(1);
            scheduler.setThreadNamePrefix("vault-renewal-");
            scheduler.initialize();

            // Automatically renew tokens before expiration
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(
                configService.getVaultUri().resolve("v1").toString()));
            this.sessionManager = new TtlAwareSessionManager(this.authentication,
                    restTemplate, scheduler, configService.getTokenRenewalDelay());

            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            try {
                requestFactory.setConnectTimeout(configService.getConnectionTimeout());
            }
            catch (GuacamoleException e) {
                logger.debug("Using default vault endpoint connection timeout: " + e.getMessage());
            }
             try {
                requestFactory.setReadTimeout(configService.getRequestTimeout());
            }
            catch (GuacamoleException e) {
                logger.debug("Using default vault endpoint request timeout: " + e.getMessage());
            }
            this.vaultTemplate = new VaultTemplate(endpoint, requestFactory, this.sessionManager);
        }
        catch (Exception e) {
            throw new GuacamoleException("Error initializing Vault client: " + e.getMessage());
        }

        // Initialize the cache with maximum size of 1MB, and cache expiry with
        // forced cleanup
        // FIXME I'd really like to do something like "Array.fill(v, '\0');" in
        // the removal listener to ensure that passwords are no longer in memory.
        // However, both spring-core-vault and Guacamole store these values
        // elsewhere as immutable String values, So even if I stored them in the
        // cache as char[] copies of the password would be eleswhere as String
        // values in memory.. A VaultConverter function could deal with the
        // spring-vault-core part of the problem, but not Gaucamole.
        try {
            cache = Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofMillis(configService.getVaultCacheLifetime()))
                    .maximumSize(1_000_000)
                    .build();
        }
        catch (GuacamoleException e) {
            throw new GuacamoleException("Can't setup OpenBao cache");
        }
    }

    /*
     * Function to detect if the the token is in fact a readable file
     * rather than a token string
     *
     * @param String token
     *     The string with the token returned from configService
     *
     * @return boolean
     *      True is the token is a readable file
     */
     private static boolean isTokenReadableFile(String token) {
         try {
             Path path = Paths.get(token);
             return Files.isRegularFile(path) && Files.isReadable(path);
        }
        catch (Exception e) {
            return false;
        }
     }

    /**
     * Lists the valid mount paths and their type
     *
     * @return Map<String, String>
     *      The map of the mount path and with the value being the type.
     *      The type can be ssh, database, kv_1 or kv_2
     */
    public Map<String, Object> listMountPaths() {

        Map<String, Object> cacheMountPaths =  cache.getIfPresent("sys/mounts");
        if (cacheMountPaths != null) {
            return cacheMountPaths;
        }

        VaultResponse response = vaultTemplate.read("sys/mounts");

        if (response == null || response.getData() == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> mounts = response.getData();
        Map<String, Object> mountPaths = new HashMap<>();

        mounts.forEach((mountPath, mountInfoObj) -> {
            if (mountInfoObj instanceof Map<?, ?>) {
                Map<?,?> mountInfo = (Map<?,?>) mountInfoObj;
                Object type = mountInfo.get("type");
                if (type instanceof String) {
                    if (type.equals("ssh")  || type.equals("database") || type.equals("ldap")) {
                        mountPaths.put(mountPath, type);
                    }
                    else if (type.equals("kv")) {
                        // Need to detect if type 1 or type 2 Key/Value engine
                        if (mountInfo.get("options") instanceof Map<?, ?>) {
                            Map<?,?> options = (Map<?,?>) mountInfo.get("options");
                            Object version = options.get("version");
                            if (version instanceof String) {
                                if (version.equals("2")) {
                                    mountPaths.put(mountPath, "kv_2");
                                }
                                else {
                                    mountPaths.put(mountPath, "kv_1");
                                }
                            }
                        }
                        else {
                            // No options, assume kv_1
                            mountPaths.put(mountPath, "kv_1");
                        }
                    }
                }
            }
        });

        cache.put("sys/mounts", mountPaths);

        return mountPaths;
    }

    /**
     * Contains information about the checked out LDAP sessions
     */
    private static class LDAPSessionInfo {
        final String checkInPath;
        final String username;
        final Instant created;

        LDAPSessionInfo(String checkInPath, String username, Instant created) {
            this.checkInPath = checkInPath;
            this.username = username;
            this.created = created;
        }
    }

    /**
     * The LDAP session interface checks out session that then can not be
     * used till they are checked in. In getTokens we have the problem that
     * we don't have access to the tunnel ID and so can't identify it. Here
     * we assume that the tunnel will connect soon after the call to getTokens
     * and so we can tag the session information created in getTokens with
     * tunnel ID. So we store the tunnel ID with the session data being created
     * and then use it in a close event for the check-in
     *
     * FIXME : This is fragile, as if two connections in parallel are being
     * created, the check-in event might be associate with the incorrect
     * tunnel ID. Need a better solution
     *
     * @param event
     *      A tunnel connect event
     */
    @Override
    public void handleEvent(Object event) throws GuacamoleException {

        if (event instanceof TunnelConnectEvent) {
            GuacamoleTunnel tunnel = ((TunnelConnectEvent) event).getTunnel();
            String uuid = tunnel.getUUID().toString();
            LDAPSessionInfo info = ldapSessions.get("creating");
            if (info != null) {
                logger.info("Saving connection UUID: {}", uuid);
                // Assume we have a service account
                ldapSessions.put(uuid, info);
                ldapSessions.remove("creating");
            }
        }
        else if (event instanceof TunnelCloseEvent) {
            GuacamoleTunnel tunnel = ((TunnelCloseEvent) event).getTunnel();
            String uuid = tunnel.getUUID().toString();
            LDAPSessionInfo session = ldapSessions.get(uuid);
            if (session != null) {
                logger.info("Closing connection UUID: {}", uuid);
                vaultTemplate.write(session.checkInPath, Map.of("service_account_names", session.username));
                ldapSessions.remove(uuid);
            }
        }

        // Do some clean up of the active LDAP sessions. If a session hasn't been
        // checked in after 2 hours, just drop it from the hashMap. As the TTL of
        // the vault is already 2 hours don't need to check it in. Don't really
        // care if the value hang around in our hashmap so don't need a dedicated
        // task for this
        ldapSessions.entrySet().removeIf(e -> Instant.now().isAfter(e.getValue().created.plusSeconds(7200)));
    }

    /**
     * Retrieves a value from a vault by its path. It first parses the
     * leading mount path from the token, ensures it is valid and uses
     * a supported secret engine. It then passes off the rest of the
     * processing to a method dedicated to each secret engine.
     *
     * @param token
     *     The Guacamole token to look up in OpenBao.
     *
     * @param config
     *     A GuacamoleConfiguration containing the connection configuration
     *     parameters.
     *
     * @return
     *     The value associated with the key.
     *
     * @throws GuacamoleException
     *     If the secret cannot be retrieved from OpenBao.
     */
    public String getValue(String token, UserContext userContext, GuacamoleConfiguration config) throws GuacamoleException {
        try {
            if (authentication == null) {
                logger.debug("Initializing OpenBao");
                init();
                logger.debug("Initialized OpenBao");
            }

            if (! token.startsWith(VAULT_TOKEN_PREFIX)) {
                throw new GuacamoleException("Invalid token Vault token: " + token);
            }

            // Before going further replace the arguments "{USERNAME}", "{SERVER}",
            // "{GATEWAY_USERNAME}" and "{GATEWAY_HOSTNAME}" in the token with their
            // with values supplied in the parameters. The value of USER here corresponds
            // to GUAC_USERNAME
            // FIXME Could this be done with the TokenFilter in OpenBaoSecretService ?
            // FIXME There is an edge case for tokens like "vault://ldap/$${USER}/{USER}/password"
            // both here and below. This seems a pretty unlikely case, so don't treat.
            String guac_username = userContext == null ? "" : userContext.self().getIdentifier();
            if (guac_username != null && !guac_username.isEmpty()
                    && token.contains("{GUAC_USERNAME}") && ! token.contains("$${GUAC_USERNAME}")) {
                token = token.replace("{GUAC_USERNAME}", guac_username);

            }
            String hostname = config.getParameter("hostname");
            if (hostname != null && !hostname.isEmpty() && !hostname.contains("${")
                    && token.contains("{HOSTNAME}") && ! token.contains("$${HOSTNAME}")) {
                token = token.replace("{HOSTNAME}", hostname);

            }
            String username = config.getParameter("username");
            if (username != null && !username.isEmpty() && !username.contains("${")
                    && token.contains("{USERNAME}") && ! token.contains("$${USERNAME}")) {
                token = token.replace("{USERNAME}", hostname);

            }
            String gatewayHostname = config.getParameter("gateway-hostname");
            if (gatewayHostname != null && !gatewayHostname.isEmpty() && !gatewayHostname.contains("${")
                    && token.contains("{GATEWAY}") && ! token.contains("$${GATEWAY}")) {
                token = token.replace("{GATEWAY}", gatewayHostname);

            }
            String gatewayUsername = config.getParameter("gateway-username");
            if (gatewayUsername != null && !gatewayUsername.isEmpty() && !gatewayUsername.contains("${")
                    && token.contains("{GATEWAY_USER}") && ! token.contains("$${GATEWAY_USER}")) {
                token = token.replace("{GATEWAY_USER}", gatewayUsername);

            }

            // Detect and validate the mount path
            Map<String, Object> mountpaths = listMountPaths();
            String mountPath = null;
            String type = null;
            for (String _path : mountpaths.keySet()) {
                if (token.startsWith(VAULT_TOKEN_PREFIX + _path) &&
                        (mountPath == null || _path.length() > mountPath.length())) {
                    mountPath = _path;
                    type = (String) mountpaths.get(_path);
                }
            }
            if (mountPath == null || mountPath.isEmpty()) {
                throw new GuacamoleException("The Vault mount path of the token is invalid: " + token);
            }

            // Find last slash to isolate the secret value in the record
            int lastSlashIndex = token.lastIndexOf('/');
            if (lastSlashIndex == -1)
                lastSlashIndex = VAULT_TOKEN_PREFIX.length();

            String path = token.substring(VAULT_TOKEN_PREFIX.length() + mountPath.length(), lastSlashIndex);
            String secret = token.substring(lastSlashIndex + 1);

            logger.info("MountPath {}, path {}, secret {}, type {}", mountPath, path, secret, type);

            switch (type) {
               case "ssh":
                   return getValueSSH(mountPath, path, secret, config);
               case "ldap":
                   return getValueLDAP(mountPath, path, secret);
               case "database":
                   return getValueDB(mountPath, path, secret);
               case "kv_1":
               case "kv_2":
                   return getValueKV(mountPath, path, secret, type);
               default:
                  throw new GuacamoleException("Unknown secret engine for the token: " + token);
            }
        }
        catch (VaultException e) {
            logger.error("ERROR : {}", e.getMessage());
            throw new GuacamoleServerException("Failed to retrieve secret from Vault Server : " + e.getMessage());
        }
    }

    /**
     * Retrieves a value from a key-value secret engine of a vault.
     *
     * @param mountPath
     *     The mountPath of the key-value secret engine on the vault server.
     *
     * @param path
     *     The path of the secret record
     *
     * @param secret
     *     The secret value to return
     *
     * @param type
     *     The type of key-value store.  Either "kv_1" or "kv_2"
     *
     * @return
     *     The value associated with the secret.
     *
     * @throws GuacamoleException
     *     If the secret cannot be retrieved from OpenBao.
     */
    private String getValueKV(String mountPath, String path, String secret, String type) throws GuacamoleException {
        // Is the key-value already in the cache
        Map<String, Object> cacheResponse =  cache.getIfPresent(mountPath + "/" + path);

        if (cacheResponse != null) {
            // The path is already cached?. Use it
            Object raw = cacheResponse.get(secret);
            if (raw instanceof String || raw instanceof Number || raw instanceof Boolean) {
                return String.valueOf(raw);
            }
            else {
                try {
                    // Stored JSON value.. Probably not usable, but return as a string
                    return objectMapper.writeValueAsString(raw);
                }
                catch (JsonProcessingException e) {
                    throw new GuacamoleException("Error json parsing returned secret: ", e);
                }
            }
        }

        VaultKeyValueOperations kvOperations;
        if ("kv_2".equals(type)) {
            kvOperations = vaultTemplate.opsForKeyValue(
                        mountPath,
                        VaultKeyValueOperations.KeyValueBackend.KV_2);
        }
        else {
            kvOperations = vaultTemplate.opsForKeyValue(
                        mountPath,
                        VaultKeyValueOperations.KeyValueBackend.KV_1);
        }

        // Get the values on the path and cache them
        VaultResponse response = kvOperations.get(path);

        if (response == null || response.getData() == null)
        {
            throw new GuacamoleServerException(
                    "Value not found in OpenBao for path: " + mountPath + "/" + path);
        }
        cache.put(mountPath + "/" + path, response.getData());

        Object raw = response.getData().get(secret);
        if (raw instanceof String || raw instanceof Number || raw instanceof Boolean) {
            return String.valueOf(raw);
        }
        else {
            try {
                // Stored JSON value.. Probably not usable, but return as a string
                return objectMapper.writeValueAsString(raw);
            }
            catch (JsonProcessingException e) {
                throw new GuacamoleException("Error json parsing returned secret: ", e);
            }
        }
    }

    /**
     * Retrieves a an ssh one-time password or signed SSH certificate
     *
     * @param mountPath
     *     The mountPath of the SSH secret engine on the vault server.
     *
     * @param path
     *     The path of the secret record representing the SSH role
     *
     * @param secret
     *     The secret value to return
     *
     * @param config
     *     A GuacamoleConfiguration containing the connection configuration
     *     parameters.
     *
     * @return
     *     The value associated with the secret.
     *
     * @throws GuacamoleException
     *     If the secret cannot be retrieved from OpenBao.
     */
    private String getValueSSH(String mountPath, String path, String secret, GuacamoleConfiguration config) throws GuacamoleException {

        // Is the key-value already in the cache. The SSH connection is for a specific
        // user, so add username in the cache path
        Map<String, Object> cacheResponse;
        String username = config.getParameter("username");
        if (path.startsWith("sign/")) {
            if (username == null || username.isEmpty()) {
                throw new GuacamoleException("The username can not be empty for SSH signed certificates");
            }
            cacheResponse = (Map<String, Object>) cache.getIfPresent(username + "-" + mountPath + path);
        }
        else if (path.startsWith("creds/")) {
            cacheResponse = (Map<String, Object>) cache.getIfPresent(mountPath + path);
        }
        else {
           throw new GuacamoleException("Unknown SSH type on path: " + mountPath + path);
        }

        if (cacheResponse != null) {
            logger.info("Cached Response");
            String retval;
            // The path is already cached?. Use it
            if (cacheResponse.get(secret) instanceof String) {
                retval = (String) cacheResponse.get(secret);
            }
            else {
                try {
                    // Stored JSON value.. Probably not usable, but return as a string
                    retval = objectMapper.writeValueAsString(cacheResponse.get(secret));
                }
                catch (JsonProcessingException e) {
                    throw new GuacamoleException("Error json parsing returned secret: ", e);
                }
            }

            if (retval == null || retval.isEmpty()) {
                throw new GuacamoleException("SSH secret '" + secret + "' not found on the path : " + mountPath +"/" + path);
            }
            // One-time password so needs to be cleared after first use
            if (path.startsWith("creds/")) {
                cache.invalidate(mountPath + path);
            }

            return retval;
        }

        // Detect if wanting one-time password or signed certificate
        if (path.startsWith("creds/")) {
            VaultResponse response =
                vaultTemplate.write(mountPath +  path, Map.of("ip", "0.0.0.0"));

            if (response == null || response.getData() == null) {
                throw new GuacamoleException("No response from Vault SSH engine");
            }

            String password = (String) response.getData().get("key");
            String user = (String) response.getData().get("username");

            if (password == null || user == null) {
                throw new GuacamoleException("Invalid SSH OTP response");
            }

            // Cache the retrieved user and otp
            cache.put(mountPath + path, Map.of("username", user, "password", password));

            if (secret.equals("username")) {
                return user;
            }
            else if (secret.equals("password")) {
                return password;
            }
        }
        else if (path.startsWith("sign/")) {

            OpenBaoSshKeys sshKeys = new OpenBaoSshKeys(configService.getSshType());
            Map<String, Object> request = Map.of(
                    "public_key", sshKeys.publicSsh,
                    "valid_principals", username,
                    "extensions", Map.of("permit-pty", ""),
                    "ttl", configService.getSshConnectionTimeout());

            VaultResponse vaultResponse =
                    vaultTemplate.write(mountPath + path, request);

            if (vaultResponse == null || vaultResponse.getData() == null) {
                throw new GuacamoleException("No response from Vault SSH engine");
            }

            String signedCert = (String) vaultResponse.getData().get("signed_key");
            logger.info("SSH Cert response keys: {}", vaultResponse.getData().keySet());

            if (signedCert == null) {
                throw new GuacamoleException("Vault did not return a signed SSH certificate");
            }

            cache.put(username + "-" + mountPath + path, Map.of(
                    "private", sshKeys.privateSshPem,
                    "public", signedCert));

            if (secret.equals("private")) {
                return sshKeys.privateSshPem;
            }
            else if (secret.equals("public")) {
                return signedCert;            }
        }

        throw new GuacamoleException("SSH secret '" + secret + "' not found on the path : " + mountPath + path);
    }

    /**
     * Retrieves a username or password from an LDAP secret engine. The type of
     * account supported might be static, dynamic or service accounts
     *
     * @param mountPath
     *     The mountPath of the LDAP secret engine on the vault server.
     *
     * @param path
     *     The path of the secret record representing the LDAP role
     *
     * @param secret
     *     The secret value to return
     *
     * @return
     *     The value associated with the secret.
     *
     * @throws GuacamoleException
     *     If the secret cannot be retrieved from OpenBao.
     */
    private String getValueLDAP(String mountPath, String path, String secret) throws GuacamoleException {
        if (!secret.equals("username") && ! secret.equals("password")) {
            throw new GuacamoleException("LDAP secret '" + secret + "' not found on the path : " + mountPath + path);
        }

        // Is the value already in the cache
        // FIXME Need a unique ConnectID while caching for dynamic at service roles as token same
        // are the same for different credentials
        Map<String, Object> cacheResponse =  (Map<String, Object>) cache.getIfPresent(mountPath + path);

        if (cacheResponse != null) {
            logger.info("Cached Response");
            String retval;
            // The path is already cached?. Use it
            if (cacheResponse.get(secret) instanceof String) {
                retval = (String) cacheResponse.get(secret);
            }
            else {
                try {
                    // Stored JSON value.. Probably not usable, but return as a string
                    return objectMapper.writeValueAsString(cacheResponse.get(secret));
                }
                catch (JsonProcessingException e) {
                    throw new GuacamoleException("Error json parsing returned secret: ", e);
                }
            }

            if (retval == null || retval.isEmpty()) {
                throw new GuacamoleException("LDAP secret '" + secret + "' not found on the path : " + mountPath + path);
            }
            return retval;
        }

        VaultResponse response;
        if (path.startsWith("static") || path.startsWith("creds/")) {
            response = vaultTemplate.read(mountPath + path);
        }
        else if (path.startsWith("library/")) {
            response = vaultTemplate.write(mountPath + path + "/check-out", Map.of("ttl", "2h"));
        }
        else {
           throw new GuacamoleException("Unknown LDAP type on path: " + mountPath +"/" + path);
        }

        if (response == null || response.getData() == null) {
            throw new GuacamoleException("No response from LDAP secrets engine");
        }

        String username;
        String password = (String) response.getData().get("password");
        if (path.startsWith("library/")) {
            username = (String) response.getData().get("service_account_name");
        }
        else {
            username = (String) response.getData().get("username");
        }

        if (username == null || password == null) {
            // Particularly for service accounts this might happen if there is
            // no account available for checkout
            throw new IllegalStateException("Invalid LDAP static credential response");
        }

        if (path.startsWith("library/")) {
            // Register a listener to check-in the account for a TunnelClose Event
            LDAPSessionInfo info = new LDAPSessionInfo(mountPath + path + "/check-in", username, Instant.now());
            ldapSessions.put("creating", info);
        }

        // Cache the retrieved user and password
        cache.put(mountPath + path, Map.of("username", username, "password", password));
        logger.info("LDAP :  " + username + " : " + password);

        if (secret.equals("username")) {
            return username;
        }
        return password;
    }

    /**
     * Retrieves a username or password from a Databse secret engine.
     *
     * @param mountPath
     *     The mountPath of the database secret engine on the vault server.
     *
     * @param path
     *     The path of the secret record representing the LDAP role
     *
     * @param secret
     *     The secret value to return
     *
     * @return
     *     The value associated with the secret.
     *
     * @throws GuacamoleException
     *     If the secret cannot be retrieved from OpenBao.
     */
    private String getValueDB(String mountPath, String path, String secret) throws GuacamoleException {
        if (!secret.equals("username") && !secret.equals("password")) {
            throw new GuacamoleException("Database secret '" + secret + "' not found on the path : " + mountPath +"/" + path);
        }

        // Is the key-value already in the cache
        Map<String, Object> cacheResponse = (Map<String, Object>) cache.getIfPresent(mountPath + path);

        if (cacheResponse != null) {
            String retval;
            // The path is already cached?. Use it
            if (cacheResponse.get(secret) instanceof String) {
                retval = (String) cacheResponse.get(secret);
            }
            else {
                try {
                    // Stored JSON value.. Probably not usable, but return as a string
                    return objectMapper.writeValueAsString(cacheResponse.get(secret));
                }
                catch (JsonProcessingException e) {
                    throw new GuacamoleException("Error json parsing returned secret: ", e);
                }
            }

            if (retval == null || retval.isEmpty()) {
                throw new GuacamoleException("Data secret '" + secret + "' not found on the path : " + mountPath +"/" + path);
            }
            return retval;
        }

        VaultResponse response = vaultTemplate.read(mountPath + "/creds/" + path);

        if (response == null || response.getData() == null) {
            throw new GuacamoleException("No response from Databse secrets engine");
        }

        String username = (String) response.getData().get("username");
        String password = (String) response.getData().get("password");

        if (username == null || password == null) {
            throw new IllegalStateException("Invalid Database credential response");
        }

        // Cache the retrieved user and password
        cache.put(mountPath + path, Map.of("username", username, "password", password));
        logger.debug("DB :  " + username + " : " + password);

        if (secret.equals("username")) {
            return username;
        }
        return password;
    }

    /**
     * Release the automatic renewal of the tokens on shutdown
     */
    public void shutdown() {
        this.sessionManager.close();
    }
}
