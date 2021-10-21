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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.filter.AndNode;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.OrNode;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.guacamole.auth.ldap.LDAPAuthenticationProvider;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.auth.ldap.ConnectedLDAPConfiguration;
import org.apache.guacamole.auth.ldap.ObjectQueryService;
import org.apache.guacamole.auth.ldap.group.UserGroupService;
import org.apache.guacamole.auth.ldap.user.LDAPAuthenticatedUser;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.TokenInjectingConnection;
import org.apache.guacamole.net.auth.simple.SimpleConnection;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
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
    private static final Logger logger = LoggerFactory.getLogger(ConnectionService.class);

    /**
     * Service for executing LDAP queries.
     */
    @Inject
    private ObjectQueryService queryService;

    /**
     * Service for retrieving user groups.
     */
    @Inject
    private UserGroupService userGroupService;
    
    /**
     * The objectClass that is present on any Guacamole connections stored
     * in LDAP.
     */
    public static final String CONNECTION_LDAP_OBJECT_CLASS = "guacConfigGroup";
    
    /**
     * The attribute name that uniquely identifies a Guacamole connection object
     * in LDAP.
     */
    public static final String LDAP_ATTRIBUTE_NAME_ID = "cn";
    
    /**
     * The LDAP attribute name where the Guacamole connection protocol is stored.
     */
    public static final String LDAP_ATTRIBUTE_NAME_PROTOCOL = "guacConfigProtocol";
    
    /**
     * The LDAP attribute name that contains any connection parameters.
     */
    public static final String LDAP_ATTRIBUTE_NAME_PARAMETER = "guacConfigParameter";
    
    /**
     * The LDAP attribute name that provides group-based access control for
     * Guacamole connection objects.
     */
    public static final String LDAP_ATTRIBUTE_NAME_GROUPS = "seeAlso";
    
    /**
     * A list of all attribute names that could be associated with a Guacamole
     * connection object in LDAP.
     */
    public static final Collection<String> GUAC_CONFIG_LDAP_ATTRIBUTES =
            Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
                    LDAP_ATTRIBUTE_NAME_ID,
                    LDAP_ATTRIBUTE_NAME_PROTOCOL,
                    LDAP_ATTRIBUTE_NAME_PARAMETER,
                    LDAP_ATTRIBUTE_NAME_GROUPS
            )));
            
            

    /**
     * Returns all Guacamole connections accessible to the given user.
     *
     * @param user
     *     The AuthenticatedUser object associated with the user who is
     *     currently authenticated with Guacamole.
     *
     * @return
     *     All connections accessible to the user currently bound under the
     *     given LDAP connection, as a map of connection identifier to
     *     corresponding connection object.
     *
     * @throws GuacamoleException
     *     If an error occurs preventing retrieval of connections.
     */
    public Map<String, Connection> getConnections(LDAPAuthenticatedUser user)
            throws GuacamoleException {

        ConnectedLDAPConfiguration ldapConfig = user.getLDAPConfiguration();

        // Do not return any connections if base DN is not specified
        Dn configurationBaseDN = ldapConfig.getConfigurationBaseDN();
        if (configurationBaseDN == null)
            return Collections.<String, Connection>emptyMap();

        try {

            // Get the search filter for finding connections accessible by the
            // current user
            ExprNode connectionSearchFilter = getConnectionSearchFilter(user);

            // Find all Guacamole connections for the given user by
            // looking for direct membership in the guacConfigGroup
            // and possibly any groups the user is a member of that are
            // referred to in the seeAlso attribute of the guacConfigGroup.
            List<Entry> results = queryService.search(ldapConfig, ldapConfig.getLDAPConnection(),
                    configurationBaseDN, connectionSearchFilter, 0, GUAC_CONFIG_LDAP_ATTRIBUTES);

            // Return a map of all readable connections
            return queryService.asMap(results, (entry) -> {

                // Get common name (CN)
                Attribute cn = entry.get(LDAP_ATTRIBUTE_NAME_ID);
                
                if (cn == null) {
                    logger.warn("{} is missing a {}.",
                            CONNECTION_LDAP_OBJECT_CLASS, LDAP_ATTRIBUTE_NAME_ID);
                    return null;
                }
                
                String cnName;
                
                try {
                    cnName = cn.getString();
                }
                catch (LdapInvalidAttributeValueException e) {
                    logger.error("Invalid value for {} attribute: {}",
                            LDAP_ATTRIBUTE_NAME_ID, e.getMessage());
                    logger.debug("LDAP exception while getting CN attribute.", e);
                    return null;
                }

                // Get associated protocol
                Attribute protocol = entry.get(LDAP_ATTRIBUTE_NAME_PROTOCOL);
                if (protocol == null) {
                    logger.warn("{} \"{}\" is missing the "
                              + "required \"{}\" attribute.",
                            CONNECTION_LDAP_OBJECT_CLASS,
                            cnName, LDAP_ATTRIBUTE_NAME_PROTOCOL);
                    return null;
                }

                // Set protocol
                GuacamoleConfiguration config = new GuacamoleConfiguration();
                try {
                    config.setProtocol(protocol.getString());
                }
                catch (LdapInvalidAttributeValueException e) {
                    logger.error("Invalid value of the protocol entry: {}",
                            e.getMessage());
                    logger.debug("LDAP exception when getting protocol value.", e);
                    return null;
                }

                // Get parameters, if any
                Attribute parameterAttribute = entry.get(LDAP_ATTRIBUTE_NAME_PARAMETER);
                if (parameterAttribute != null) {

                    // For each parameter
                    while (parameterAttribute.size() > 0) {
                        String parameter;
                        try {
                            parameter = parameterAttribute.getString();
                        }
                        catch (LdapInvalidAttributeValueException e) {
                            logger.warn("Parameter value not valid for {}: {}",
                                    cnName, e.getMessage());
                            logger.debug("LDAP exception when getting parameter value.",
                                    e);
                            return null;
                        }
                        parameterAttribute.remove(parameter);

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

                // Store connection using cn for both identifier and name
                Connection connection = new SimpleConnection(cnName, cnName, config, true);
                connection.setParentIdentifier(LDAPAuthenticationProvider.ROOT_CONNECTION_GROUP);

                // Inject LDAP-specific tokens only if LDAP handled user
                // authentication
                if (user instanceof LDAPAuthenticatedUser)
                    connection = new TokenInjectingConnection(connection, user.getTokens());

                return connection;

            });

        }
        catch (LdapException e) {
            throw new GuacamoleServerException("Error while querying for connections.", e);
        }

    }

    /**
     * Returns an LDAP search filter which queries all connections accessible
     * by the given user.
     *
     * @param user
     *     The AuthenticatedUser object associated with the user who is
     *     currently authenticated with Guacamole.
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
    private ExprNode getConnectionSearchFilter(LDAPAuthenticatedUser user)
            throws LdapException, GuacamoleException {

        ConnectedLDAPConfiguration config = user.getLDAPConfiguration();
        Dn userDN = config.getBindDN();

        AndNode searchFilter = new AndNode();

        // Add the prefix to the search filter, prefix filter searches for guacConfigGroups with the userDN as the member attribute value
        searchFilter.addNode(new EqualityNode("objectClass", CONNECTION_LDAP_OBJECT_CLASS));
        
        // Apply group filters
        OrNode groupFilter = new OrNode();
        groupFilter.addNode(new EqualityNode(config.getMemberAttribute(),
            userDN.toString()));

        // Additionally filter by group membership if the current user is a
        // member of any user groups
        List<Entry> userGroups = userGroupService.getParentUserGroupEntries(config, userDN);
        if (!userGroups.isEmpty()) {
            userGroups.forEach(entry ->
                groupFilter.addNode(new EqualityNode(LDAP_ATTRIBUTE_NAME_GROUPS,entry.getDn().toString()))
            );
        }

        // Complete the search filter.
        searchFilter.addNode(groupFilter);

        return searchFilter;
    }

}
