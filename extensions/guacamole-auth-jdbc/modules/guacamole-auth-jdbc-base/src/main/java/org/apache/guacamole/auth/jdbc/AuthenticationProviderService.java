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

package org.apache.guacamole.auth.jdbc;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.UserContext;

/**
 * Service which authenticates users based on credentials and provides for
 * the creation of corresponding, new UserContext objects for authenticated
 * users.
 *
 * @author Michael Jumper
 */
public interface AuthenticationProviderService  {

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
            Credentials credentials) throws GuacamoleException;

    /**
     * Returning a new UserContext instance for the given already-authenticated
     * user. A new placeholder account will be created for any user that does
     * not already exist within the database.
     *
     * @param authenticationProvider
     *     The AuthenticationProvider on behalf of which the UserContext is
     *     being produced.
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
    public UserContext getUserContext(AuthenticationProvider authenticationProvider,
            AuthenticatedUser authenticatedUser) throws GuacamoleException;

}
