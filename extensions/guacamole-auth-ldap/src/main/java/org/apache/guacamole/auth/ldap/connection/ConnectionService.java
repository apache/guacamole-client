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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
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
            LdapConnection ldapConnection) throws GuacamoleException {

        // Do not return any connections if base DN is not specified
        Dn configurationBaseDN = confService.getConfigurationBaseDN();
        if (configurationBaseDN == null)
            return Collections.<String, Connection>emptyMap();

        try {

            // Pull the current user DN from the LDAP connection
            LdapConnectionConfig ldapConnectionConfig = ((LdapNetworkConnection)ldapConnection).getConfig();
            String userDN = ldapConnectionConfig.getName();

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
            SearchRequest request = new SearchRequestImpl();
            request.setBase(configurationBaseDN);
            request.setDerefAliases(confService.getDereferenceAliases());
            request.setScope(SearchScope.SUBTREE);
            request.setFilter(connectionSearchFilter);
            request.setSizeLimit(confService.getMaxResults());
            request.setTimeLimit(confService.getOperationTimeout());
            request.setTypesOnly(false);

            SearchCursor results = ldapConnection.search(request);

            // Build token filter containing credential tokens
            TokenFilter tokenFilter = new TokenFilter();
            StandardTokens.addStandardTokens(tokenFilter, user);

            // Produce connections for each readable configuration
            Map<String, Connection> connections = new HashMap<String, Connection>();
            while (results.next()) {

                // Get the entry
                Response response = results.get();
                Entry entry;
                if (response instanceof SearchResultEntry)
                    entry = ((SearchResultEntry)results).getEntry();
                else
                    continue;

                // Get common name (CN)
                Attribute cn = entry.get("cn");
                if (cn == null) {
                    logger.warn("guacConfigGroup is missing a cn.");
                    continue;
                }

                // Get associated protocol
                Attribute protocol = entry.get("guacConfigProtocol");
                if (protocol == null) {
                    logger.warn("guacConfigGroup \"{}\" is missing the "
                              + "required \"guacConfigProtocol\" attribute.",
                            cn.getString());
                    continue;
                }

                // Set protocol
                GuacamoleConfiguration config = new GuacamoleConfiguration();
                config.setProtocol(protocol.getString());

                // Get parameters, if any
                Attribute parameterAttribute = entry.get("guacConfigParameter");
                if (parameterAttribute != null) {

                    // For each parameter
                    Iterator parameters = parameterAttribute.iterator();
                    while (parameters.hasNext()) {

                        String parameter = (String)parameters.next();

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

                // Store connection using cn for both identifier and name
                String name = cn.getString();
                Connection connection = new SimpleConnection(name, name, config);
                connection.setParentIdentifier(LDAPAuthenticationProvider.ROOT_CONNECTION_GROUP);
                connections.put(name, connection);

            }

            // Return map of all connections
            return connections;

        }
        catch (LdapException e) {
            throw new GuacamoleServerException("Error while querying for connections.", e);
        }
        catch (CursorException e) {
            throw new GuacamoleServerException("Error while iterating over LDAP search results.", e);
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
     * @throws LdapException
     *     If an error occurs preventing retrieval of user groups.
     *
     * @throws GuacamoleException
     *     If an error occurs retrieving the group base DN.
     */
    private String getConnectionSearchFilter(String userDN,
            LdapConnection ldapConnection)
            throws LdapException, GuacamoleException {

        // Create a search filter for the connection search
        StringBuilder connectionSearchFilter = new StringBuilder();

        // Add the prefix to the search filter, prefix filter searches for guacConfigGroups with the userDN as the member attribute value
        connectionSearchFilter.append("(&(objectClass=guacConfigGroup)(|(member=");
        connectionSearchFilter.append(escapingService.escapeLDAPSearchFilter(userDN));
        connectionSearchFilter.append(")");

        // If group base DN is specified search for user groups
        Dn groupBaseDN = confService.getGroupBaseDN();
        if (groupBaseDN != null) {

            // Get all groups the user is a member of starting at the groupBaseDN, excluding guacConfigGroups
            SearchRequest request = new SearchRequestImpl();
            request.setBase(groupBaseDN);
            request.setDerefAliases(confService.getDereferenceAliases());
            request.setScope(SearchScope.SUBTREE);
            request.setFilter("(&(!(objectClass=guacConfigGroup))(member=" + escapingService.escapeLDAPSearchFilter(userDN) + "))");
            request.setSizeLimit(confService.getMaxResults());
            request.setTimeLimit(confService.getOperationTimeout());
            request.setTypesOnly(false);

            SearchCursor userRoleGroupResults = ldapConnection.search(request);

            // Append the additional user groups to the LDAP filter
            // Now the filter will also look for guacConfigGroups that refer
            // to groups the user is a member of
            // The guacConfig group uses the seeAlso attribute to refer
            // to these other groups
            try {
                while (userRoleGroupResults.next()) {
                    Response response = userRoleGroupResults.get();
                    if (response instanceof SearchResultEntry) {
                        Entry entry = ((SearchResultEntry)response).getEntry();
                        connectionSearchFilter.append("(seeAlso=").append(escapingService.escapeLDAPSearchFilter(entry.getDn().toString())).append(")");
                    }
                }
            }
            catch (CursorException e) {
                throw new GuacamoleServerException("Error while iterating over LDAP search results.", e);
            }
        }

        // Complete the search filter.
        connectionSearchFilter.append("))");

        return connectionSearchFilter.toString();
    }

}

