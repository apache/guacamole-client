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

import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleUnauthorizedException;
import org.apache.guacamole.auth.jdbc.user.ModeledAuthenticatedUser;
import org.apache.guacamole.auth.jdbc.user.ModeledUser;
import org.apache.guacamole.auth.jdbc.user.UserService;
import org.apache.guacamole.io.GuacamoleReader;
import org.apache.guacamole.net.DelegatingGuacamoleTunnel;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.protocol.FilteredGuacamoleReader;
import org.apache.guacamole.protocol.GuacamoleFilter;
import org.apache.guacamole.protocol.GuacamoleInstruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * A tunnel implementation that enforces access window restriction for the
 * provided ModeledUser, throwing a GuacamoleUnauthorizedException if the
 * user's configured access window has closed, or if the user has become
 * disabled. All other tunnel implementation is delegated to the underlying
 * tunnel object.
 */
public class AccessEnforcingDelegatingTunnel extends DelegatingGuacamoleTunnel {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(
            AccessEnforcingDelegatingTunnel.class);

    /**
     * The number of milliseconds between subsequent refreshes of the user
     * from the DB.
     */
    private static final long USER_MODEL_REFRESH_INTERVAL = 10000;

    /**
     * The user who's access window restrictions should be applied for the
     * wrapped tunnel.
     */
    private final AtomicReference<ModeledUser> user;

    /**
     * A thread that will continously refresh the user
     */
    private final Thread userRefreshThread;

    /**
     * A service to use for refreshing the user from the DB.
     */
    @Inject
    private UserService userService;

    /**
     * Create a new tunnel that will enforce the access window restrictions of
     * the provided user, during usage of the provided tunnel.
     *
     * @param tunnel
     *     The tunnel to delegate to.
     *
     * @param modeledAuthenticatedUser
     *     The user whose access restrictions should be applied.
     *
     */
    @AssistedInject
    public AccessEnforcingDelegatingTunnel(
            @Nonnull @Assisted GuacamoleTunnel tunnel,
            @Nonnull @Assisted ModeledAuthenticatedUser modeledAuthenticatedUser) {

        super(tunnel);
        this.user = new AtomicReference<>(modeledAuthenticatedUser.getUser());

        this.userRefreshThread = new Thread(() -> {
            while (true) {

                try {

                    // Fetch an up-to-date user record from the DB to ensure
                    // that any access restrictions modified while this tunnel
                    // is open will be taken into account
                    this.user.set(userService.retrieveUser(
                        modeledAuthenticatedUser.getAuthenticationProvider(),
                        modeledAuthenticatedUser));
                }

                // If an error occurs while trying to fetch the updated user,
                // log the warning / exception and stop the refresh thread
                catch (GuacamoleException e) {

                    logger.warn(
                            "Aborting user refresh thread due to error: {}",
                            e.getMessage());
                    logger.debug(
                            "Exception caught while attempting to refresh user.", e);

                    return;
                }

                try {

                    // Wait a bit before refreshing the user record again
                    Thread.sleep(USER_MODEL_REFRESH_INTERVAL);
                }

                // If interrupted by the tunnel, exit immediately
                catch (InterruptedException e) {
                    return;
                }
            }
        });
    }

    @Override
    public GuacamoleReader acquireReader() {

        // Start periodically refreshing the user record
        userRefreshThread.start();

        // Filter received instructions, checking if the user's login
        // is still valid for each one. If the login is invalid,
        // log them out immediately and close the tunnel.
        return new FilteredGuacamoleReader(
            super.acquireReader(),
            new GuacamoleFilter() {

                @Override
                public GuacamoleInstruction filter(
                        GuacamoleInstruction instruction) throws GuacamoleException {

                    // The user record, at most USER_MODEL_REFRESH_INTERVAL
                    // milliseconds old
                    ModeledUser modeledUser = user.get();

                    // If the user is outside of a valid access time window,
                    // or disabled, throw an exception to immediately log them out
                    if (
                            !modeledUser.isAccountAccessible()
                            || !modeledUser.isAccountValid()
                            || modeledUser.isDisabled()
                    ) {
                        throw new GuacamoleUnauthorizedException("Permission Denied.");
                    }

                    return instruction;
                }
            }
        );
    }

    @Override
    public void releaseReader() {

        // Interrupt the refresh thread; it will clean itself up
        userRefreshThread.interrupt();

        super.releaseReader();
    }

    @Override
    public void close() throws GuacamoleException {

        // Interrupt the refresh thread; it will clean itself up
        userRefreshThread.interrupt();

        super.close();
    }

}
