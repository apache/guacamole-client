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

package org.glyptodon.guacamole.net.basic.rest;

import org.glyptodon.guacamole.net.auth.credentials.CredentialsInfo;

/**
 * Represents an error related to either invalid or insufficient credentials
 * submitted to a REST endpoint.
 *
 * @author Michael Jumper
 */
public class APICredentialError extends APIError {

    /**
     * The required credentials.
     */
    private final CredentialsInfo info;

    /**
     * The type of error that occurred.
     */
    private final Type type;

    /**
     * All possible types of credential errors.
     */
    public enum Type {

        /**
         * The credentials provided were invalid.
         */
        INVALID,

        /**
         * The credentials provided were not necessarily invalid, but were not
         * sufficient to determine validity.
         */
        INSUFFICIENT

    }

    /**
     * Create a new APICredentialError with the specified error message and
     * credentials information.
     *
     * @param type
     *     The type of error that occurred.
     *
     * @param message
     *     The error message.
     *
     * @param info
     *     An object which describes the required credentials.
     */
    public APICredentialError(Type type, String message, CredentialsInfo info) {
        super(message);
        this.type = type;
        this.info = info;
    }

    /**
     * Returns the type of error that occurred.
     *
     * @return
     *     The type of error that occurred.
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns an object which describes the required credentials.
     *
     * @return
     *     An object which describes the required credentials.
     */
    public CredentialsInfo getInfo() {
        return info;
    }

}
