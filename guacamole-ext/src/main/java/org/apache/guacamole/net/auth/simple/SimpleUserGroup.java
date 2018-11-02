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

package org.apache.guacamole.net.auth.simple;

import java.util.Collections;
import java.util.Map;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.AbstractIdentifiable;
import org.apache.guacamole.net.auth.RelatedObjectSet;
import org.apache.guacamole.net.auth.UserGroup;
import org.apache.guacamole.net.auth.permission.ObjectPermissionSet;
import org.apache.guacamole.net.auth.permission.SystemPermissionSet;

/**
 * A read-only UserGroup implementation which has no members.
 */
public class SimpleUserGroup extends AbstractIdentifiable implements UserGroup {

    /**
     * Creates a completely uninitialized SimpleUserGroup.
     */
    public SimpleUserGroup() {
    }

    /**
     * Creates a new SimpleUserGroup having the given identifier.
     *
     * @param identifier
     *     The identifier to assign to this SimpleUserGroup.
     */
    public SimpleUserGroup(String identifier) {
        super.setIdentifier(identifier);
    }

    @Override
    public Map<String, String> getAttributes() {
        return Collections.<String, String>emptyMap();
    }

    @Override
    public void setAttributes(Map<String, String> attributes) {
        // Do nothing - there are no attributes
    }

    @Override
    public SystemPermissionSet getSystemPermissions()
            throws GuacamoleException {
        return SystemPermissionSet.EMPTY_SET;
    }

    @Override
    public ObjectPermissionSet getConnectionPermissions()
            throws GuacamoleException {
        return ObjectPermissionSet.EMPTY_SET;
    }

    @Override
    public ObjectPermissionSet getConnectionGroupPermissions()
            throws GuacamoleException {
        return ObjectPermissionSet.EMPTY_SET;
    }

    @Override
    public ObjectPermissionSet getUserPermissions()
            throws GuacamoleException {
        return ObjectPermissionSet.EMPTY_SET;
    }

    @Override
    public ObjectPermissionSet getUserGroupPermissions()
            throws GuacamoleException {
        return ObjectPermissionSet.EMPTY_SET;
    }

    @Override
    public ObjectPermissionSet getActiveConnectionPermissions()
            throws GuacamoleException {
        return ObjectPermissionSet.EMPTY_SET;
    }

    @Override
    public ObjectPermissionSet getSharingProfilePermissions() {
        return ObjectPermissionSet.EMPTY_SET;
    }

    @Override
    public RelatedObjectSet getUserGroups() throws GuacamoleException {
        return RelatedObjectSet.EMPTY_SET;
    }

    @Override
    public RelatedObjectSet getMemberUsers() throws GuacamoleException {
        return RelatedObjectSet.EMPTY_SET;
    }

    @Override
    public RelatedObjectSet getMemberUserGroups() throws GuacamoleException {
        return RelatedObjectSet.EMPTY_SET;
    }

}
