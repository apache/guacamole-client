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

package org.glyptodon.guacamole.auth.jdbc.connection;

import java.util.Date;

/**
 * A single connection record representing a past usage of a particular
 * connection.
 *
 * @author Michael Jumper
 */
public class ConnectionRecordModel {

    /**
     * The identifier of the connection associated with this connection record.
     */
    private String connectionIdentifier;

    /**
     * The database ID of the user associated with this connection record.
     */
    private Integer userID;

    /**
     * The username of the user associated with this connection record.
     */
    private String username;

    /**
     * The time the connection was initiated by the associated user.
     */
    private Date startDate;

    /**
     * The time the connection ended, or null if the end time is not known or
     * the connection is still running.
     */
    private Date endDate;

    /**
     * Returns the identifier of the connection associated with this connection
     * record.
     *
     * @return
     *     The identifier of the connection associated with this connection
     *     record.
     */
    public String getConnectionIdentifier() {
        return connectionIdentifier;
    }

    /**
     * Sets the identifier of the connection associated with this connection
     * record.
     *
     * @param connectionIdentifier
     *     The identifier of the connection to associate with this connection
     *     record.
     */
    public void setConnectionIdentifier(String connectionIdentifier) {
        this.connectionIdentifier = connectionIdentifier;
    }

    /**
     * Returns the database ID of the user associated with this connection
     * record.
     * 
     * @return
     *     The database ID of the user associated with this connection record.
     */
    public Integer getUserID() {
        return userID;
    }

    /**
     * Sets the database ID of the user associated with this connection record.
     *
     * @param userID
     *     The database ID of the user to associate with this connection
     *     record.
     */
    public void setUserID(Integer userID) {
        this.userID = userID;
    }

    /**
     * Returns the username of the user associated with this connection record.
     * 
     * @return
     *     The username of the user associated with this connection record.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username of the user associated with this connection record.
     *
     * @param username
     *     The username of the user to associate with this connection record.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the date that the associated connection was established.
     *
     * @return
     *     The date the associated connection was established.
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * Sets the date that the associated connection was established.
     *
     * @param startDate
     *     The date that the associated connection was established.
     */
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    /**
     * Returns the date that the associated connection ended, or null if no
     * end date was recorded. The lack of an end date does not necessarily
     * mean that the connection is still active.
     *
     * @return
     *     The date the associated connection ended, or null if no end date was
     *     recorded.
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * Sets the date that the associated connection ended.
     *
     * @param endDate
     *     The date that the associated connection ended.
     */
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

}
