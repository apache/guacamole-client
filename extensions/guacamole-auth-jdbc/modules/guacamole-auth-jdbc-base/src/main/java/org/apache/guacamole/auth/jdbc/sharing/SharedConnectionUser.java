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

package org.apache.guacamole.auth.jdbc.sharing;

import java.util.UUID;
import org.apache.guacamole.auth.jdbc.user.RemoteAuthenticatedUser;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;

/**
 * A temporary user who has authenticated using a share key and thus has
 * restricted access to a single shared connection.
 *
 * @author Michael Jumper
 */
public class SharedConnectionUser extends RemoteAuthenticatedUser {

    /**
     * The single shared connection to which this user has access.
     */
    private final SharedConnectionDefinition definition;

    /**
     * An arbitrary identifier guaranteed to be unique across users. Note that
     * because Guacamole users the AuthenticatedUser's identifier as the means
     * of determining overall user identity and aggregating data across
     * multiple extensions, this identifier MUST NOT match the identifier of
     * any possibly existing user (or else the user may unexpectedly gain
     * access to another identically-named user's data).
     */
    private final String identifier = UUID.randomUUID().toString();

    /**
     * Creates a new SharedConnectionUser with access solely to connection
     * described by the given SharedConnectionDefinition.
     *
     * @param authenticationProvider
     *     The AuthenticationProvider that has authenticated the given user.
     *
     * @param definition
     *     The SharedConnectionDefinition describing the connection that this
     *     user should have access to, along with any associated restrictions.
     *
     * @param credentials 
     *     The credentials given by the user when they authenticated.
     */
    public SharedConnectionUser(AuthenticationProvider authenticationProvider,
           SharedConnectionDefinition definition, Credentials credentials) {
        super(authenticationProvider, credentials);
        this.definition = definition;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(String identifier) {
        throw new UnsupportedOperationException("Shared connection users are immutable");
    }

    /**
     * Returns the SharedConnectionDefinition which describes the connection
     * that this user should have access to, along with any associated
     * restrictions.
     *
     * @return
     *     The SharedConnectionDefinition describing the connection that this
     *     user should have access to, along with any associated restrictions.
     */
    public SharedConnectionDefinition getSharedConnectionDefinition() {
        return definition;
    }

}
