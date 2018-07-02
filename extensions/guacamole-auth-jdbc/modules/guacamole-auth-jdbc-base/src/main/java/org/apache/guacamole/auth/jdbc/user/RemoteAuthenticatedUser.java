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

import java.util.HashMap;
import java.util.Map;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;

/**
 * An AuthenticatedUser that has an associated remote host.
 */
public abstract class RemoteAuthenticatedUser implements AuthenticatedUser {

    /**
     * The credentials given when this user authenticated.
     */
    private final Credentials credentials;

    /**
     * The AuthenticationProvider that authenticated this user.
     */
    private final AuthenticationProvider authenticationProvider;

    /**
     * The host from which this user authenticated.
     */
    private final String remoteHost;

    /**
     * Arbitrary attributes associated with this RemoteAuthenticatedUser object.
     */
    private Map<String, String> attributes = new HashMap<String, String>();

    @Override
    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Override
    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    /**
     * Creates a new RemoteAuthenticatedUser, deriving the associated remote
     * host from the given credentials.
     *
     * @param authenticationProvider
     *     The AuthenticationProvider that has authenticated the given user.
     *
     * @param credentials
     *     The credentials given by the user when they authenticated.
     */
    public RemoteAuthenticatedUser(AuthenticationProvider authenticationProvider,
            Credentials credentials) {
        this.authenticationProvider = authenticationProvider;
        this.credentials = credentials;
        this.remoteHost = credentials.getRemoteAddress();
    }

    @Override
    public Credentials getCredentials() {
        return credentials;
    }

    /**
     * Returns the host from which this user authenticated.
     *
     * @return
     *     The host from which this user authenticated.
     */
    public String getRemoteHost() {
        return remoteHost;
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    @Override
    public void invalidate() {
        // Nothing to invalidate
    }

}
