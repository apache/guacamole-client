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

package org.glyptodon.guacamole.auth.jdbc.user;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.AuthenticationProvider;
import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.net.auth.credentials.CredentialsInfo;
import org.glyptodon.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;

/**
 * Service which authenticates users based on credentials and provides for
 * the creation of corresponding, new UserContext objects for authenticated
 * users.
 *
 * @author Michael Jumper
 */
public class AuthenticationProviderService  {

    /**
     * Service for accessing users.
     */
    @Inject
    private UserService userService;

    /**
     * Provider for retrieving UserContext instances.
     */
    @Inject
    private Provider<UserContext> userContextProvider;

    /**
     * Authenticates the user having the given credentials, returning a new
     * AuthenticatedUser instance only if the credentials are valid. If the
     * credentials are invalid or expired, an appropriate GuacamoleException
     * will be thrown.
     *
     * @param authenticationProvider
     *     The AuthenticationProvider on behalf of which the user is being
     *     authenticated.
     *
     * @param credentials
     *     The credentials to use to produce the AuthenticatedUser.
     *
     * @return
     *     A new AuthenticatedUser instance for the user identified by the
     *     given credentials.
     *
     * @throws GuacamoleException
     *     If an error occurs during authentication, or if the given
     *     credentials are invalid or expired.
     */
    public AuthenticatedUser authenticateUser(AuthenticationProvider authenticationProvider,
            Credentials credentials) throws GuacamoleException {

        // Authenticate user
        AuthenticatedUser user = userService.retrieveAuthenticatedUser(authenticationProvider, credentials);
        if (user != null)
            return user;

        // Otherwise, unauthorized
        throw new GuacamoleInvalidCredentialsException("Invalid login", CredentialsInfo.USERNAME_PASSWORD);

    }

    /**
     * Returning a new UserContext instance for the given already-authenticated
     * user. A new placeholder account will be created for any user that does
     * not already exist within the database.
     *
     * @param authenticatedUser
     *     The credentials to use to produce the UserContext.
     *
     * @return
     *     A new UserContext instance for the user identified by the given
     *     credentials.
     *
     * @throws GuacamoleException
     *     If an error occurs during authentication, or if the given
     *     credentials are invalid or expired.
     */
    public UserContext getUserContext(org.glyptodon.guacamole.net.auth.AuthenticatedUser authenticatedUser)
                throws GuacamoleException {

        // Retrieve user account for already-authenticated user
        ModeledUser user = userService.retrieveUser(authenticatedUser);
        if (user == null)
            return null;

        // Link to user context
        UserContext context = userContextProvider.get();
        context.init(user.getCurrentUser());
        return context;

    }

}
