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

package org.glyptodon.guacamole.net.basic.rest;

import java.util.List;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleResourceNotFoundException;
import org.glyptodon.guacamole.net.auth.Connection;
import org.glyptodon.guacamole.net.auth.ConnectionGroup;
import org.glyptodon.guacamole.net.auth.Directory;
import org.glyptodon.guacamole.net.auth.User;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.basic.GuacamoleSession;
import org.glyptodon.guacamole.net.basic.rest.connectiongroup.APIConnectionGroup;

/**
 * Provides easy access and automatic error handling for retrieval of objects,
 * such as users, connections, or connection groups. REST API semantics, such
 * as the special root connection group identifier, are also handled
 * automatically.
 */
public class ObjectRetrievalService {

    /**
     * Retrieves a single UserContext from the given GuacamoleSession, which
     * may contain multiple UserContexts.
     *
     * @param session
     *     The GuacamoleSession to retrieve the UserContext from.
     *
     * @param identifier
     *     The unique identifier of the AuthenticationProvider that created the
     *     UserContext being retrieved. Only one UserContext per
     *     AuthenticationProvider can exist.
     *
     * @return
     *     The UserContext that was created by the AuthenticationProvider
     *     having the given identifier.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the UserContext, or if the
     *     UserContext does not exist.
     */
    public UserContext retrieveUserContext(GuacamoleSession session,
            String identifier) throws GuacamoleException {

        // Get list of UserContexts
        List<UserContext> userContexts = session.getUserContexts();

        // Locate and return the UserContext associated with the
        // AuthenticationProvider having the given identifier, if any
        for (UserContext userContext : userContexts) {
            if (userContext.getAuthenticationProvider().getIdentifier().equals(identifier))
                return userContext;
        }

        throw new GuacamoleResourceNotFoundException("Session not associated with authentication provider \"" + identifier + "\".");

    }

    /**
     * Retrieves a single user from the given user context.
     *
     * @param userContext
     *     The user context to retrieve the user from.
     *
     * @param identifier
     *     The identifier of the user to retrieve.
     *
     * @return
     *     The user having the given identifier.
     *
     * @throws GuacamoleException 
     *     If an error occurs while retrieving the user, or if the
     *     user does not exist.
     */
    public User retrieveUser(UserContext userContext,
            String identifier) throws GuacamoleException {

        // Get user directory
        Directory<User> directory = userContext.getUserDirectory();

        // Pull specified user
        User user = directory.get(identifier);
        if (user == null)
            throw new GuacamoleResourceNotFoundException("No such user: \"" + identifier + "\"");

        return user;

    }

    /**
     * Retrieves a single connection from the given user context.
     *
     * @param userContext
     *     The user context to retrieve the connection from.
     *
     * @param identifier
     *     The identifier of the connection to retrieve.
     *
     * @return
     *     The connection having the given identifier.
     *
     * @throws GuacamoleException 
     *     If an error occurs while retrieving the connection, or if the
     *     connection does not exist.
     */
    public Connection retrieveConnection(UserContext userContext,
            String identifier) throws GuacamoleException {

        // Get connection directory
        Directory<Connection> directory = userContext.getConnectionDirectory();

        // Pull specified connection
        Connection connection = directory.get(identifier);
        if (connection == null)
            throw new GuacamoleResourceNotFoundException("No such connection: \"" + identifier + "\"");

        return connection;

    }

    /**
     * Retrieves a single connection group from the given user context. If
     * the given identifier the REST API root identifier, the root connection
     * group will be returned. The underlying authentication provider may
     * additionally use a different identifier for root.
     *
     * @param userContext
     *     The user context to retrieve the connection group from.
     *
     * @param identifier
     *     The identifier of the connection group to retrieve.
     *
     * @return
     *     The connection group having the given identifier, or the root
     *     connection group if the identifier the root identifier.
     *
     * @throws GuacamoleException 
     *     If an error occurs while retrieving the connection group, or if the
     *     connection group does not exist.
     */
    public ConnectionGroup retrieveConnectionGroup(UserContext userContext,
            String identifier) throws GuacamoleException {

        // Use root group if identifier is the standard root identifier
        if (identifier != null && identifier.equals(APIConnectionGroup.ROOT_IDENTIFIER))
            return userContext.getRootConnectionGroup();

        // Pull specified connection group otherwise
        Directory<ConnectionGroup> directory = userContext.getConnectionGroupDirectory();
        ConnectionGroup connectionGroup = directory.get(identifier);

        if (connectionGroup == null)
            throw new GuacamoleResourceNotFoundException("No such connection group: \"" + identifier + "\"");

        return connectionGroup;

    }

}
