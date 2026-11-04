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

package org.apache.guacamole.cluster;

/**
 * The identity a session token resolves to. Identity only: no credentials and
 * no permissions. Permissions are always re-derived from the database when a
 * session is rebuilt, so that revoking access takes effect immediately rather
 * than at token expiry.
 */
public class TokenIdentity {

    /**
     * The identifier of the authenticated user.
     */
    private final String username;

    /**
     * The identifier of the authentication provider which authenticated them.
     */
    private final String authProviderIdentifier;

    /**
     * The address the session was authenticated from.
     */
    private final String remoteAddress;

    /**
     * The hostname the session was authenticated from, or null if unknown.
     */
    private final String remoteHostname;

    /**
     * The time authentication occurred, in milliseconds since the epoch.
     */
    private final long authenticatedTime;

    /**
     * Creates a new TokenIdentity describing the session behind a token.
     *
     * @param username
     *     The identifier of the authenticated user.
     *
     * @param authProviderIdentifier
     *     The identifier of the authentication provider which authenticated
     *     them. Rehydration is allowed only for providers which declare
     *     themselves rehydratable.
     *
     * @param remoteAddress
     *     The address the session was authenticated from.
     *
     * @param remoteHostname
     *     The hostname the session was authenticated from, or null if unknown.
     *
     * @param authenticatedTime
     *     The time authentication occurred, in milliseconds since the epoch.
     */
    public TokenIdentity(String username, String authProviderIdentifier,
            String remoteAddress, String remoteHostname, long authenticatedTime) {
        this.username = username;
        this.authProviderIdentifier = authProviderIdentifier;
        this.remoteAddress = remoteAddress;
        this.remoteHostname = remoteHostname;
        this.authenticatedTime = authenticatedTime;
    }

    /**
     * Returns the identifier of the authenticated user.
     *
     * @return
     *     The identifier of the authenticated user.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the identifier of the authentication provider which
     * authenticated the user.
     *
     * @return
     *     The identifier of the authenticating provider.
     */
    public String getAuthProviderIdentifier() {
        return authProviderIdentifier;
    }

    /**
     * Returns the address the session was authenticated from.
     *
     * @return
     *     The address the session was authenticated from.
     */
    public String getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * Returns the hostname the session was authenticated from.
     *
     * @return
     *     The hostname the session was authenticated from, or null if unknown.
     */
    public String getRemoteHostname() {
        return remoteHostname;
    }

    /**
     * Returns the time authentication occurred.
     *
     * @return
     *     The time authentication occurred, in milliseconds since the epoch.
     */
    public long getAuthenticatedTime() {
        return authenticatedTime;
    }

}
