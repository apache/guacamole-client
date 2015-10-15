/*
 * Copyright (C) 2015 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.glyptodon.guacamole.net.auth.credentials;

import org.glyptodon.guacamole.GuacamoleUnauthorizedException;

/**
 * A security-related exception thrown when access is denied to a user because
 * of a problem related to the provided credentials. Additional information
 * describing the form of valid credentials is provided.
 *
 * @author Michael Jumper
 */
public class GuacamoleCredentialsException extends GuacamoleUnauthorizedException {

    /**
     * Information describing the form of valid credentials.
     */
    private final CredentialsInfo credentialsInfo;
    
    /**
     * Creates a new GuacamoleInvalidCredentialsException with the given
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
    public GuacamoleCredentialsException(String message, Throwable cause,
            CredentialsInfo credentialsInfo) {
        super(message, cause);
        this.credentialsInfo = credentialsInfo;
    }

    /**
     * Creates a new GuacamoleInvalidCredentialsException with the given
     * message and associated credential information.
     *
     * @param message
     *     A human readable description of the exception that occurred.
     *
     * @param credentialsInfo
     *     Information describing the form of valid credentials.
     */
    public GuacamoleCredentialsException(String message, CredentialsInfo credentialsInfo) {
        super(message);
        this.credentialsInfo = credentialsInfo;
    }

    /**
     * Creates a new GuacamoleInvalidCredentialsException with the given cause
     * and associated credential information.
     *
     * @param cause
     *     The cause of this exception.
     *
     * @param credentialsInfo
     *     Information describing the form of valid credentials.
     */
    public GuacamoleCredentialsException(Throwable cause, CredentialsInfo credentialsInfo) {
        super(cause);
        this.credentialsInfo = credentialsInfo;
    }

    /**
     * Returns information describing the form of valid credentials.
     *
     * @return
     *     Information describing the form of valid credentials.
     */
    public CredentialsInfo getCredentialsInfo() {
        return credentialsInfo;
    }

}
