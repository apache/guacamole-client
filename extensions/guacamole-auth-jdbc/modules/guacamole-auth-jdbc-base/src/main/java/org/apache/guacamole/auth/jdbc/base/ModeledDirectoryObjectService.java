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
import java.util.Set;
import org.apache.guacamole.auth.jdbc.user.AuthenticatedUser;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleSecurityException;
import org.apache.guacamole.auth.jdbc.permission.ObjectPermissionMapper;
import org.apache.guacamole.auth.jdbc.permission.ObjectPermissionModel;
import org.apache.guacamole.auth.jdbc.user.UserModel;
import org.apache.guacamole.net.auth.Identifiable;
import org.apache.guacamole.net.auth.permission.ObjectPermission;
import org.apache.guacamole.net.auth.permission.ObjectPermissionSet;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating objects within directories. This service will automatically
 * enforce the permissions of the current user.
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
public abstract class ModeledDirectoryObjectService<InternalType extends ModeledDirectoryObject<ModelType>,
        ExternalType extends Identifiable, ModelType extends ObjectModel>
    implements DirectoryObjectService<InternalType, ExternalType> {

    /**
     * All object permissions which are implicitly granted upon creation to the
     * creator of the object.
     */
    private static final ObjectPermission.Type[] IMPLICIT_OBJECT_PERMISSIONS = {
        ObjectPermission.Type.READ,
        ObjectPermission.Type.UPDATE,
        ObjectPermission.Type.DELETE,
        ObjectPermission.Type.ADMINISTER
    };
    
    /**
     * Returns an instance of a mapper for the type of object used by this
     * service.
     *
     * @return
     *     A mapper which provides access to the model objects associated with
     *     the objects used by this service.
     */
    protected abstract ModeledDirectoryObjectMapper<ModelType> getObjectMapper();

    /**
     * Returns an instance of a mapper for the type of permissions that affect
     * the type of object used by this service.
     *
     * @return
     *     A mapper which provides access to the model objects associated with
     *     the permissions that affect the objects used by this service.
     */
    protected abstract ObjectPermissionMapper getPermissionMapper();

    /**
     * Returns an instance of an object which is backed by the given model
     * object.
     *
     * @param currentUser
     *     The user for whom this object is being created.
     *
     * @param model
     *     The model object to use to back the returned object.
     *
     * @return
     *     An object which is backed by the given model object.
     */
    protected abstract InternalType getObjectInstance(AuthenticatedUser currentUser,
            ModelType model);

    /**
     * Returns an instance of a model object which is based on the given
     * object.
     *
     * @param currentUser
     *     The user for whom this model object is being created.
     *
     * @param object 
     *     The object to use to produce the returned model object.
     *
     * @return
     *     A model object which is based on the given object.
     */
    protected abstract ModelType getModelInstance(AuthenticatedUser currentUser,
            ExternalType object);

    /**
     * Returns whether the given user has permission to create the type of
     * objects that this directory object service manages.
     *
     * @param user
     *     The user being checked.
     *
     * @return
     *     true if the user has object creation permission relevant to this
     *     directory object service, false otherwise.
     * 
     * @throws GuacamoleException
     *     If permission to read the user's permissions is denied.
     */
    protected abstract boolean hasCreatePermission(AuthenticatedUser user)
            throws GuacamoleException;

    /**
     * Returns whether the given user has permission to perform a certain
     * action on a specific object managed by this directory object service.
     *
     * @param user
     *     The user being checked.
     *
     * @param identifier
     *     The identifier of the object to check.
     *
     * @param type
     *     The type of action that will be performed.
     *
     * @return
     *     true if the user has object permission relevant described, false
     *     otherwise.
     * 
     * @throws GuacamoleException
     *     If permission to read the user's permissions is denied.
     */
    protected boolean hasObjectPermission(AuthenticatedUser user,
            String identifier, ObjectPermission.Type type)
            throws GuacamoleException {

        // Get object permissions
        ObjectPermissionSet permissionSet = getPermissionSet(user);
        
        // Return whether permission is granted
        return user.getUser().isAdministrator()
            || permissionSet.hasPermission(type, identifier);

    }
 
    /**
     * Returns the permission set associated with the given user and related
     * to the type of objects handled by this directory object service.
     *
     * @param user
     *     The user whose permissions are being retrieved.
     *
     * @return
     *     A permission set which contains the permissions associated with the
     *     given user and related to the type of objects handled by this
     *     directory object service.
     * 
     * @throws GuacamoleException
     *     If permission to read the user's permissions is denied.
     */
    protected abstract ObjectPermissionSet getPermissionSet(AuthenticatedUser user)
            throws GuacamoleException;

    /**
     * Returns a collection of objects which are backed by the models in the
     * given collection.
     *
     * @param currentUser
     *     The user for whom these objects are being created.
     *
     * @param models
     *     The model objects to use to back the objects within the returned
     *     collection.
     *
     * @return
     *     A collection of objects which are backed by the models in the given
     *     collection.
     */
    protected Collection<InternalType> getObjectInstances(AuthenticatedUser currentUser,
            Collection<ModelType> models) {

        // Create new collection of objects by manually converting each model
        Collection<InternalType> objects = new ArrayList<InternalType>(models.size());
        for (ModelType model : models)
            objects.add(getObjectInstance(currentUser, model));

        return objects;
        
    }

    /**
     * Called before any object is created through this directory object
     * service. This function serves as a final point of validation before
     * the create operation occurs. In its default implementation,
     * beforeCreate() performs basic permissions checks.
     *
     * @param user
     *     The user creating the object.
     *
     * @param model
     *     The model of the object being created.
     *
     * @throws GuacamoleException
     *     If the object is invalid, or an error prevents validating the given
     *     object.
     */
    protected void beforeCreate(AuthenticatedUser user,
            ModelType model ) throws GuacamoleException {

        // Verify permission to create objects
        if (!user.getUser().isAdministrator() && !hasCreatePermission(user))
            throw new GuacamoleSecurityException("Permission denied.");

    }

    /**
     * Called before any object is updated through this directory object
     * service. This function serves as a final point of validation before
     * the update operation occurs. In its default implementation,
     * beforeUpdate() performs basic permissions checks.
     *
     * @param user
     *     The user updating the existing object.
     *
     * @param model
     *     The model of the object being updated.
     *
     * @throws GuacamoleException
     *     If the object is invalid, or an error prevents validating the given
     *     object.
     */
    protected void beforeUpdate(AuthenticatedUser user,
            ModelType model) throws GuacamoleException {

        // By default, do nothing.
        if (!hasObjectPermission(user, model.getIdentifier(), ObjectPermission.Type.UPDATE))
            throw new GuacamoleSecurityException("Permission denied.");

    }

    /**
     * Called before any object is deleted through this directory object
     * service. This function serves as a final point of validation before
     * the delete operation occurs. In its default implementation,
     * beforeDelete() performs basic permissions checks.
     *
     * @param user
     *     The user deleting the existing object.
     *
     * @param identifier
     *     The identifier of the object being deleted.
     *
     * @throws GuacamoleException
     *     If the object is invalid, or an error prevents validating the given
     *     object.
     */
    protected void beforeDelete(AuthenticatedUser user,
            String identifier) throws GuacamoleException {

        // Verify permission to delete objects
        if (!hasObjectPermission(user, identifier, ObjectPermission.Type.DELETE))
            throw new GuacamoleSecurityException("Permission denied.");

    }

    @Override
    public InternalType retrieveObject(AuthenticatedUser user,
            String identifier) throws GuacamoleException {

        // Pull objects having given identifier
        Collection<InternalType> objects = retrieveObjects(user, Collections.singleton(identifier));

        // If no such object, return null
        if (objects.isEmpty())
            return null;

        // The object collection will have exactly one element unless the
        // database has seriously lost integrity
        assert(objects.size() == 1);

        // Return first and only object 
        return objects.iterator().next();

    }

    @Override
    public Collection<InternalType> retrieveObjects(AuthenticatedUser user,
            Collection<String> identifiers) throws GuacamoleException {

        // Do not query if no identifiers given
        if (identifiers.isEmpty())
            return Collections.<InternalType>emptyList();

        Collection<ModelType> objects;

        // Bypass permission checks if the user is a system admin
        if (user.getUser().isAdministrator())
            objects = getObjectMapper().select(identifiers);

        // Otherwise only return explicitly readable identifiers
        else
            objects = getObjectMapper().selectReadable(user.getUser().getModel(), identifiers);
        
        // Return collection of requested objects
        return getObjectInstances(user, objects);
        
    }

    /**
     * Returns a collection of permissions that should be granted due to the
     * creation of the given object. These permissions need not be granted
     * solely to the user creating the object.
     * 
     * @param user
     *     The user creating the object.
     * 
     * @param model
     *     The object being created.
     * 
     * @return
     *     The collection of implicit permissions that should be granted due to
     *     the creation of the given object.
     */
    protected Collection<ObjectPermissionModel> getImplicitPermissions(AuthenticatedUser user,
            ModelType model) {
        
        // Build list of implicit permissions
        Collection<ObjectPermissionModel> implicitPermissions =
                new ArrayList<ObjectPermissionModel>(IMPLICIT_OBJECT_PERMISSIONS.length);

        UserModel userModel = user.getUser().getModel();
        for (ObjectPermission.Type permission : IMPLICIT_OBJECT_PERMISSIONS) {

            // Create model which grants this permission to the current user
            ObjectPermissionModel permissionModel = new ObjectPermissionModel();
            permissionModel.setUserID(userModel.getObjectID());
            permissionModel.setUsername(userModel.getIdentifier());
            permissionModel.setType(permission);
            permissionModel.setObjectIdentifier(model.getIdentifier());

            // Add permission
            implicitPermissions.add(permissionModel);

        }
        
        return implicitPermissions;

    }

    @Override
    public InternalType createObject(AuthenticatedUser user, ExternalType object)
        throws GuacamoleException {

        ModelType model = getModelInstance(user, object);
        beforeCreate(user, model);
        
        // Create object
        getObjectMapper().insert(model);

        // Set identifier on original object
        object.setIdentifier(model.getIdentifier());

        // Add implicit permissions
        getPermissionMapper().insert(getImplicitPermissions(user, model));

        return getObjectInstance(user, model);

    }

    @Override
    public void deleteObject(AuthenticatedUser user, String identifier)
        throws GuacamoleException {

        beforeDelete(user, identifier);
        
        // Delete object
        getObjectMapper().delete(identifier);

    }

    @Override
    public void updateObject(AuthenticatedUser user, InternalType object)
        throws GuacamoleException {

        ModelType model = object.getModel();
        beforeUpdate(user, model);
        
        // Update object
        getObjectMapper().update(model);

    }

    @Override
    public Set<String> getIdentifiers(AuthenticatedUser user)
        throws GuacamoleException {

        // Bypass permission checks if the user is a system admin
        if (user.getUser().isAdministrator())
            return getObjectMapper().selectIdentifiers();

        // Otherwise only return explicitly readable identifiers
        else
            return getObjectMapper().selectReadableIdentifiers(user.getUser().getModel());

    }

}
