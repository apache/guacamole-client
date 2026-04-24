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

import com.google.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.Connectable;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.apache.guacamole.token.TokenFilter;
import org.apache.guacamole.vault.openbao.secret.OpenBaoClient;
import org.apache.guacamole.vault.secret.VaultSecretService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenBao implementation of VaultSecretService.
 * Retrieves secrets from OpenBao based on parameters of the logged-in user.
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

    @Override
    public String canonicalize(String nameComponent) {
        try {

            // As vault notation is essentially a URL, encode all components
            // using standard URL escaping
            return URLEncoder.encode(nameComponent, "UTF-8");

        }
        catch (UnsupportedEncodingException e) {
            throw new UnsupportedOperationException("Unexpected lack of UTF-8 support.", e);
        }
    }

    /**
     * Returns a Future which eventually completes with the value of the secret
     * having the given name. If no such secret exists, the Future will be
     * completed with null. The secrets retrieved from this method are independent
     * of the context of the particular connection being established, or any
     * associated user context.
     *
     * @param name
     *     The name of the secret to retrieve.
     *
     * @return
     *     A Future which completes with value of the secret having the given
     *     name. If no such secret exists, the Future will be completed with
     *     null. If an error occurs asynchronously which prevents retrieval of
     *     the secret, that error will be exposed through an ExecutionException
     *     when an attempt is made to retrieve the value from the Future.
     *
     * @throws GuacamoleException
     *     If the secret cannot be retrieved due to an error.
     */
    @Override
    public Future<String> getValue(String token) throws GuacamoleException {
        // Empty parameters in this context
        Map<String, String> parameters = Map.of("username", "",
                "hostname", "",
                "gateway-username", "",
                "gateway-hostname", "");
        String value = openBaoClient.getValue(token, parameters);
        return CompletableFuture.completedFuture(value);                
    }

    @Override
    public Future<String> getValue(UserContext userContext, Connectable connectable, String token)
            throws GuacamoleException {
        // Empty parameters in this context
        Map<String, String> parameters = Map.of("username", "",
                "hostname", "",
                "gateway-username", "",
                "gateway-hostname", "");
        String value = openBaoClient.getValue(token, parameters);
        return CompletableFuture.completedFuture(value);
    }

    /*
     * Returns a map of token names to corresponding Futures which eventually
     * complete with the value of that token, where each token is dynamically
     * defined based on connection parameters. If a vault implementation allows
     * for predictable secrets based on the parameters of a connection, this
     * function should be implemented to provide automatic tokens for those
     * secrets and remove the need for manual mapping via YAML.
     *
     * @param userContext
     *     The user context from which the connectable originated.
     *
     * @param connectable
     *     The connection or connection group for which the tokens are being replaced.
     *
     * @param config
     *     The configuration of the Guacamole connection for which tokens are
     *     being generated. This configuration may be empty or partial,
     *     depending on the underlying implementation.
     *
     * @param filter
     *     A TokenFilter instance that applies any tokens already available to
     *     be applied to the configuration of the Guacamole connection. These
     *     tokens will consist of tokens already supplied to connect().
     *
     * @return
     *     A map of token names to their corresponding future values, where
     *     each token and value may be dynamically determined based on the
     *     connection configuration.
     *
     * @throws GuacamoleException
     *     If an error occurs producing the tokens and values required for the
     *     given configuration.
     */
    @Override
    public Map<String, Future<String>> getTokens(UserContext userContext,
            Connectable connectable, GuacamoleConfiguration config,
            TokenFilter filter) throws GuacamoleException {

        Map<String, Future<String>> tokens = new HashMap<>();
        Map<String, String> parameters = config.getParameters();

        Pattern tokenPattern = Pattern.compile("\\$\\{(" + OpenBaoClient.VAULT_TOKEN_PREFIX + ".+)\\}");

        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            Matcher tokenMatcher = tokenPattern.matcher(entry.getValue());
            while (tokenMatcher.find()) {
                String token = tokenMatcher.group(1);
                String value = openBaoClient.getValue(token, parameters);
                tokens.put(token, CompletableFuture.completedFuture(value));
            }
        }

        return tokens;
    }
}
