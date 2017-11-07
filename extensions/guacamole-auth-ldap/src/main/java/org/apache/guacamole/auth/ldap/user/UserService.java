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

package org.apache.guacamole.auth.ldap.user;

import com.google.inject.Inject;
import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPReferralException;
import com.novell.ldap.LDAPSearchResults;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.guacamole.auth.ldap.ConfigurationService;
import org.apache.guacamole.auth.ldap.EscapingService;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.auth.ldap.LDAPGuacamoleProperties;
import org.apache.guacamole.net.auth.User;
import org.apache.guacamole.net.auth.simple.SimpleUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for queries the users visible to a particular Guacamole user
 * according to an LDAP directory.
 */
public class UserService {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(UserService.class);

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
     * Adds all Guacamole users accessible to the user currently bound under
     * the given LDAP connection to the provided map. Only users with the
     * specified attribute are added. If the same username is encountered
     * multiple times, warnings about possible ambiguity will be logged.
     *
     * @param ldapConnection
     *     The current connection to the LDAP server, associated with the
     *     current user.
     *
     * @return
     *     All users accessible to the user currently bound under the given
     *     LDAP connection, as a map of connection identifier to corresponding
     *     user object.
     *
     * @throws GuacamoleException
     *     If an error occurs preventing retrieval of users.
     */
    private void putAllUsers(Map<String, User> users, LDAPConnection ldapConnection,
            String usernameAttribute) throws GuacamoleException {

        try {

            // Build a filter using the configured or default user search filter
            // to find all user objects in the LDAP tree
            StringBuilder userSearchFilter = new StringBuilder();
            userSearchFilter.append("(&");
            userSearchFilter.append(confService.getUserSearchFilter());
            userSearchFilter.append("(");
            userSearchFilter.append(escapingService.escapeLDAPSearchFilter(usernameAttribute));
            userSearchFilter.append("=*))");
         
            // Find all Guacamole users underneath base DN
            LDAPSearchResults results = ldapConnection.search(
                confService.getUserBaseDN(),
                LDAPConnection.SCOPE_SUB,
                userSearchFilter.toString(),
                null,
                false,
                confService.getLDAPSearchConstraints()
            );

            // Read all visible users
            while (results.hasMore()) {

                try {

                    LDAPEntry entry = results.next();

                    // Get username from record
                    LDAPAttribute username = entry.getAttribute(usernameAttribute);
                    if (username == null) {
                        logger.warn("Queried user is missing the username attribute \"{}\".", usernameAttribute);
                        continue;
                    }

                    // Store user using their username as the identifier
                    String identifier = username.getStringValue();
                    if (users.put(identifier, new SimpleUser(identifier)) != null)
                        logger.warn("Possibly ambiguous user account: \"{}\".", identifier);

                }

                // Deal with errors trying to follow referrals
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
        catch (LDAPException e) {
            throw new GuacamoleServerException("Error while querying users.", e);
        }

    }

    /**
     * Returns all Guacamole users accessible to the user currently bound under
     * the given LDAP connection.
     *
     * @param ldapConnection
     *     The current connection to the LDAP server, associated with the
     *     current user.
     *
     * @return
     *     All users accessible to the user currently bound under the given
     *     LDAP connection, as a map of connection identifier to corresponding
     *     user object.
     *
     * @throws GuacamoleException
     *     If an error occurs preventing retrieval of users.
     */
    public Map<String, User> getUsers(LDAPConnection ldapConnection)
            throws GuacamoleException {

        // Build map of users by querying each username attribute separately
        Map<String, User> users = new HashMap<String, User>();
        for (String usernameAttribute : confService.getUsernameAttributes()) {

            // Attempt to pull all users with given attribute
            try {
                putAllUsers(users, ldapConnection, usernameAttribute);
            }

            // Log any errors non-fatally
            catch (GuacamoleException e) {
                logger.warn("Could not query list of all users for attribute \"{}\": {}",
                        usernameAttribute, e.getMessage());
                logger.debug("Error querying list of all users.", e);
            }

        }

        // Return map of all users
        return users;

    }

    /**
     * Generates a properly-escaped LDAP query which finds all objects having
     * at least one username attribute set to the specified username, where
     * the possible username attributes are defined within
     * guacamole.properties.
     *
     * @param username
     *     The username that the resulting LDAP query should search for within
     *     objects within the LDAP directory.
     *
     * @return
     *     An LDAP query which will search for arbitrary LDAP objects
     *     containing at least one username attribute set to the specified
     *     username.
     *
     * @throws GuacamoleException
     *     If the LDAP query cannot be generated because the list of username
     *     attributes cannot be parsed from guacamole.properties.
     */
    private String generateLDAPQuery(String username)
            throws GuacamoleException {

        List<String> usernameAttributes = confService.getUsernameAttributes();

        // Build LDAP query for users having at least one username attribute
        // and with the configured or default search filter
        StringBuilder ldapQuery = new StringBuilder();
        ldapQuery.append("(&");
        ldapQuery.append(confService.getUserSearchFilter());

        // Include all attributes within OR clause if there are more than one
        if (usernameAttributes.size() > 1)
            ldapQuery.append("(|");

        // Add equality comparison for each possible username attribute
        for (String usernameAttribute : usernameAttributes) {
            ldapQuery.append("(");
            ldapQuery.append(escapingService.escapeLDAPSearchFilter(usernameAttribute));
            ldapQuery.append("=");
            ldapQuery.append(escapingService.escapeLDAPSearchFilter(username));
            ldapQuery.append(")");
        }

        // Close OR clause, if any
        if (usernameAttributes.size() > 1)
            ldapQuery.append(")");

        // Close overall query (AND clause)
        ldapQuery.append(")");

        return ldapQuery.toString();

    }

    /**
     * Returns a list of all DNs corresponding to the users having the given
     * username. If multiple username attributes are defined, or if uniqueness
     * is not enforced across the username attribute, it is possible that this
     * will return multiple DNs.
     *
     * @param ldapConnection
     *     The connection to the LDAP server to use when querying user DNs.
     *
     * @param username
     *     The username of the user whose corresponding user account DNs are
     *     to be retrieved.
     *
     * @return
     *     A list of all DNs corresponding to the users having the given
     *     username. If no such DNs exist, this list will be empty.
     *
     * @throws GuacamoleException
     *     If an error occurs while querying the user DNs, or if the username
     *     attribute property cannot be parsed within guacamole.properties.
     */
    public List<String> getUserDNs(LDAPConnection ldapConnection,
            String username) throws GuacamoleException {

        try {

            List<String> userDNs = new ArrayList<String>();

            // Find all Guacamole users underneath base DN and matching the
            // specified username
            LDAPSearchResults results = ldapConnection.search(
                confService.getUserBaseDN(),
                LDAPConnection.SCOPE_SUB,
                generateLDAPQuery(username),
                null,
                false,
                confService.getLDAPSearchConstraints()
            );

            // Add all DNs for found users
            while (results.hasMore()) {
                try {
                    LDAPEntry entry = results.next();
                    userDNs.add(entry.getDN());
                }
          
                // Deal with errors following referrals
                catch (LDAPReferralException e) {
                    if (confService.getFollowReferrals()) {
                        logger.error("Error trying to follow a referral: {}", e.getFailedReferral());
                        logger.debug("Encountered an error trying to follow a referral.", e);
                        throw new GuacamoleServerException("Failed while trying to follow referrals.", e);
                    }
                    else {
                        logger.warn("Given a referral, not following it. Error was: {}", e.getMessage());
                        logger.debug("Given a referral, but configured to not follow them.", e);
                    }
                }
            }

            // Return all discovered DNs (if any)
            return userDNs;

        }
        catch (LDAPException e) {
            throw new GuacamoleServerException("Error while query user DNs.", e);
        }

    }

    /**
     * Determines the DN which corresponds to the user having the given
     * username. The DN will either be derived directly from the user base DN,
     * or queried from the LDAP server, depending on how LDAP authentication
     * has been configured.
     *
     * @param username
     *     The username of the user whose corresponding DN should be returned.
     *
     * @return
     *     The DN which corresponds to the user having the given username.
     *
     * @throws GuacamoleException
     *     If required properties are missing, and thus the user DN cannot be
     *     determined.
     */
    public String deriveUserDN(String username)
            throws GuacamoleException {

        // Pull username attributes from properties
        List<String> usernameAttributes = confService.getUsernameAttributes();

        // We need exactly one base DN to derive the user DN
        if (usernameAttributes.size() != 1) {
            logger.warn(String.format("Cannot directly derive user DN when "
                      + "multiple username attributes are specified. Please "
                      + "define an LDAP search DN using the \"%s\" property "
                      + "in your \"guacamole.properties\".",
                      LDAPGuacamoleProperties.LDAP_SEARCH_BIND_DN.getName()));
            return null;
        }

        // Derive user DN from base DN
        return
                    escapingService.escapeDN(usernameAttributes.get(0))
            + "=" + escapingService.escapeDN(username)
            + "," + confService.getUserBaseDN();

    }

}
