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

package org.apache.guacamole.auth.jdbc.connectiongroup;

import java.util.Collection;
import java.util.Map;
import org.apache.guacamole.auth.common.connectiongroup.ConnectionGroupModelInterface;
import org.apache.guacamole.auth.common.connectiongroup.ConnectionGroupServiceAbstract;
import org.apache.guacamole.auth.common.connectiongroup.ConnectionGroupServiceInterface;
import org.apache.guacamole.auth.common.connectiongroup.ModeledConnectionGroup;
import org.apache.guacamole.auth.common.permission.ObjectPermissionMapperInterface;
import org.apache.guacamole.auth.common.permission.ObjectPermissionModelInterface;
import org.apache.guacamole.auth.common.user.ModeledAuthenticatedUser;
import org.apache.guacamole.auth.common.user.UserModelInterface;
import org.apache.guacamole.auth.jdbc.base.ModeledDirectoryObjectService;
import org.apache.guacamole.net.auth.ConnectionGroup;
import org.apache.guacamole.net.auth.permission.ObjectPermission;
import com.google.inject.Inject;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating connection groups.
 */
public class ConnectionGroupService extends ConnectionGroupServiceAbstract
        implements ConnectionGroupServiceInterface {

    @Inject
    public ConnectionGroupService(
            Map<String, ObjectPermissionMapperInterface> mappers) {
        super(mappers);
    }

    @Override
    protected ConnectionGroupModel getModelInstance(
            ModeledAuthenticatedUser currentUser,
            final ConnectionGroup object) {

        // Create new ModeledConnectionGroup backed by blank model
        ConnectionGroupModel model = new ConnectionGroupModel();
        ModeledConnectionGroup connectionGroup = getObjectInstance(currentUser,
                model);

        // Set model contents through ModeledConnectionGroup, copying the
        // provided connection group
        connectionGroup.setParentIdentifier(object.getParentIdentifier());
        connectionGroup.setName(object.getName());
        connectionGroup.setType(object.getType());
        connectionGroup.setAttributes(object.getAttributes());

        return model;

    }

    /**
     * Returns whether the given string is a valid identifier within the JDBC
     * authentication extension. Invalid identifiers may result in SQL errors
     * from the underlying database when used in queries.
     *
     * @param identifier
     *            The string to check for validity.
     *
     * @return true if the given string is a valid identifier, false otherwise.
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
            Collection<ObjectPermissionModelInterface> implicitPermissions,
            ConnectionGroupModelInterface model,
            ObjectPermission.Type permission) {

        ModeledDirectoryObjectService.getNewModelPermission(userModel,
                implicitPermissions, model, permission);

    }

}
