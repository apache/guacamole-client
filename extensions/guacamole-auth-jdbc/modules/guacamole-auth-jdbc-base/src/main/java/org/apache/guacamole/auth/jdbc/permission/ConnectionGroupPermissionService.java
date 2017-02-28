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

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.guacamole.auth.jdbc.user.ModeledAuthenticatedUser;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.jdbc.user.ModeledUser;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * deleting connection group permissions. This service will automatically
 * enforce the permissions of the current user.
 */
public class ConnectionGroupPermissionService extends ModeledObjectPermissionService {

    /**
     * Mapper for connection group permissions.
     */
    @Inject
    private ConnectionGroupPermissionMapper connectionGroupPermissionMapper;
    
    /**
     * Provider for connection group permission sets.
     */
    @Inject
    private Provider<ConnectionGroupPermissionSet> connectionGroupPermissionSetProvider;
    
    @Override
    protected ObjectPermissionMapper getPermissionMapper() {
        return connectionGroupPermissionMapper;
    }

    @Override
    public ObjectPermissionSet getPermissionSet(ModeledAuthenticatedUser user,
            ModeledUser targetUser) throws GuacamoleException {

        // Create permission set for requested user
        ObjectPermissionSet permissionSet = connectionGroupPermissionSetProvider.get();
        permissionSet.init(user, targetUser);

        return permissionSet;
        
    }

}
