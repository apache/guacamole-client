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

package org.apache.guacamole.auth.ssl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.ssl.SSLAuthenticationSessionManager;
import org.apache.guacamole.net.event.AuthenticationFailureEvent;
import org.apache.guacamole.net.event.AuthenticationSuccessEvent;
import org.apache.guacamole.net.event.listener.Listener;

/**
 * A Listener that will reactivate or invalidate SSL auth sessions depending on
 * overall auth success or failure.
 */
@Singleton
public class AuthenticationEventListener implements Listener {

    /**
     * Session manager for generating and maintaining unique tokens to
     * represent the authentication flow of a user who has only partially
     * authenticated.
     *
     * Requires static injection due to the fact that the webapp just calls the
     * constructor directly when creating new Listeners. The instances will not
     * be constructed by guice.
     */
    @Inject
    private static SSLAuthenticationSessionManager sessionManager;

    @Override
    public void handleEvent(Object event) throws GuacamoleException {

        if (event instanceof AuthenticationSuccessEvent)

            // After an auth attempt has fully succeeded, invalidate the session
            // associated with the successful login event so it can't be reused
            sessionManager.invalidateSession(
                AuthenticationProviderService.getSessionIdentifier(
                    ((AuthenticationSuccessEvent) event).getCredentials()));

        else if (event instanceof AuthenticationFailureEvent)

            // If the SSL auth succeeded, but other auth providers failed to
            // authenticate the user associated with the credentials in this
            // failure event, they may wish to make another login attempt. To
            // avoid an infinite login attempt loop, re-enable the session
            // associated with these credentials, allowing the auth attempt to be 
            // resumed without requiring another round trip to the SSL service.
            sessionManager.reactivateSession(
                AuthenticationProviderService.getSessionIdentifier(
                    ((AuthenticationFailureEvent) event).getCredentials()));

    }

}
