/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole.auth.jdbc.connection;

import java.util.Date;

/**
 * A single connection record representing a past usage of a particular
 * connection. If the connection was being shared, the sharing profile used to
 * join the connection is included in the record.
 */
public class ConnectionRecordModel {

    /**
     * The identifier of the connection associated with this connection record.
     */
    private String connectionIdentifier;

    /**
     * The name of the connection associated with this connection record.
     */
    private String connectionName;

    /**
     * The identifier of the sharing profile associated with this connection
     * record. If no sharing profile was used, or the sharing profile that was
     * used was deleted, this will be null.
     */
    private String sharingProfileIdentifier;

    /**
     * The name of the sharing profile associated with this connection record.
     * If no sharing profile was used, this will be null. If the sharing profile
     * that was used was deleted, this will still contain the name of the
     * sharing profile at the time that the connection was used.
     */
    private String sharingProfileName;

    /**
     * The database ID of the user associated with this connection record.
     */
    private Integer userID;

    /**
     * The username of the user associated with this connection record.
     */
    private String username;

    /**
     * The remote host associated with this connection record.
     */
    private String remoteHost;

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
     * Returns the name of the connection associated with this connection
     * record.
     *
     * @return
     *     The name of the connection associated with this connection
     *     record.
     */
    public String getConnectionName() {
        return connectionName;
    }


    /**
     * Sets the name of the connection associated with this connection
     * record.
     *
     * @param connectionName
     *     The name of the connection to associate with this connection
     *     record.
     */
    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    /**
     * Returns the identifier of the sharing profile associated with this
     * connection record. If no sharing profile was used, or the sharing profile
     * that was used was deleted, this will be null.
     *
     * @return
     *     The identifier of the sharing profile associated with this connection
     *     record, or null if no sharing profile was used or if the sharing
     *     profile that was used was deleted.
     */
    public String getSharingProfileIdentifier() {
        return sharingProfileIdentifier;
    }

    /**
     * Sets the identifier of the sharing profile associated with this
     * connection record. If no sharing profile was used, this should be null.
     *
     * @param sharingProfileIdentifier
     *     The identifier of the sharing profile associated with this
     *     connection record, or null if no sharing profile was used.
     */
    public void setSharingProfileIdentifier(String sharingProfileIdentifier) {
        this.sharingProfileIdentifier = sharingProfileIdentifier;
    }

    /**
     * Returns the human-readable name of the sharing profile associated with this
     * connection record. If no sharing profile was used, this will be null.
     *
     * @return
     *     The human-readable name of the sharing profile associated with this
     *     connection record, or null if no sharing profile was used.
     */
    public String getSharingProfileName() {
        return sharingProfileName;
    }

    /**
     * Sets the human-readable name of the sharing profile associated with this
     * connection record. If no sharing profile was used, this should be null.
     *
     * @param sharingProfileName
     *     The human-readable name of the sharing profile associated with this
     *     connection record, or null if no sharing profile was used.
     */
    public void setSharingProfileName(String sharingProfileName) {
        this.sharingProfileName = sharingProfileName;
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
     * Returns the remote host associated with this connection record.
     *
     * @return
     *     The remote host associated with this connection record.
     */
    public String getRemoteHost() {
        return remoteHost;
    }

    /**
     * Sets the remote host associated with this connection record.
     *
     * @param remoteHost
     *     The remote host to associate with this connection record.
     */
    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
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
