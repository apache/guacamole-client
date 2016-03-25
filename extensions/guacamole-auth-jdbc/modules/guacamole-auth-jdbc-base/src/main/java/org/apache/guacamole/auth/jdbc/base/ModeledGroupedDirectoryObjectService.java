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

package org.apache.guacamole.auth.jdbc.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleSecurityException;
import org.apache.guacamole.auth.jdbc.user.AuthenticatedUser;
import org.apache.guacamole.net.auth.Identifiable;
import org.apache.guacamole.net.auth.permission.ObjectPermission;
import org.apache.guacamole.net.auth.permission.ObjectPermissionSet;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating objects that can be within connection groups. This service will
 * automatically enforce the permissions of the current user.
 *
 * @author Michael Jumper
 * @param <InternalType>
 *     The specific internal implementation of the type of object this service
 *     provides access to.
 *
 * @param <ExternalType>
 *     The external interface or implementation of the type of object this
 *     service provides access to, as defined by the guacamole-ext API.
 *
 * @param <ModelType>
 *     The underlying model object used to represent InternalType in the
 *     database.
 */
public abstract class ModeledGroupedDirectoryObjectService<InternalType extends ModeledGroupedDirectoryObject<ModelType>,
        ExternalType extends Identifiable, ModelType extends GroupedObjectModel>
        extends ModeledDirectoryObjectService<InternalType, ExternalType, ModelType> {

    /**
     * Returns the set of parent connection groups that are modified by the
     * given model object (by virtue of the object changing parent groups). If
     * the model is not changing parents, the resulting collection will be
     * empty.
     *
     * @param user
     *     The user making the given changes to the model.
     *
     * @param identifier
     *     The identifier of the object that has been modified, if it exists.
     *     If the object is being created, this will be null.
     *
     * @param model
     *     The model that has been modified, if any. If the object is being
     *     deleted, this will be null.
     *
     * @return
     *     A collection of the identifiers of all parent connection groups
     *     that will be affected (updated) by the change.
     *
     * @throws GuacamoleException
     *     If an error occurs while determining which parent connection groups
     *     are affected.
     */
    protected Collection<String> getModifiedGroups(AuthenticatedUser user,
            String identifier, ModelType model) throws GuacamoleException {

        // Get old parent identifier
        String oldParentIdentifier = null;
        if (identifier != null) {
            ModelType current = retrieveObject(user, identifier).getModel();
            oldParentIdentifier = current.getParentIdentifier();
        }

        // Get new parent identifier
        String parentIdentifier = null;
        if (model != null) {

            parentIdentifier = model.getParentIdentifier();

            // If both parents have the same identifier, nothing has changed
            if (parentIdentifier != null && parentIdentifier.equals(oldParentIdentifier))
                return Collections.<String>emptyList();

        }

        // Return collection of all non-root groups involved
        Collection<String> groups = new ArrayList<String>(2);
        if (oldParentIdentifier != null) groups.add(oldParentIdentifier);
        if (parentIdentifier    != null) groups.add(parentIdentifier);
        return groups;

    }

    /**
     * Returns whether the given user has permission to modify the parent
     * connection groups affected by the modifications made to the given model
     * object.
     *
     * @param user
     *     The user who changed the model object.
     *
     * @param identifier
     *     The identifier of the object that has been modified, if it exists.
     *     If the object is being created, this will be null.
     *
     * @param model
     *     The model that has been modified, if any. If the object is being
     *     deleted, this will be null.
     *
     * @return
     *     true if the user has update permission for all modified groups,
     *     false otherwise.
     *
     * @throws GuacamoleException
     *     If an error occurs while determining which parent connection groups
     *     are affected.
     */
    protected boolean canUpdateModifiedGroups(AuthenticatedUser user,
            String identifier, ModelType model) throws GuacamoleException {

        // If user is an administrator, no need to check
        if (user.getUser().isAdministrator())
            return true;
        
        // Verify that we have permission to modify any modified groups
        Collection<String> modifiedGroups = getModifiedGroups(user, identifier, model);
        if (!modifiedGroups.isEmpty()) {

            ObjectPermissionSet permissionSet = user.getUser().getConnectionGroupPermissions();
            Collection<String> updateableGroups = permissionSet.getAccessibleObjects(
                Collections.singleton(ObjectPermission.Type.UPDATE),
                modifiedGroups
            );

            return updateableGroups.size() == modifiedGroups.size();
            
        }

        return true;

    }

    @Override
    protected void beforeCreate(AuthenticatedUser user,
            ModelType model) throws GuacamoleException {

        super.beforeCreate(user, model);
        
        // Validate that we can update all applicable parent groups
        if (!canUpdateModifiedGroups(user, null, model))
            throw new GuacamoleSecurityException("Permission denied.");
        
    }

    @Override
    protected void beforeUpdate(AuthenticatedUser user,
            ModelType model) throws GuacamoleException {

        super.beforeUpdate(user, model);

        // Validate that we can update all applicable parent groups
        if (!canUpdateModifiedGroups(user, model.getIdentifier(), model))
            throw new GuacamoleSecurityException("Permission denied.");
        
    }

    @Override
    protected void beforeDelete(AuthenticatedUser user,
            String identifier) throws GuacamoleException {

        super.beforeDelete(user, identifier);

        // Validate that we can update all applicable parent groups
        if (!canUpdateModifiedGroups(user, identifier, null))
            throw new GuacamoleSecurityException("Permission denied.");
        
    }

}
