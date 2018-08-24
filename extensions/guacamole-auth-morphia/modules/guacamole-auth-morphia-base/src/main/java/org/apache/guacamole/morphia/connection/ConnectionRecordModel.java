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

package org.apache.guacamole.morphia.connection;

import org.apache.guacamole.morphia.base.ActivityRecordModel;
import org.apache.guacamole.morphia.sharingprofile.SharingProfileModel;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Reference;

/**
 * 
 * A single connection record representing a past usage of a particular
 * connection. If the connection was being shared, the sharing profile used to
 * join the connection is included in the record.
 * 
 * guacamole_connection_history: { id: string, user: UserModel, username:
 * string, remote_host: string, connection: ConnectionModel, connection_name:
 * string, sharing_profile: SharingProfileModel, sharing_profile_name: string,
 * start_date: date, end_date: date }
 *
 */
@Entity("guacamole_connection_history")
public class ConnectionRecordModel extends ActivityRecordModel {

    /**
     * The identifier of the connection associated with this connection record.
     */
    @Reference(value = "connection")
    private ConnectionModel connection;

    /**
     * The name of the connection associated with this connection record.
     */
    @Property("connection_name")
    private String connectionName;

    /**
     * The identifier of the sharing profile associated with this connection
     * record. If no sharing profile was used, or the sharing profile that was
     * used was deleted, this will be null.
     */
    @Reference(value = "sharing_profile")
    private SharingProfileModel sharingProfile;

    /**
     * The name of the sharing profile associated with this connection record.
     * If no sharing profile was used, this will be null. If the sharing profile
     * that was used was deleted, this will still contain the name of the
     * sharing profile at the time that the connection was used.
     */
    @Property("sharing_profile_name")
    private String sharingProfileName;

    /**
     * Gets the connection.
     *
     * @return the connection
     */
    public ConnectionModel getConnection() {
        return connection;
    }

    /**
     * Sets the connection.
     *
     * @param connection
     *            the new connection
     */
    public void setConnection(ConnectionModel connection) {
        this.connection = connection;
    }

    /**
     * Gets the connection name.
     *
     * @return the connection name
     */
    public String getConnectionName() {
        return connectionName;
    }

    /**
     * Sets the connection name.
     *
     * @param connectionName
     *            the new connection name
     */
    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    /**
     * Gets the sharing profile.
     *
     * @return the sharing profile
     */
    public SharingProfileModel getSharingProfile() {
        return sharingProfile;
    }

    /**
     * Sets the sharing profile.
     *
     * @param sharingProfile
     *            the new sharing profile
     */
    public void setSharingProfile(SharingProfileModel sharingProfile) {
        this.sharingProfile = sharingProfile;
    }

    /**
     * Gets the sharing profile name.
     *
     * @return the sharing profile name
     */
    public String getSharingProfileName() {
        return sharingProfileName;
    }

    /**
     * Sets the sharing profile name.
     *
     * @param sharingProfileName
     *            the new sharing profile name
     */
    public void setSharingProfileName(String sharingProfileName) {
        this.sharingProfileName = sharingProfileName;
    }

}
