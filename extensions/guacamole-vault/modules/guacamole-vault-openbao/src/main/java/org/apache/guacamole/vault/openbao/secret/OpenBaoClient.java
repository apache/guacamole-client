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
import java.io.IOException;
import java.net.URI;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.HashMap;
import java.util.Map;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.apache.guacamole.vault.openbao.conf.OpenBaoConfigurationService;
import org.apache.guacamole.vault.openbao.secret.FileTokenAuthentication;
import org.apache.guacamole.vault.openbao.secret.UsernamePasswordAuthentication;
import org.apache.guacamole.vault.openbao.secret.UsernamePasswordAuthenticationOptions;
import org.apache.guacamole.vault.openbao.secret.TimeoutVaultTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.LifecycleAwareSessionManager;
import org.springframework.vault.authentication.SessionManager;
import org.springframework.vault.authentication.SimpleSessionManager;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultClients;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.lease.event.SecretLeaseErrorEvent;
import org.springframework.vault.core.lease.event.SecretLeaseExpiredEvent;
import org.springframework.vault.core.lease.SecretLeaseContainer;
import org.springframework.vault.core.VaultKeyValueOperations;
import org.springframework.vault.VaultException;
import org.springframework.vault.support.VaultResponse;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
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
    @Inject
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
    private TimeoutVaultTemplate vaultTemplate;

    /**
     * Vault client authentication object
     */
    private ClientAuthentication authentication;

    /**
     * A Vault lease container used to automatically renew tokens
     * or username/password access.
     */
    private SecretLeaseContainer leaseContainer;

    /**
     * Complete the instantiation of the class on first use
     */
    public void init() throws GuacamoleException {
        try {
            VaultEndpoint endpoint = VaultEndpoint.from(configService.getVaultUri().resolve("v1"));


            if (configService.getVaultToken() != null) {
                if (isTokenReadableFile(configService.getVaultToken())) {
                    this.authentication =
                            new FileTokenAuthentication(configService.getVaultToken(),
                                    endpoint, new RestTemplate());
                }
                else {
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

            if (configService.getVaultToken() != null &&
                    ! isTokenReadableFile(configService.getVaultToken())) {
                this.vaultTemplate = new TimeoutVaultTemplate(endpoint,
                        new SimpleClientHttpRequestFactory(),
                        new SimpleSessionManager(this.authentication));
            }
            else {
                ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
                taskScheduler.setPoolSize(1);
                taskScheduler.setThreadNamePrefix("vault-token-renewal-");
                taskScheduler.initialize();
                RestOperations restOperations = VaultClients.createRestTemplate(endpoint,
                        new SimpleClientHttpRequestFactory());
                LifecycleAwareSessionManager session =
                        new LifecycleAwareSessionManager(this.authentication, taskScheduler, restOperations);

                this.vaultTemplate = new TimeoutVaultTemplate(endpoint,
                        new SimpleClientHttpRequestFactory(), session);
            }
            try {
                this.vaultTemplate.setConnectionTimeout(configService.getConnectionTimeout());
            }
            catch (GuacamoleException e) {
                logger.debug("Using default vault endpoint connection timeout: " + e.getMessage());
            }
             try {
                this.vaultTemplate.setRequestTimeout(configService.getRequestTimeout());
            }
            catch (GuacamoleException e) {
                logger.debug("Using default vault endpoint request timeout: " + e.getMessage());
            }
        }
        catch (Exception e) {
            throw new GuacamoleException("Error initializing Vault client: ", e);
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
                    if (type.equals("ssh")  || type.equals("database")) {
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
    public String getValue(String token, GuacamoleConfiguration config) throws GuacamoleException {
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
            // with values supplied in the parameters
            // FIXME Could this be done with the TokenFilter in OpenBaoSecretService ?
            // FIXME There is an edge case for tokens like "vault://ldap/$${USER}/{USER}/password"
            // both here and below. This seems a pretty unlikely case, so don't treat.
            String username = config.getParameter("username");
            if (username != null && !username.isEmpty()
                    && token.contains("{USER}") && ! token.contains("$${USER}")) {
                token.replace("{USER}", username);

            }
            String hostname = config.getParameter("hostname");
            if (hostname != null && !hostname.isEmpty()
                    && token.contains("{SERVER}") && ! token.contains("$${SERVER}")) {
                token.replace("{SERVER}", hostname);

            }
            String gatewayHostname = config.getParameter("gateway-hostname");
            if (gatewayHostname != null && !gatewayHostname.isEmpty()
                    && token.contains("{GATEWAY}") && ! token.contains("$${GATEWAY}")) {
                token.replace("{GATEWAY}", gatewayHostname);

            }
            String gatewayUsername = config.getParameter("gateway-username");
            if (gatewayUsername != null && !gatewayUsername.isEmpty()
                    && token.contains("{GATEWAY_USER}") && ! token.contains("$${GATEWAY_USER}")) {
                token.replace("{GATEWAY_USER}", gatewayUsername);

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


        if (response.getData() == null)
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
        // Is the key-value already in the cache
        Map<String, Object> cacheResponse = (Map<String, Object>) cache.getIfPresent(mountPath + "/" + path);

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
                throw new GuacamoleException("SSH secret '" + secret + "' not found on the path : " + mountPath +"/" + path);
            }
            return retval;
        }

        // Detect if wanting one-time password or signed certificate
        if (path.startsWith("otp/")) {
            VaultResponse response =
                vaultTemplate.write(mountPath + "/creds/" + path.substring(4), Map.of("ip", "0.0.0.0"));
            try {
              logger.info("  KV Response : {}", objectMapper.writeValueAsString(response.getData()));
            }
            catch (Exception e) {
              logger.info(" KV Response Error : {}", e);
            }

            if (response == null || response.getData() == null) {
                throw new GuacamoleException("No response from Vault SSH engine");
            }

            String password = (String) response.getData().get("key");
            String user = (String) response.getData().get("username");

            if (password == null || user == null) {
                throw new GuacamoleException("Invalid SSH OTP response");
            }

            // Cache the retrieved user and otp
            cache.put(mountPath + "/" + path, Map.of("username", user, "password", password));
            logger.debug("SSH OTP :  " + user + " : " + password);

            if (secret.equals("username")) {
                return user;
            }
            else if (secret.equals("password")) {
                return password;
            }
        }
        else if (path.startsWith("cert/")) {
            String username = config.getParameter("username");
            if (username == null || username.isEmpty()) {
                throw new GuacamoleException("The username can not be empty for SSH signed certificates");
            }

            OpenBaoSshKeys sshKeys = new OpenBaoSshKeys(configService.getSshType());

            Map<String, Object> request = Map.of(
                    "public_key", sshKeys.publicSsh,
                    "valid_principals", username,
                    "extensions", Map.of("permit-pty", ""),
                    "ttl", configService.getSshConnectionTimeout()
            );

            VaultResponse vaultResponse =
                    vaultTemplate.write(mountPath + "/sign/" + path.substring(5), request);

            if (vaultResponse == null || vaultResponse.getData() == null) {
                throw new GuacamoleException("No response from Vault SSH engine");
            }

            String signedCert = (String) vaultResponse.getData().get("signed_key");

            if (signedCert == null) {
                throw new GuacamoleException("Vault did not return a signed SSH certificate");
            }

            cache.put(mountPath + "/" + path, Map.of(
                    "private", sshKeys.privateSshPem,
                    "public", signedCert));
            logger.info("SSH CERT :  " + sshKeys.privateSshPem + " : " + signedCert);

            if (secret.equals("private")) {
                return sshKeys.privateSshPem;
            }
            else if (secret.equals("public")) {
                return signedCert;
            }
        }
        else {
           throw new GuacamoleException("Unknown SSH type on path: " + mountPath + path);
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
        if (secret != "username" && secret != "password") {
            throw new GuacamoleException("LDAP secret '" + secret + "' not found on the path : " + mountPath +"/" + path);
        }

        // Is the value already in the cache
        Map<String, Object> cacheResponse =  (Map<String, Object>) cache.getIfPresent(mountPath + "/" + path);

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
                throw new GuacamoleException("SSH secret '" + secret + "' not found on the path : " + mountPath +"/" + path);
            }
            return retval;
        }

        VaultResponse response;
        if (path.startsWith("static/")) {
            response = vaultTemplate.read(mountPath + "/static-creds/" + path.substring(7));
        }
        else if (path.startsWith("dynamic/")) {
            response = vaultTemplate.read(mountPath + "/creds/" + path.substring(8));
        }
        else if (path.startsWith("service/")) {
            response = vaultTemplate.read(mountPath + "/" + path.substring(8) + "/check-out");
        }
        else {
           throw new GuacamoleException("Unknown LDAP type on path: " + mountPath +"/" + path);
        }

        if (response == null || response.getData() == null) {
            throw new GuacamoleException("No response from LDAP secrets engine");
        }

        String username;
        String password = (String) response.getData().get("password");
        if (path.startsWith("service/")) {
            username = (String) response.getData().get("service_account_name");
        }
        else {
            username = (String) response.getData().get("username");
        }

        if (username == null || password == null) {
            throw new IllegalStateException("Invalid LDAP static credential response");
        }

        // Cache the retrieved user and password
        cache.put(mountPath + "/" + path, Map.of("username", username, "password", password));
        logger.debug("LDAP :  " + username + " : " + password);

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
        if (secret != "username" && secret != "password") {
            throw new GuacamoleException("Database secret '" + secret + "' not found on the path : " + mountPath +"/" + path);
        }

        // Is the key-value already in the cache
        Map<String, Object> cacheResponse = (Map<String, Object>) cache.getIfPresent(mountPath + "/" + path);

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
        cache.put(mountPath + "/" + path, Map.of("username", username, "password", password));
        logger.debug("DB :  " + username + " : " + password);

        if (secret.equals("username")) {
            return username;
        }
        return password;
    }

    /**
     * Release the automatic renewal of the AppRole token on shutown
     */
    public void shutdown() {
        leaseContainer.stop();
    }
}
