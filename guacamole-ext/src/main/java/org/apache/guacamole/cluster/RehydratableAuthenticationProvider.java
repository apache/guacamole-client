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

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.Credentials;

/**
 * An AuthenticationProvider whose sessions may be rebuilt on another replica
 * from a username alone.
 *
 * Implementing this interface is a security assertion, and the default is to
 * deny. A provider may implement it only if a session of its own carries no
 * state beyond the username that authentication established. Providers that
 * fail that test, and must never implement it:
 *
 *   - Second-factor providers such as guacamole-auth-totp, which record that a
 *     session passed its second factor in memory. Rebuilding from a username
 *     alone would issue an authenticated session that never saw a TOTP code.
 *   - SSO providers, which carry assertion state from the identity provider.
 *     Rebuilding would fabricate an authentication the identity provider never
 *     issued.
 *   - Providers holding session-scoped secrets, such as guacamole-vault.
 */
public interface RehydratableAuthenticationProvider {

    /**
     * Rebuilds the AuthenticatedUser for a session which was authenticated
     * against this provider on another replica.
     *
     * Implementations MUST re-check that the account still exists and is still
     * permitted to log in, and MUST NOT accept a username that no longer
     * resolves to an account.
     *
     * @param username
     *     The identifier of the user whose session is being rebuilt.
     *
     * @param skeleton
     *     Credentials carrying the request's remote address and hostname, and
     *     no secret. A rehydrated session has no password to present.
     *
     * @return
     *     The rebuilt AuthenticatedUser, or null if the session may not be
     *     rebuilt.
     *
     * @throws GuacamoleException
     *     If an error prevents the user from being rebuilt.
     */
    AuthenticatedUser rehydrate(String username, Credentials skeleton)
            throws GuacamoleException;

}
