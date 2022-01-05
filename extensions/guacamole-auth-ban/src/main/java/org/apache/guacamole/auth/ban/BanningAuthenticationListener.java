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

package org.apache.guacamole.auth.ban;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.credentials.GuacamoleInsufficientCredentialsException;
import org.apache.guacamole.net.event.AuthenticationFailureEvent;
import org.apache.guacamole.net.event.AuthenticationSuccessEvent;
import org.apache.guacamole.net.event.listener.Listener;

/**
 * Listener implementation which automatically tracks authentication failures
 * such that further authentication attempts may be automatically blocked by
 * {@link BanningAuthenticationProvider} if they match configured criteria.
 */
public class BanningAuthenticationListener implements Listener {

    /**
     * Shared tracker of addresses that have repeatedly failed authentication.
     */
    private static AuthenticationFailureTracker tracker;

    /**
     * Assigns the shared tracker instance used by both the {@link BanningAuthenticationProvider}
     * and this listener. This function MUST be invoked with the tracker
     * created for BanningAuthenticationProvider as soon as possible (during
     * construction of BanningAuthenticationProvider), or processing of
     * received events will fail internally.
     *
     * @param tracker
     *     The tracker instance to use for received authentication events.
     */
    public static void setAuthenticationFailureTracker(AuthenticationFailureTracker tracker) {
        BanningAuthenticationListener.tracker = tracker;
    }

    @Override
    public void handleEvent(Object event) throws GuacamoleException {

        if (event instanceof AuthenticationFailureEvent) {

            AuthenticationFailureEvent failure = (AuthenticationFailureEvent) event;

            // Requests for additional credentials are not failures per se,
            // but continuations of a multi-request authentication attempt that
            // has not yet succeeded OR failed
            if (failure.getFailure() instanceof GuacamoleInsufficientCredentialsException) {
                tracker.notifyAuthenticationRequestReceived(failure.getCredentials());
                return;
            }

            // Consider all other errors to be failed auth attempts
            tracker.notifyAuthenticationFailed(failure.getCredentials());

        }

        else if (event instanceof AuthenticationSuccessEvent) {
            AuthenticationSuccessEvent success = (AuthenticationSuccessEvent) event;
            tracker.notifyAuthenticationSuccess(success.getCredentials());
        }

    }

}
