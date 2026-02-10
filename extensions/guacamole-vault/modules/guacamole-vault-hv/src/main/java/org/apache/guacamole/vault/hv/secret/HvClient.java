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
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import javax.annotation.Nullable;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.vault.hv.GuacamoleExceptionSupplier;
import org.apache.guacamole.vault.hv.conf.HvConfigurationService;
import org.apache.guacamole.vault.hv.secret.HvTimedSecretData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client which retrieves records from Hashicorp Vault.
 */
public class HvClient {

    /**
     * Name of the HTTP header for the token, as specified in documentation:
     * https://developer.hashicorp.com/vault/docs/auth/token
     */
    static final String HASHICORP_VAULT_HTTP_HEADER_TOKEN = "X-Vault-Token";

    /**
     * API version.
     */
    static final String HASHICORP_VAULT_HTTP_VERSION = "/v1/";

    /**
     * Name of the Guacamole token to resolve on a Hashicorp Vault (secret path
     * is set in the token modifier).
     */
    static final String HASHICORP_VAULT_TOKEN_PREFIX = "HASHIVAULT:";

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(HvClient.class);

    /**
     * HttpClient for this class.
     */
    private final HttpClient httpClient;

    /**
     * ObjectMapper for this class.
     */
    private final ObjectMapper objectMapper;

    /**
     * The HV configuration associated with this client instance.
     */
    private final Map<String, String> hvConfig;

    /**
     * The maximum amount of time that an entry will be stored in the cache
     * before being refreshed, in milliseconds.
     */
    private long cacheLifetime;

    /**
     * All records retrieved from Hashicorp Vault, where each key is the
     * UID of the corresponding record. The contents of this Map are
     * automatically updated.
     */
    private final ConcurrentMap<String, HvTimedSecretData> cachedSecrets = new ConcurrentHashMap<>();

    /**
     * All in-flight HTTP requests to Vault, it prevents multiple queries
     * on the same path.
     */
    private final ConcurrentMap<String, CompletableFuture<JsonNode>> inFlightRequests = new ConcurrentHashMap<>();

    /**
     * Create a new HV client based around the provided HV configuration and
     * API timeout setting.
     *
     * @param hvConfig
     *     The HV configuration to use when retrieving properties from HV.
     */
    @AssistedInject
    public HvClient(@Assisted Map<String, String> hvConfig) {
        this.hvConfig = hvConfig;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();

        this.cacheLifetime = 60000;
        if (hvConfig.containsKey(HvConfigurationService.PARAM_NAME_CACHE_LIFETIME)) {
            String strCacheLifetime = hvConfig.get(HvConfigurationService.PARAM_NAME_CACHE_LIFETIME);
            try {
                this.cacheLifetime = Long.parseLong(strCacheLifetime);
            }
            catch (NumberFormatException e) {
                logger.warn("Invalid {} in HV config: {}", HvConfigurationService.PARAM_NAME_CACHE_LIFETIME, strCacheLifetime);
            }
        }
    }

    /**
     * Returns the value of the secret stored within Hashicorp Vault.
     *
     * @param notation
     *     The HV notation of the secret to retrieve.
     *
     * @return
     *     A Future which completes with the value of the secret represented by
     *     the given HV notation, or null if there is no such secret.
     *
     * @throws GuacamoleException
     *     If the requested secret cannot be retrieved or the HV notation
     *     is invalid.
     */
    public Future<String> getSecret(String notation) throws GuacamoleException {
        return getSecret(notation, null);
    }

    /**
     * Returns the value of the secret stored within Hashicorp Vault.
     *
     * @param notation
     *     The HV notation of the secret to retrieve.
     *
     * @param fallbackFunction
     *     A function to invoke in order to produce a Future for return,
     *     if the requested secret is not found. If the provided Function
     *     is null, it will not be run.
     *
     * @return
     *     A Future which completes with the value of the secret represented by
     *     the given HV notation, or empty string if there is no such secret to
     *     remove the token.
     *
     * @throws GuacamoleException
     *     If the requested secret cannot be retrieved or the HV notation
     *     is invalid.
     */
    public Future<String> getSecret(String notation,
            @Nullable GuacamoleExceptionSupplier<Future<String>> fallbackFunction)
            throws GuacamoleException {

        // If it's not an HV token, fail
        if (!notation.startsWith(HASHICORP_VAULT_TOKEN_PREFIX))
            throw new GuacamoleException("Invalid token HV notation: " + notation);

        /*
         * HASHIVAULT:path/to/secret  <-- the Guacamole token name and its modifier
         *            ^^^^^^^         <-- this is the path
         *                    ^^^^^^  <-- this is the secret (or key in HV terms)
         */
        int lastSlashIndex = notation.lastIndexOf('/');
        if (lastSlashIndex == -1)
            lastSlashIndex = HASHICORP_VAULT_TOKEN_PREFIX.length();

        String path = notation.substring(HASHICORP_VAULT_TOKEN_PREFIX.length(), lastSlashIndex);
        String secret = notation.substring(lastSlashIndex + 1);

        // Try to get the whole secret from cache and return key
        HvTimedSecretData cachedSecret = cachedSecrets.get(path);
        if (cachedSecret != null && System.currentTimeMillis() < cachedSecret.dateCreated + cacheLifetime) {
            try {
                JsonNode secretNode = cachedSecret.jsonNode.get("data").get("data").get(secret);
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
        CompletableFuture<JsonNode> futureResponse = inFlightRequests.computeIfAbsent(path, k -> {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    // Perform the Vault query
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(hvConfig.get(HvConfigurationService.PARAM_NAME_VAULT_URL) + HASHICORP_VAULT_HTTP_VERSION + path))
                        .header(HASHICORP_VAULT_HTTP_HEADER_TOKEN, hvConfig.get(HvConfigurationService.PARAM_NAME_VAULT_TOKEN))
                        .GET()
                        .build();
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    JsonNode jsonNode = objectMapper.readTree(response.body());

                    return jsonNode;

                }
                catch (Exception e) {
                    logger.warn("Vault query failed for {} with {}", path, e);
                    throw new CompletionException("Vault query failed for " + path, e);
                }
            });
        });

        return futureResponse.whenComplete((jsonNode, ex) -> {
            // Put newly received JSON data into the cache
            if (ex == null && jsonNode != null) {
                HvTimedSecretData secretToCache = new HvTimedSecretData();
                secretToCache.dateCreated = System.currentTimeMillis();
                secretToCache.jsonNode = jsonNode;
                cachedSecrets.put(path, secretToCache);
            }

            // Now that the cache is filled, this in-flight request is obsolete and must be removed
            inFlightRequests.remove(path);

        }).thenApply(jsonNode -> {
            /*
             * Extract and return the secret
             * If there's no data.data, it's weird so fail with an exception
             */
            try {
                JsonNode secretNode = jsonNode.get("data").get("data").get(secret);
                if (secretNode == null) {
                    logger.warn("Could not find {}/{}", path, secret);
                    return "";
                }
                return secretNode.asText();
            }
            catch (Exception e) {
                throw new CompletionException("Failed to parse JSON response for path " + path, e);
            }

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
}
