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

package org.glyptodon.guacamole.auth.json;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.apache.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;
import org.glyptodon.guacamole.auth.json.user.AuthenticatedUser;
import org.glyptodon.guacamole.auth.json.user.UserContext;
import org.glyptodon.guacamole.auth.json.user.UserData;
import org.glyptodon.guacamole.auth.json.user.UserDataService;

/**
 * Service providing convenience functions for the JSONAuthenticationProvider.
 *
 * @author Michael Jumper
 */
public class AuthenticationProviderService {

    /**
     * Service for deriving Guacamole extension API data from UserData objects.
     */
    @Inject
    private UserDataService userDataService;

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

        // Pull UserData from credentials, if possible
        UserData userData = userDataService.fromCredentials(credentials);
        if (userData == null)
            throw new GuacamoleInvalidCredentialsException("Permission denied.", CredentialsInfo.EMPTY);

        // Produce AuthenticatedUser associated with derived UserData
        AuthenticatedUser authenticatedUser = authenticatedUserProvider.get();
        authenticatedUser.init(credentials, userData);
        return authenticatedUser;

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
    public UserContext getUserContext(org.apache.guacamole.net.auth.AuthenticatedUser authenticatedUser)
            throws GuacamoleException {

        // The JSONAuthenticationProvider only provides data for users it has
        // authenticated itself
        if (!(authenticatedUser instanceof AuthenticatedUser))
            return null;

        // Return UserContext containing data from the authenticated user's
        // associated UserData object
        UserContext userContext = userContextProvider.get();
        userContext.init(((AuthenticatedUser) authenticatedUser).getUserData());
        return userContext;

    }

}
