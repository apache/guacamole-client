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

package org.apache.guacamole.morphia.base;

import java.util.Date;

import org.apache.guacamole.morphia.user.UserModel;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Reference;

/**
 * A single activity record representing an arbitrary activity performed by a
 * user.
 */
public class ActivityRecordModel {

    /** The id. */
    @Id
    @Property("id")
    private ObjectId id;

    /** The user. */
    @Reference(value = "user")
    private UserModel user;

    /** The username. */
    @Property("username")
    private String username;

    /** The remote host. */
    @Property("remote_host")
    private String remoteHost;

    /** The start date. */
    @Property("start_date")
    private Date startDate;

    /** The end date. */
    @Property("end_date")
    private Date endDate;

    /**
     * Gets the id.
     *
     * @return the id
     */
    public String getId() {
        return id.toString();
    }

    /**
     * Sets the id.
     *
     * @param id
     *            the new id
     */
    public void setId(String id) {
        this.id = new ObjectId(id);
    }

    /**
     * Gets the user.
     *
     * @return the user
     */
    public UserModel getUser() {
        return user;
    }

    /**
     * Sets the user.
     *
     * @param user
     *            the new user
     */
    public void setUser(UserModel user) {
        this.user = user;
    }

    /**
     * Returns the username of the user that performed the activity associated
     * with this record.
     * 
     * @return The username of the user that performed the activity associated
     *         with this record.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username of the user that performed the activity associated with
     * this record.
     *
     * @param username
     *            The username of the user that performed the activity
     *            associated with this record.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the remote host associated with the user that performed the
     * activity.
     *
     * @return The remote host associated with the user that performed the
     *         activity.
     */
    public String getRemoteHost() {
        return remoteHost;
    }

    /**
     * Sets the remote host associated with the user that performed the
     * activity.
     *
     * @param remoteHost
     *            The remote host associated with the user that performed the
     *            activity.
     */
    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    /**
     * Returns the time the activity was initiated by the associated user.
     *
     * @return The time the activity was initiated by the associated user.
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * Sets the time the activity was initiated by the associated user.
     *
     * @param startDate
     *            The time the activity was initiated by the associated user.
     */
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    /**
     * Returns the time the activity ended, or null if the end time is not known
     * or the activity is still in progress.
     *
     * @return The time the activity ended, or null if the end time is not known
     *         or the activity is still in progress.
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * Sets the time the activity ended, if known.
     *
     * @param endDate
     *            The time the activity ended, or null if the end time is not
     *            known or the activity is still in progress.
     */
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

}
