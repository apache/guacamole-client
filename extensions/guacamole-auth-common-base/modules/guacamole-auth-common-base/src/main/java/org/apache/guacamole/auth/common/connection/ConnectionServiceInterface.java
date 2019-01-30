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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.common.base.ActivityRecordSearchTerm;
import org.apache.guacamole.auth.common.base.ActivityRecordSortPredicate;
import org.apache.guacamole.auth.common.user.ModeledAuthenticatedUser;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.ConnectionRecord;
import org.apache.guacamole.protocol.GuacamoleClientInformation;

public interface ConnectionServiceInterface {

	/**
     * Retrieves the connection history of the given connection, including any
     * active connections.
     *
     * @param user
     *            The user retrieving the connection history.
     *
     * @param connection
     *            The connection whose history is being retrieved.
     *
     * @return The connection history of the given connection, including any
     *         active connections.
     *
     * @throws GuacamoleException
     *             If permission to read the connection history is denied.
     */
	public List<ConnectionRecord> retrieveHistory(ModeledAuthenticatedUser user,
            ModeledConnection connection) throws GuacamoleException;
	
	/**
     * Retrieves the connection history records matching the given criteria.
     * Retrieves up to <code>limit</code> connection history records matching
     * the given terms and sorted by the given predicates. Only history records
     * associated with data that the given user can read are returned.
     *
     * @param user
     *            The user retrieving the connection history.
     *
     * @param requiredContents
     *            The search terms that must be contained somewhere within each
     *            of the returned records.
     *
     * @param sortPredicates
     *            A list of predicates to sort the returned records by, in order
     *            of priority.
     *
     * @param limit
     *            The maximum number of records that should be returned.
     *
     * @return The connection history of the given connection, including any
     *         active connections.
     *
     * @throws GuacamoleException
     *             If permission to read the connection history is denied.
     */
	public List<ConnectionRecord> retrieveHistory(ModeledAuthenticatedUser user,
            Collection<ActivityRecordSearchTerm> requiredContents,
            List<ActivityRecordSortPredicate> sortPredicates, int limit)
            throws GuacamoleException;

	/**
     * Connects to the given connection as the given user, using the given
     * client information. If the user does not have permission to read the
     * connection, permission will be denied.
     *
     * @param user
     *            The user connecting to the connection.
     *
     * @param connection
     *            The connection being connected to.
     *
     * @param info
     *            Information associated with the connecting client.
     *
     * @param tokens
     *     A Map containing the token names and corresponding values to be
     *     applied as parameter tokens when establishing the connection.
     *
     * @return A connected GuacamoleTunnel associated with a newly-established
     *         connection.
     *
     * @throws GuacamoleException
     *             If permission to connect to this connection is denied.
     */
	public GuacamoleTunnel connect(ModeledAuthenticatedUser user,
            ModeledConnection connection, GuacamoleClientInformation info,
            Map<String, String> tokens) throws GuacamoleException;

	/**
     * Retrieves all parameters visible to the given user and associated with
     * the connection having the given identifier. If the given user has no
     * access to such parameters, or no such connection exists, the returned map
     * will be empty.
     *
     * @param user
     *            The user retrieving connection parameters.
     *
     * @param identifier
     *            The identifier of the connection whose parameters are being
     *            retrieved.
     *
     * @return A new map of all parameter name/value pairs that the given user
     *         has access to.
     */
	public Map<String, String> retrieveParameters(ModeledAuthenticatedUser currentUser, String identifier);

	/**
     * Returns the set of all identifiers for all connections within the
     * connection group having the given identifier. Only connections that the
     * user has read access to will be returned.
     * 
     * Permission to read the connection group having the given identifier is
     * NOT checked.
     *
     * @param user
     *            The user retrieving the identifiers.
     * 
     * @param identifier
     *            The identifier of the parent connection group, or null to
     *            check the root connection group.
     *
     * @return The set of all identifiers for all connections in the connection
     *         group having the given identifier that the user has read access
     *         to.
     *
     * @throws GuacamoleException
     *             If an error occurs while reading identifiers.
     */
	public Set<String> getIdentifiersWithin(ModeledAuthenticatedUser currentUser, String identifier) throws GuacamoleException;

	public Connection retrieveObject(ModeledAuthenticatedUser currentUser, String identifier) throws GuacamoleException;

	public Collection<ModeledConnection> retrieveObjects(ModeledAuthenticatedUser currentUser,
			Collection<String> identifiers) throws GuacamoleException;

	public Set<String> getIdentifiers(ModeledAuthenticatedUser currentUser) throws GuacamoleException;

	public ModeledConnection createObject(ModeledAuthenticatedUser currentUser, Connection object) throws GuacamoleException;

	public void updateObject(ModeledAuthenticatedUser currentUser, ModeledConnection connection) throws GuacamoleException;

	public void deleteObject(ModeledAuthenticatedUser currentUser, String identifier) throws GuacamoleException;
}
