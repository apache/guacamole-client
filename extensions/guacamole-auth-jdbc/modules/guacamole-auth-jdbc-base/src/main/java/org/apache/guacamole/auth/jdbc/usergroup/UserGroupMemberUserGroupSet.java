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

import com.google.inject.Inject;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.jdbc.base.ObjectRelationMapper;
import org.apache.guacamole.auth.jdbc.base.RelatedObjectSet;
import org.apache.guacamole.net.auth.permission.ObjectPermissionSet;

/**
 * RelatedObjectSet implementation which represents the one-to-many
 * relationship between a particular user group and its user group members.
 */
public class UserGroupMemberUserGroupSet extends RelatedObjectSet<ModeledUserGroup, UserGroupModel> {

    /**
     * Mapper for the relation between user groups and their user group members.
     */
    @Inject
    private UserGroupMemberUserGroupMapper userGroupMemberUserGroupMapper;

    @Override
    protected ObjectRelationMapper<UserGroupModel> getObjectRelationMapper() {
        return userGroupMemberUserGroupMapper;
    }

    @Override
    protected ObjectPermissionSet
        getParentObjectEffectivePermissionSet() throws GuacamoleException {
        return getCurrentUser().getUser().getEffectivePermissions().getUserGroupPermissions();
    }

    @Override
    protected ObjectPermissionSet getChildObjectEffectivePermissionSet()
            throws GuacamoleException {
        return getCurrentUser().getUser().getEffectivePermissions().getUserGroupPermissions();
    }

}
