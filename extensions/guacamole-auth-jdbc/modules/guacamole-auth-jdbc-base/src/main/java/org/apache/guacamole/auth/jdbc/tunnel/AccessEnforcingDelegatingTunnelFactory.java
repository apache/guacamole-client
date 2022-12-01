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

package org.apache.guacamole.auth.jdbc.tunnel;

import javax.annotation.Nonnull;

import org.apache.guacamole.auth.jdbc.user.ModeledAuthenticatedUser;
import org.apache.guacamole.net.GuacamoleTunnel;

/**
 * A factory for creating AccessEnforcingDelegatingTunnel instances.
 */
public interface AccessEnforcingDelegatingTunnelFactory {

    /**
     * Create and return a new AccessEnforcingDelegatingTunnel wrapping the
     * provided tunnel and enforcing the user restrictions associated with the
     * provided user.
     *
     * @param tunnel
     *     The tunnel to delegate to.
     *
     * @param user
     *     The user whose access window restrictions should be applied for the
     *     wrapped tunel.
     *
     * @return
     *     A new AccessEnforcingDelegatingTunnel wrapping the provided tunnel
     *     and enforcing the user restrictions associated with the provided
     *     user.
     *
     */
    public AccessEnforcingDelegatingTunnel create(
            @Nonnull GuacamoleTunnel tunnel,
            @Nonnull ModeledAuthenticatedUser user);

}
