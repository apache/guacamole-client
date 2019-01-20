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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.jdbc.base.ActivityRecordSearchTerm;
import org.apache.guacamole.auth.jdbc.base.ActivityRecordSortPredicate;
import org.apache.guacamole.auth.jdbc.user.ModeledAuthenticatedUser;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.ConnectionRecord;
import org.apache.guacamole.protocol.GuacamoleClientInformation;

public interface ConnectionServiceInterface {

	public List<ConnectionRecord> retrieveHistory(ModeledAuthenticatedUser user,
            ModeledConnection connection) throws GuacamoleException;
	
	public Connection retrieveObject(ModeledAuthenticatedUser currentUser, String identifier) throws GuacamoleException;

	public List<ConnectionRecord> retrieveHistory(ModeledAuthenticatedUser user,
            Collection<ActivityRecordSearchTerm> requiredContents,
            List<ActivityRecordSortPredicate> sortPredicates, int limit)
            throws GuacamoleException;

	public GuacamoleTunnel connect(ModeledAuthenticatedUser user,
            ModeledConnection connection, GuacamoleClientInformation info,
            Map<String, String> tokens) throws GuacamoleException;

	public Map<String, String> retrieveParameters(ModeledAuthenticatedUser currentUser, String identifier);

	public Set<String> getIdentifiersWithin(ModeledAuthenticatedUser currentUser, String identifier) throws GuacamoleException;

	public Collection<ModeledConnection> retrieveObjects(ModeledAuthenticatedUser currentUser,
			Collection<String> identifiers) throws GuacamoleException;

	public Set<String> getIdentifiers(ModeledAuthenticatedUser currentUser) throws GuacamoleException;

	public ModeledConnection createObject(ModeledAuthenticatedUser currentUser, Connection object) throws GuacamoleException;

	public void updateObject(ModeledAuthenticatedUser currentUser, ModeledConnection connection) throws GuacamoleException;

	public void deleteObject(ModeledAuthenticatedUser currentUser, String identifier) throws GuacamoleException;
}
