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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.guacamole.auth.common.connection.ConnectionModelInterface;
import org.apache.guacamole.auth.common.connection.ConnectionParameterModelInterface;
import org.apache.guacamole.auth.common.connection.ConnectionServiceAbstract;
import org.apache.guacamole.auth.common.connection.ConnectionServiceInterface;
import org.apache.guacamole.auth.common.connection.ModeledConnection;
import org.apache.guacamole.auth.common.permission.ObjectPermissionMapperInterface;
import org.apache.guacamole.auth.common.permission.ObjectPermissionModelInterface;
import org.apache.guacamole.auth.common.user.ModeledAuthenticatedUser;
import org.apache.guacamole.auth.common.user.UserModelInterface;
import org.apache.guacamole.auth.jdbc.permission.ObjectPermissionModel;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.permission.ObjectPermission.Type;

import com.google.inject.Inject;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating connections.
 */
public class ConnectionService extends ConnectionServiceAbstract implements ConnectionServiceInterface {

	@Inject
    public ConnectionService(Map<String, ObjectPermissionMapperInterface> mappers) {
		super(mappers);
	}

	@Override
    protected ConnectionModelInterface getModelInstance(ModeledAuthenticatedUser currentUser,
            final Connection object) {

        // Create new ModeledConnection backed by blank model
        ConnectionModel model = new ConnectionModel();
        ModeledConnection connection = getObjectInstance(currentUser, model);

        // Set model contents through ModeledConnection, copying the provided connection
        connection.setParentIdentifier(object.getParentIdentifier());
        connection.setName(object.getName());
        connection.setConfiguration(object.getConfiguration());
        connection.setAttributes(object.getAttributes());

        return model;
        
    }
    
    /**
     * Given an arbitrary Guacamole connection, produces a collection of
     * parameter model objects containing the name/value pairs of that
     * connection's parameters.
     *
     * @param connection
     *     The connection whose configuration should be used to produce the
     *     collection of parameter models.
     *
     * @return
     *     A collection of parameter models containing the name/value pairs
     *     of the given connection's parameters.
     */
    protected Collection<ConnectionParameterModelInterface> getParameterModels(ModeledConnection connection) {

        Map<String, String> parameters = connection.getConfiguration().getParameters();
        
        // Convert parameters to model objects
        Collection<ConnectionParameterModelInterface> parameterModels = new ArrayList<ConnectionParameterModelInterface>(parameters.size());
        for (Map.Entry<String, String> parameterEntry : parameters.entrySet()) {

            // Get parameter name and value
            String name = parameterEntry.getKey();
            String value = parameterEntry.getValue();

            // There is no need to insert empty parameters
            if (value == null || value.isEmpty())
                continue;
            
            // Produce model object from parameter
            ConnectionParameterModel model = new ConnectionParameterModel();
            model.setConnectionIdentifier(connection.getIdentifier());
            model.setName(name);
            model.setValue(value);

            // Add model to list
            parameterModels.add(model);
            
        }

        return parameterModels;

    }
    
    /**
     * Returns whether the given string is a valid identifier within the JDBC
     * authentication extension. Invalid identifiers may result in SQL errors
     * from the underlying database when used in queries.
     *
     * @param identifier
     *     The string to check for validity.
     *
     * @return
     *     true if the given string is a valid identifier, false otherwise.
     */
    protected boolean isValidIdentifier(String identifier) {

        // Empty identifiers are invalid
        if (identifier.isEmpty())
            return false;

        // Identifier is invalid if any non-numeric characters are present
        for (int i = 0; i < identifier.length(); i++) {
            if (!Character.isDigit(identifier.charAt(i)))
                return false;
        }

        // Identifier is valid - contains only numeric characters
        return true;

    }

	@Override
	protected void createModelPermission(UserModelInterface userModel,
			Collection<ObjectPermissionModelInterface> implicitPermissions, ConnectionModelInterface model,
			Type permission) {
			
		// Create model which grants this permission to the current user
		ObjectPermissionModel permissionModel = new ObjectPermissionModel();
        permissionModel.setEntityID(userModel.getEntityID());
        permissionModel.setType(permission);
        permissionModel.setObjectIdentifier(model.getIdentifier());

        // Add permission
        implicitPermissions.add(permissionModel);
		
	}

}
