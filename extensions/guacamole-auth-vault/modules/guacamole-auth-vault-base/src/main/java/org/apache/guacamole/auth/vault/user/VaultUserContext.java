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

package org.apache.guacamole.auth.vault.user;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import java.util.HashMap;
import java.util.Map;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.vault.conf.VaultConfigurationService;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.ConnectionGroup;
import org.apache.guacamole.net.auth.TokenInjectingUserContext;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.auth.vault.secret.VaultSecretService;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.apache.guacamole.token.GuacamoleTokenUndefinedException;
import org.apache.guacamole.token.TokenFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UserContext implementation which automatically injects tokens containing the
 * values of secrets retrieved from a vault.
 */
public class VaultUserContext extends TokenInjectingUserContext {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(VaultUserContext.class);

    /**
     * The name of the token which will be replaced with the username of the
     * current user if specified within the name of a secret. This token
     * applies to both connections and connection groups.
     */
    private static final String USERNAME_TOKEN = "GUAC_USERNAME";

    /**
     * The name of the token which will be replaced with the name of the
     * current connection group if specified within the name of a secret. This
     * token only applies only to connection groups.
     */
    private static final String CONNECTION_GROUP_NAME_TOKEN = "CONNECTION_GROUP_NAME";

    /**
     * The name of the token which will be replaced with the identifier of the
     * current connection group if specified within the name of a secret. This
     * token only applies only to connection groups.
     */
    private static final String CONNECTION_GROUP_IDENTIFIER_TOKEN = "CONNECTION_GROUP_ID";

    /**
     * The name of the token which will be replaced with the \"hostname\"
     * connection parameter of the current connection if specified within the
     * name of a secret. This token only applies only to connections.
     */
    private static final String CONNECTION_HOSTNAME_TOKEN = "CONNECTION_HOSTNAME";

    /**
     * The name of the token which will be replaced with the \"username\"
     * connection parameter of the current connection if specified within the
     * name of a secret. This token only applies only to connections.
     */
    private static final String CONNECTION_USERNAME_TOKEN = "CONNECTION_USERNAME";

    /**
     * The name of the token which will be replaced with the name of the
     * current connection if specified within the name of a secret. This token
     * only applies only to connections.
     */
    private static final String CONNECTION_NAME_TOKEN = "CONNECTION_NAME";

    /**
     * The name of the token which will be replaced with the identifier of the
     * current connection if specified within the name of a secret. This token
     * only applies only to connections.
     */
    private static final String CONNECTION_IDENTIFIER_TOKEN = "CONNECTION_ID";

    /**
     * Service for retrieving configuration information.
     */
    @Inject
    private VaultConfigurationService confService;

    /**
     * Service for retrieving the values of secrets stored in a vault.
     */
    @Inject
    private VaultSecretService secretService;

    /**
     * Creates a new VaultUserContext which automatically injects tokens
     * containing values of secrets retrieved from a vault. The given
     * UserContext is decorated such that connections and connection groups
     * will receive additional tokens during the connection process.
     *
     * Note that this class depends on concrete implementations of the
     * following classes to be provided via dependency injection:
     *
     *     - VaultConfigurationService
     *     - VaultSecretService
     *
     * Bindings providing these concrete implementations will need to be
     * provided by subclasses of VaultAuthenticationProviderModule for each
     * supported vault.
     *
     * @param userContext
     *     The UserContext instance to decorate.
     */
    @AssistedInject
    public VaultUserContext(@Assisted UserContext userContext) {
        super(userContext);
    }

    /**
     * Creates a new TokenFilter instance with token values set for all tokens
     * which are not specific to connections or connection groups. Currently,
     * this is only the username token ("GUAC_USERNAME").
     *
     * @return
     *     A new TokenFilter instance with token values set for all tokens
     *     which are not specific to connections or connection groups.
     */
    private TokenFilter createFilter() {
        TokenFilter filter = new TokenFilter();
        filter.setToken(USERNAME_TOKEN, self().getIdentifier());
        return filter;
    }

    /**
     * Retrieve all applicable tokens and corresponding values from the vault,
     * using the given TokenFilter to filter tokens within the secret names
     * prior to retrieving those secrets.
     *
     * @param tokenMapping
     *     The mapping dictating the name of the secret which maps to each
     *     parameter token, where the key is the name of the parameter token
     *     and the value is the name of the secret. The name of the secret
     *     may contain its own tokens, which will be substituted using values
     *     from the given filter.
     *
     * @param filter
     *     The filter to use to substitute values for tokens in the names of
     *     secrets to be retrieved from the vault.
     *
     * @return
     *     The tokens which should be added to the in-progress call to
     *     connect().
     *
     * @throws GuacamoleException
     *     If the value for any applicable secret cannot be retrieved from the
     *     vault due to an error.
     */
    private Map<String, String> getTokens(Map<String, String> tokenMapping,
            TokenFilter filter) throws GuacamoleException {

        Map<String, String> tokens = new HashMap<>();

        // Populate map with tokens containing the values of all secrets
        // indicated in the token mapping
        for (Map.Entry<String, String> entry : tokenMapping.entrySet()) {

            // Translate secret pattern into secret name, ignoring any
            // secrets which cannot be translated
            String secretName;
            try {
                secretName = secretService.canonicalize(filter.filterStrict(entry.getValue()));
            }
            catch (GuacamoleTokenUndefinedException e) {
                logger.debug("Secret for token \"{}\" will not be retrieved. "
                        + "Token \"{}\" within mapped secret name has no "
                        + "defined value in the current context.",
                        entry.getKey(), e.getTokenName());
                continue;
            }

            // If a value is defined for the secret in question, store that
            // value under the mapped token
            String tokenName = entry.getKey();
            String secretValue = secretService.getValue(secretName);
            if (secretValue != null) {
                tokens.put(tokenName, secretValue);
                logger.debug("Token \"{}\" populated with value from "
                        + "secret \"{}\".", tokenName, secretName);
            }
            else
                logger.debug("Token \"{}\" not populated. Mapped "
                        + "secret \"{}\" has no value.",
                        tokenName, secretName);

        }

        return tokens;

    }

    @Override
    protected Map<String, String> getTokens(ConnectionGroup connectionGroup)
            throws GuacamoleException {

        String name = connectionGroup.getName();
        String identifier = connectionGroup.getIdentifier();
        logger.debug("Injecting tokens from vault for connection group "
                + "\"{}\" (\"{}\").", identifier, name);

        // Add general and connection-group-specific tokens
        TokenFilter filter = createFilter();
        filter.setToken(CONNECTION_GROUP_NAME_TOKEN, name);
        filter.setToken(CONNECTION_GROUP_IDENTIFIER_TOKEN, identifier);

        // Substitute tokens producing secret names, retrieving and storing
        // those secrets as parameter tokens
        return getTokens(confService.getTokenMapping(), filter);

    }

    @Override
    protected Map<String, String> getTokens(Connection connection)
            throws GuacamoleException {

        String name = connection.getName();
        String identifier = connection.getIdentifier();
        logger.debug("Injecting tokens from vault for connection \"{}\" "
                + "(\"{}\").", identifier, name);

        // Add general and connection-specific tokens
        TokenFilter filter = createFilter();
        filter.setToken(CONNECTION_NAME_TOKEN, connection.getName());
        filter.setToken(CONNECTION_IDENTIFIER_TOKEN, identifier);

        // Add hostname and username tokens if available (implementations are
        // not required to expose connection configuration details)

        GuacamoleConfiguration config = connection.getConfiguration();

        String hostname = config.getParameter("hostname");
        if (hostname != null)
            filter.setToken(CONNECTION_HOSTNAME_TOKEN, hostname);
        else
            logger.debug("Hostname for connection \"{}\" (\"{}\") not "
                    + "available. \"{}\" token will not be populated in "
                    + "secret names.", identifier, name,
                    CONNECTION_HOSTNAME_TOKEN);

        String username = config.getParameter("username");
        if (username != null)
            filter.setToken(CONNECTION_USERNAME_TOKEN, username);
        else
            logger.debug("Username for connection \"{}\" (\"{}\") not "
                    + "available. \"{}\" token will not be populated in "
                    + "secret names.", identifier, name,
                    CONNECTION_USERNAME_TOKEN);

        // Substitute tokens producing secret names, retrieving and storing
        // those secrets as parameter tokens
        return getTokens(confService.getTokenMapping(), filter);

    }

}
