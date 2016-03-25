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
     * The name of the connection associated with this connection record.
     */
    private String connectionName;

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
