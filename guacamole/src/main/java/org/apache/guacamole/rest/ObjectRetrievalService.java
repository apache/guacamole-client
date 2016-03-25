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

package org.apache.guacamole.rest;

import java.util.List;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleResourceNotFoundException;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.ConnectionGroup;
import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.net.auth.User;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.GuacamoleSession;
import org.apache.guacamole.rest.connectiongroup.APIConnectionGroup;

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
     * @param authProviderIdentifier
     *     The unique identifier of the AuthenticationProvider that created the
     *     UserContext being retrieved. Only one UserContext per User per
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
            String authProviderIdentifier) throws GuacamoleException {

        // Get list of UserContexts
        List<UserContext> userContexts = session.getUserContexts();

        // Locate and return the UserContext associated with the
        // AuthenticationProvider having the given identifier, if any
        for (UserContext userContext : userContexts) {

            // Get AuthenticationProvider associated with current UserContext
            AuthenticationProvider authProvider = userContext.getAuthenticationProvider();

            // If AuthenticationProvider identifier matches, done
            if (authProvider.getIdentifier().equals(authProviderIdentifier))
                return userContext;

        }

        throw new GuacamoleResourceNotFoundException("Session not associated with authentication provider \"" + authProviderIdentifier + "\".");

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
     * Retrieves a single user from the given GuacamoleSession.
     *
     * @param session
     *     The GuacamoleSession to retrieve the user from.
     *
     * @param authProviderIdentifier
     *     The unique identifier of the AuthenticationProvider that created the
     *     UserContext from which the user should be retrieved. Only one
     *     UserContext per User per AuthenticationProvider can exist.
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
    public User retrieveUser(GuacamoleSession session, String authProviderIdentifier,
            String identifier) throws GuacamoleException {

        UserContext userContext = retrieveUserContext(session, authProviderIdentifier);
        return retrieveUser(userContext, identifier);

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
     * Retrieves a single connection from the given GuacamoleSession.
     *
     * @param session
     *     The GuacamoleSession to retrieve the connection from.
     *
     * @param authProviderIdentifier
     *     The unique identifier of the AuthenticationProvider that created the
     *     UserContext from which the connection should be retrieved. Only one
     *     UserContext per User per AuthenticationProvider can exist.
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
    public Connection retrieveConnection(GuacamoleSession session,
            String authProviderIdentifier, String identifier)
            throws GuacamoleException {

        UserContext userContext = retrieveUserContext(session, authProviderIdentifier);
        return retrieveConnection(userContext, identifier);

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

    /**
     * Retrieves a single connection group from the given GuacamoleSession. If
     * the given identifier is the REST API root identifier, the root
     * connection group will be returned. The underlying authentication
     * provider may additionally use a different identifier for root.
     *
     * @param session
     *     The GuacamoleSession to retrieve the connection group from.
     *
     * @param authProviderIdentifier
     *     The unique identifier of the AuthenticationProvider that created the
     *     UserContext from which the connection group should be retrieved.
     *     Only one UserContext per User per AuthenticationProvider can exist.
     *
     * @param identifier
     *     The identifier of the connection group to retrieve.
     *
     * @return
     *     The connection group having the given identifier, or the root
     *     connection group if the identifier is the root identifier.
     *
     * @throws GuacamoleException 
     *     If an error occurs while retrieving the connection group, or if the
     *     connection group does not exist.
     */
    public ConnectionGroup retrieveConnectionGroup(GuacamoleSession session,
            String authProviderIdentifier, String identifier) throws GuacamoleException {

        UserContext userContext = retrieveUserContext(session, authProviderIdentifier);
        return retrieveConnectionGroup(userContext, identifier);

    }

}
