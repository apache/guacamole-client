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

package org.apache.guacamole.auth.common.permission;

import java.util.Set;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.common.base.EntityModelInterface;
import org.apache.guacamole.auth.common.base.ModeledPermissions;
import org.apache.guacamole.auth.common.user.ModeledAuthenticatedUser;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * deleting sharing profile permissions. This service will automatically enforce
 * the permissions of the current user.
 */
public abstract class SharingProfilePermissionServiceAbstract extends ModeledObjectPermissionServiceAbstract {

    /**
     * Mapper for sharing profile permissions.
     */
    @Inject
    private SharingProfilePermissionMapperInterface sharingProfilePermissionMapper;

    /**
     * Provider for sharing profile permission sets.
     */
    @Inject
    private Provider<SharingProfilePermissionSet> sharingProfilePermissionSetProvider;

    @Override
    protected SharingProfilePermissionMapperInterface getPermissionMapper() {
        return sharingProfilePermissionMapper;
    }

    @Override
    public ObjectPermissionSet getPermissionSet(ModeledAuthenticatedUser user,
    		ModeledPermissions<? extends EntityModelInterface> targetEntity,
            Set<String> effectiveGroups) throws GuacamoleException {

        // Create permission set for requested user
        ObjectPermissionSet permissionSet = sharingProfilePermissionSetProvider.get();
        permissionSet.init(user, targetEntity, effectiveGroups);

        return permissionSet;

    }

}
