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

import com.google.common.base.Objects;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.Attributes;
import org.apache.guacamole.net.auth.Connectable;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.ConnectionGroup;
import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.net.auth.User;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.net.event.TunnelCloseEvent;
import org.apache.guacamole.net.event.TunnelConnectEvent;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.apache.guacamole.token.TokenFilter;
import org.apache.guacamole.vault.hv.GuacamoleExceptionSupplier;
import org.apache.guacamole.vault.hv.conf.HvAttributeService;
import org.apache.guacamole.vault.hv.conf.HvConfigurationService;
import org.apache.guacamole.vault.hv.conf.HvConfigurationService.VaultInfo;
import org.apache.guacamole.vault.hv.user.HvDirectory;
import org.apache.guacamole.vault.secret.VaultSecretService;
import org.apache.guacamole.vault.secret.WindowsUsername;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service which retrieves secrets from Hashicorp Vault.
 * The configuration used to connect to HV can be set at a global
 * level using guacamole.properties, or using a connection group
 * attribute.
 */
@Singleton
public class HvSecretService implements VaultSecretService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(HvSecretService.class);

    /**
     * Service for retrieving configuration information.
     */
    private HvConfigurationService confService;

    /**
     * Factory for creating HV client instances.
     */
    private HvClientFactory hvClientFactory;

    /**
     * Public constructor for Guice, so that we can instantiate the existing
     * Vault clients early, to avoid problems with expiring tokens
     *
     * @param confService
     *      Service for retrieving configuration information
     *
     * @param hvClientFactory
     *      Factory for creating HV client instances
     */
    @Inject
    public HvSecretService(HvConfigurationService confService, HvClientFactory hvClientFactory) {
        this.confService = confService;
        this.hvClientFactory = hvClientFactory;

        // Instantiate the HvClient early to start Token renewal of main Vault account.
        // FIXME: Don't have access to the root ConnectionGroup here, so can't instantiate
        // the per ConnectionGroup vaults, which MUST have a non expiring means of
        // authentication to avoid issues
        try {
            VaultInfo vaultInfo = confService.new VaultInfo(confService.getVaultUri(),
                    confService.getVaultToken(),
                    confService.getVaultUsername(),
                    confService.getVaultPassword());
            if (isVaultInfoValid(vaultInfo)) {
                HvClient client = getClient(confService.new VaultInfo(confService.getVaultUri(),
                        confService.getVaultToken(),
                        confService.getVaultUsername(),
                        confService.getVaultPassword()));
            }
        }
        catch (GuacamoleException e) {
            logger.error("Can't initialize HvClient : {}", e.getMessage());
        }
    }

    /**
     * A map of HV VaultInfo configurations to associated HV client instances.
     * A distinct HV client will exist for every VaultInfo.
     */
    static private final ConcurrentMap<VaultInfo, HvClient> hvClientMap = new ConcurrentHashMap<>();

    /**
     * Create and return a HV client for the provided HV config if not already
     * present in the client map, otherwise return the existing client entry.
     *
     * @param hvConfig
     *     The base-64 encoded JSON HV config blob associated with the client entry.
     *     If an associated entry does not already exist, it will be created using
     *     this configuration.
     *
     * @return
     *     A HV client for the provided HV config if not already present in the
     *     client map, otherwise the existing client entry.
     *
     * @throws GuacamoleException
     *     If an error occurs while creating the HV client.
     */
    private HvClient getClient(@Nonnull VaultInfo vaultInfo)
            throws GuacamoleException {
        // If a client already exists for the provided config, use it
        HvClient hvClient = hvClientMap.get(vaultInfo);
        if (hvClient != null)
            return hvClient;

        // Create and store a new HV client instance for the provided HV config blob
        hvClient = hvClientFactory.create(vaultInfo);
        HvClient prevClient = hvClientMap.putIfAbsent(vaultInfo, hvClient);

        // If the client was already set before this thread got there, use the existing one
        return prevClient != null ? prevClient : hvClient;
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

            // As HV notation is essentially a URL, encode all components
            // using standard URL escaping
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
    public Future<String> getValue( UserContext userContext,
            Connectable connectable, String name) throws GuacamoleException {
        GuacamoleConfiguration config;
        if (connectable instanceof Connection) {
            config = ((Connection) connectable).getConfiguration();
        }
        else {
            config = new GuacamoleConfiguration();
        }

        // Create an application wide client
        VaultInfo vaultInfo = confService.new VaultInfo(confService.getVaultUri(),
                confService.getVaultToken(),
                confService.getVaultUsername(),
                confService.getVaultPassword());
        HvClient client;
        if (isVaultInfoValid(vaultInfo))
            client = getClient(vaultInfo);
        else
            client = null;

        // Create a connection group client
        HvClient connectionClient  = getConnectionGroupHvClient(userContext, connectable);

        // Configure per user client if configured
        HvClient userClient = getUserHvClient(userContext, connectable);

        // Use a key including GUAC_USERNAME, to at least prevent a user stealing the
        // session of another due to timing issues. The ssh certificates of keys
        // of token from vault-token-mapping.yml must have an explicit username associated
        // with it
        final String username = config.getParameter("username");
        final String guac_username = userContext.self().getIdentifier();
        final String finalName = prepareToken(name.replaceFirst(":(LOWER|UPPER|OPTIONAL)$", ""),
                userContext, config, new TokenFilter());
        final String key = guac_username + "-" + username + "-" +
                finalName.substring(0, finalName.lastIndexOf('/'));

        if (client == null) {
            if (connectionClient == null) {
                if (userClient == null) {
                    return null;
                }
                else {
                    return userClient.getSecret(finalName, username, key);
                }
            }
            else {
                return connectionClient.getSecret(finalName, username, key)
                    .handle((value, ex) -> {
                        if (ex == null) {
                            return CompletableFuture.<String>completedFuture(value);
                        }

                        if (userClient != null) {
                            try {
                                return userClient.getSecret(finalName, username, key);
                            } catch (GuacamoleException e) {
                                return CompletableFuture.<String>failedFuture(e);
                            }
                        }

                        return CompletableFuture.<String>completedFuture(null);
                    })
                    .thenCompose(Function.identity());
            }
        }
        else {
            return client.getSecret(finalName, username, key)
                .handle((value, ex) -> {
                    if (ex == null) {
                        return CompletableFuture.<String>completedFuture(value);
                    }

                    if (connectionClient != null) {
                        try {
                            return connectionClient.getSecret(finalName, username, key);
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

                    if (userClient != null) {
                        try {
                            return userClient.getSecret(finalName, username, key);
                        } catch (GuacamoleException e) {
                            return CompletableFuture.<String>failedFuture(e);
                        }
                    }

                    return CompletableFuture.<String>completedFuture(null);
                })
                .thenCompose(Function.identity());
        }
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
    public Future<String> getValue(String name) throws GuacamoleException {
        // Ensure modifiers are removed
        name = name.replaceFirst(":(LOWER|UPPER|OPTIONAL)$", "");

        // Use the default HV configuration from guacamole.properties
        VaultInfo vaultInfo = confService.new VaultInfo(confService.getVaultUri(),
                confService.getVaultToken(),
                confService.getVaultUsername(),
                confService.getVaultPassword());
        if (isVaultInfoValid(vaultInfo))
            return getClient(vaultInfo).getSecret(name, "", name.substring(0, name.lastIndexOf('/')));
        else
            return null;
    }

    /**
     * Returns true if the VaultInfo configuration seems valid
     *
     * @param vaultInfo
     *      The VaultInfo variable to test
     *
     * @return
     *      True is the value in non null, URI is set and at least one of
     *      token or username/password id set
     */
     private Boolean isVaultInfoValid(VaultInfo vaultInfo) {
        if (vaultInfo == null || vaultInfo.Uri == null || vaultInfo.Uri.toString().trim().isEmpty())
            return false;
        if (vaultInfo.Token != null && !vaultInfo.Token.trim().isEmpty())
            return true;
        if (vaultInfo.Username == null || vaultInfo.Username.trim().isEmpty())
            return false;
        if (vaultInfo.Password != null && !vaultInfo.Password.trim().isEmpty())
            return true;
        return false;
     }

    /**
     * Search for a HV configuration attribute, recursing up the connection group tree
     * until a connection group with the appropriate attribute is found. If the HV config
     * is found, it will be returned. If not, the default value from the config file will
     * be returned.
     *
     * @param userContext
     *     The userContext associated with the connection or connection group.
     *
     * @param connectable
     *     A connection or connection group for which the tokens are being replaced.
     *
     * @return
     *     The value of the HV configuration attributes if found in the tree, the default
     *     HV config defined in guacamole.properties otherwise.
     *
     * @throws GuacamoleException
     *     If an error occurs while attempting to retrieve the HV config attribute, or if
     *     no HV config is found in the connection group tree, and the value is also not
     *     defined in the config file.
     */
    @Nonnull
    private  HvClient getConnectionGroupHvClient(UserContext userContext,
            Connectable connectable) throws GuacamoleException {

        // Check to make sure it's a usable type before proceeding
        if (!(connectable instanceof Connection) && !(connectable instanceof ConnectionGroup)) {
            logger.warn(
                "Unsupported Connectable type: {}; skipping HV config lookup.",
                connectable.getClass()
            );

            // Use the default value if searching is impossible
            return getClient(confService.new VaultInfo(confService.getVaultUri(),
                    confService.getVaultToken(),
                    confService.getVaultUsername(),
                    confService.getVaultPassword()));
        }

        // For connections, start searching the parent group for the HV config
        // For connection groups, start searching the group directly
        String parentIdentifier = (connectable instanceof Connection)
                ? ((Connection) connectable).getParentIdentifier()
                : ((ConnectionGroup) connectable).getIdentifier();

        // Keep track of all group identifiers seen while recursing up the tree
        // in case there's a cycle - if the same identifier is ever seen twice,
        // the search is over.
        Set<String> observedIdentifiers = new HashSet<>();
        observedIdentifiers.add(parentIdentifier);

        // Use the unwrapped connection group directory to avoid HV config
        // value sanitization
        Directory<ConnectionGroup> connectionGroupDirectory = (
                (HvDirectory<ConnectionGroup>) userContext.getConnectionGroupDirectory()
            ).getUnderlyingDirectory();

        while (true) {
            // Fetch the parent group, if one exists
            ConnectionGroup group = connectionGroupDirectory.get(parentIdentifier);
            if (group == null)
                break;

            // If the current connection group has HV configuration attributes
            // set to a non-empty value, return immediately
            Map<String, String> hvConfig = group.getAttributes();

            if (hvConfig.get(HvAttributeService.HV_URI_ATTRIBUTE) == null)
                break;

            VaultInfo vaultInfo = confService.new VaultInfo(
                    URI.create(hvConfig.get(HvAttributeService.HV_URI_ATTRIBUTE)),
                    hvConfig.get(HvAttributeService.HV_TOKEN_ATTRIBUTE),
                    hvConfig.get(HvAttributeService.HV_USERNAME_ATTRIBUTE),
                    hvConfig.get(HvAttributeService.HV_PASSWORD_ATTRIBUTE)
            );

            if (isVaultInfoValid(vaultInfo))
                return getClient(vaultInfo);

            // Otherwise, keep searching up the tree until an appropriate configuration is found
            parentIdentifier = group.getParentIdentifier();

            // If the parent is a group that's already been seen, this is a cycle, so there's no
            // need to search any further
            if (!observedIdentifiers.add(parentIdentifier))
                break;
        }

        // If no HV configuration was ever found, use the default value
        return getClient(confService.new VaultInfo(confService.getVaultUri(),
                confService.getVaultToken(),
                confService.getVaultUsername(),
                confService.getVaultPassword()));
    }

    /**
     * Returns true if user-level HV configuration is enabled for the given
     * Connectable, false otherwise.
     *
     * @param connectable
     *     The connectable to check for whether user-level HV configs are
     *     enabled.
     *
     * @return
     *     True if user-level HV configuration is enabled for the given
     *     Connectable, false otherwise.
     */
    private boolean isHvUserConfigEnabled(Connectable connectable) {

        // User-level config is enabled IFF the appropriate attribute is set to true
        if (connectable instanceof Attributes)
            return HvAttributeService.TRUTH_VALUE.equals(((Attributes) connectable).getAttributes().get(
                HvAttributeService.HV_USER_CONFIG_ENABLED_ATTRIBUTE));

        // If there's no attributes to check, the user config cannot be enabled
        return false;

    }

    /**
     * Return the HV config blob for the current user IFF user HV configs
     * are enabled globally, and are enabled for the given connectable. If no
     * HV config exists for the given user or HV configs are not enabled,
     * null will be returned.
     *
     * @param userContext
     *    The user context from which the current user should be fetched.
     *
     * @param connectable
     *    The connectable to which the connection is being established. This
     *    is the connection which will be checked to see if user HV configs
     *    are enabled.
     *
   * @return
     *     The value of the user HV configuration attributes if found in the tree,
     *      the default HV config defined in guacamole.properties otherwise.
     *
     * @throws GuacamoleException
     *     If an error occurs while attempting to fetch the HV config.
     */
    private HvClient getUserHvClient(UserContext userContext,
            Connectable connectable) throws GuacamoleException {

        // If user HV configs are enabled globally, and for the given connectable,
        // return the user-specific HV config, if one exists
        if (confService.getAllowUserConfig() && isHvUserConfigEnabled(connectable)) {

            // Get the underlying user, to avoid the KSM config sanitization
            User self = (((HvDirectory<User>) userContext.getUserDirectory())
                    .getUnderlyingDirectory().get(userContext.self().getIdentifier()));

            // If the current user has HV configuration attributes
            // set to a non-empty value, return immediately
            Map<String, String> hvConfig = self.getAttributes();

            if (hvConfig.get(HvAttributeService.HV_URI_ATTRIBUTE) != null) {
                VaultInfo vaultInfo = confService.new VaultInfo(
                        URI.create(hvConfig.get(HvAttributeService.HV_URI_ATTRIBUTE)),
                        hvConfig.get(HvAttributeService.HV_TOKEN_ATTRIBUTE),
                        hvConfig.get(HvAttributeService.HV_USERNAME_ATTRIBUTE),
                        hvConfig.get(HvAttributeService.HV_PASSWORD_ATTRIBUTE)
                );

                if (isVaultInfoValid(vaultInfo)) {
                    logger.debug("Using User Vault configuration");
                    return getClient(vaultInfo);
                }
            }
        }

        return null;
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

        // Create an application wide client
        VaultInfo vaultInfo = confService.new VaultInfo(confService.getVaultUri(),
                confService.getVaultToken(),
                confService.getVaultUsername(),
                confService.getVaultPassword());
        HvClient client = getClient(vaultInfo);

        // Create a connection group client
        HvClient connectionClient = getConnectionGroupHvClient(userContext, connectable);

        // Configure per user client if configured
        HvClient userClient = getUserHvClient(userContext, connectable);

        // Remove optional token parameter modifier and match only our own tokens
        Pattern tokenPattern = Pattern.compile("\\$\\{(" + client.VAULT_TOKEN_PREFIX  +
                "(?:[^{}:]|:(?!(?:LOWER|UPPER|OPTIONAL)(?=\\}))|\\{(?:[^{}]|\\{[^{}]*\\})*\\})+" +
                ")(:(?:(LOWER|UPPER|OPTIONAL))(?=\\}))?\\}");

        // To keep the tokens for the same connection associated with each other in the
        // cache, for tokens that might create a confusion, we cache them with a shared
        // key
        String key = UUID.randomUUID().toString();

        // Resolve any tokens in the username for use in possible ssh certificate
        String username = filter.filter(config.getParameter("username"));

        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            Matcher tokenMatcher = tokenPattern.matcher(entry.getValue());
            while (tokenMatcher.find()) {
                String notation = tokenMatcher.group(1);
                String finalName = prepareToken(notation, userContext, config, filter);

                if (client == null) {
                    if (connectionClient == null) {
                        if (userClient == null) {
                            tokens.put(notation, null);
                        }
                        else {
                            tokens.put(notation, userClient.getSecret(finalName, username, key));
                        }
                    }
                    else {
                        tokens.put(notation, connectionClient.getSecret(finalName, username, key)
                            .handle((value, ex) -> {
                                if (ex == null) {
                                    return CompletableFuture.<String>completedFuture(value);
                                }

                                if (userClient != null) {
                                    try {
                                        return userClient.getSecret(finalName, username, key);
                                    } catch (GuacamoleException e) {
                                        return CompletableFuture.<String>failedFuture(e);
                                    }
                                }

                                return CompletableFuture.<String>completedFuture(null);
                            })
                            .thenCompose(Function.identity()));
                    }
                }
                else {
                    tokens.put(notation, client.getSecret(finalName, username, key)
                        .handle((value, ex) -> {
                            if (ex == null) {
                                return CompletableFuture.<String>completedFuture(value);
                            }

                            if (connectionClient != null) {
                                try {
                                    return connectionClient.getSecret(finalName, username, key);
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

                            if (userClient != null) {
                                try {
                                    return userClient.getSecret(finalName, username, key);
                                } catch (GuacamoleException e) {
                                    return CompletableFuture.<String>failedFuture(e);
                                }
                            }

                            return CompletableFuture.<String>completedFuture(null);
                        })
                        .thenCompose(Function.identity()));
                }
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
     * The LDAP session interface checks out sessions that then can not be
     * used till they are checked in. In getTokens we have the problem that
     * we don't have access to the tunnel ID and so can't identify it.
     * Guacamole is also not guarenteed to generate a TunnelCloseEvent.
     *
     * So this function is fragile and relies on the fact that the TunnelConnectEvent
     * will be running a few tens of milliseconds after the getTokens command to
     * limit the risk of confusing two connections. There is still a small risk
     * of error here. This needs a better solution
     *
     * @param event
     *      A TunnelConnectEvent or TunnelCloseEvent
     */
    static public void treatLdapSession(Object event) {
        if (event instanceof TunnelConnectEvent) {
            String id = ((TunnelConnectEvent) event).getTunnel().getUUID().toString();
            for (Map.Entry<VaultInfo, HvClient> entry : hvClientMap.entrySet()) {
                HvClient client = entry.getValue();
                if (client.treatLdapSession(client.VAULT_LDAP_SESSION, id)) {
                    break;
                }
            }
        }
        else {
            String id = ((TunnelCloseEvent) event).getTunnel().getUUID().toString();
            for (Map.Entry<VaultInfo, HvClient> entry : hvClientMap.entrySet()) {
                HvClient client = entry.getValue();
                if (client.treatLdapSession(id, null)) {
                    break;
                }
            }
        }
    }
}
