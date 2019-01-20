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

package org.apache.guacamole.auth.jdbc.permission;

import java.util.Collection;
import java.util.Set;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.jdbc.base.EntityModelInterface;
import org.apache.guacamole.auth.jdbc.base.ModeledPermissions;
import org.apache.guacamole.auth.jdbc.user.ModeledAuthenticatedUser;
import org.apache.guacamole.net.auth.permission.SystemPermission;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * deleting system permissions. This service will automatically enforce the
 * permissions of the current user.
 */
public interface SystemPermissionServiceInterface {

	public SystemPermissionSet getPermissionSet(ModeledAuthenticatedUser user,
            ModeledPermissions<? extends EntityModelInterface> targetEntity,
            Set<String> effectiveGroups) throws GuacamoleException;
	
	public Set<SystemPermission> retrievePermissions(ModeledAuthenticatedUser user,
            ModeledPermissions<? extends EntityModelInterface> targetEntity,
            Set<String> effectiveGroups) throws GuacamoleException;

	public boolean hasPermission(ModeledAuthenticatedUser user,
            ModeledPermissions<? extends EntityModelInterface> targetEntity,
            SystemPermission.Type type, Set<String> effectiveGroups)
            throws GuacamoleException;

	public void createPermissions(ModeledAuthenticatedUser user,
            ModeledPermissions<? extends EntityModelInterface> targetEntity,
            Collection<SystemPermission> permissions)
            throws GuacamoleException;

	public void deletePermissions(ModeledAuthenticatedUser user,
            ModeledPermissions<? extends EntityModelInterface> targetEntity,
            Collection<SystemPermission> permissions)
            throws GuacamoleException;

}
