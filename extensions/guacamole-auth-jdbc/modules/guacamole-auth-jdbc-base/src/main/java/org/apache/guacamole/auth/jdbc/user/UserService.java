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

package org.apache.guacamole.auth.jdbc.user;

import java.util.Collection;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.common.permission.ObjectPermissionModelInterface;
import org.apache.guacamole.auth.common.user.ModeledAuthenticatedUser;
import org.apache.guacamole.auth.common.user.ModeledUserAbstract;
import org.apache.guacamole.auth.common.user.UserModelInterface;
import org.apache.guacamole.auth.common.user.UserServiceAbstract;
import org.apache.guacamole.auth.common.user.UserServiceInterface;
import org.apache.guacamole.auth.jdbc.base.ModeledDirectoryObjectService;
import org.apache.guacamole.auth.jdbc.permission.ObjectPermissionModel;
import org.apache.guacamole.net.auth.User;
import org.apache.guacamole.net.auth.permission.ObjectPermission.Type;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating users.
 */
public class UserService extends UserServiceAbstract implements UserServiceInterface {
    
    @Override
    protected UserModel getModelInstance(ModeledAuthenticatedUser currentUser,
            final User object) throws GuacamoleException {

        // Create new ModeledUser backed by blank model
        UserModel model = new UserModel();
        ModeledUserAbstract user = getObjectInstance(currentUser, model);

        // Set model contents through ModeledUser, copying the provided user
        user.setIdentifier(object.getIdentifier());
        user.setPassword(object.getPassword());
        user.setAttributes(object.getAttributes());

        return model;
        
    }
    
    @Override
	protected void createBaseEntity(UserModelInterface model) {
		entityMapper.insert(model);
	}
    
    @Override
    protected Collection<ObjectPermissionModelInterface> getImplicitPermissions(
            ModeledAuthenticatedUser user, UserModelInterface model) {

        // Get original set of implicit permissions
    	// Build list of implicit permissions
        Collection<ObjectPermissionModelInterface> implicitPermissions = super.getImplicitPermissions(user, model);

        loadPermissions(model, model, implicitPermissions, IMPLICIT_USER_PERMISSIONS);

        return implicitPermissions;
    }
  
	@Override
	protected void createModelPermission(UserModelInterface model,
			Collection<ObjectPermissionModelInterface> implicitPermissions, UserModelInterface userModel,
			Type permission) {
		
		ModeledDirectoryObjectService.getNewModelPermission(userModel, implicitPermissions, model, permission);

	}  
	
    @Override
	protected Class<? extends ObjectPermissionModelInterface> getClassPermissionModel() {
		return ObjectPermissionModel.class;
	}
  
}
