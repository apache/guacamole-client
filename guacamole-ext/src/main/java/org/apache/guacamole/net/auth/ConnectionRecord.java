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

package org.apache.guacamole.net.auth;

import java.util.Date;

/**
 * A logging record describing when a user started and ended usage of a
 * particular connection.
 */
public interface ConnectionRecord {

    /**
     * Returns the identifier of the connection associated with this
     * connection record.
     *
     * @return
     *     The identifier of the connection associated with this connection
     *     record.
     */
    public String getConnectionIdentifier();
    
    /**
     * Returns the name of the connection associated with this connection
     * record.
     *
     * @return
     *     The name of the connection associated with this connection record.
     */
    public String getConnectionName();

    /**
     * Returns the identifier of the sharing profile that was used to access the
     * connection associated with this connection record. If the connection was
     * accessed directly (without involving a sharing profile), this will be
     * null.
     *
     * @return
     *     The identifier of the sharing profile used to access the connection
     *     associated with this connection record, or null if the connection
     *     was accessed directly.
     */
    public String getSharingProfileIdentifier();

    /**
     * Returns the name of the sharing profile that was used to access the
     * connection associated with this connection record. If the connection was
     * accessed directly (without involving a sharing profile), this will be
     * null.
     *
     * @return
     *     The name of the sharing profile used to access the connection
     *     associated with this connection record, or null if the connection
     *     was accessed directly.
     */
    public String getSharingProfileName();

    /**
     * Returns the date and time the connection began.
     *
     * @return The date and time the connection began.
     */
    public Date getStartDate();

    /**
     * Returns the date and time the connection ended, if applicable.
     *
     * @return The date and time the connection ended, or null if the
     *         connection is still running or if the end time is unknown.
     */
    public Date getEndDate();

    /**
     * Returns the hostname or IP address of the remote host that used the
     * connection associated with this record, if known. If the hostname or IP
     * address is not known, null is returned.
     *
     * @return
     *     The hostname or IP address of the remote host, or null if this
     *     information is not available.
     */
    public String getRemoteHost();

    /**
     * Returns the name of the user who used or is using the connection at the
     * times given by this connection record.
     *
     * @return The name of the user who used or is using the associated
     *         connection.
     */
    public String getUsername();

    /**
     * Returns whether the connection associated with this record is still
     * active.
     *
     * @return true if the connection associated with this record is still
     *         active, false otherwise.
     */
    public boolean isActive();

}
