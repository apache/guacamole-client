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

package org.apache.guacamole.auth.ldap.connection;

import com.google.inject.Inject;
import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPReferralException;
import com.novell.ldap.LDAPSearchResults;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import org.apache.guacamole.auth.ldap.LDAPAuthenticationProvider;
import org.apache.guacamole.auth.ldap.ConfigurationService;
import org.apache.guacamole.auth.ldap.EscapingService;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.simple.SimpleConnection;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.apache.guacamole.token.StandardTokens;
import org.apache.guacamole.token.TokenFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for querying the connections available to a particular Guacamole
 * user according to an LDAP directory.
 */
public class ConnectionService {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(ConnectionService.class);

    /**
     * Service for escaping parts of LDAP queries.
     */
    @Inject
    private EscapingService escapingService;

    /**
     * Service for retrieving LDAP server configuration information.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * Returns all Guacamole connections accessible to the user currently bound
     * under the given LDAP connection.
     *
     * @param user
     *     The AuthenticatedUser object associated with the user who is
     *     currently authenticated with Guacamole.
     *
     * @param ldapConnection
     *     The current connection to the LDAP server, associated with the
     *     current user.
     *
     * @return
     *     All connections accessible to the user currently bound under the
     *     given LDAP connection, as a map of connection identifier to
     *     corresponding connection object.
     *
     * @throws GuacamoleException
     *     If an error occurs preventing retrieval of connections.
     */
    public Map<String, Connection> getConnections(AuthenticatedUser user,
            LDAPConnection ldapConnection) throws GuacamoleException {

        // Do not return any connections if base DN is not specified
        String configurationBaseDN = confService.getConfigurationBaseDN();
        if (configurationBaseDN == null)
            return Collections.<String, Connection>emptyMap();

        try {

            // Pull the current user DN from the LDAP connection
            String userDN = ldapConnection.getAuthenticationDN();

            // getConnections() will only be called after a connection has been
            // authenticated (via non-anonymous bind), thus userDN cannot
            // possibly be null
            assert(userDN != null);

            // Get the search filter for finding connections accessible by the
            // current user
            String connectionSearchFilter = getConnectionSearchFilter(userDN, ldapConnection);

            // Find all Guacamole connections for the given user by
            // looking for direct membership in the guacConfigGroup
            // and possibly any groups the user is a member of that are
            // referred to in the seeAlso attribute of the guacConfigGroup.
            LDAPSearchResults results = ldapConnection.search(
                configurationBaseDN,
                LDAPConnection.SCOPE_SUB,
                connectionSearchFilter,
                null,
                false,
                confService.getLDAPSearchConstraints()
            );

            // Build token filter containing credential tokens
            TokenFilter tokenFilter = new TokenFilter();
            StandardTokens.addStandardTokens(tokenFilter, user);

            // Produce connections for each readable configuration
            Map<String, Connection> connections = new HashMap<String, Connection>();
            while (results.hasMore()) {

                try {

                    LDAPEntry entry = results.next();

                    // Get common name (CN)
                    LDAPAttribute cn = entry.getAttribute("cn");
                    if (cn == null) {
                        logger.warn("guacConfigGroup is missing a cn.");
                        continue;
                    }

                    // Get associated protocol
                    LDAPAttribute protocol = entry.getAttribute("guacConfigProtocol");
                    if (protocol == null) {
                        logger.warn("guacConfigGroup \"{}\" is missing the "
                                  + "required \"guacConfigProtocol\" attribute.",
                                cn.getStringValue());
                        continue;
                    }

                    // Set protocol
                    GuacamoleConfiguration config = new GuacamoleConfiguration();
                    config.setProtocol(protocol.getStringValue());

                    // Get parameters, if any
                    LDAPAttribute parameterAttribute = entry.getAttribute("guacConfigParameter");
                    if (parameterAttribute != null) {

                        // For each parameter
                        Enumeration<?> parameters = parameterAttribute.getStringValues();
                        while (parameters.hasMoreElements()) {

                            String parameter = (String) parameters.nextElement();

                            // Parse parameter
                            int equals = parameter.indexOf('=');
                            if (equals != -1) {

                                // Parse name
                                String name = parameter.substring(0, equals);
                                String value = parameter.substring(equals+1);

                                config.setParameter(name, value);

                            }

                        }

                    }

                    // Filter the configuration, substituting all defined tokens
                    tokenFilter.filterValues(config.getParameters());

                    // Store connection using cn for both identifier and name
                    String name = cn.getStringValue();
                    Connection connection = new SimpleConnection(name, name, config);
                    connection.setParentIdentifier(LDAPAuthenticationProvider.ROOT_CONNECTION_GROUP);
                    connections.put(name, connection);

                }

                // Deal with issues following LDAP referrals
                catch (LDAPReferralException e) {
                    if (confService.getFollowReferrals()) {
                        logger.error("Could not follow referral: {}", e.getFailedReferral());
                        logger.debug("Error encountered trying to follow referral.", e);
                        throw new GuacamoleServerException("Could not follow LDAP referral.", e);
                    }
                    else {
                        logger.warn("Given a referral, but referrals are disabled. Error was: {}", e.getMessage());
                        logger.debug("Got a referral, but configured to not follow them.", e);
                    }
                }

            }

            // Return map of all connections
            return connections;

        }
        catch (LDAPException e) {
            throw new GuacamoleServerException("Error while querying for connections.", e);
        }

    }

    /**
     * Returns an LDAP search filter which queries all connections accessible
     * by the user having the given DN.
     *
     * @param userDN
     *     DN of the user to search for associated guacConfigGroup connections.
     *
     * @param ldapConnection
     *     LDAP connection to use if additional information must be queried to
     *     produce the filter, such as groups driving RBAC.
     *
     * @return
     *     An LDAP search filter which queries all guacConfigGroup objects
     *     accessible by the user having the given DN.
     *
     * @throws LDAPException
     *     If an error occurs preventing retrieval of user groups.
     *
     * @throws GuacamoleException
     *     If an error occurs retrieving the group base DN.
     */
    private String getConnectionSearchFilter(String userDN,
            LDAPConnection ldapConnection)
            throws LDAPException, GuacamoleException {

        // Create a search filter for the connection search
        StringBuilder connectionSearchFilter = new StringBuilder();

        // Add the prefix to the search filter, prefix filter searches for guacConfigGroups with the userDN as the member attribute value
        connectionSearchFilter.append("(&(objectClass=guacConfigGroup)(|(member=");
        connectionSearchFilter.append(escapingService.escapeLDAPSearchFilter(userDN));
        connectionSearchFilter.append(")");

        // If group base DN is specified search for user groups
        String groupBaseDN = confService.getGroupBaseDN();
        if (groupBaseDN != null) {

            // Get all groups the user is a member of starting at the groupBaseDN, excluding guacConfigGroups
            LDAPSearchResults userRoleGroupResults = ldapConnection.search(
                groupBaseDN,
                LDAPConnection.SCOPE_SUB,
                "(&(!(objectClass=guacConfigGroup))(member=" + escapingService.escapeLDAPSearchFilter(userDN) + "))",
                null,
                false,
                confService.getLDAPSearchConstraints()
            );

            // Append the additional user groups to the LDAP filter
            // Now the filter will also look for guacConfigGroups that refer
            // to groups the user is a member of
            // The guacConfig group uses the seeAlso attribute to refer
            // to these other groups
            while (userRoleGroupResults.hasMore()) {
                try {
                    LDAPEntry entry = userRoleGroupResults.next();
                    connectionSearchFilter.append("(seeAlso=").append(escapingService.escapeLDAPSearchFilter(entry.getDN())).append(")");
                }

                catch (LDAPReferralException e) {
                    if (confService.getFollowReferrals()) {
                        logger.error("Could not follow referral: {}", e.getFailedReferral());
                        logger.debug("Error encountered trying to follow referral.", e);
                        throw new GuacamoleServerException("Could not follow LDAP referral.", e);
                    }
                    else {
                        logger.warn("Given a referral, but referrals are disabled. Error was: {}", e.getMessage());
                        logger.debug("Got a referral, but configured to not follow them.", e);
                    }
                }
            }
        }

        // Complete the search filter.
        connectionSearchFilter.append("))");

        return connectionSearchFilter.toString();
    }

}

