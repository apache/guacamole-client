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

package org.glyptodon.guacamole.auth.json.user;

import com.google.inject.Inject;
import org.apache.guacamole.net.auth.AbstractUserContext;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.net.auth.User;

/**
 * An implementation of UserContext specific to the JSONAuthenticationProvider
 * which obtains all data from the encrypted JSON provided during
 * authentication.
 *
 * @author Michael Jumper
 */
public class UserContext extends AbstractUserContext {

    /**
     * The identifier reserved for the root connection group.
     */
    public static final String ROOT_CONNECTION_GROUP = DEFAULT_ROOT_CONNECTION_GROUP;

    /**
     * Reference to the AuthenticationProvider associated with this
     * UserContext.
     */
    @Inject
    private AuthenticationProvider authProvider;

    /**
     * Service for deriving Guacamole extension API data from UserData objects.
     */
    @Inject
    private UserDataService userDataService;

    /**
     * The UserData object associated with the user to whom this UserContext
     * belongs.
     */
    private UserData userData;

    /**
     * Initializes this UserContext using the data associated with the provided
     * UserData object.
     *
     * @param userData
     *     The UserData object derived from the JSON data received when the
     *     user authenticated.
     */
    public void init(UserData userData) {
        this.userData = userData;
    }

    @Override
    public User self() {
        return userDataService.getUser(userData);
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return authProvider;
    }

    @Override
    public Directory<Connection> getConnectionDirectory() {
        return userDataService.getConnectionDirectory(userData);
    }

}
