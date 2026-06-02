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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
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
    static final String VAULT_TOKEN_PREFIX = "vault://";

    /**
     * The path prefix for the path-help REST API in the Vault
     */
    static final String VAULT_PATH_HELP = "/sys/internal/ui/mounts/";

    /**
     * Name temporary entry in the ldap sessions for session being cosntructed
     */
    static final String VAULT_LDAP_SESSION = "checkin";

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
    private final Map<String, LDAPSessionInfo> ldapSessions = new HashMap<>();

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
    public HvClient(@Assisted VaultInfo vaultInfo) {
        this.vaultInfo = vaultInfo;
        this.objectMapper = new ObjectMapper();

        VaultEndpoint endpoint = VaultEndpoint.from(vaultInfo.Uri.resolve("v1"));

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(vaultInfo.ConnectionTimeout);
        requestFactory.setReadTimeout(vaultInfo.RequestTimeout);

        RestTemplate restTemplate = new RestTemplate(requestFactory);
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(
            vaultInfo.Uri.resolve("v1").toString()));
        ClientAuthentication authentication;

        if (vaultInfo.Token != null) {
            if (isTokenReadableFile(vaultInfo.Token)) {
                authentication =
                        new FileTokenAuthentication(vaultInfo.Token);
            }
            else {
                authentication = new TokenAuthentication(vaultInfo.Token);
            }
        }
        else if (vaultInfo.Username != null && vaultInfo.Password != null) {
            UsernamePasswordAuthenticationOptions options =
                UsernamePasswordAuthenticationOptions.builder()
                    .username(vaultInfo.Username)
                    .password(vaultInfo.Password)
                    .build();

            authentication =
                new UsernamePasswordAuthentication(options, endpoint, restTemplate);
        }
        else {
            logger.error("Either a vault token or Username/Password must be supplied");
            authentication = new TokenAuthentication("s.ThisTokenWontWork");
        }

        // Create a task scheduler for our token renewal
        ThreadPoolTaskScheduler taskscheduler = new ThreadPoolTaskScheduler();
        taskscheduler.setPoolSize(1);
        taskscheduler.setThreadNamePrefix("vault-renewal-");
        taskscheduler.initialize();

        // Session manager to automatically renew tokens before expiration
        TtlAwareSessionManager sessionManager = new TtlAwareSessionManager(authentication,
                restTemplate, taskscheduler, vaultInfo.TokenRenewalDelay);

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
                vaultInfo.CacheLifetime, Duration.ofMillis(vaultInfo.CacheLifetime).toSeconds());
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMillis(vaultInfo.CacheLifetime))
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
    public JsonNode getSecretsEngine(String path) {
        JsonNode cacheResponse = cache.getIfPresent(VAULT_PATH_HELP + path);
        if (cacheResponse != null) {
            return cacheResponse;
        }

        VaultResponse response = vaultTemplate.read(VAULT_PATH_HELP + path);
        Map<String, Object> data = response.getData();
        String type = String.valueOf(data.get("type"));

        if (type.equals("kv")) {
            // Need to detect if type 1 or type 2 Key/Value engine
            if (data.get("options") instanceof Map<?, ?>) {
                Map<?,?> options = (Map<?,?>) data.get("options");
                if ("2".equals(String.valueOf(options.get("version")))) {
                    type = "kv_2";
                }
                else {
                    type = "kv_1";
                }
            }
            else {
                // No options, assume kv_1
                type = "kv_1";
            }
        }
        JsonNode map = objectMapper.valueToTree(Map.of("type", type,
                "path", String.valueOf(data.get("path"))));
        cache.put(VAULT_PATH_HELP + path, map);

        return map;
    }

    /**
     * Contains information about the checked out LDAP sessions
     */
    private static class LDAPSessionInfo {
        final String checkInPath;
        final String username;
        final Boolean initialized;
        final Instant created;

        public LDAPSessionInfo(String checkInPath, String username) {
            this.checkInPath = checkInPath;
            this.username = username;
            this.initialized = false;
            this.created = Instant.now();
        }

        public LDAPSessionInfo(String checkInPath, String username, Boolean initialized) {
            this.checkInPath = checkInPath;
            this.username = username;
            this.initialized = initialized;
            this.created = Instant.now();
        }
    }

    /**
     * The LDAP session interface checks out session that then can not be
     * used till they are checked in. In getTokens we have the problem that
     * we don't have access to the tunnel ID and so can't identify it.
     * Guacamole is also not guarenteed to generate a TunnelCloseEvent.
     *
     * So this function is fragile and relies on the fact that the TunnelConnectEvent
     * will be running a few tens of milliseconds after the getTokens command to
     * limit the risk of confusing two connection. There is still a small risk
     * of error here.
     *
     * @param id
     *      The id of the tunnel ldap session
     *
     * @param tunnelId
     *      The tunnel ID to store if we are treating a TunnelConnectEvent
     *
     * @return
     *      Returns true if our client treats this element
     */
    public Boolean treatLdapSession(String id, @Nullable String tunnelId) {
        LDAPSessionInfo session = ldapSessions.get(id);
        if (session != null) {
            if (tunnelId != null) {
                logger.debug("Storing connection ID: {}", tunnelId);
                ldapSessions.put(tunnelId, new LDAPSessionInfo(session.checkInPath, session.username, true));
            }
            else if (session.initialized) {
                // FIXME : We don't always receive the TunnelCloseEvent
                // So this checkin function only kinda works
                logger.debug("Removing stored LDAP session: {}", id);
                vaultTemplate.write(session.checkInPath, Map.of("service_account_names", session.username));
            }
            ldapSessions.remove(id);

            // Do some clean up of the active LDAP sessions. If a session hasn't been
            // checked in after 2 hours, just drop it from the hashMap. As the TTL of
            // the vault is already 2 hours don't need to check it in. Don't really
            // care if the value hang around in our hashmap so don't need a dedicated
            // task for this
            ldapSessions.entrySet().removeIf(e -> Instant.now().isAfter(e.getValue().created.plusSeconds(7200)));

            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Returns the value of the secret stored within Hashicorp Vault.
     *
     * @param notation
     *     The HV notation of the secret to retrieve.
     *
     * @param username
     *     The connection username, that must be non null for SSH certificate
     *     generation.
     *
     * @param key
     *     A pseudo-unique key to use to stored cached secrets, to keep secrets
     *     associated with the same connection together, even if they vault token
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
    public CompletableFuture<String> getSecret(String notation, String username, String key) throws GuacamoleException {

        // If it's not an HV token, fail
        if (!notation.startsWith(VAULT_TOKEN_PREFIX))
            throw new GuacamoleException("Invalid token Vault notation: " + notation);

        /*
         * vault://path/to/secret  <-- the Guacamole token name and its modifier
         *            ^^^^^^^         <-- this is the path
         *                    ^^^^^^  <-- this is the secret (or key in HV terms)
         */
        int lastSlashIndex = notation.lastIndexOf('/');
        if (lastSlashIndex == -1)
            lastSlashIndex = VAULT_TOKEN_PREFIX.length();

        String path = notation.substring(VAULT_TOKEN_PREFIX.length(), lastSlashIndex);
        String secret = notation.substring(lastSlashIndex + 1);

        JsonNode cachedSecrets = cache.getIfPresent(key);

        if (cachedSecrets != null) {
            logger.debug("Using cached data for token : {}", notation);
            try {
                JsonNode secretNode = cachedSecrets.get(secret);
                if (secretNode == null) {
                    logger.warn("Could not find {}/{}", path, secret);
                    return CompletableFuture.completedFuture("");
                }
                return CompletableFuture.completedFuture(secretNode.asText());
            }
            catch (Exception e) {
                throw new GuacamoleException("Failed to extract secret from cached JSON for " + notation, e);
            }
        }

        // Cache miss, either get an existing in-flight request or create a new one
        CompletableFuture<JsonNode> futureResponse = inFlightRequests.computeIfAbsent(key, k -> {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    String type = getSecretsEngine(path).get("type").asText();
                    String mountPath = getSecretsEngine(path).get("path").asText();
                    String newpath = path.substring(mountPath.length());
                    logger.debug("Vault {}, {}, {}, {}", type, mountPath, path, secret);
                    JsonNode jsonNode;
                    switch (type) {
                        case "ssh":
                            jsonNode = getValueSSH(mountPath, newpath, username);
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
                }

                catch (Exception e) {
                    logger.warn("Vault query failed for {} with {}", path, e.getMessage());
                    throw new CompletionException("Vault query failed for " + path, e);
                }
            })
            .orTimeout((long) vaultInfo.CacheLifetime, TimeUnit.MILLISECONDS);
        });

        return futureResponse.whenComplete((jsonNode, ex) -> {
            // Put newly received JSON data into the cache
            if (ex == null && jsonNode != null) {
                logger.debug("Putting data to cache : {}", notation);
                cache.put(key, jsonNode);
            }

            // Now that the cache is filled, this in-flight request is obsolete and must be removed
            inFlightRequests.remove(key);

        }).thenApply(jsonNode -> {
            /*
             * Extract and return the secret
             */
            JsonNode secretNode = jsonNode.get(secret);
            if (secretNode == null) {
                logger.warn("Could not find {}/{}", path, secret);
                return "";
            }
            return secretNode.asText();
        }).exceptionally(e -> {
            // Make sure that the exception is a GuacamoleException
            Throwable cause = e.getCause();
            String errorMessage = (cause != null) ? cause.getMessage() : "Unknown error";

            if (cause instanceof GuacamoleException)
                throw new CompletionException(cause);

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
     * @throws GuacamoleException
     *     If the secrets cannot be retrieved from the Vault.
     */
    private JsonNode getValueKV(String mountPath, String path, VaultKeyValueOperations.KeyValueBackend type) throws VaultException {
        VaultKeyValueOperations kvOperations = vaultTemplate.opsForKeyValue(mountPath, type);

        // Get the values on the path and cache them
        VaultResponse response = kvOperations.get(path);

        if (response == null || response.getData() == null)
        {
            throw new VaultException("Value not found in Vault for path: " + path);
        }

        return objectMapper.valueToTree(response.getData());
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
     * @param username
     *     The connection username that must be non null for SSH certificate
     *     generation.
     *
     * @return
     *     The values associated with the path.
     *
     * @throws GuacamoleException
     *     If the secrets cannot be retrieved from the Vault.
     */
    private JsonNode getValueSSH(String mountPath, String path, String username) throws VaultException {
        if (path.startsWith("creds/")) {
            if (username == null || username.isEmpty()) {
                throw new VaultException("The username can not be empty for SSH signed certificates");
            }

            VaultResponse response =
                vaultTemplate.write(mountPath + path, Map.of("ip", "0.0.0.0"));

            if (response == null || response.getData() == null) {
                throw new VaultException("No response from Vault SSH engine");
            }
            Map<String, Object> retval = response.getData();
            retval.put("password", retval.get("key"));

            return objectMapper.valueToTree(retval);
        }
        else if (path.startsWith("sign/")) {
            HvSshKeys sshKeys = new HvSshKeys(vaultInfo.SshType);
            Map<String, Object> request = Map.of(
                    "public_key", sshKeys.publicSsh,
                    "valid_principals", username,
                    "extensions", Map.of("permit-pty", ""),
                    "ttl", vaultInfo.SshConnectionTimeout);

            VaultResponse vaultResponse = vaultTemplate.write(mountPath + path, request);

            if (vaultResponse == null || vaultResponse.getData() == null) {
                throw new VaultException("No response from Vault SSH engine");
            }

            String signedCert = (String) vaultResponse.getData().get("signed_key");

            if (signedCert == null) {
                throw new VaultException("Vault did not return a signed SSH certificate");
            }

            return objectMapper.valueToTree(Map.of("private", sshKeys.privateSshPem,
                    "public", signedCert,
                    "unsigned", sshKeys.publicSsh));
        }
        else {
           throw new VaultException("Unknown SSH type on path: " + mountPath + path);
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
     * @throws GuacamoleException
     *     If the secrets cannot be retrieved from the Vault.
     */
    private JsonNode getValueLDAP(String mountPath, String path) throws VaultException {
        VaultResponse response;
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

        Map<String, Object> retval = response.getData();
        if (path.startsWith("library/")) {
            String username = String.valueOf(retval.get("service_account_name"));
            retval.put("username", username);

            // Register a listener to check-in the account for a TunnelClose Event
            logger.info("Caching session : {}", username);
            LDAPSessionInfo info = new LDAPSessionInfo(mountPath + path + "/check-in", username);
            ldapSessions.put(VAULT_LDAP_SESSION, info);
        }

        return objectMapper.valueToTree(retval);
    }

    /**
     * Retrieves a username or password from a Database secret engine.
     * Before version 1.10 the data could only have username/password
     * After there can be static preconfigured fields that the vault tokens
     * might access.
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
     * @throws GuacamoleException
     *     If the secrets cannot be retrieved from the Vault.
     */
    private JsonNode getValueDB(String mountPath, String path) throws VaultException {
        VaultResponse response = vaultTemplate.read(mountPath + path);

        if (response == null || response.getData() == null) {
            throw new VaultException("No response from Database secrets engine");
        }

        return objectMapper.valueToTree(response.getData());
    }
}
