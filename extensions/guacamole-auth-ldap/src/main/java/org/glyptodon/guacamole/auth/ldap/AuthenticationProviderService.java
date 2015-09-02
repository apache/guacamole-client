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
import org.glyptodon.guacamole.auth.ldap.user.AuthenticatedUser;
import org.glyptodon.guacamole.auth.ldap.user.UserContext;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.net.auth.credentials.CredentialsInfo;
import org.glyptodon.guacamole.net.auth.credentials.GuacamoleInsufficientCredentialsException;
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

        LDAPConnection ldapConnection;

        // Require username
        if (credentials.getUsername() == null) {
            logger.debug("Anonymous bind is not currently allowed by the LDAP authentication provider.");
            return null;
        }

        // Require password, and do not allow anonymous binding
        if (credentials.getPassword() == null
                || credentials.getPassword().length() == 0) {
            logger.debug("Anonymous bind is not currently allowed by the LDAP authentication provider.");
            return null;
        }

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

            // Construct user DN
            String userDN =
                        escapingService.escapeDN(confService.getUsernameAttribute())
                + "=" + escapingService.escapeDN(credentials.getUsername())
                + "," + confService.getUserBaseDN();

            // Bind as user
            try {
                ldapConnection.bind(LDAPConnection.LDAP_V3, userDN,
                        credentials.getPassword().getBytes("UTF-8"));
            }
            catch (UnsupportedEncodingException e) {
                logger.error("Unexpected lack of support for UTF-8: {}", e.getMessage());
                logger.debug("Support for UTF-8 (as required by Java spec) not found.", e);
                return null;
            }

            // Disconnect if an error occurs during bind
            catch (LDAPException e) {
                ldapConnection.disconnect();
                throw e;
            }

        }
        catch (LDAPException e) {
            logger.debug("LDAP bind failed.", e);
            return null;
        }

        return ldapConnection;

    }

    public AuthenticatedUser authenticateUser(Credentials credentials)
            throws GuacamoleException {

        // Attempt bind
        LDAPConnection ldapConnection = bindAs(credentials);
        if (ldapConnection == null)
            throw new GuacamoleInsufficientCredentialsException("Permission denied.", CredentialsInfo.USERNAME_PASSWORD);

        try {

            // Return AuthenticatedUser if bind succeeds
            AuthenticatedUser authenticatedUser = authenticatedUserProvider.get();
            authenticatedUser.init(credentials);
            return authenticatedUser;

        }

        // Always disconnect
        finally {

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

    }

}
