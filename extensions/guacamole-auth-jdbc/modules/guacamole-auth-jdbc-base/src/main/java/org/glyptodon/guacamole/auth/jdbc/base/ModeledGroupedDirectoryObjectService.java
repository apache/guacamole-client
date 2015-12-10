/*
 * Copyright (C) 2015 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.glyptodon.guacamole.auth.jdbc.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.auth.jdbc.user.AuthenticatedUser;
import org.glyptodon.guacamole.net.auth.Identifiable;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermission;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermissionSet;

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
