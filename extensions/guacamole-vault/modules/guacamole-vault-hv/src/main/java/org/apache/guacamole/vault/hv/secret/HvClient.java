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

package org.apache.guacamole.vault.hv.secret;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.vault.hv.conf.HvConfigurationService.VaultInfo;
import org.apache.guacamole.vault.hv.vault.FileTokenAuthentication;
import org.apache.guacamole.vault.hv.vault.UsernamePasswordAuthentication;
import org.apache.guacamole.vault.hv.vault.UsernamePasswordAuthenticationOptions;
import org.apache.guacamole.vault.hv.vault.TtlAwareSessionManager;
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

/**
 * Client which retrieves records from Hashicorp Vault.
 */
public class HvClient {

    /**
     * Name of the Guacamole token to resolve on a Hashicorp Vault (secret path
     * is set in the token modifier).
     */
    public static final String VAULT_TOKEN_PREFIX = "vault://";

    /**
     * The path prefix for the path-help REST API in the Vault
     */
    private static final String VAULT_PATH_HELP = "/sys/internal/ui/mounts/";

    /**
     * Name temporary entry in the ldap sessions for session being constructed
     */
    public static final String VAULT_LDAP_SESSION = "checkin";

    /**
     * Map of valid query parameter keys and valid values of these keys in vault
     * token query parameters
     */
    public static final Map<String, String[]> VAULT_QUERY_PARAMS = Map.of(
            HvSshKeys.VAULT_SSH_KEY_TYPE, new String[] {HvSshKeys.RSA, HvSshKeys.EC256, HvSshKeys.ED25519},
            HvSshKeys.VAULT_SSH_KEY_BITS, new String[] {"256", "384", "521", "2048", "4096"});

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(HvClient.class);

    /**
     * ObjectMapper for this class.
     */
    private final ObjectMapper objectMapper;

    /**
     * All in-flight HTTP requests to Vault, it prevents multiple queries
     * on the same path.
     */
    private final ConcurrentMap<String, CompletableFuture<JsonNode>> inFlightRequests = new ConcurrentHashMap<>();

    /**
     * Cache of secrets recently fetched
     */
    private final Cache<String, JsonNode> cache;

    /**
     * Vault template that will be used with all of the mount paths
     */
    private final VaultTemplate vaultTemplate;

    /**
     * A HashMap of the checked out LDAP Sessions
     */
    private final Map<String, LDAPSessionInfo> ldapSessions = new ConcurrentHashMap<>();

    /**
     * The HV configuration associated with this client instance.
     */
    private final VaultInfo vaultInfo;

    /**
     * Create a new HV client based around the provided HV configuration and
     * API timeout setting.
     *
     * @param vaultInfo
     *     The HV configuration to use when retrieving properties from HV.
     */
    @AssistedInject
    public HvClient(@Assisted final VaultInfo vaultInfo) throws GuacamoleException {
        this.vaultInfo = vaultInfo;
        this.objectMapper = new ObjectMapper();

        final VaultEndpoint endpoint = VaultEndpoint.from(vaultInfo.getVaultUri().resolve("v1"));

        final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(vaultInfo.getConnectionTimeout());
        requestFactory.setReadTimeout(vaultInfo.getRequestTimeout());

        final RestTemplate restTemplate = new RestTemplate(requestFactory);
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(
            vaultInfo.getVaultUri().resolve("v1").toString()));
        final ClientAuthentication authentication;

        if (vaultInfo.getVaultToken() != null) {
            if (isTokenReadableFile(vaultInfo.getVaultToken())) {
                authentication =
                        new FileTokenAuthentication(vaultInfo.getVaultToken());
            }
            else {
                authentication = new TokenAuthentication(vaultInfo.getVaultToken());
            }
        }
        else if (vaultInfo.getVaultUsername() != null && vaultInfo.getVaultPassword() != null) {
            final UsernamePasswordAuthenticationOptions options =
                UsernamePasswordAuthenticationOptions.builder()
                    .username(vaultInfo.getVaultUsername())
                    .password(vaultInfo.getVaultPassword())
                    .build();

            authentication =
                new UsernamePasswordAuthentication(options, endpoint, restTemplate);
        }
        else {
            logger.error("Either a vault token or Username/Password must be supplied");
            authentication = new TokenAuthentication("s.ThisTokenWontWork");
        }

        // Create a task scheduler for our token renewal
        final ThreadPoolTaskScheduler taskscheduler = new ThreadPoolTaskScheduler();
        taskscheduler.setPoolSize(1);
        taskscheduler.setThreadNamePrefix("vault-renewal-");
        taskscheduler.initialize();

        // Session manager to automatically renew tokens before expiration
        final TtlAwareSessionManager sessionManager = new TtlAwareSessionManager(authentication,
                restTemplate, taskscheduler, vaultInfo.getTokenRenewalDelay());

        this.vaultTemplate = new VaultTemplate(endpoint, requestFactory, sessionManager);


        // Initialize the cache with maximum size of 1MB, and cache expiry with
        // forced cleanup
        // FIXME I'd really like to do something like "Array.fill(v, '\0');" in
        // the removal listener to ensure that passwords are no longer in memory.
        // However, both spring-core-vault and Guacamole store these values
        // elsewhere as immutable String values, So even if I stored them in the
        // cache as char[] copies of the password would be elsewhere as String
        // values in memory.. A VaultConverter function could deal with the
        // spring-vault-core part of the problem, but not Gaucamole.
        logger.debug("Initialize Cache with expiry of {}, {} seconds",
                vaultInfo.getVaultCacheLifetime(),
                Duration.ofMillis(vaultInfo.getVaultCacheLifetime()).toSeconds());
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMillis(vaultInfo.getVaultCacheLifetime()))
                .maximumSize(1_000_000)
                .build();
    }

    /**
     * Function to detect if the the token is in fact a readable file
     * rather than a token string
     *
     * @param token
     *     The string with the token returned from configService
     *
     * @return
     *      True is the token is a readable file
     */
     private static boolean isTokenReadableFile(final String token) {
         try {
             final Path path = Paths.get(token);
             return Files.isRegularFile(path) && Files.isReadable(path);
        }
        catch (SecurityException e) {
            // File not readbale, permission denied
            return false;
        }
     }

    /**
     * Return the secret engine type and mount path using the internal Vault
     * path-help functionality, no need for access to /sys/mounts which might
     * be a security risk
     *
     * @param path
     *     The path to test for the secret engine type
     *
     * @return
     *     A Map with the secret engine type and its mount path
     */
    public JsonNode getSecretsEngine(final String path) {
        final JsonNode cacheResponse = cache.getIfPresent(VAULT_PATH_HELP + path);
        if (cacheResponse != null) {
            return cacheResponse;
        }

        final VaultResponse response = vaultTemplate.read(VAULT_PATH_HELP + path);
        final Map<String, Object> data = response.getData();
        String type = String.valueOf(data.get("type"));

        if ("kv".equals(type)) {
            final String version;
            if (data.get("options") instanceof Map<?, ?>) {
                final Map<?,?> options = (Map<?,?>) data.get("options");        
                version = String.valueOf(options.get("version"));
            }
            else {
                version = null;
            }
            type = "2".equals(version) ? "kv_2" : "kv_1"; 
        }

        final JsonNode map = objectMapper.valueToTree(Map.of("type", type,
                "path", String.valueOf(data.get("path"))));
        cache.put(VAULT_PATH_HELP + path, map);

        return map;
    }

    /**
     * Contains information about the checked out LDAP sessions
     */
    private static final class LDAPSessionInfo {
        /** Stores the check-in path of an active ldap-session */
        private final String checkInPath;
        /** Stores the service account username of the checked out account */
        private final String username;
        /** Is true if the TunnelConnectEvent has been detected */
        private final boolean initialized;
        /** The date the account was checked-out, allowing automatic check-in after 2h */
        private final Instant created;

        private LDAPSessionInfo(final String checkInPath, final String username) {
            this.checkInPath = checkInPath;
            this.username = username;
            this.initialized = false;
            this.created = Instant.now();
        }

        private LDAPSessionInfo(final String checkInPath, final String username, final boolean initialized) {
            this.checkInPath = checkInPath;
            this.username = username;
            this.initialized = initialized;
            this.created = Instant.now();
        }
        
        private String getCheckInPath() { return checkInPath; }
        private String getUsername() { return username; }
        private boolean isInitialized() { return initialized; }
        private Instant getCreated() { return created; }
    }

    /**
     * The LDAP session interface checks out sessions that then can not be
     * used till they are checked in. In getTokens we have the problem that
     * we don't have access to the tunnel ID and so can't identify it.
     * Guacamole is also not guarenteed to generate a TunnelCloseEvent.
     *
     * So this function is fragile and relies on the fact that the TunnelConnectEvent
     * will be running a few tens of milliseconds after the getTokens command to
     * limit the risk of confusing two connections. There is still a small risk
     * of error here.
     *
     * @param tunnelId
     *      The id of the tunnel of the ldap session. In a TunnelConnectEvent
     *      this is the temporary id used when the session was checked out
     *
     * @param tunnelIdNew
     *      The tunnel ID to store if we are treating a TunnelConnectEvent
     *
     * @return
     *      Returns true if our client treats this element
     */
    public Boolean treatLdapSession(final String tunnelId, @Nullable final String tunnelIdNew) {
        final LDAPSessionInfo session = ldapSessions.get(tunnelId);
        if (session != null) {
            if (tunnelIdNew != null) {
                logger.debug("Storing connection ID: {}", tunnelIdNew);
                ldapSessions.put(tunnelIdNew, new LDAPSessionInfo(session.getCheckInPath(), session.getUsername(), true));
            }
            else if (session.isInitialized()) {
                // FIXME : We don't always receive the TunnelCloseEvent
                // So this checkin function only kinda works
                logger.debug("Removing stored LDAP session: {}", tunnelId);
                vaultTemplate.write(session.getCheckInPath(), Map.of("service_account_names", session.getUsername()));
            }
            ldapSessions.remove(tunnelId);

            // Do some clean up of the active LDAP sessions. If a session hasn't been
            // checked in after 2 hours, just drop it from the hashMap. As the TTL of
            // the vault is already 2 hours don't need to check it in. Don't really
            // care if the value hangs around in our hashmap so don't need a dedicated
            // task for this
            ldapSessions.entrySet().removeIf(e -> Instant.now().isAfter(e.getValue().getCreated().plusSeconds(7200)));

            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Parse a string of query parameters and return a Map of the key/value of
     * the parameters for the valid query parameters
     *
     * @param query
     *     The query string extracted from the end of the token
     *
     * @return
     *     A Map the the query parameters extracted
     */
    private Map<String, String> parseQueryParam(final String query) {
        Map<String, String> queryMap = new ConcurrentHashMap<>();
        if (query != null && !query.isBlank()) {
            for (String pair : query.split("&")) {
                final int idx = pair.indexOf("=");
                final String key = idx > 0 ? pair.substring(0, idx) : pair;
                final String val = idx > 0 && pair.length() > idx + 1 ? pair.substring(idx +1) : null;

                VAULT_QUERY_PARAMS.forEach((k, v) -> {
                    if (val != null && k.equals(key) && Arrays.asList(v).contains(val))
                        queryMap.put(key, val);
                });
            }
        }
        return queryMap;
    }

    /**
     * Returns the value of the secret stored within OpenBao/Hashicorp Vault.
     *
     * @param notation
     *     The HV notation of the secret to retrieve.
     *
     * @param username
     *     The connection username, that must be non null for SSH certificate
     *     generation.
     *
     * @param key
     *     A pseudo-unique key to use to store cached secrets, to keep secrets
     *     associated with the same connection together, even if the vault token
     *     itself is not unique.
     *
     * @return
     *     A Future which completes with the value of the secret represented by
     *     the given HV notation, or null if there is no such secret.
     *
     * @throws GuacamoleException
     *     If the requested secret cannot be retrieved or the HV notation
     *     is invalid.
     */
    public CompletableFuture<String> getSecret(final String notation, final String username, final String key) throws GuacamoleException {

        // If it's not an HV token, fail
        if (!notation.startsWith(VAULT_TOKEN_PREFIX)) {
            throw new GuacamoleException("Invalid token Vault notation: " + notation);
        }

        /*
         * vault://path/to/secret?k=v    <-- the Guacamole token name and its modifier
         *         ^^^^^^^               <-- this is the path
         *                 ^^^^^^        <-- this is the secret (or key in HV terms)
         *                       ^^^^    <-- These are the query parameters
         */
        int lastSlashIndex = notation.lastIndexOf('/');
        if (lastSlashIndex == -1) {
            lastSlashIndex = VAULT_TOKEN_PREFIX.length();
        }
        int lastQueryIndex = notation.lastIndexOf('?');
        final String query;
        if (lastQueryIndex == -1 || lastQueryIndex < lastSlashIndex) {
            lastQueryIndex = notation.length();
            query = "";
        }
        else {
            query = notation.substring(lastQueryIndex + 1);
        }
        final Map<String, String> queryMap = parseQueryParam(query);
        final String path = notation.substring(VAULT_TOKEN_PREFIX.length(), lastSlashIndex);
        final String secret = notation.substring(lastSlashIndex + 1, lastQueryIndex);

        final JsonNode cachedSecrets = cache.getIfPresent(key);

        if (cachedSecrets != null) {
            logger.debug("Using cached data for token : {}", notation);
            final JsonNode secretNode = cachedSecrets.get(secret);
            if (secretNode == null) {
                logger.warn("Could not find {}/{}", path, secret);
                return CompletableFuture.completedFuture("");
            }
            return CompletableFuture.completedFuture(secretNode.asText());
        }
        
        long cacheLifetimeTmp;
        try {
            cacheLifetimeTmp = (long) vaultInfo.getVaultCacheLifetime();
        }
        catch (GuacamoleException e) {
            cacheLifetimeTmp = 5000L;
        }
        final long cacheLifetime = cacheLifetimeTmp;

        // Cache miss, either get an existing in-flight request or create a new one
        final CompletableFuture<JsonNode> futureResponse = inFlightRequests.computeIfAbsent(key, k -> {
            return CompletableFuture.supplyAsync(() -> {
                final String type = getSecretsEngine(path).get("type").asText();
                final String mountPath = getSecretsEngine(path).get("path").asText();
                final String newpath = path.substring(mountPath.length());
                logger.debug("Vault {}, {}, {}, {}, {}", type, mountPath, path, secret, queryMap.keySet());

                final JsonNode jsonNode;
                switch (type) {
                    case "ssh":
                        // Only the SSH engine takes query arguments at the moment
                        jsonNode = getValueSSH(mountPath, newpath, username, queryMap);
                        break;
                    case "ldap":
                        jsonNode = getValueLDAP(mountPath, newpath);
                        break;
                    case "database":
                        jsonNode = getValueDB(mountPath, newpath);
                        break;
                    case "kv_1":
                        jsonNode = getValueKV(mountPath, newpath, VaultKeyValueOperations.KeyValueBackend.KV_1);
                        break;
                    case "kv_2":
                        jsonNode = getValueKV(mountPath, newpath, VaultKeyValueOperations.KeyValueBackend.KV_2);
                        break;
                    default:
                       throw new IllegalArgumentException("Unknown secret engine for the token: '" + type +"'");
                }
                return jsonNode;
            })
            .orTimeout(cacheLifetime, TimeUnit.MILLISECONDS);
        });

        return futureResponse.whenComplete((jsonNode, ex) -> {
            // Put newly received JSON data into the cache
            if (ex == null && jsonNode != null) {
                logger.debug("Putting data to cache : {}", notation);
                cache.put(key, jsonNode);
            }

            // Now that the cache is filled, this in-flight request is obsolete and 
            // must be removed
            inFlightRequests.remove(key);

        }).thenApply(jsonNode -> {
            /*
             * Extract and return the secret
             */
            final JsonNode secretNode = jsonNode.get(secret);
            if (secretNode == null) {
                logger.warn("Could not find {}/{}", path, secret);
                return "";
            }
            return secretNode.asText();
        }).exceptionally(e -> {
            // Make sure that the exception is a GuacamoleException
            final Throwable cause = e.getCause();
            final String errorMessage = (cause != null) ? cause.getMessage() : "Unknown error";
            logger.warn("Vault query failed for {} with {}", path, errorMessage);

            if (cause instanceof GuacamoleException) {
                throw new CompletionException(cause);
            }

            throw new CompletionException(
                new GuacamoleException("Vault query failed for " + path + ": " + errorMessage, cause));
        });
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
     * @param type
     *     The type of key-value store. Either "kv_1" or "kv_2"
     *
     * @return
     *     The values associated with the path.
     *
     * @throws VaultException
     *     If the secrets cannot be retrieved from the Vault.
     */
    private JsonNode getValueKV(final String mountPath, final String path, final VaultKeyValueOperations.KeyValueBackend type) throws VaultException {
        final VaultKeyValueOperations kvOperations = vaultTemplate.opsForKeyValue(mountPath, type);

        // Get the values on the path and cache them
        final VaultResponse response = kvOperations.get(path);

        if (response == null || response.getData() == null)
        {
            throw new VaultException("Value not found in Vault for path: " + path);
        }

        return objectMapper.valueToTree(response.getData());
    }

    /**
     * Retrieves an ssh one-time password or signed SSH certificate
     *
     * @param mountPath
     *     The mountPath of the SSH secret engine on the vault server.
     *
     * @param path
     *     The path of the secret record representing the SSH role
     *
     * @param username
     *     The connection username that must be non null for SSH certificate
     *     generation.
     *
     * @return
     *     The values associated with the path.
     *
     * @throws VaultException
     *     If the secrets cannot be retrieved from the Vault.
     */
    private JsonNode getValueSSH(final String mountPath, final String path,
            final String username, final Map<String, String> queryMap) throws VaultException {
        // SSH One-time passwords
        if (path.startsWith("creds/")) {

            final VaultResponse response =
                vaultTemplate.write(mountPath + path, Map.of("ip", "0.0.0.0"));

            if (response == null || response.getData() == null) {
                throw new VaultException("No response from Vault SSH engine");
            }
            final Map<String, Object> retval = response.getData();
            retval.put("password", retval.get("key"));

            return objectMapper.valueToTree(retval);
        }

        if (!path.startsWith("sign/") && !path.startsWith("issue/")) {
            throw new VaultException("Unknown SSH type on path: " + mountPath + path);
        }

        if (username == null || username.isEmpty()) {
            throw new VaultException("The username can not be empty for SSH signed certificates");
        }

        final HvSshKeys sshKeys;
        final Map<String, Object> request;
        try {
            final String sshType = queryMap.getOrDefault(HvSshKeys.VAULT_SSH_KEY_TYPE, vaultInfo.getSshType());

            if (path.startsWith("sign/")) {
                sshKeys = new HvSshKeys(sshType);
                request = Map.of(
                        "public_key", sshKeys.getPublic(),
                        "valid_principals", username,
                        "extensions", Map.of("permit-pty", ""),
                        "ttl", vaultInfo.getSshConnectionTimeout());
            }
            else {
                final String keyBits = queryMap.getOrDefault(HvSshKeys.VAULT_SSH_KEY_BITS,
                        (sshType == HvSshKeys.EC256 ? "256" : "4096"));
                sshKeys = null;
                request = Map.of(
                        "valid_principals", username,
                        "extensions", Map.of("permit-pty", ""),
                        "key_type", sshType,
                        "key_bits", keyBits,
                        "ttl", vaultInfo.getSshConnectionTimeout());
            }
        }
        catch (GuacamoleException e) {
            throw new VaultException("Error reading Vault configuration : " + e.getMessage());
        }

        final VaultResponse vaultResponse = vaultTemplate.write(mountPath + path, request);

        if (vaultResponse == null || vaultResponse.getData() == null) {
            throw new VaultException("No response from Vault SSH engine");
        }

        final Map<String, Object> data = vaultResponse.getData();
        final String signedKey = (String) data.get("signed_key");
        final String privateKey = (path.startsWith("sign/") ? sshKeys.getPrivate() : (String) data.get("private_key"));

        if (signedKey == null || privateKey == null) {
            throw new VaultException("Vault did not return a signed SSH certificate");
        }

        if (path.startsWith("sign/")) {
            return objectMapper.valueToTree(Map.of("private", privateKey,
                    "private_key", privateKey,
                    "public", signedKey,
                    "signed_key", signedKey,
                    "unsigned", sshKeys.getPublic()));
        }
        else {
            return objectMapper.valueToTree(Map.of("private", privateKey,
                    "private_key", privateKey,
                    "public", signedKey,
                    "signed_key", signedKey));
        }
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
     * @return
     *     The values associated with the path.
     *
     * @throws VaultException
     *     If the secrets cannot be retrieved from the Vault.
     */
    private JsonNode getValueLDAP(final String mountPath, final String path) throws VaultException {
        final VaultResponse response;
        if (path.startsWith("static") || path.startsWith("creds/")) {
            response = vaultTemplate.read(mountPath + path);
        }
        else if (path.startsWith("library/")) {
            response = vaultTemplate.write(mountPath + path + "/check-out", Map.of("ttl", "2h"));
        }
        else {
           throw new VaultException("Unknown LDAP type on path: " + mountPath + path);
        }

        if (response == null || response.getData() == null) {
            throw new VaultException("No response from LDAP secrets engine");
        }

        final Map<String, Object> retval = response.getData();
        if (path.startsWith("library/")) {
            final String username = String.valueOf(retval.get("service_account_name"));
            retval.put("username", username);

            // Register a listener to check-in the account for a TunnelClose Event
            logger.info("Caching session : {}", username);
            final LDAPSessionInfo info = new LDAPSessionInfo(mountPath + path + "/check-in", username);
            ldapSessions.put(VAULT_LDAP_SESSION, info);
        }

        return objectMapper.valueToTree(retval);
    }

    /**
     * Retrieves a username or password from a Database secret engine.
     * Before Hashicorp version 1.10 the data could only have username/password
     * After there can be additional static preconfigured fields that the vault
     * tokens might access.
     *
     * @param mountPath
     *     The mountPath of the database secret engine on the vault server.
     *
     * @param path
     *     The path of the secret record representing the LDAP role
     *
     * @return
     *     The values associated with the path.
     *
     * @throws VaultException
     *     If the secrets cannot be retrieved from the Vault.
     */
    private JsonNode getValueDB(final String mountPath, final String path) throws VaultException {
        final VaultResponse response = vaultTemplate.read(mountPath + path);

        if (response == null || response.getData() == null) {
            throw new VaultException("No response from Database secrets engine");
        }

        return objectMapper.valueToTree(response.getData());
    }
}
