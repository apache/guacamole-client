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
import org.apache.guacamole.net.event.TunnelCloseEvent;
import org.apache.guacamole.net.event.TunnelConnectEvent;
import org.apache.guacamole.net.GuacamoleTunnel;
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
public class OpenBaoClient {
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
     * A task scheduler for remove terminated checked out LDAP Sessions
     */
    ThreadPoolTaskScheduler scheduler;

    /**
     * Constructor allowing early injection of configuration and initialization
     * to start the token renewal process as early as possible
     *
     * @param configService
     *     The injected configuration service 
     */
    public OpenBaoClient(OpenBaoConfigurationService configService) {
      this.configService = configService;

        try {
            VaultEndpoint endpoint = VaultEndpoint.from(configService.getVaultUri().resolve("v1"));

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

            RestTemplate restTemplate = new RestTemplate(requestFactory);
            restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(
                configService.getVaultUri().resolve("v1").toString()));

            if (configService.getVaultToken() != null) {
                if (isTokenReadableFile(configService.getVaultToken())) {
                    logger.debug("File Token : {}", configService.getVaultToken());
                    this.authentication =
                            new FileTokenAuthentication(configService.getVaultToken());
                }
                else {
                    logger.debug("Token : {}", configService.getVaultToken());
                    this.authentication = new TokenAuthentication(configService.getVaultToken());
                }
            }
            else if (configService.getVaultUsername() != null && configService.getVaultPassword() != null) {
                UsernamePasswordAuthenticationOptions options =
                    UsernamePasswordAuthenticationOptions.builder()
                        .username(configService.getVaultUsername())
                        .password(configService.getVaultPassword())
                        .build();

                logger.debug("Username/Password : {}, {}", configService.getVaultUsername(),
                        configService.getVaultPassword());
                this.authentication =
                    new UsernamePasswordAuthentication(options, endpoint, restTemplate);
            }
            else {
                throw new GuacamoleException("Either a vault token or Username/Password must be supplied");
            }

            // Create a task scheduler for our token renewal
            ThreadPoolTaskScheduler taskscheduler = new ThreadPoolTaskScheduler();
            taskscheduler.setPoolSize(1);
            taskscheduler.setThreadNamePrefix("vault-renewal-");
            taskscheduler.initialize();

            // Session manager to automatically renew tokens before expiration
            this.sessionManager = new TtlAwareSessionManager(this.authentication,
                    restTemplate, taskscheduler, configService.getTokenRenewalDelay());

            this.vaultTemplate = new VaultTemplate(endpoint, requestFactory, this.sessionManager);
        }
        catch (Exception e) {
            logger.error("Error initializing Vault client: {}",  e.getMessage());
        }

        // Initialize the cache with maximum size of 1MB, and cache expiry with
        // forced cleanup
        // FIXME I'd really like to do something like "Array.fill(v, '\0');" in
        // the removal listener to ensure that passwords are no longer in memory.
        // However, both spring-core-vault and Guacamole store these values
        // elsewhere as immutable String values, So even if I stored them in the
        // cache as char[] copies of the password would be elsewhere as String
        // values in memory.. A VaultConverter function could deal with the
        // spring-vault-core part of the problem, but not Gaucamole.
        try {
            cache = Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofMillis(configService.getVaultCacheLifetime()))
                    .maximumSize(1_000_000)
                    .build();
        }
        catch (GuacamoleException e) {
            logger.error("Can't setup OpenBao cache");
        }
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
     * Lists the valid mount paths and their type
     *
     * @return
     *      The map of the mount path and with the value being the type.
     *      The type can be ssh, ldap, database, kv_1 or kv_2
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
     * @param event
     *      A TunnelConnectEvent or TunnelCloseEvent     
     */
    public void connectLdapSession(Object event) {
        if (event instanceof TunnelConnectEvent) {
            LDAPSessionInfo session = ldapSessions.get("checkin");
            if (session != null) {
                String id = ((TunnelConnectEvent) event).getTunnel().getUUID().toString();
                logger.debug("Storing connection ID: {}", id);
                ldapSessions.put(id, new LDAPSessionInfo(session.checkInPath, session.username, true));
                ldapSessions.remove("checkin");
            }
        }
        else {
            String id = ((TunnelCloseEvent) event).getTunnel().getUUID().toString();
            LDAPSessionInfo session = ldapSessions.get(id);
            if (session != null && session.initialized) {
                // FIXME : We don't always receive the TunnelCloseEvent
                // So this checkin function only kinda works 
                logger.debug("Removing stored LDAP session: {}", id);
                vaultTemplate.write(session.checkInPath, Map.of("service_account_names", session.username));
                ldapSessions.remove(id);
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
     *     The value associated with the token.
     *
     * @throws GuacamoleException
     *     If the secret cannot be retrieved from the Vault.
     */
    public String getValue(String token, String username, String key) throws GuacamoleException {
        try {
            if (! token.startsWith(VAULT_TOKEN_PREFIX)) {
                throw new GuacamoleException("Invalid token Vault token: " + token);
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

            logger.debug("MountPath {}, path {}, secret {}, type {}", mountPath, path, secret, type);

            Map<String, Object> cacheResponse = (Map<String, Object>) cache.getIfPresent(key);

            Object raw;
            if (cacheResponse != null) {
                raw = cacheResponse.get(secret);        
            }
            else {
                Map<String, Object> response;
            
                switch (type) {
                    case "ssh":
                        response = getValueSSH(mountPath, path, username);
                        break;
                    case "ldap":
                        response = getValueLDAP(mountPath, path);
                        break;
                    case "database":
                        response = getValueDB(mountPath, path);
                        break;
                    case "kv_1":
                        response = getValueKV(mountPath, path, VaultKeyValueOperations.KeyValueBackend.KV_1);
                        break;
                    case "kv_2":
                        response = getValueKV(mountPath, path, VaultKeyValueOperations.KeyValueBackend.KV_2);
                        break;
                    default:
                       throw new GuacamoleException("Unknown secret engine for the token: " + token);
                }

                cache.put(key, response);
                raw = response.get(secret);
            }
          
            if (raw == null) {
                throw new VaultException("Secret '" + secret + "' not found on the path '" + mountPath + path + "'");
            }
            else if (raw instanceof String || raw instanceof Number || raw instanceof Boolean) {
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
        catch (VaultException e) {
            logger.error("Failed to retrieve secret from the vault : {}", e.getMessage());
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
     * @param type
     *     The type of key-value store. Either "kv_1" or "kv_2"
     *
     * @return
     *     The values associated with the path.
     *
     * @throws GuacamoleException
     *     If the secrets cannot be retrieved from the Vault.
     */
    private Map<String, Object> getValueKV(String mountPath, String path, VaultKeyValueOperations.KeyValueBackend type) throws GuacamoleException {
        VaultKeyValueOperations kvOperations = vaultTemplate.opsForKeyValue(mountPath, type);

        // Get the values on the path and cache them
        VaultResponse response = kvOperations.get(path);

        if (response == null || response.getData() == null)
        {
            throw new GuacamoleServerException("Value not found in Vault for path: " + mountPath + path);
        }
        
        return response.getData();
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
    private Map<String, Object> getValueSSH(String mountPath, String path, String username) throws GuacamoleException {
        if (path.startsWith("creds/")) {
            if (username == null || username.isEmpty()) {
                throw new GuacamoleException("The username can not be empty for SSH signed certificates");
            }
            
            VaultResponse response =
                vaultTemplate.write(mountPath +  path, Map.of("ip", "0.0.0.0"));

            if (response == null || response.getData() == null) {
                throw new GuacamoleException("No response from Vault SSH engine");
            }
            
            return response.getData();           
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

            if (signedCert == null) {
                throw new GuacamoleException("Vault did not return a signed SSH certificate");
            }

            return Map.of("private", sshKeys.privateSshPem,
                    "public", signedCert,
                    "unsigned", sshKeys.publicSsh);        
        }
        else {
           throw new GuacamoleException("Unknown SSH type on path: " + mountPath + path);
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
    private Map<String, Object> getValueLDAP(String mountPath, String path) throws GuacamoleException {    
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
        
        Map<String, Object> retval = response.getData();
        if (path.startsWith("library/")) {
            String username = String.valueOf(retval.get("service_account_name"));
            retval.put("username", username);
            
            // Register a listener to check-in the account for a TunnelClose Event
            logger.info("Caching session : {}", username);
            LDAPSessionInfo info = new LDAPSessionInfo(mountPath + path + "/check-in", username);
            ldapSessions.put("checkin", info);
        }
        
        return retval;
    }

    /**
     * Retrieves a username or password from a Databse secret engine.
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
    private Map<String, Object> getValueDB(String mountPath, String path) throws GuacamoleException {
        VaultResponse response = vaultTemplate.read(mountPath +  path);        
        
        if (response == null || response.getData() == null) {
            throw new GuacamoleException("No response from Databse secrets engine");
        }

        return response.getData();
    }

    /**
     * Release the automatic renewal of the tokens on shutdown
     */
    public void shutdown() {
        this.sessionManager.close();
    }
}
