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

package org.apache.guacamole.auth.ldap;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.auth.ldap.group.UserGroupService;
import org.apache.guacamole.auth.ldap.user.LDAPAuthenticatedUser;
import org.apache.guacamole.auth.ldap.user.LDAPUserContext;
import org.apache.guacamole.auth.ldap.user.UserService;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.apache.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;
import org.apache.guacamole.token.TokenName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service providing convenience functions for the LDAP AuthenticationProvider
 * implementation.
 */
public class AuthenticationProviderService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationProviderService.class);
    
    /**
     * The prefix that will be used when generating tokens.
     */
    public static final String LDAP_ATTRIBUTE_TOKEN_PREFIX = "LDAP_";

    /**
     * Service for creating and managing connections to LDAP servers.
     */
    @Inject
    private LDAPConnectionService ldapService;

    /**
     * Service for retrieving LDAP server configuration information.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * Service for retrieving users and their corresponding LDAP DNs.
     */
    @Inject
    private UserService userService;

    /**
     * Service for retrieving user groups.
     */
    @Inject
    private UserGroupService userGroupService;

    /**
     * Provider for AuthenticatedUser objects.
     */
    @Inject
    private Provider<LDAPAuthenticatedUser> authenticatedUserProvider;

    /**
     * Provider for UserContext objects.
     */
    @Inject
    private Provider<LDAPUserContext> userContextProvider;

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
    private String getUserBindDN(String username)
            throws GuacamoleException {

        // If a search DN is provided, search the LDAP directory for the DN
        // corresponding to the given username
        String searchBindDN = confService.getSearchBindDN();
        if (searchBindDN != null) {

            // Create an LDAP connection using the search account
            LDAPConnection searchConnection = ldapService.bindAs(
                searchBindDN,
                confService.getSearchBindPassword()
            );

            // Warn of failure to find
            if (searchConnection == null) {
                logger.error("Unable to bind using search DN \"{}\"", searchBindDN);
                return null;
            }

            try {

                // Retrieve all DNs associated with the given username
                List<String> userDNs = userService.getUserDNs(searchConnection, username);
                if (userDNs.isEmpty())
                    return null;

                // Warn if multiple DNs exist for the same user
                if (userDNs.size() != 1) {
                    logger.warn("Multiple DNs possible for user \"{}\": {}", username, userDNs);
                    return null;
                }

                // Return the single possible DN
                return userDNs.get(0);

            }

            // Always disconnect
            finally {
                ldapService.disconnect(searchConnection);
            }

        }

        // Otherwise, derive user DN from base DN
        return userService.deriveUserDN(username);

    }

    /**
     * Binds to the LDAP server using the provided Guacamole credentials. The
     * DN of the user is derived using the LDAP configuration properties
     * provided in guacamole.properties, as is the server hostname and port
     * information.
     *
     * @param credentials
     *     The credentials to use to bind to the LDAP server.
     *
     * @return
     *     A bound LDAP connection, or null if the connection could not be
     *     bound.
     *
     * @throws GuacamoleException
     *     If an error occurs while binding to the LDAP server.
     */
    private LDAPConnection bindAs(Credentials credentials)
        throws GuacamoleException {

        // Get username and password from credentials
        String username = credentials.getUsername();
        String password = credentials.getPassword();

        // Require username
        if (username == null || username.isEmpty()) {
            logger.debug("Anonymous bind is not currently allowed by the LDAP authentication provider.");
            return null;
        }

        // Require password, and do not allow anonymous binding
        if (password == null || password.isEmpty()) {
            logger.debug("Anonymous bind is not currently allowed by the LDAP authentication provider.");
            return null;
        }

        // Determine user DN
        String userDN = getUserBindDN(username);
        if (userDN == null) {
            logger.debug("Unable to determine DN for user \"{}\".", username);
            return null;
        }

        // Bind using user's DN
        return ldapService.bindAs(userDN, password);

    }

    /**
     * Returns an AuthenticatedUser representing the user authenticated by the
     * given credentials. Also adds custom LDAP attributes to the
     * AuthenticatedUser.
     *
     * @param credentials
     *     The credentials to use for authentication.
     *
     * @return
     *     An AuthenticatedUser representing the user authenticated by the
     *     given credentials.
     *
     * @throws GuacamoleException
     *     If an error occurs while authenticating the user, or if access is
     *     denied.
     */
    public LDAPAuthenticatedUser authenticateUser(Credentials credentials)
            throws GuacamoleException {

        // Attempt bind
        LDAPConnection ldapConnection;
        try {
            ldapConnection = bindAs(credentials);
        }
        catch (GuacamoleException e) {
            logger.error("Cannot bind with LDAP server: {}", e.getMessage());
            logger.debug("Error binding with LDAP server.", e);
            ldapConnection = null;
        }

        // If bind fails, permission to login is denied
        if (ldapConnection == null)
            throw new GuacamoleInvalidCredentialsException("Permission denied.", CredentialsInfo.USERNAME_PASSWORD);

        try {

            // Retrieve group membership of the user that just authenticated
            Set<String> effectiveGroups =
                    userGroupService.getParentUserGroupIdentifiers(ldapConnection,
                            ldapConnection.getAuthenticationDN());

            // Return AuthenticatedUser if bind succeeds
            LDAPAuthenticatedUser authenticatedUser = authenticatedUserProvider.get();
            authenticatedUser.init(credentials, getAttributeTokens(ldapConnection, credentials.getUsername()), effectiveGroups);
            return authenticatedUser;

        }
        // Always disconnect
        finally {
            ldapService.disconnect(ldapConnection);
        }

    }

    /**
     * Returns parameter tokens generated from LDAP attributes on the user
     * currently bound under the given LDAP connection. The attributes to be
     * converted into parameter tokens must be explicitly listed in
     * guacamole.properties. If no attributes are specified or none are
     * found on the LDAP user object, an empty map is returned.
     *
     * @param ldapConnection
     *     LDAP connection to use to read the attributes of the user.
     *
     * @param username
     *     The username of the user whose attributes are to be queried.
     *
     * @return
     *     A map of parameter tokens generated from attributes on the user
     *     currently bound under the given LDAP connection, as a map of token
     *     name to corresponding value, or an empty map if no attributes are
     *     specified or none are found on the user object.
     *
     * @throws GuacamoleException
     *     If an error occurs retrieving the user DN or the attributes.
     */
    private Map<String, String> getAttributeTokens(LDAPConnection ldapConnection,
            String username) throws GuacamoleException {

        // Get attributes from configuration information
        List<String> attrList = confService.getAttributes();

        // If there are no attributes there is no reason to search LDAP
        if (attrList.isEmpty())
            return Collections.<String, String>emptyMap();

        // Build LDAP query parameters
        String[] attrArray = attrList.toArray(new String[attrList.size()]);
        String userDN = getUserBindDN(username);

        Map<String, String> tokens = new HashMap<>();
        try {

            // Get LDAP attributes by querying LDAP
            LDAPEntry userEntry = ldapConnection.read(userDN, attrArray);
            if (userEntry == null)
                return Collections.<String, String>emptyMap();

            LDAPAttributeSet attrSet = userEntry.getAttributeSet();
            if (attrSet == null)
                return Collections.<String, String>emptyMap();

            // Convert each retrieved attribute into a corresponding token
            for (Object attrObj : attrSet) {
                LDAPAttribute attr = (LDAPAttribute)attrObj;
                tokens.put(TokenName.canonicalize(attr.getName(),
                        LDAP_ATTRIBUTE_TOKEN_PREFIX), attr.getStringValue());
            }

        }
        catch (LDAPException e) {
            throw new GuacamoleServerException("Could not query LDAP user attributes.", e);
        }

        return tokens;

    }

    /**
     * Returns a UserContext object initialized with data accessible to the
     * given AuthenticatedUser.
     *
     * @param authenticatedUser
     *     The AuthenticatedUser to retrieve data for.
     *
     * @return
     *     A UserContext object initialized with data accessible to the given
     *     AuthenticatedUser.
     *
     * @throws GuacamoleException
     *     If the UserContext cannot be created due to an error.
     */
    public LDAPUserContext getUserContext(AuthenticatedUser authenticatedUser)
            throws GuacamoleException {

        // Bind using credentials associated with AuthenticatedUser
        Credentials credentials = authenticatedUser.getCredentials();
        LDAPConnection ldapConnection = bindAs(credentials);
        if (ldapConnection == null)
            return null;

        try {

            // Build user context by querying LDAP
            LDAPUserContext userContext = userContextProvider.get();
            userContext.init(authenticatedUser, ldapConnection);
            return userContext;

        }

        // Always disconnect
        finally {
            ldapService.disconnect(ldapConnection);
        }

    }

}
