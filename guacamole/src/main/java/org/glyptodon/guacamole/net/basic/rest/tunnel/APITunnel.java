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

package org.glyptodon.guacamole.net.basic.rest.tunnel;

import java.util.Date;
import org.glyptodon.guacamole.net.auth.ConnectionRecord;

/**
 * Tunnel-related information which may be exposed through the REST endpoints.
 * 
 * @author Michael Jumper
 */
public class APITunnel {

    /**
     * The identifier of the connection associated with this tunnel.
     */
    private final String identifier;
    
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
     * The UUID of the tunnel.
     */
    private final String uuid;
    
    /**
     * Creates a new APITunnel, copying the information from the given
     * connection record.
     *
     * @param record
     *     The record to copy data from.
     */
    public APITunnel(ConnectionRecord record) {
        this.identifier = record.getIdentifier();
        this.startDate  = record.getStartDate();
        this.remoteHost = record.getRemoteHost();
        this.username   = record.getUsername();
        this.uuid       = "STUB"; // STUB
    }

    /**
     * Returns the identifier of the connection associated with this tunnel.
     *
     * @return
     *     The identifier of the connection associated with this tunnel.
     */
    public String getIdentifier() {
        return identifier;
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
     * Returns the UUID of the underlying Guacamole tunnel. Absolutely every
     * Guacamole tunnel has an associated UUID.
     *
     * @return
     *     The UUID of the underlying Guacamole tunnel.
     */
    public String getUUID() {
        return uuid;
    }
    
}
