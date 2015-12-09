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

package org.glyptodon.guacamole.auth.ldap;

/**
 * All possible encryption methods which may be used when connecting to an LDAP
 * server.
 *
 * @author Michael Jumper
 */
public enum EncryptionMethod {

    /**
     * No encryption will be used. All data will be sent to the LDAP server in
     * plaintext. Unencrypted LDAP connections use port 389 by default.
     */
    NONE(389),

    /**
     * The connection to the LDAP server will be encrypted with SSL. LDAP over
     * SSL (LDAPS) will use port 636 by default.
     */
    SSL(636),

    /**
     * The connection to the LDAP server will be encrypted using STARTTLS. TLS
     * connections are negotiated over the standard LDAP port of 389 - the same
     * port used for unencrypted traffic.
     */
    STARTTLS(389);

    /**
     * The default port of this specific encryption method. As with most
     * protocols, the default port for LDAP varies by whether SSL is used.
     */
    public final int DEFAULT_PORT;

    /**
     * Initializes this encryption method such that it is associated with the
     * given default port.
     *
     * @param defaultPort
     *     The default port to associate with this encryption method.
     */
    private EncryptionMethod(int defaultPort) {
        this.DEFAULT_PORT = defaultPort;
    }

}
