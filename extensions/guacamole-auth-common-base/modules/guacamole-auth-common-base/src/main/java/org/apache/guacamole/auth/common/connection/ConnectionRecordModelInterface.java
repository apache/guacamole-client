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

package org.apache.guacamole.auth.common.connection;

import org.apache.guacamole.auth.common.base.ActivityRecordModelInterface;

/**
 * 
 * A single connection record representing a past usage of a particular
 * connection. If the connection was being shared, the sharing profile used to
 * join the connection is included in the record.
 * 
 */
public interface ConnectionRecordModelInterface extends ActivityRecordModelInterface {

	/**
     * Returns the name of the connection associated with this connection
     * record.
     *
     * @return
     *     The name of the connection associated with this connection
     *     record.
     */
	public String getConnectionName();

	/**
     * Returns the human-readable name of the sharing profile associated with this
     * connection record. If no sharing profile was used, this will be null.
     *
     * @return
     *     The human-readable name of the sharing profile associated with this
     *     connection record, or null if no sharing profile was used.
     */
	public String getSharingProfileName();

	/**
     * Sets the name of the connection associated with this connection
     * record.
     *
     * @param connectionName
     *     The name of the connection to associate with this connection
     *     record.
     */
	public void setConnectionName(String connectionName);

	/**
     * Sets the human-readable name of the sharing profile associated with this
     * connection record. If no sharing profile was used, this should be null.
     *
     * @param sharingProfileName
     *     The human-readable name of the sharing profile associated with this
     *     connection record, or null if no sharing profile was used.
     */
	public void setSharingProfileName(String sharingProfileName);

	/**
     * Returns the identifier of the connection associated with this connection
     * record.
     *
     * @return
     *     The identifier of the connection associated with this connection
     *     record.
     */
	public String getConnectionIdentifier();

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
	public String getSharingProfileIdentifier();

}
