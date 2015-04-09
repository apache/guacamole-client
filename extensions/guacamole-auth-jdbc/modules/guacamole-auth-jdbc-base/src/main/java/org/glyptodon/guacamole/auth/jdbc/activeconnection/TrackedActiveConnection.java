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

package org.glyptodon.guacamole.auth.jdbc.activeconnection;

import java.util.Date;
import org.glyptodon.guacamole.auth.jdbc.base.RestrictedObject;
import org.glyptodon.guacamole.auth.jdbc.tunnel.ActiveConnectionRecord;
import org.glyptodon.guacamole.auth.jdbc.user.AuthenticatedUser;
import org.glyptodon.guacamole.net.GuacamoleTunnel;
import org.glyptodon.guacamole.net.auth.ActiveConnection;

/**
 * An implementation of the ActiveConnection object which has an associated
 * ActiveConnectionRecord.
 *
 * @author Michael Jumper
 */
public class TrackedActiveConnection extends RestrictedObject implements ActiveConnection {

    /**
     * The identifier of this active connection.
     */
    private String identifier;

    /**
     * The identifier of the associated connection.
     */
    private String connectionIdentifier;

    /**
     * The date and time this active connection began.
     */
    private Date startDate;

    /**
     * The remote host that initiated this connection.
     */
    private String remoteHost;

    /**
     * The username of the user that initiated this connection.
     */
    private String username;

    /**
     * The underlying GuacamoleTunnel.
     */
    private GuacamoleTunnel tunnel;

    /**
     * Initializes this TrackedActiveConnection, copying the data associated
     * with the given active connection record. At a minimum, the identifier
     * of this active connection will be set, the start date, and the
     * identifier of the associated connection will be copied. If requested,
     * sensitive information like the associated username will be copied, as
     * well.
     *
     * @param currentUser
     *     The user that created or retrieved this object.
     *
     * @param activeConnectionRecord
     *     The active connection record to copy.
     *
     * @param includeSensitiveInformation
     *     Whether sensitive data should be copied from the connection record
     *     as well. This includes the remote host, associated tunnel, and
     *     username.
     */
    public void init(AuthenticatedUser currentUser,
            ActiveConnectionRecord activeConnectionRecord,
            boolean includeSensitiveInformation) {

        super.init(currentUser);
        
        // Copy all non-sensitive data from given record
        this.connectionIdentifier = activeConnectionRecord.getConnection().getIdentifier();
        this.identifier           = activeConnectionRecord.getUUID().toString();
        this.startDate            = activeConnectionRecord.getStartDate();

        // Include sensitive data, too, if requested
        if (includeSensitiveInformation) {
            this.remoteHost = activeConnectionRecord.getRemoteHost();
            this.tunnel     = activeConnectionRecord.getTunnel();
            this.username   = activeConnectionRecord.getUsername();
        }

    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
 
    @Override
    public String getConnectionIdentifier() {
        return connectionIdentifier;
    }

    @Override
    public void setConnectionIdentifier(String connnectionIdentifier) {
        this.connectionIdentifier = connnectionIdentifier;
    }

    @Override
    public Date getStartDate() {
        return startDate;
    }

    @Override
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    @Override
    public String getRemoteHost() {
        return remoteHost;
    }

    @Override
    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public GuacamoleTunnel getTunnel() {
        return tunnel;
    }

    @Override
    public void setTunnel(GuacamoleTunnel tunnel) {
        this.tunnel = tunnel;
    }

}
