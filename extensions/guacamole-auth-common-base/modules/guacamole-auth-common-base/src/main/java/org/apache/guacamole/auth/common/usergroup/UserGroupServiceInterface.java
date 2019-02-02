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

package org.apache.guacamole.auth.common.usergroup;

import java.util.Collection;
import java.util.Set;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.common.user.ModeledAuthenticatedUser;
import org.apache.guacamole.net.auth.UserGroup;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating user groups.
 */
public interface UserGroupServiceInterface {

    public ModeledUserGroup retrieveObject(ModeledAuthenticatedUser currentUser,
            String identifier) throws GuacamoleException;

    public Collection<ModeledUserGroup> retrieveObjects(
            ModeledAuthenticatedUser currentUser,
            Collection<String> identifiers) throws GuacamoleException;

    public Set<String> getIdentifiers(ModeledAuthenticatedUser currentUser)
            throws GuacamoleException;

    public ModeledUserGroup createObject(ModeledAuthenticatedUser currentUser,
            UserGroup object) throws GuacamoleException;

    public void updateObject(ModeledAuthenticatedUser currentUser,
            ModeledUserGroup group) throws GuacamoleException;

    public void deleteObject(ModeledAuthenticatedUser currentUser,
            String identifier) throws GuacamoleException;

}
