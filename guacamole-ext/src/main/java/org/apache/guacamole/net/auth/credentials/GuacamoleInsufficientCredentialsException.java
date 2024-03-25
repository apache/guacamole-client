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

package org.apache.guacamole.net.auth.credentials;

/**
 * A security-related exception thrown when access is denied to a user because
 * the provided credentials are not sufficient for authentication to succeed.
 * The validity or invalidity of the given credentials is not specified, and
 * more information is needed before a decision can be made. Additional
 * information describing the form of valid credentials is provided.
 */
public class GuacamoleInsufficientCredentialsException extends GuacamoleCredentialsException {

/**
 * The default state token to use when no specific state information is provided.
 */
private static final String DEFAULT_STATE = "";

/**
 * The default expiration timestamp to use when no specific expiration is provided,
 * effectively indicating that the state token does not expire.
 */
private static final long DEFAULT_EXPIRES = -1L;

/**
 * An opaque value that may be used by a client to maintain state across requests
 * which are part of the same authentication transaction.
 */
protected final String state;

/**
 * The timestamp after which the state token associated with the authentication process
 * should no longer be considered valid, expressed as the number of milliseconds since
 * UNIX epoch.
 */
protected final long expires;

    /**
     * Creates a new GuacamoleInsufficientCredentialsException with the specified
     * message, the credential information required for authentication, the state
     * token associated with the authentication process, and an expiration timestamp.
     *
     * @param message
     *     A human-readable description of the exception that occurred.
     *
     * @param credentialsInfo
     *     Information describing the form of valid credentials.
     *
     * @param state
     *     An opaque value that may be used by a client to maintain state
     *     across requests which are part of the same authentication transaction.
     *
     * @param expires
     *     The timestamp after which the state token associated with the
     *     authentication process should no longer be considered valid, expressed
     *     as the number of milliseconds since UNIX epoch.
     */
   public GuacamoleInsufficientCredentialsException(String message,
            CredentialsInfo credentialsInfo, String state, long expires) {
        super(message, credentialsInfo);
        this.state = state;
        this.expires = expires;
    }

    /**
     * Creates a new GuacamoleInsufficientCredentialsException with the given
     * message, cause, and associated credential information.
     *
     * @param message
     *     A human readable description of the exception that occurred.
     *
     * @param cause
     *     The cause of this exception.
     *
     * @param credentialsInfo
     *     Information describing the form of valid credentials.
     */
    public GuacamoleInsufficientCredentialsException(String message, Throwable cause,
            CredentialsInfo credentialsInfo) {
        super(message, cause, credentialsInfo);
        this.state = DEFAULT_STATE;
        this.expires = DEFAULT_EXPIRES;
    }

    /**
     * Creates a new GuacamoleInsufficientCredentialsException with the given
     * message and associated credential information.
     *
     * @param message
     *     A human readable description of the exception that occurred.
     *
     * @param credentialsInfo
     *     Information describing the form of valid credentials.
     */
    public GuacamoleInsufficientCredentialsException(String message, CredentialsInfo credentialsInfo) {
        super(message, credentialsInfo);
        this.state = DEFAULT_STATE;
        this.expires = DEFAULT_EXPIRES;
    }

    /**
     * Creates a new GuacamoleInsufficientCredentialsException with the given
     * cause and associated credential information.
     *
     * @param cause
     *     The cause of this exception.
     *
     * @param credentialsInfo
     *     Information describing the form of valid credentials.
     */
    public GuacamoleInsufficientCredentialsException(Throwable cause, CredentialsInfo credentialsInfo) {
        super(cause, credentialsInfo);
        this.state = DEFAULT_STATE;
        this.expires = DEFAULT_EXPIRES;
    }

    /**
     * Retrieves the state token associated with the authentication process.
     *
     * @return The opaque state token used to maintain consistency across multiple
     *         requests in the same authentication transaction.
     */
    public String getState() {
        return state;
    }

    /**
     * Retrieves the expiration timestamp of the state token, specified as the
     * number of milliseconds since the UNIX epoch.
     *
     * @return The expiration timestamp of the state token, or a negative value if
     *         the token does not expire.
     */
    public long getExpires() {
        return expires;
    }

}
