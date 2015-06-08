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
import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.net.auth.credentials.CredentialsInfo;
import org.glyptodon.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;

/**
 * Service which creates new UserContext instances for valid users based on
 * credentials.
 *
 * @author Michael Jumper
 */
public class UserContextService  {

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
     * UserContext instance only if the credentials are valid. If the
     * credentials are invalid or expired, an appropriate GuacamoleException
     * will be thrown.
     *
     * @param credentials
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
    public org.glyptodon.guacamole.net.auth.UserContext
        getUserContext(Credentials credentials)
                throws GuacamoleException {

        // Authenticate user
        ModeledUser user = userService.retrieveUser(credentials);
        if (user != null) {

            // Upon successful authentication, return new user context
            UserContext context = userContextProvider.get();
            context.init(user.getCurrentUser());
            return context;

        }

        // Otherwise, unauthorized
        throw new GuacamoleInvalidCredentialsException("Invalid login", CredentialsInfo.USERNAME_PASSWORD);

    }

}
