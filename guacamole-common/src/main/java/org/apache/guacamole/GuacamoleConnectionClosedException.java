/*
 * Copyright (C) 2014 Glyptodon LLC
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

package org.apache.guacamole;

import org.apache.guacamole.protocol.GuacamoleStatus;


/**
 * An exception which is thrown when an operation cannot be performed because
 * its corresponding connection is closed.
 *
 * @author Michael Jumper
 */
public class GuacamoleConnectionClosedException extends GuacamoleServerException {

    /**
     * Creates a new GuacamoleConnectionClosedException with the given message
     * and cause.
     *
     * @param message A human readable description of the exception that
     *                occurred.
     * @param cause The cause of this exception.
     */
    public GuacamoleConnectionClosedException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new GuacamoleConnectionClosedException with the given message.
     *
     * @param message A human readable description of the exception that
     *                occurred.
     */
    public GuacamoleConnectionClosedException(String message) {
        super(message);
    }

    /**
     * Creates a new GuacamoleConnectionClosedException with the given cause.
     *
     * @param cause The cause of this exception.
     */
    public GuacamoleConnectionClosedException(Throwable cause) {
        super(cause);
    }

    @Override
    public GuacamoleStatus getStatus() {
        return GuacamoleStatus.SERVER_ERROR;
    }

}
