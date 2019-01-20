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

package org.apache.guacamole.auth.jdbc.base;

import java.util.Collection;

import org.apache.guacamole.auth.jdbc.permission.ObjectPermissionModel;
import org.apache.guacamole.auth.jdbc.permission.ObjectPermissionModelInterface;
import org.apache.guacamole.auth.jdbc.user.UserModelInterface;
import org.apache.guacamole.net.auth.Identifiable;
import org.apache.guacamole.net.auth.permission.ObjectPermission.Type;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating objects within directories. This service will automatically
 * enforce the permissions of the current user.
 *
 * @param <InternalType>
 *     The specific internal implementation of the type of object this service
 *     provides access to.
 *
 * @param <ExternalType>
 *     The external interface or implementation of the type of object this
 *     service provides access to, as defined by the guacamole-ext API.
 *
 * @param <ModelType>
 *     The underlying model object used to represent InternalType in the
 *     database.
 */
public abstract class ModeledDirectoryObjectService<InternalType extends ModeledDirectoryObject<ModelType>,
        ExternalType extends Identifiable, ModelType extends ObjectModel>
	extends ModeledDirectoryObjectServiceAbstract<InternalType, ExternalType, ModelType> {

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
	@Override
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
        
	public static void getNewModelPermission(UserModelInterface userModel,
			Collection<ObjectPermissionModelInterface> implicitPermissions, ObjectModelInterface model,
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
