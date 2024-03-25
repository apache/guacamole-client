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
package org.apache.guacamole.rest.auth;

import org.apache.guacamole.net.auth.Credentials;

/**
 * Encapsulates the state information required for resuming an authentication
 * process. This includes an expiration timestamp to determine state validity
 * and the original credentials submitted by the user.
 */
public class ResumableAuthenticationState {
    
    /**
     * The timestamp at which this state should no longer be considered valid,
     * measured in milliseconds since the Unix epoch.
     */
    private long expirationTimestamp;
    
    /**
     * The original user credentials that were submitted at the start of the
     * authentication process.
     */
    private Credentials credentials;

    /**
     * Constructs a new ResumableAuthenticationState object with the specified
     * expiration timestamp and user credentials.
     *
     * @param expirationTimestamp
     *     The timestamp in milliseconds since the Unix epoch when this state
     *     expires and can no longer be used to resume authentication.
     *
     * @param credentials
     *     The Credentials object initially submitted by the user and associated
     *     with this resumable state.
     */
    public ResumableAuthenticationState(long expirationTimestamp, Credentials credentials) {
        this.expirationTimestamp = expirationTimestamp;
        this.credentials = credentials;
    }

    /**
     * Checks if this resumable state has expired based on the stored expiration
     * timestamp and the current system time.
     *
     * @return 
     *     True if the current system time is after the expiration timestamp,
     *     indicating that the state is expired; false otherwise.
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expirationTimestamp;
    }

    /**
     * Retrieves the original credentials associated with this resumable state.
     *
     * @return 
     *     The Credentials object containing user details that were submitted
     *     when the state was created.
     */
    public Credentials getCredentials() {
        return this.credentials;
    }
}
