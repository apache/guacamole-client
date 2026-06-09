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
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
import org.apache.guacamole.vault.hv.conf.HvAttributeService;
import org.apache.guacamole.vault.hv.conf.HvConfigurationService;
import org.apache.guacamole.vault.hv.conf.HvConfigurationService.VaultInfo;
import org.apache.guacamole.vault.hv.user.HvDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Provider for a OpenBao/Hashicorp Vault client for a particular
 * configuration.
 */
@Singleton
public class HvClientProvider {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(HvClientProvider.class);

    /**
     * Service for retrieving configuration information.
     */
    private final HvConfigurationService confService;

    /**
     * Factory for creating HV client instances.
     */
    private final HvClientFactory hvClientFactory;

    /**
     * A map of HV VaultInfo configurations to associated HV client instances.
     * A distinct HV client will exist for every VaultInfo.
     */
    private static final ConcurrentMap<VaultInfo, HvClient> hvClientMap = new ConcurrentHashMap<>();


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
    public HvClientProvider(final HvConfigurationService confService,
                            final HvClientFactory hvClientFactory) {
        this.confService = confService;
        this.hvClientFactory = hvClientFactory;
    }

    /**
     * Create and return a HV client for the provided HV config if not already
     * present in the client map, otherwise return the existing client entry.
     *
     * @param vaultInfo
     *     The Vault configuration information associated with this instance.
     *     If an associated entry does not already exist, it will be created using
     *     this configuration.
     *
     * @return
     *     A HV client for the provided HV configuration if not already present in
     *     the client map, otherwise the existing client entry.
     *
     * @throws GuacamoleException
     *     If an error occurs while creating the HV client.
     */
    private HvClient getHvClient(final VaultInfo vaultInfo)
            throws GuacamoleException {
        if (vaultInfo.isVaultInfoInvalid()) {
            return null;
        }

        // If a client already exists for the provided config, use it
        HvClient hvClient = hvClientMap.get(vaultInfo);
        if (hvClient != null) {
            return hvClient;
        }

        // Create and store a new HV client instance for the provided HV config blob
        hvClient = hvClientFactory.create(vaultInfo);
        final HvClient prevClient = hvClientMap.putIfAbsent(vaultInfo, hvClient);

        // If the client was already set before this thread got there, use the existing one
        return prevClient != null ? prevClient : hvClient;
    }

    /**
     * Returns the application-wide HVClient if one exists, else null.
     *
     * @return
     *     The HVClient instances associated with the application-wide configuration
     *     attributes from guacamole.properties.
     *
     * @throws GuacamoleException
     *     If an error occurs while attempting to retrieve the HV configuration attribute.
     */
    public HvClient getAppHvClient() throws GuacamoleException {

        final VaultInfo vaultInfo = confService.new VaultInfo(
                confService.getVaultUri(),
                confService.getVaultToken(),
                confService.getVaultUsername(),
                confService.getVaultPassword());

        return getHvClient(vaultInfo);
    }

    /**
     * Search for HV configuration attributes, recursing up the connection group tree
     * until a connection group with the appropriate attribute is found. If the HV
     * configuration is found, the corresponding HVClient will be returned, else null.
     *
     * @param userContext
     *     The userContext associated with the connection or connection group.
     *
     * @param connectable
     *     A connection or connection group for which the tokens are being replaced.
     *
     * @return
     *     The value of the HVClient corresponding to the configuration attributes if
     *     found in the tree, else null
     *
     * @throws GuacamoleException
     *     If an error occurs while attempting to retrieve the HV configurations attributes,
     *     or if no HV configuration is found in the connection group tree, and the value is
     *     also not defined in the config file.
     */
    public HvClient getConnectionGroupHvClient(final UserContext userContext,
            final Connectable connectable) throws GuacamoleException {
        HvClient client = null;

        // Check to make sure it's a usable type before proceeding
        if ((connectable instanceof Connection) || (connectable instanceof ConnectionGroup)) {
            // For connections, start searching the parent group for the HV config
            // For connection groups, start searching the group directly
            String parentIdentifier = (connectable instanceof Connection)
                    ? ((Connection) connectable).getParentIdentifier()
                    : ((ConnectionGroup) connectable).getIdentifier();

            // Keep track of all group identifiers seen while recursing up the tree
            // in case there's a cycle - if the same identifier is ever seen twice,
            // the search is over.
            final Set<String> observedIdentifiers = new HashSet<>();

            // Use the unwrapped connection group directory to avoid HV config
            // value sanitization
            final Directory<ConnectionGroup> connectionGroupDirectory = (
                    (HvDirectory<ConnectionGroup>) userContext.getConnectionGroupDirectory()
                ).getUnderlyingDirectory();

            // If the parent is a group that's already been seen, this is a cycle, so
            // there's no need to search any further
            while (client == null && observedIdentifiers.add(parentIdentifier)) {
                // Fetch the parent group, if one exists
                final ConnectionGroup group = connectionGroupDirectory.get(parentIdentifier);
                if (group == null) {
                    break;
                }

                // If the current connection group has HV configuration attributes
                // set to a non-empty value, return immediately
                final Map<String, String> hvConfig = group.getAttributes();

                if (hvConfig.get(HvAttributeService.HV_URI_ATTRIBUTE) == null) {
                    break;
                }
                final VaultInfo vaultInfo = confService.new VaultInfo(
                        URI.create(hvConfig.get(HvAttributeService.HV_URI_ATTRIBUTE)),
                        hvConfig.get(HvAttributeService.HV_TOKEN_ATTRIBUTE),
                        hvConfig.get(HvAttributeService.HV_USERNAME_ATTRIBUTE),
                        hvConfig.get(HvAttributeService.HV_PASSWORD_ATTRIBUTE));

                client = getHvClient(vaultInfo);

                // Otherwise, keep searching up the tree until an appropriate
                // configuration is found
                parentIdentifier = group.getParentIdentifier();
            }
        }
        else {
            logger.warn(
                "Unsupported Connectable type: {}; skipping HV config lookup.",
                connectable.getClass()
            );
        }

        return client;
    }

    /**
     * Return the HVclient for the current user IFF User HV configuration
     * is enabled globally, and are enabled for the given connectable. If no
     * HV configuartion exists for the given user or HV configurations are not
     * enabled, null will be returned.
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
     *     The value of the user HVClient if found for the current user, else null.
     *
     * @throws GuacamoleException
     *     If an error occurs while attempting to fetch the HV config.
     */
    public HvClient getUserHvClient(final UserContext userContext,
            final Connectable connectable) throws GuacamoleException {
        HvClient client = null;

        // If user HV configs are enabled globally, and for the given connectable,
        // use the user-specific HV config, if one exists

        if (confService.allowUserConfig() && (connectable instanceof Attributes) &&
            HvAttributeService.TRUTH_VALUE.equals(((Attributes) connectable).getAttributes().get(
                HvAttributeService.HV_USER_CONFIG_ENABLED_ATTRIBUTE))) {

            // Get the underlying user
            final User self = ((HvDirectory<User>) userContext.getUserDirectory())
                    .getUnderlyingDirectory().get(userContext.self().getIdentifier());

            // If the current user has HV configuration attributes
            // set to a empty values, return immediately
            final Map<String, String> hvConfig = self.getAttributes();

            if (hvConfig.get(HvAttributeService.HV_URI_ATTRIBUTE) != null) {
                final VaultInfo vaultInfo = confService.new VaultInfo(
                        URI.create(hvConfig.get(HvAttributeService.HV_URI_ATTRIBUTE)),
                        hvConfig.get(HvAttributeService.HV_TOKEN_ATTRIBUTE),
                        hvConfig.get(HvAttributeService.HV_USERNAME_ATTRIBUTE),
                        hvConfig.get(HvAttributeService.HV_PASSWORD_ATTRIBUTE));

                client = getHvClient(vaultInfo);
            }
        }

        return client;
    }

    /**
     * Return an ordered list of non null HVClients. The application-wide
     * client is first if non null, followed by the ConnectionGroup client,
     * and finally the User client. This order is important to ensure that
     * the adminsitrator always has control of the injected tokens.
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
     *     The values of the HVClient corresponding to the application-wide,
     *     connectionGroup and User HVClients in an ordered list.
     *
     * @throws GuacamoleException
     *     If an error occurs while attempting to fetch the HV config.
     */
    public List<HvClient> getHvClients(final UserContext userContext,
            final Connectable connectable) throws GuacamoleException {
        final List<HvClient> clients = new ArrayList<>();

        // Create an application-wide client
        final HvClient client = getAppHvClient();
        if (client != null) {
            clients.add(client);
        }

        // Create a connection group client
        final HvClient connectionClient = getConnectionGroupHvClient(userContext, connectable);
        if (connectionClient != null) {
            clients.add(connectionClient);
        }

        // Configure per user client if configured
        final HvClient userClient = getUserHvClient(userContext, connectable);
        if (userClient != null) {
            clients.add(userClient);
        }

        return clients;
    }

    /**
     * The LDAP session interface checks out sessions that then can not be
     * used till they are checked in. This function searchs all HVClient
     * until one responds that it has treated the event.
     *
     * @param event
     *      A TunnelConnectEvent or TunnelCloseEvent
     */
    public static void treatLdapSession(final Object event) {
        if (event instanceof TunnelConnectEvent) {
            final String tunnelId = ((TunnelConnectEvent) event).getTunnel().getUUID().toString();
            for (final Map.Entry<VaultInfo, HvClient> entry : hvClientMap.entrySet()) {
                final HvClient client = entry.getValue();
                if (client.treatLdapSession(client.VAULT_LDAP_SESSION, tunnelId)) {
                    break;
                }
            }
        }
        else {
            final String tunnelId = ((TunnelCloseEvent) event).getTunnel().getUUID().toString();
            for (final Map.Entry<VaultInfo, HvClient> entry : hvClientMap.entrySet()) {
                final HvClient client = entry.getValue();
                if (client.treatLdapSession(tunnelId, null)) {
                    break;
                }
            }
        }
    }

    /**
     * Factory for creating HvClient instances.
     */
    public interface HvClientFactory {

        /**
         * Returns a new instance of a HvClient instance associated with
         * the provided HV configuration options and API interval.
         *
         * @param VaultInfo
         *      The Vault configuration information associated with the instance.
         *      to create.
         *
         * @return
         *     A new HvClient instance associated with the provided HV configuration
         *     options.
         */
        HvClient create(@Nonnull VaultInfo vaultInfo);

    }
}

