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

package org.apache.guacamole.morphia.permission;

import com.google.inject.Inject;
import com.google.inject.Provider;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.morphia.connection.ConnectionPermissionModel;
import org.apache.guacamole.morphia.user.ModeledAuthenticatedUser;
import org.apache.guacamole.morphia.user.ModeledUser;
import org.apache.guacamole.net.auth.permission.ObjectPermission;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * deleting connection permissions. This service will automatically enforce the
 * permissions of the current user.
 */
public class ConnectionPermissionService
        extends ModeledObjectPermissionService {

    /**
     * Mapper for connection permissions.
     */
    @Inject
    private ConnectionPermissionMapper connectionPermissionMapper;

    /**
     * Provider for connection permission sets.
     */
    @Inject
    private Provider<ConnectionPermissionSet> connectionPermissionSetProvider;

    @Override
    protected ObjectPermissionMapper getPermissionMapper() {
        return connectionPermissionMapper;
    }

    @Override
    public ObjectPermissionSet getPermissionSet(ModeledAuthenticatedUser user,
            ModeledUser targetUser) throws GuacamoleException {

        // Create permission set for requested user
        ObjectPermissionSet permissionSet = connectionPermissionSetProvider
                .get();
        permissionSet.init(user, targetUser);

        return permissionSet;

    }

    @Override
    protected ObjectPermissionModel getModelInstance(ModeledUser targetUser,
            ObjectPermission permission) {

        ConnectionPermissionModel model = new ConnectionPermissionModel();

        // Populate model object with data from user and permission
        model.setUser(targetUser.getModel());
        model.setUsername(targetUser.getModel().getUsername());
        model.setType(permission.getType());
        model.setObjectIdentifier(permission.getObjectIdentifier());

        return model;

    }

}
