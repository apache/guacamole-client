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

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.Connectable;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.apache.guacamole.token.TokenFilter;
import org.apache.guacamole.vault.secret.VaultSecretService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * OpenBao implementation of VaultSecretService.
 * Retrieves RDP passwords from OpenBao based on the logged-in Guacamole username.
 */
public class OpenBaoSecretService implements VaultSecretService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(OpenBaoSecretService.class);

    /**
     * Client for communicating with OpenBao.
     */
    @Inject
    private OpenBaoClient openBaoClient;

    /**
     * Constructor that logs when the service is created.
     */
    public OpenBaoSecretService() {
        logger.info("OpenBaoSecretService initialized");
    }

    /**
     * The token pattern for OpenBao secrets: ${OPENBAO_SECRET}
     */
    public static final String OPENBAO_SECRET_TOKEN = "${OPENBAO_SECRET}";

    /**
     * The token pattern for Guacamole username: ${GUAC_USERNAME}
     */
    public static final String GUAC_USERNAME_TOKEN = "${GUAC_USERNAME}";

    @Override
    public String canonicalize(String token) {
        // Return the canonical form for tokens we recognize
        if (token == null)
            return null;

        // Remove ${} wrapper and return just the token name
        if (OPENBAO_SECRET_TOKEN.equals(token)) {
            return "OPENBAO_SECRET";
        }

        if (GUAC_USERNAME_TOKEN.equals(token)) {
            return "GUAC_USERNAME";
        }

        // Not our token
        return null;
    }

    @Override
    public Future<String> getValue(String token) throws GuacamoleException {
        // This method is called for simple token lookups without user context
        logger.warn("getValue(String) called without user context - cannot determine username");
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Future<String> getValue(UserContext userContext, Connectable connectable, String token)
            throws GuacamoleException {

        logger.info("getValue() called with token: {}", token);

        // Get the logged-in Guacamole username
        String username = userContext.self().getIdentifier();

        // Handle GUAC_USERNAME token - return the Guacamole username
        if ("GUAC_USERNAME".equals(token)) {
            logger.info("getValue() returning username: '{}'", username);
            return CompletableFuture.completedFuture(username);
        }

        // Handle OPENBAO_SECRET token - fetch password from OpenBao
        if ("OPENBAO_SECRET".equals(token)) {
            logger.info("Retrieving OpenBao secret for username: {}", username);

            try {
                // Fetch the secret from OpenBao using the username
                JsonObject response = openBaoClient.getSecret(username);

                // Extract the password field
                String password = openBaoClient.extractPassword(response);

                if (password != null) {
                    logger.info("Successfully retrieved password from OpenBao for user: {} (length: {})", username, password.length());
                    return CompletableFuture.completedFuture(password);
                } else {
                    logger.warn("Password field not found in OpenBao for user: {}", username);
                    return CompletableFuture.completedFuture(null);
                }

            } catch (GuacamoleException e) {
                logger.error("Failed to retrieve secret from OpenBao for user: {}", username, e);
                // Return null instead of throwing to allow connection attempt with empty password
                return CompletableFuture.completedFuture(null);
            }
        }

        // Not a recognized token
        logger.warn("Token '{}' not recognized, returning null", token);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Map<String, Future<String>> getTokens(UserContext userContext,
            Connectable connectable,
            GuacamoleConfiguration config,
            TokenFilter tokenFilter) throws GuacamoleException {

        Map<String, Future<String>> tokens = new java.util.HashMap<>();
        String username = userContext.self().getIdentifier();

        // Add GUAC_USERNAME token (always available)
        tokens.put("GUAC_USERNAME", CompletableFuture.completedFuture(username));

        // Add OPENBAO_SECRET token (fetch from OpenBao)
        try {
            JsonObject response = openBaoClient.getSecret(username);
            String password = openBaoClient.extractPassword(response);
            if (password != null) {
                tokens.put("OPENBAO_SECRET", CompletableFuture.completedFuture(password));
                logger.info("Added token OPENBAO_SECRET with password from OpenBao (length: {})", password.length());
            } else {
                logger.warn("Password not found in OpenBao for user: {}", username);
            }
        } catch (Exception e) {
            logger.error("Failed to get secret from OpenBao for user: {}", username, e);
        }

        logger.info("Returning {} tokens: {}", tokens.size(), tokens.keySet());
        return tokens;
    }
}
