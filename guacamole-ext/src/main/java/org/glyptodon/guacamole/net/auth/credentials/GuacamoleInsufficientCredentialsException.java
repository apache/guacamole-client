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

/**
 * A security-related exception thrown when access is denied to a user because
 * the provided credentials are not sufficient for authentication to succeed.
 * The validity or invalidity of the given credentials is not specified, and
 * more information is needed before a decision can be made. Additional
 * information describing the form of valid credentials is provided.
 *
 * @author Michael Jumper
 */
public class GuacamoleInsufficientCredentialsException extends GuacamoleCredentialsException {

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
    }

}
