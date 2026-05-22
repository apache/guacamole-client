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
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.Connectable;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.apache.guacamole.token.TokenFilter;
import org.apache.guacamole.vault.openbao.secret.OpenBaoClient;
import org.apache.guacamole.vault.openbao.secret.OpenBaoClientProvider;
import org.apache.guacamole.vault.secret.VaultSecretService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenBao implementation of VaultSecretService.
 * Retrieves secrets from OpenBao based on parameters of the logged-in user.
 */
@Singleton
public class OpenBaoSecretService implements VaultSecretService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(OpenBaoSecretService.class);

    /**
     * Client for communicating with OpenBao.
     */
    private final Provider<OpenBaoClient> openBaoClientProvider;

    /**
     * Constructor that loads when the service is created, forcing early
     * start of Vault token renewal. Inject OpenBaoClient via Provider
     * to avoid circular Guice dependency
     *
     * @param openBaoClientProvider
     *     A Provider for the OpenBaoClient singleton
     */
    @Inject
    public OpenBaoSecretService(Provider<OpenBaoClient> openBaoClientProvider) {
        this.openBaoClientProvider = openBaoClientProvider;
        logger.debug("OpenBaoSecretService initialized");
    }

    /**
     * Get a Guice cached copy of the OpenBaoClient Singleton
     *
     * @return
     *     A singleton OpenBaoClient instance
     */
    private OpenBaoClient client() {
        return openBaoClientProvider.get();
    }

    /**
     * As vault notation is essentially a URL, encode all components
     * using standard URL escaping.
     *
     * @param nameComponent
     *     The token to be canonicalized
     *
     * @return
     *     The canonicalized token
     */
    @Override
    public String canonicalize(String nameComponent) {
        try {
            return URLEncoder.encode(nameComponent, "UTF-8");

        }
        catch (UnsupportedEncodingException e) {
            throw new UnsupportedOperationException("Unexpected lack of UTF-8 support.", e);
        }
    }

    /**
     * Before going further replace the arguments "{GUAC_USERNAME}","{USERNAME}",
     * "{HOSTNAME}", "{GATEWAY_USERNAME}" and "{GATEWAY_HOSTNAME}" in the token with
     * values supplied in the parameters.
     *
     * @param token
     *     A token that might or might not include sub-tokens to be replaced
     *
     * @param userContext
     *     The user context from which the connectable originated.
     *
     * @param config
     *     The configuration of the Guacamole connection for which tokens are
     *     being generated. This configuration may be empty or partial,
     *     depending on the underlying implementation.
     *
     * @return
     *     A token with the sub-tokens included eplaced.
     */
    private String prepareToken(String token, UserContext userContext, GuacamoleConfiguration config, TokenFilter filter) {
        // FIXME There is an edge case for tokens like "vault://ldap/$${USER}/{USER}/password"
        // both here and below. This seems a pretty unlikely case, so don't treat.
        String guac_username = userContext == null ? "" : userContext.self().getIdentifier();
        if (guac_username != null && !guac_username.isEmpty()
                && token.contains("{GUAC_USERNAME}") && ! token.contains("$${GUAC_USERNAME}")) {
            token = token.replace("{GUAC_USERNAME}", filter.filter(guac_username));

        }
        String hostname = config.getParameter("hostname");
        if (hostname != null && !hostname.isEmpty() && !hostname.contains("${")
                && token.contains("{HOSTNAME}") && ! token.contains("$${HOSTNAME}")) {
            token = token.replace("{HOSTNAME}", filter.filter(hostname));

        }
        String username = config.getParameter("username");
        if (username != null && !username.isEmpty() && !username.contains("${")
                && token.contains("{USERNAME}") && ! token.contains("$${USERNAME}")) {
            token = token.replace("{USERNAME}", filter.filter(username));

        }
        String gatewayHostname = config.getParameter("gateway-hostname");
        if (gatewayHostname != null && !gatewayHostname.isEmpty() && !gatewayHostname.contains("${")
                && token.contains("{GATEWAY}") && ! token.contains("$${GATEWAY}")) {
            token = token.replace("{GATEWAY}", filter.filter(gatewayHostname));

        }
        String gatewayUsername = config.getParameter("gateway-username");
        if (gatewayUsername != null && !gatewayUsername.isEmpty() && !gatewayUsername.contains("${")
                && token.contains("{GATEWAY_USER}") && ! token.contains("$${GATEWAY_USER}")) {
            token = token.replace("{GATEWAY_USER}", filter.filter(gatewayUsername));

        }

        return token;
    }

    /**
     * Returns a Future which eventually completes with the value of the secret
     * having the given name. If no such secret exists, the Future will be
     * completed with null. The secrets retrieved from this method are independent
     * of the context of the particular connection being established, or any
     * associated user context.
     *
     * @param token
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
        // This function is only called for connection less tokens defined in
        // guacamole.properties.vlt. Should probably refuse SSH and LDAP vault
        // secrets in that case, but the key can be generic without risk
        token = token.replaceFirst(":(LOWER|UPPER|OPTIONAL)$", "");
        String value = client().getValue(token, "", token.substring(0, token.lastIndexOf('/')));

        return CompletableFuture.completedFuture(value);
    }

    /**
     * Returns a Future which eventually completes with the value of the secret
     * having the given name. If no such secret exists, the Future will be
     * completed with null. The secrets retrieved from this method are independent
     * of the context of the particular connection being established, or any
     * associated user context.
     *
     * @param userContext
     *     The user context from which the connectable originated.
     *
     * @param connectable
     *     The connection or connection group for which the tokens are being replaced.
     *
     * @param token
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
    public Future<String> getValue(UserContext userContext, Connectable connectable, String token)
            throws GuacamoleException {
        GuacamoleConfiguration config;
        if (connectable instanceof Connection) {
            config = ((Connection) connectable).getConfiguration();
        }
        else {
            config = new GuacamoleConfiguration();
        }
        // Use a key including GUAC_USERNAME, to at least prevent a user stealing the
        // session of another due to timing issues. The ssh certificates of keys
        // of token from vault-token-mapping.yml must have an explicit username associated
        // with it
        String username = config.getParameter("username");
        String guac_username = userContext.self().getIdentifier();
        String key = guac_username + "-" + username + "-" + token.substring(0, token.lastIndexOf('/'));
        token = token.replaceFirst(":(LOWER|UPPER|OPTIONAL)$", "");
        String value = client().getValue(prepareToken(token, userContext, config, new TokenFilter()), username, key);

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

        // Remove optional token parameter modifier
        Pattern tokenPattern = Pattern.compile("\\$\\{(" + client().VAULT_TOKEN_PREFIX +
                ".+?)(\\:(LOWER|UPPER|OPTIONAL))?\\}");

        // To keep the tokens for the same connection associated with each other in the
        // cache, for tokens that might create a confusion, we cache them with a shared
        // key
        String key = UUID.randomUUID().toString();

        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            Matcher tokenMatcher = tokenPattern.matcher(entry.getValue());
            while (tokenMatcher.find()) {
                String token = tokenMatcher.group(1);

                // Resolve any tokens in the username for use in possible ssh certificate
                String username = filter.filter(config.getParameter("username"));

                String value = client().getValue(prepareToken(token, userContext, config, filter), username, key);
                tokens.put(token, CompletableFuture.completedFuture(value));
            }
        }

        // Don't print secret values even at debug level. Still needed for testing
        // so keep it in comments. Please note the v.get() will cause this code to
        // wait for completion of the Future values.
        //logger.debug("Returning {} Vault tokens:", tokens.size());
        //tokens.forEach((k, v) -> {
        //    try {
        //        logger.debug("    {} : {}", k, v.get());
        //    } catch (Exception e) {
        //        logger.debug("    {} => ERROR: {}", k, e);
        //    }});

        // Simpler, innocuous debugging message
        logger.debug("Returning {} Vault tokens: {}", tokens.size(), tokens.keySet());

        return tokens;
    }
}
