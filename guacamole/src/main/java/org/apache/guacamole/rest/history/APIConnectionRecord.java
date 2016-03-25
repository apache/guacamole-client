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

package org.apache.guacamole.rest.history;

import java.util.Date;
import org.apache.guacamole.net.auth.ConnectionRecord;

/**
 * A connection record which may be exposed through the REST endpoints.
 *
 * @author Michael Jumper
 */
public class APIConnectionRecord {

    /**
     * The identifier of the connection associated with this record.
     */
    private final String connectionIdentifier;

    /**
     * The identifier of the connection associated with this record.
     */
    private final String connectionName;

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
        this.connectionIdentifier = record.getConnectionIdentifier();
        this.connectionName       = record.getConnectionName();
        this.startDate            = record.getStartDate();
        this.endDate              = record.getEndDate();
        this.remoteHost           = record.getRemoteHost();
        this.username             = record.getUsername();
        this.active               = record.isActive();
    }

    /**
     * Returns the identifier of the connection associated with this
     * record.
     *
     * @return
     *     The identifier of the connection associated with this record.
     */
    public String getConnectionIdentifier() {
        return connectionIdentifier;
    }

    /**
     * Returns the name of the connection associated with this record.
     *
     * @return
     *     The name of the connection associated with this record.
     */
    public String getConnectionName() {
        return connectionName;
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
