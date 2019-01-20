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

package org.apache.guacamole.auth.jdbc.usergroup;

import java.util.Collection;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.jdbc.base.ModeledDirectoryObjectService;
import org.apache.guacamole.auth.jdbc.permission.ObjectPermissionModel;
import org.apache.guacamole.auth.jdbc.permission.ObjectPermissionModelInterface;
import org.apache.guacamole.auth.jdbc.user.ModeledAuthenticatedUser;
import org.apache.guacamole.auth.jdbc.user.UserModelInterface;
import org.apache.guacamole.net.auth.UserGroup;
import org.apache.guacamole.net.auth.permission.ObjectPermission.Type;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating user groups.
 */
public class UserGroupService extends UserGroupServiceAbstract implements UserGroupServiceInterface {
    
    @Override
    protected UserGroupModel getModelInstance(ModeledAuthenticatedUser currentUser,
            final UserGroup object) throws GuacamoleException {

        // Create new ModeledUserGroup backed by blank model
        UserGroupModel model = new UserGroupModel();
        ModeledUserGroup group = getObjectInstance(currentUser, model);

        // Set model contents through ModeledUser, copying the provided group
        group.setIdentifier(object.getIdentifier());
        group.setAttributes(object.getAttributes());

        return model;
        
    }

	@Override
	protected void createBaseEntity(UserGroupModelInterface model) {
		entityMapper.insert(model);
	}

	@Override
	protected void createModelPermission(UserModelInterface userModel,
			Collection<ObjectPermissionModelInterface> implicitPermissions, UserGroupModelInterface model,
			Type permission) {
		
		ModeledDirectoryObjectService.getNewModelPermission(userModel, implicitPermissions, model, permission);
		
	}

	@Override
	protected Class<? extends ObjectPermissionModelInterface> getClassPermissionModel() {
		return ObjectPermissionModel.class;
	}

}
