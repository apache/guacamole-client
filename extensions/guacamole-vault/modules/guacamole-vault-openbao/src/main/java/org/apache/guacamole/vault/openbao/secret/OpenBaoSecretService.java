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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.Connectable;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.apache.guacamole.token.TokenFilter;
import org.apache.guacamole.vault.secret.VaultSecretService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenBao implementation of VaultSecretService. Resolves the legacy
 * {@code ${OPENBAO_SECRET}} and {@code ${GUAC_USERNAME}} tokens as well
 * as an arbitrary-path syntax of the form
 * {@code openbao:<path>[:<field>]}, which, when supplied as a secret
 * name via the YAML token mapping, retrieves the given field from the
 * named secret under the configured mount.
 */
public class OpenBaoSecretService implements VaultSecretService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(OpenBaoSecretService.class);

    /**
     * Prefix identifying the arbitrary-path secret name syntax:
     * {@code openbao:<path>[:<field>]}.
     */
    private static final String OPENBAO_PREFIX = "openbao:";

    /**
     * The secret name used for the legacy per-user password lookup.
     */
    private static final String OPENBAO_SECRET_NAME = "OPENBAO_SECRET";

    /**
     * The secret name used to resolve to the logged-in Guacamole username.
     */
    private static final String GUAC_USERNAME_NAME = "GUAC_USERNAME";

    /**
     * The default field fetched from an OpenBao secret when no explicit
     * {@code :field} suffix is supplied.
     */
    private static final String DEFAULT_FIELD = "password";

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

    @Override
    public String canonicalize(String token) {

        if (token == null)
            return null;

        // Existing names (including their ${...} form) are passed through.
        if (OPENBAO_SECRET_NAME.equals(token) || ("${" + OPENBAO_SECRET_NAME + "}").equals(token))
            return OPENBAO_SECRET_NAME;

        if (GUAC_USERNAME_NAME.equals(token) || ("${" + GUAC_USERNAME_NAME + "}").equals(token))
            return GUAC_USERNAME_NAME;

        // Arbitrary-path form: let it flow through unchanged so getValue()
        // can parse it.
        if (token.startsWith(OPENBAO_PREFIX))
            return token;

        return null;
    }

    @Override
    public Future<String> getValue(String token) throws GuacamoleException {
        // Without user context we cannot resolve per-user lookups.
        logger.warn("getValue(String) called without user context - cannot determine username");
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Future<String> getValue(UserContext userContext, Connectable connectable, String token)
            throws GuacamoleException {

        if (token == null)
            return CompletableFuture.completedFuture(null);

        String username = userContext.self().getIdentifier();

        // Legacy: ${GUAC_USERNAME} → username
        if (GUAC_USERNAME_NAME.equals(token))
            return CompletableFuture.completedFuture(username);

        // Legacy: ${OPENBAO_SECRET} → password from <mount>/data/<username>
        if (OPENBAO_SECRET_NAME.equals(token))
            return CompletableFuture.completedFuture(
                    fetchField(username, DEFAULT_FIELD, username));

        // Additive: openbao:<path>[:<field>]
        if (token.startsWith(OPENBAO_PREFIX)) {
            String spec = token.substring(OPENBAO_PREFIX.length());
            int sep = spec.lastIndexOf(':');

            String path;
            String field;
            if (sep > 0 && sep < spec.length() - 1) {
                path  = spec.substring(0, sep);
                field = spec.substring(sep + 1);
            }
            else {
                path  = spec;
                field = DEFAULT_FIELD;
            }

            if (path.isEmpty()) {
                logger.warn("Empty path supplied for token: {}", token);
                return CompletableFuture.completedFuture(null);
            }

            return CompletableFuture.completedFuture(fetchField(path, field, username));
        }

        logger.debug("Token \"{}\" not recognized by OpenBao secret service", token);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Retrieves {@code field} from the OpenBao secret at {@code path},
     * logging the supplied {@code contextLabel} for diagnostics. Returns
     * null rather than throwing when retrieval fails, to preserve the
     * existing behavior of allowing Guacamole to proceed (possibly with
     * an empty credential).
     *
     * @param path
     *     Path of the secret to fetch, relative to the configured mount.
     *
     * @param field
     *     Name of the field to extract.
     *
     * @param contextLabel
     *     Identifier used only for log messages.
     *
     * @return
     *     The field value, or null on failure or if the field is absent.
     */
    private String fetchField(String path, String field, String contextLabel) {
        try {
            JsonObject response = openBaoClient.getSecret(path);
            String value = openBaoClient.extractField(response, field);

            if (value == null)
                logger.warn("Field \"{}\" not found in OpenBao secret at \"{}\" (context: {})",
                        field, path, contextLabel);

            return value;
        }
        catch (GuacamoleException e) {
            logger.error("Failed to retrieve secret \"{}\" from OpenBao (context: {}): {}",
                    path, contextLabel, e.getMessage());
            logger.debug("Underlying exception:", e);
            return null;
        }
    }

    @Override
    public Map<String, Future<String>> getTokens(UserContext userContext,
            Connectable connectable,
            GuacamoleConfiguration config,
            TokenFilter tokenFilter) throws GuacamoleException {

        Map<String, Future<String>> tokens = new HashMap<>();
        String username = userContext.self().getIdentifier();

        // GUAC_USERNAME is always available.
        tokens.put(GUAC_USERNAME_NAME, CompletableFuture.completedFuture(username));

        // Best-effort pre-population of OPENBAO_SECRET from the per-user
        // secret at <mount>/data/<username>. Missing/unreachable secrets
        // are logged but do not abort token resolution.
        try {
            JsonObject response = openBaoClient.getSecret(username);
            String password = openBaoClient.extractPassword(response);
            if (password != null)
                tokens.put(OPENBAO_SECRET_NAME,
                        CompletableFuture.completedFuture(password));
            else
                logger.warn("Password not found in OpenBao for user: {}", username);
        }
        catch (GuacamoleException e) {
            logger.warn("Failed to pre-populate OPENBAO_SECRET for user {}: {}",
                    username, e.getMessage());
            logger.debug("Underlying exception:", e);
        }

        logger.debug("Returning {} OpenBao tokens: {}", tokens.size(), tokens.keySet());
        return tokens;
    }
}
