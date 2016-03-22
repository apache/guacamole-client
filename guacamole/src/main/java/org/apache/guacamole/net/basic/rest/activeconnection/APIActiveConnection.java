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

package org.apache.guacamole.net.basic.rest.activeconnection;

import java.util.Date;
import org.apache.guacamole.net.auth.ActiveConnection;

/**
 * Information related to active connections which may be exposed through the
 * REST endpoints.
 * 
 * @author Michael Jumper
 */
public class APIActiveConnection {

    /**
     * The identifier of the active connection itself.
     */
    private final String identifier;

    /**
     * The identifier of the connection associated with this
     * active connection.
     */
    private final String connectionIdentifier;
    
    /**
     * The date and time the connection began.
     */
    private final Date startDate;

    /**
     * The host from which the connection originated, if known.
     */
    private final String remoteHost;
    
    /**
     * The name of the user who used or is using the connection.
     */
    private final String username;

    /**
     * Creates a new APIActiveConnection, copying the information from the given
     * active connection.
     *
     * @param connection
     *     The active connection to copy data from.
     */
    public APIActiveConnection(ActiveConnection connection) {
        this.identifier           = connection.getIdentifier();
        this.connectionIdentifier = connection.getConnectionIdentifier();
        this.startDate            = connection.getStartDate();
        this.remoteHost           = connection.getRemoteHost();
        this.username             = connection.getUsername();
    }

    /**
     * Returns the identifier of the connection associated with this tunnel.
     *
     * @return
     *     The identifier of the connection associated with this tunnel.
     */
    public String getConnectionIdentifier() {
        return connectionIdentifier;
    }
    
    /**
     * Returns the date and time the connection began.
     *
     * @return
     *     The date and time the connection began.
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * Returns the remote host from which this connection originated.
     *
     * @return
     *     The remote host from which this connection originated.
     */
    public String getRemoteHost() {
        return remoteHost;
    }

    /**
     * Returns the name of the user who used or is using the connection at the
     * times given by this tunnel.
     *
     * @return
     *     The name of the user who used or is using the associated connection.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the identifier of the active connection itself. This is
     * distinct from the connection identifier, and uniquely identifies a
     * specific use of a connection.
     *
     * @return
     *     The identifier of the active connection.
     */
    public String getIdentifier() {
        return identifier;
    }
    
}
