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

package org.apache.guacamole.net.auth;

import java.util.Date;
import org.apache.guacamole.net.GuacamoleTunnel;

/**
 * A pairing of username and GuacamoleTunnel representing an active usage of a
 * particular connection.
 *
 * @author Michael Jumper
 */
public interface ActiveConnection extends Identifiable {

    /**
     * Returns the identifier of the connection being actively used. Unlike the
     * other information stored in this object, the connection identifier must
     * be present and MAY NOT be null.
     *
     * @return
     *     The identifier of the connection being actively used.
     */
    String getConnectionIdentifier();

    /**
     * Sets the identifier of the connection being actively used.
     *
     * @param connnectionIdentifier
     *     The identifier of the connection being actively used.
     */
    void setConnectionIdentifier(String connnectionIdentifier);
    
    /**
     * Returns the date and time the connection began.
     *
     * @return
     *     The date and time the connection began, or null if this
     *     information is not available.
     */
    Date getStartDate();

    /**
     * Sets the date and time the connection began.
     *
     * @param startDate 
     *     The date and time the connection began, or null if this
     *     information is not available.
     */
    void setStartDate(Date startDate);

    /**
     * Returns the hostname or IP address of the remote host that initiated the
     * connection, if known. If the hostname or IP address is not known, null
     * is returned.
     *
     * @return
     *     The hostname or IP address of the remote host, or null if this
     *     information is not available.
     */
    String getRemoteHost();

    /**
     * Sets the hostname or IP address of the remote host that initiated the
     * connection.
     * 
     * @param remoteHost 
     *     The hostname or IP address of the remote host, or null if this
     *     information is not available.
     */
    void setRemoteHost(String remoteHost);

    /**
     * Returns the name of the user who is using this connection.
     *
     * @return
     *     The name of the user who is using this connection, or null if this
     *     information is not available.
     */
    String getUsername();

    /**
     * Sets the name of the user who is using this connection.
     *
     * @param username 
     *     The name of the user who is using this connection, or null if this
     *     information is not available.
     */
    void setUsername(String username);

    /**
     * Returns the connected GuacamoleTunnel being used. This may be null if
     * access to the underlying tunnel is denied.
     *
     * @return
     *     The connected GuacamoleTunnel, or null if permission is denied.
     */
    GuacamoleTunnel getTunnel();

    /**
     * Sets the connected GuacamoleTunnel being used.
     *
     * @param tunnel
     *     The connected GuacamoleTunnel, or null if permission is denied.
     */
    void setTunnel(GuacamoleTunnel tunnel);
    
}
