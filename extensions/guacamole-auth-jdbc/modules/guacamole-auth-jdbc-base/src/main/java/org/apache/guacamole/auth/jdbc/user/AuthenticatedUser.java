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

package org.apache.guacamole.auth.jdbc.user;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;

/**
 * Associates a user with the credentials they used to authenticate.
 *
 * @author Michael Jumper 
 */
public class AuthenticatedUser extends RemoteAuthenticatedUser {

    /**
     * The user that authenticated.
     */
    private final ModeledUser user;

    /**
     * The connections which have been committed for use by this user in the
     * context of a balancing connection group. Balancing connection groups
     * will preferentially choose connections within this set, unless those
     * connections are not children of the group in question. If a group DOES
     * have at least one child connection within this set, no connections that
     * are not in this set will be used.
     */
    private final Set<String> preferredConnections =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    /**
     * Creates a new AuthenticatedUser associating the given user with their
     * corresponding credentials.
     *
     * @param authenticationProvider
     *     The AuthenticationProvider that has authenticated the given user.
     *
     * @param user
     *     The user this object should represent.
     *
     * @param credentials 
     *     The credentials given by the user when they authenticated.
     */
    public AuthenticatedUser(AuthenticationProvider authenticationProvider,
            ModeledUser user, Credentials credentials) {
        super(authenticationProvider, credentials);
        this.user = user;
    }

    /**
     * Returns the user that authenticated.
     *
     * @return 
     *     The user that authenticated.
     */
    public ModeledUser getUser() {
        return user;
    }

    /**
     * Returns whether the connection having the given identifier has been
     * marked as preferred for this user's current Guacamole session. A
     * preferred connection is always chosen in favor of other connections when
     * it is a child of a balancing connection group.
     *
     * @param identifier
     *     The identifier of the connection to test.
     *
     * @return
     *     true if the connection having the given identifier has been marked
     *     as preferred, false otherwise.
     */
    public boolean isPreferredConnection(String identifier) {
        return preferredConnections.contains(identifier);
    }

    /**
     * Marks the connection having the given identifier as preferred for this
     * user's current Guacamole session. A preferred connection is always chosen
     * in favor of other connections when it is a child of a balancing
     * connection group.
     *
     * @param identifier
     *     The identifier of the connection to prefer.
     */
    public void preferConnection(String identifier) {
        preferredConnections.add(identifier);
    }

    @Override
    public String getIdentifier() {
        return user.getIdentifier();
    }

    @Override
    public void setIdentifier(String identifier) {
        user.setIdentifier(identifier);
    }

}
