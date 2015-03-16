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

package org.glyptodon.guacamole.net.basic.rest.connection;

import java.util.Date;
import org.glyptodon.guacamole.net.auth.ConnectionRecord;

/**
 * A connection record which may be exposed through the REST endpoints.
 * 
 * @author Michael Jumper
 */
public class APIConnectionRecord {

    /**
     * The date and time the connection began.
     */
    private final Date startDate;

    /**
     * The date and time the connection ended, or null if the connection is
     * still running or if the end time is unknown.
     */
    private final Date endDate;

    /**
     * The host from which the connection originated, if known.
     */
    private final String remoteHost;
    
    /**
     * The name of the user who used or is using the connection.
     */
    private final String username;

    /**
     * Whether the connection is currently active.
     */
    private final boolean active;

    /**
     * Creates a new APIConnectionRecord, copying the data from the given
     * record.
     *
     * @param record
     *     The record to copy data from.
     */
    public APIConnectionRecord(ConnectionRecord record) {
        this.startDate  = record.getStartDate();
        this.endDate    = record.getEndDate();
        this.remoteHost = record.getRemoteHost();
        this.username   = record.getUsername();
        this.active     = record.isActive();
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
     * Returns the date and time the connection ended, if applicable.
     *
     * @return
     *     The date and time the connection ended, or null if the connection is
     *     still running or if the end time is unknown.
     */
    public Date getEndDate() {
        return endDate;
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
     * times given by this connection record.
     *
     * @return
     *     The name of the user who used or is using the associated connection.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns whether the connection associated with this record is still
     * active.
     *
     * @return
     *     true if the connection associated with this record is still active,
     *     false otherwise.
     */
    public boolean isActive() {
        return active;
    }
    
}
