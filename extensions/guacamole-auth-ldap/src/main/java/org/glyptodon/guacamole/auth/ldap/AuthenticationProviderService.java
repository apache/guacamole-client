/*
 * Copyright (C) 2015 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.glyptodon.guacamole.auth.ldap;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import org.glyptodon.guacamole.auth.ldap.user.AuthenticatedUser;
import org.glyptodon.guacamole.auth.ldap.user.UserContext;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.auth.ldap.user.UserService;
import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.net.auth.credentials.CredentialsInfo;
import org.glyptodon.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service providing convenience functions for the LDAP AuthenticationProvider
 * implementation.
 *
 * @author Michael Jumper
 */
public class AuthenticationProviderService {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(AuthenticationProviderService.class);

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
     * Provider for AuthenticatedUser objects.
     */
    @Inject
    private Provider<AuthenticatedUser> authenticatedUserProvider;

    /**
     * Provider for UserContext objects.
     */
    @Inject
    private Provider<UserContext> userContextProvider;

    /**
     * Disconnects the given LDAP connection, logging any failure to do so
     * appropriately.
     *
     * @param ldapConnection
     *     The LDAP connection to disconnect.
     */
    private void disconnect(LDAPConnection ldapConnection) {

        // Attempt disconnect
        try {
            ldapConnection.disconnect();
        }

        // Warn if disconnect unexpectedly fails
        catch (LDAPException e) {
            logger.warn("Unable to disconnect from LDAP server: {}", e.getMessage());
            logger.debug("LDAP disconnect failed.", e);
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
    private String getUserBindDN(String username)
            throws GuacamoleException {

        // If a search DN is provided, search the LDAP directory for the DN
        // corresponding to the given username
        String searchBindDN = confService.getSearchBindDN();
        if (searchBindDN != null) {

            // Create an LDAP connection using the search account
            LDAPConnection searchConnection = bindAs(
                searchBindDN,
                confService.getSearchBindPassword()
            );

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
                disconnect(searchConnection);
            }

        }

        // Otherwise, derive user DN from base DN
        return userService.deriveUserDN(username);

    }

    /**
     * Binds to the LDAP server using the provided user DN and password.
     *
     * @param userDN
     *     The DN of the user to bind as, or null to bind anonymously.
     *
     * @param password
     *     The password to use when binding as the specified user, or null to
     *     attempt to bind without a password.
     *
     * @return
     *     A bound LDAP connection, or null if the connection could not be
     *     bound.
     *
     * @throws GuacamoleException
     *     If an error occurs while binding to the LDAP server.
     */
    private LDAPConnection bindAs(String userDN, String password)
            throws GuacamoleException {

        LDAPConnection ldapConnection;

        // Connect to LDAP server
        try {
            ldapConnection = new LDAPConnection();
            ldapConnection.connect(
                confService.getServerHostname(),
                confService.getServerPort()
            );
        }
        catch (LDAPException e) {
            logger.error("Unable to connect to LDAP server: {}", e.getMessage());
            logger.debug("Failed to connect to LDAP server.", e);
            return null;
        }

        // Bind using provided credentials
        try {

            byte[] passwordBytes;
            try {

                // Convert password into corresponding byte array
                if (password != null)
                    passwordBytes = password.getBytes("UTF-8");
                else
                    passwordBytes = null;

            }
            catch (UnsupportedEncodingException e) {
                logger.error("Unexpected lack of support for UTF-8: {}", e.getMessage());
                logger.debug("Support for UTF-8 (as required by Java spec) not found.", e);
                disconnect(ldapConnection);
                return null;
            }

            // Bind as user
            ldapConnection.bind(LDAPConnection.LDAP_V3, userDN, passwordBytes);

        }

        // Disconnect if an error occurs during bind
        catch (LDAPException e) {
            logger.debug("LDAP bind failed.", e);
            disconnect(ldapConnection);
            return null;
        }

        return ldapConnection;
        
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
        return bindAs(userDN, password);

    }

    /**
     * Returns an AuthenticatedUser representing the user authenticated by the
     * given credentials.
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
    public AuthenticatedUser authenticateUser(Credentials credentials)
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

            // Return AuthenticatedUser if bind succeeds
            AuthenticatedUser authenticatedUser = authenticatedUserProvider.get();
            authenticatedUser.init(credentials);
            return authenticatedUser;

        }

        // Always disconnect
        finally {
            disconnect(ldapConnection);
        }

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
    public UserContext getUserContext(org.glyptodon.guacamole.net.auth.AuthenticatedUser authenticatedUser)
            throws GuacamoleException {

        // Bind using credentials associated with AuthenticatedUser
        Credentials credentials = authenticatedUser.getCredentials();
        LDAPConnection ldapConnection = bindAs(credentials);
        if (ldapConnection == null)
            return null;

        try {

            // Build user context by querying LDAP
            UserContext userContext = userContextProvider.get();
            userContext.init(authenticatedUser, ldapConnection);
            return userContext;

        }

        // Always disconnect
        finally {
            disconnect(ldapConnection);
        }

    }

}
