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

package org.apache.guacamole.auth.saml.acs;

import com.google.common.base.Predicates;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manager service that temporarily stores SAML authentication attempts while
 * the authentication flow is underway. Authentication attempts are represented
 * as temporary authentication sessions, allowing authentication attempts to
 * span multiple requests and redirects. Invalid or stale authentication
 * sessions are automatically purged from storage.
 */
@Singleton
public class AuthenticationSessionManager {

    /**
     * Generator of arbitrary, unique, unpredictable identifiers.
     */
    @Inject
    private IdentifierGenerator idGenerator;

    /**
     * Map of authentication session identifiers to their associated
     * {@link AuthenticationSession}.
     */
    private final ConcurrentMap<String, AuthenticationSession> sessions =
        new ConcurrentHashMap<>();

    /**
     * Executor service which runs the periodic cleanup task
     */
    private final ScheduledExecutorService executor =
            Executors.newScheduledThreadPool(1);

    /**
     * Creates a new AuthenticationSessionManager that manages in-progress
     * SAML authentication attempts. Invalid, stale sessions are automatically
     * cleaned up.
     */
    public AuthenticationSessionManager() {
        executor.scheduleAtFixedRate(() -> {
            sessions.values().removeIf(Predicates.not(AuthenticationSession::isValid));
        }, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Resumes the Guacamole side of the authentication process that was
     * previously deferred through a call to defer(). Once invoked, the
     * provided value ceases to be valid for future calls to resume().
     *
     * @param identifier
     *     The unique string returned by the call to defer(). For convenience,
     *     this value may safely be null.
     *
     * @return
     *     The {@link AuthenticationSession} originally provided when defer()
     *     was invoked, or null if the session is no longer valid or no such
     *     value was returned by defer().
     */
    public AuthenticationSession resume(String identifier) {

        if (identifier != null) {
            AuthenticationSession session = sessions.remove(identifier);
            if (session != null && session.isValid())
                return session;
        }

        return null;

    }

    /**
     * Returns the identity finally asserted by the SAML IdP at the end of the
     * authentication process represented by the authentication session with
     * the given identifier. If there is no such authentication session, or no
     * valid identity has been asserted by the SAML IdP for that session, null
     * is returned.
     *
     * @param identifier
     *     The unique string returned by the call to defer(). For convenience,
     *     this value may safely be null.
     *
     * @return
     *     The identity finally asserted by the SAML IdP at the end of the
     *     authentication process represented by the authentication session
     *     with the given identifier, or null if there is no such identity.
     */
    public AssertedIdentity getIdentity(String identifier) {

        AuthenticationSession session = resume(identifier);
        if (session != null)
            return session.getIdentity();

        return null;

    }

    /**
     * Defers the Guacamole side of authentication for the user having the
     * given authentication session such that it may be later resumed through a
     * call to resume(). If authentication is never resumed, the session will
     * automatically be cleaned up after it ceases to be valid.
     *
     * @param session
     *     The {@link AuthenticationSession} representing the in-progress SAML
     *     authentication attempt.
     *
     * @return
     *     A unique and unpredictable string that may be used to represent the
     *     given session when calling resume().
     */
    public String defer(AuthenticationSession session) {
        String identifier = idGenerator.generateIdentifier();
        sessions.put(identifier, session);
        return identifier;
    }

    /**
     * Shuts down the executor service that periodically removes all invalid
     * authentication sessions. This must be invoked when the SAML extension is
     * shut down in order to avoid resource leaks.
     */
    public void shutdown() {
        executor.shutdownNow();
    }

}
