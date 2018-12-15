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

package org.apache.guacamole.auth.ldap.group;

import com.google.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.NotNode;
import org.apache.directory.api.ldap.model.filter.PresenceNode;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.guacamole.auth.ldap.conf.ConfigurationService;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.ldap.ObjectQueryService;
import org.apache.guacamole.net.auth.UserGroup;
import org.apache.guacamole.net.auth.simple.SimpleUserGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for querying user group membership and retrieving user groups
 * visible to a particular Guacamole user.
 */
public class UserGroupService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(UserGroupService.class);

    /**
     * Service for retrieving LDAP server configuration information.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * Service for executing LDAP queries.
     */
    @Inject
    private ObjectQueryService queryService;

    /**
     * Returns the base search filter which should be used to retrieve user
     * groups which do not represent Guacamole connections. As excluding the
     * guacConfigGroup object class may not work as expected if it is not
     * defined (may always return zero results), it should only be explicitly
     * excluded if it is expected to have been defined.
     *
     * @return
     *     The base search filter which should be used to retrieve user groups.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    private ExprNode getGroupSearchFilter() throws GuacamoleException {

        // Explicitly exclude guacConfigGroup object class only if it should
        // be assumed to be defined (query may fail due to no such object
        // class existing otherwise)
        if (confService.getConfigurationBaseDN() != null)
            return new NotNode(new EqualityNode("objectClass","guacConfigGroup"));

        // Read any object as a group if LDAP is not being used for connection
        // storage (guacConfigGroup)
        return new PresenceNode("objectClass");

    }

    /**
     * Returns all Guacamole user groups accessible to the user currently bound
     * under the given LDAP connection.
     *
     * @param ldapConnection
     *     The current connection to the LDAP server, associated with the
     *     current user.
     *
     * @return
     *     All user groups accessible to the user currently bound under the
     *     given LDAP connection, as a map of user group identifier to
     *     corresponding UserGroup object.
     *
     * @throws GuacamoleException
     *     If an error occurs preventing retrieval of user groups.
     */
    public Map<String, UserGroup> getUserGroups(LdapNetworkConnection ldapConnection)
            throws GuacamoleException {

        // Do not return any user groups if base DN is not specified
        Dn groupBaseDN = confService.getGroupBaseDN();
        if (groupBaseDN == null)
            return Collections.emptyMap();

        // Retrieve all visible user groups which are not guacConfigGroups
        Collection<String> attributes = confService.getGroupNameAttributes();
        List<Entry> results = queryService.search(
            ldapConnection,
            groupBaseDN,
            getGroupSearchFilter(),
            attributes,
            null
        );

        // Convert retrieved user groups to map of identifier to Guacamole
        // user group object
        return queryService.asMap(results, entry -> {

            // Translate entry into UserGroup object having proper identifier
            try {
                String name = queryService.getIdentifier(entry, attributes);
                if (name != null)
                    return new SimpleUserGroup(name);
            }
            catch (LdapInvalidAttributeValueException e) {
                return null;
            }

            // Ignore user groups which lack a name attribute
            logger.debug("User group \"{}\" is missing a name attribute "
                    + "and will be ignored.", entry.getDn().toString());
            return null;

        });

    }

    /**
     * Returns the LDAP entries representing all user groups that the given
     * user is a member of. Only user groups which are readable by the current
     * user will be retrieved.
     *
     * @param ldapConnection
     *     The current connection to the LDAP server, associated with the
     *     current user.
     *
     * @param userDN
     *     The DN of the user whose group membership should be retrieved.
     *
     * @return
     *     The LDAP entries representing all readable parent user groups of the
     *     user having the given DN.
     *
     * @throws GuacamoleException
     *     If an error occurs preventing retrieval of user groups.
     */
    public List<Entry> getParentUserGroupEntries(LdapNetworkConnection ldapConnection,
            Dn userDN) throws GuacamoleException {

        // Do not return any user groups if base DN is not specified
        Dn groupBaseDN = confService.getGroupBaseDN();
        if (groupBaseDN == null)
            return Collections.emptyList();

        // Get all groups the user is a member of starting at the groupBaseDN,
        // excluding guacConfigGroups
        return queryService.search(
            ldapConnection,
            groupBaseDN,
            getGroupSearchFilter(),
            Collections.singleton(confService.getMemberAttribute()),
            userDN.toString()
        );

    }

    /**
     * Returns the identifiers of all user groups that the given user is a
     * member of. Only identifiers of user groups which are readable by the
     * current user will be retrieved.
     *
     * @param ldapConnection
     *     The current connection to the LDAP server, associated with the
     *     current user.
     *
     * @param userDN
     *     The DN of the user whose group membership should be retrieved.
     *
     * @return
     *     The identifiers of all readable parent user groups of the user
     *     having the given DN.
     *
     * @throws GuacamoleException
     *     If an error occurs preventing retrieval of user groups.
     */
    public Set<String> getParentUserGroupIdentifiers(LdapNetworkConnection ldapConnection,
            Dn userDN) throws GuacamoleException {

        Collection<String> attributes = confService.getGroupNameAttributes();
        List<Entry> userGroups = getParentUserGroupEntries(ldapConnection, userDN);

        Set<String> identifiers = new HashSet<>(userGroups.size());
        userGroups.forEach(entry -> {

            // Determine unique identifier for user group
            try {
                String name = queryService.getIdentifier(entry, attributes);
                if (name != null)
                    identifiers.add(name);

                // Ignore user groups which lack a name attribute
                else
                    logger.debug("User group \"{}\" is missing a name attribute "
                            + "and will be ignored.", entry.getDn().toString());
            }
            catch (LdapInvalidAttributeValueException e) {
                logger.debug("User group missing identifier.", e.getMessage());
            }

        });

        return identifiers;

    }

}
