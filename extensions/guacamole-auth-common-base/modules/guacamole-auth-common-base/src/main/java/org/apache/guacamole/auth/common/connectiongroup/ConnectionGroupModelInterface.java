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

package org.apache.guacamole.auth.common.connectiongroup;

import java.util.Set;

import org.apache.guacamole.auth.common.base.ChildObjectModelInterface;
import org.apache.guacamole.net.auth.ConnectionGroup.Type;

/**
 * Object representation of a Guacamole connection group, as represented in the
 * database.
 * 
 */
public interface ConnectionGroupModelInterface extends ChildObjectModelInterface {

	/**
     * Returns the name associated with this connection group.
     *
     * @return
     *     The name associated with this connection group.
     */
	public String getName();
	
	/**
     * Sets the name associated with this connection group.
     *
     * @param name
     *     The name to associate with this connection group.
     */
	public void setName(String name);
	
	/**
     * Sets the type of this connection group, such as organizational or
     * balancing.
     *
     * @param type
     *     The type of this connection group.
     */
	public void setType(Type type);

	/**
     * Returns the type of this connection group, such as organizational or
     * balancing.
     *
     * @return
     *     The type of this connection group.
     */
	public Type getType();

	/**
     * Returns the identifiers of all readable child connections within this
     * connection group. This is set only when the parent connection group is
     * queried, and has no effect when a connection group is inserted, updated,
     * or deleted.
     *
     * @return
     *     The identifiers of all readable child connections within this
     *     connection group.
     */
	public Set<String> getConnectionIdentifiers();

	/**
     * Returns the identifiers of all readable child connection groups within
     * this connection group. This is set only when the parent connection group
     * is queried, and has no effect when a connection group is inserted,
     * updated, or deleted.
     *
     * @return
     *     The identifiers of all readable child connection groups within this
     *     connection group.
     */
	public Set<String> getConnectionGroupIdentifiers();

	/**
     * Returns the maximum number of connections that can be established to
     * this connection group concurrently.
     *
     * @return
     *     The maximum number of connections that can be established to this
     *     connection group concurrently, zero if no restriction applies, or
     *     null if the default restrictions should be applied.
     */
	public Integer getMaxConnections();

	/**
     * Returns the maximum number of connections that can be established to
     * this connection group concurrently by any one user.
     *
     * @return
     *     The maximum number of connections that can be established to this
     *     connection group concurrently by any one user, zero if no
     *     restriction applies, or null if the default restrictions should be
     *     applied.
     */
	public Integer getMaxConnectionsPerUser();

	/**
     * Returns whether individual users should be consistently assigned the same
     * connection within a balancing group until they log out.
     *
     * @return
     *     Whether individual users should be consistently assigned the same
     *     connection within a balancing group until they log out.
     */
	public boolean isSessionAffinityEnabled();

	/**
     * Sets the maximum number of connections that can be established to this
     * connection group concurrently.
     *
     * @param maxConnections
     *     The maximum number of connections that can be established to this
     *     connection group concurrently, zero if no restriction applies, or
     *     null if the default restrictions should be applied.
     */
	public void setMaxConnections(Integer parse);

	/**
     * Sets the maximum number of connections that can be established to this
     * connection group concurrently by any one user.
     *
     * @param maxConnectionsPerUser
     *     The maximum number of connections that can be established to this
     *     connection group concurrently by any one user, zero if no
     *     restriction applies, or null if the default restrictions should be
     *     applied.
     */
	public void setMaxConnectionsPerUser(Integer parse);

	/**
     * Sets whether individual users should be consistently assigned the same
     * connection within a balancing group until they log out.
     *
     * @param sessionAffinityEnabled
     *     Whether individual users should be consistently assigned the same
     *     connection within a balancing group until they log out.
     */
	public void setSessionAffinityEnabled(boolean equals);

}
