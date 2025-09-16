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

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.google.common.base.Objects;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
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
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.apache.guacamole.token.TokenFilter;
import org.apache.guacamole.vault.hv.GuacamoleExceptionSupplier;
import org.apache.guacamole.vault.hv.conf.HvAttributeService;
import org.apache.guacamole.vault.hv.conf.HvConfigurationService;
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
    private static final Logger logger = LoggerFactory.getLogger(VaultSecretService.class);

    /**
     * Service for retrieving configuration information.
     */
    @Inject
    private HvConfigurationService confService;

    /**
     * Factory for creating HV client instances.
     */
    @Inject
    private HvClientFactory hvClientFactory;

    /**
     * A map of base-64 encoded JSON HV config blobs to associated HV client instances.
     * A distinct HV client will exist for every HV config.
     */
    private final ConcurrentMap<String, HvClient> hvClientMap = new ConcurrentHashMap<>();

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
    private HvClient getClient(@Nonnull String hvConfigBase64)
            throws GuacamoleException {

        // If a client already exists for the provided config, use it
        HvClient hvClient = hvClientMap.get(hvConfigBase64);
        if (hvClient != null)
            return hvClient;

        // Create and store a new HV client instance for the provided HV config blob
        Map<String, String> hvConfig = confService.parseHvConfig(hvConfigBase64);
        hvClient = hvClientFactory.create(hvConfig);
        HvClient prevClient = hvClientMap.putIfAbsent(hvConfigBase64, hvClient);

        // If the client was already set before this thread got there, use the existing one
        return prevClient != null ? prevClient : hvClient;
    }

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

    @Override
    public Future<String> getValue( UserContext userContext,
            Connectable connectable, String name) throws GuacamoleException {

        // Attempt to find a HV config for this connection or group
        String hvConfig = getConnectionGroupHvConfig(userContext, connectable);

        return getClient(hvConfig).getSecret(name, new GuacamoleExceptionSupplier<Future<String>>() {

            @Override
            public Future<String> get() throws GuacamoleException {

                // Get the user-supplied HV config, if allowed by config and
                // set by the user
                String userHvConfig = getUserHVConfig(userContext, connectable);

                // If the user config happens to be the same as admin-defined one,
                // don't bother trying again
                if (userHvConfig != null && !Objects.equal(userHvConfig, hvConfig))
                    return getClient(userHvConfig).getSecret(name);

                return CompletableFuture.completedFuture(null);
            }

        });
    }

    @Override
    public Future<String> getValue(String name) throws GuacamoleException {
        // Use the default HV configuration from guacamole.properties
        return getClient(confService.getHvConfig()).getSecret(name);
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
     *     The value of the HV configuration attribute if found in the tree, the default
     *     HV config blob defined in guacamole.properties otherwise.
     *
     * @throws GuacamoleException
     *     If an error occurs while attempting to retrieve the HV config attribute, or if
     *     no HV config is found in the connection group tree, and the value is also not
     *     defined in the config file.
     */
    @Nonnull
    private String getConnectionGroupHvConfig(UserContext userContext,
            Connectable connectable) throws GuacamoleException {

        // Check to make sure it's a usable type before proceeding
        if (!(connectable instanceof Connection) && !(connectable instanceof ConnectionGroup)) {
            logger.warn(
                "Unsupported Connectable type: {}; skipping HV config lookup.",
                connectable.getClass()
            );

            // Use the default value if searching is impossible
            return confService.getHvConfig();
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

            // If the current connection group has the HV configuration attribute
            // set to a non-empty value, return immediately
            String hvConfig = group.getAttributes().get(HvAttributeService.HV_CONFIGURATION_ATTRIBUTE);
            if (hvConfig != null && !hvConfig.trim().isEmpty())
                return hvConfig;

            // Otherwise, keep searching up the tree until an appropriate configuration is found
            parentIdentifier = group.getParentIdentifier();

            // If the parent is a group that's already been seen, this is a cycle, so there's no
            // need to search any further
            if (!observedIdentifiers.add(parentIdentifier))
                break;
        }


        // If no HV configuration was ever found, use the default value
        return confService.getHvConfig();
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
     *    The base64 encoded HV config blob for the current user if one
     *    exists, and if user HV configs are enabled globally and for the
     *    provided connectable.
     *
     * @throws GuacamoleException
     *     If an error occurs while attempting to fetch the HV config.
     */
    private String getUserHVConfig(UserContext userContext,
            Connectable connectable) throws GuacamoleException {

        return null;
    }

    @Override
    public Map<String, Future<String>> getTokens(UserContext userContext,
            Connectable connectable, GuacamoleConfiguration config,
            TokenFilter filter) throws GuacamoleException {

        Map<String, Future<String>> tokens = new HashMap<>();
        Map<String, String> parameters = config.getParameters();

        String hvConfigBase64 = getConnectionGroupHvConfig(userContext, connectable);
        HvClient client = getClient(hvConfigBase64);
        Pattern tokenPattern = Pattern.compile("\\$\\{(" + client.HASHICORP_VAULT_TOKEN_PREFIX + "[^}]+)\\}");

        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            Matcher tokenMatcher = tokenPattern.matcher(entry.getValue());
            while (tokenMatcher.find()) {
                String notation = tokenMatcher.group(1);
                tokens.put(notation, client.getSecret(notation));
            }
        }

        return tokens;

    }

}
