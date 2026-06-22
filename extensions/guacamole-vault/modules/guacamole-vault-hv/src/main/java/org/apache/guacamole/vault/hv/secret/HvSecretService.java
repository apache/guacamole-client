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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.Connectable;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.apache.guacamole.token.TokenFilter;
import org.apache.guacamole.vault.secret.VaultSecretService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service which retrieves secrets from OpenBao/Hashicorp Vault.
 * The configuration used to connect to HV can be set at a global
 * level using guacamole.properties, using a connection group
 * attribute, or users attributes.
 */
@Singleton
public class HvSecretService implements VaultSecretService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(HvSecretService.class);

    /**
     * Provider for HV client instances.
     */
    private final HvClientProvider hvClientProvider;

    /**
     * Public constructor for Guice, so that we can instantiate the existing
     * Vault clients early, to avoid problems with expiring tokens
     *
     * @param hvClientFactory
     *      Factory for creating HV client instances
     */
    @Inject
    public HvSecretService(final HvClientProvider hvClientProvider) {
        this.hvClientProvider = hvClientProvider;

        // Instantiate the HvClient early to start Token renewal of main Vault account.
        // FIXME: Don't have access to the root ConnectionGroup here, so can't instantiate
        // the per ConnectionGroup vaults, which MUST have a non expiring means of
        // authentication to avoid issues
        try {
            hvClientProvider.getAppHvClient();
        }
        catch (GuacamoleException e) {
            logger.error("Can't initialize HvClient : {}", e.getMessage());
        }
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
    public String canonicalize(final String nameComponent) {

        // As HV notation is essentially a URL, encode all components
        // using standard URL escaping
        return URLEncoder.encode(nameComponent, StandardCharsets.UTF_8);
    }

    /**
     * Helper function for prepareToken that that decides if a sub-token
     * exists in the token and if it should be replaced
     *
     * @param oldtoken
     *      The token string potentially containing a sub-token to replace
     *
     * @param placeholder
     *      The sub-token value to look for. For example "{USERNAME}"
     *
     * @param filter
     *      A TokenFilter to use of the value if the sub-token itself is a token
     *
     *  @return
     *      The token with the sub-token replaced with its value
     */
    private String applyToken(final String oldtoken, String value,
            final String placeholder, final TokenFilter filter) {
        String token = oldtoken;

        // FIXME There is an edge case for tokens like
        //     vault://ldap/$${USERNAME}/{USERNAME}/password
        // This seems a pretty unlikely case, so don't treat.
        if (value != null && !value.isEmpty()) {
            value = filter.filter(value);
            if (!value.contains("${") && token.contains(placeholder) && !token.contains("$$" + placeholder)) {
                token = token.replace(placeholder, value);
            }
        }
        return token;
    }

    /**
     * Before going further replace the arguments "{GUAC_USERNAME}","{USERNAME}",
     * "{HOSTNAME}", "{GATEWAY_USERNAME}" and "{GATEWAY_HOSTNAME}" in the token with
     * values supplied in the parameters.
     *
     * @param oldtoken
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
     *     A token with the sub-tokens included replaced.
     */
    private String prepareToken(final String oldtoken, final UserContext userContext,
            final GuacamoleConfiguration config, final TokenFilter filter) {
        String token = oldtoken;

        final String guacUsername = userContext == null ? "" : userContext.self().getIdentifier();
        token = applyToken(token, guacUsername, "{GUAC_USERNAME}", filter);
        token = applyToken(token, config.getParameter("hostname"), "{HOSTNAME}", filter);
        token = applyToken(token, config.getParameter("username"), "{USERNAME}", filter);
        token = applyToken(token, config.getParameter("gateway-hostname"), "{GATEWAY}", filter);
        token = applyToken(token, config.getParameter("gateway-username"), "{GATEWAY_USER}", filter);

        return token;
    }

    /**
     * Returns a Future which eventually completes with the value of the secret
     * having the given name. If no such secret exists, the Future will be
     * completed with null.
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
    public Future<String> getValue(final UserContext userContext,
            final Connectable connectable, final String name) throws GuacamoleException {
        final GuacamoleConfiguration config;
        if (connectable instanceof Connection) {
            config = ((Connection) connectable).getConfiguration();
        }
        else {
            config = new GuacamoleConfiguration();
        }

        // An ordered array of non null HvClients
        final List<HvClient> clients = hvClientProvider.getHvClients(userContext, connectable);
        if (clients.size() == 0) {
            return null;
        }

        // Use a key including GUAC_USERNAME, to at least prevent a user stealing the
        // session of another due to timing issues. The ssh certificates of tokens
        // from vault-token-mapping.yml must have an explicit username associated
        // with it
        final String username = config.getParameter("username");
        final String guacUsername = userContext.self().getIdentifier();
        final String finalName = prepareToken(name.replaceFirst(":(LOWER|UPPER|OPTIONAL)$", ""),
                userContext, config, new TokenFilter());
        final String key = guacUsername + "-" + username + "-" +
                finalName.substring(0, finalName.lastIndexOf('/'));

        return resolveSecret(clients, finalName, username, key);
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
    public Future<String> getValue(final String token) throws GuacamoleException {
        // Ensure modifiers are removed
        final String name = token.replaceFirst(":(LOWER|UPPER|OPTIONAL)$", "");

        final HvClient client = hvClientProvider.getAppHvClient();
        if (client != null) {
            return client.getSecret(name, "", name.substring(0, name.lastIndexOf('/')));
        }
        else {
            return null;
        }
    }

    /*
     * Returns a map of token names to corresponding Futures which eventually
     * complete with the value of that token, where each token is dynamically
     * defined based on connection parameters.
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
    public Map<String, Future<String>> getTokens(final UserContext userContext,
            final Connectable connectable, final GuacamoleConfiguration config,
            final TokenFilter filter) throws GuacamoleException {

        final Map<String, Future<String>> tokens = new ConcurrentHashMap<>();

        // An ordered array of non null HvClients
        final List<HvClient> clients = hvClientProvider.getHvClients(userContext, connectable);
        if (clients.size() == 0) {
            return tokens;
        }

        // Remove optional token parameter modifier and match only our own tokens
        final Pattern tokenPattern = Pattern.compile("\\$\\{(" + HvClient.VAULT_TOKEN_PREFIX  +
                "(?:[^{}:]|:(?!(?:LOWER|UPPER|OPTIONAL)(?=\\}))|\\{(?:[^{}]|\\{[^{}]*\\})*\\})+" +
                ")(:(?:(LOWER|UPPER|OPTIONAL))(?=\\}))?\\}");

        // To keep the tokens for the same connection associated with each other in the
        // cache, for tokens that might create a confusion, we cache them with a shared
        // key
        final String key = UUID.randomUUID().toString();

        // Resolve any tokens in the username for use in possible ssh certificate
        final String username = filter.filter(config.getParameter("username"));

        final Map<String, String> parameters = config.getParameters();
        for (final Map.Entry<String, String> entry : parameters.entrySet()) {
            final Matcher tokenMatcher = tokenPattern.matcher(entry.getValue());
            while (tokenMatcher.find()) {
                final String notation = tokenMatcher.group(1);
                final String finalName = prepareToken(notation, userContext, config, filter);

                tokens.put(notation, resolveSecret(clients, finalName, username, key + 
                        finalName.substring(0, finalName.lastIndexOf('/'))));
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
        //        logger.debug("    {} => ERROR: {}", k, e.getMessage());
        //    }});

        // Simpler, innocuous debugging message
        logger.debug("Returning {} Vault tokens: {}", tokens.size(), tokens.keySet());

        return tokens;

    }

    /**
     * Given a List of possible HvClients to use, attempt to resolve a secret value
     * with the HvClient that returns a value
     *
     * @param clients
     *      An ordered list of HvClient with application-wide client, ConnectionGroup
     *      client followed by User client values, to ensure the adminsitrator always
     *      has the last word on token resolution
     *
     * @param FinalName
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
     */
    private Future<String> resolveSecret(final List<HvClient> clients, final String finalName,
            final String username, final String key) throws GuacamoleException {
        if (clients.size() == 0) {
            return CompletableFuture.<String>completedFuture(null);
        }
        return clients.get(0).getSecret(finalName, username, key)
            .handle((value, ex) -> {
                if (ex == null) {
                    return CompletableFuture.<String>completedFuture(value);
                }

                if (clients.size() > 1) {
                    try {
                        return clients.get(1).getSecret(finalName, username, key);
                    } catch (GuacamoleException e) {
                        return CompletableFuture.<String>failedFuture(e);
                    }
                }

                return CompletableFuture.<String>failedFuture(ex);
            })
            .thenCompose(Function.identity())
            .handle((value, ex) -> {
                if (ex == null) {
                    return CompletableFuture.<String>completedFuture(value);
                }

                if (clients.size() > 2) {
                    try {
                        return clients.get(2).getSecret(finalName, username, key);
                    } catch (GuacamoleException e) {
                        return CompletableFuture.<String>failedFuture(e);
                    }
                }

                return CompletableFuture.<String>completedFuture(null);
            })
            .thenCompose(Function.identity());
    }
}
