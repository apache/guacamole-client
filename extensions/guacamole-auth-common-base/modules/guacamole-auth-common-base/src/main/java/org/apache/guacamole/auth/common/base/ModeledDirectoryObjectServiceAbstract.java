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

package org.apache.guacamole.auth.common.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleSecurityException;
import org.apache.guacamole.auth.common.permission.ObjectPermissionMapperInterface;
import org.apache.guacamole.auth.common.permission.ObjectPermissionModelInterface;
import org.apache.guacamole.auth.common.user.ModeledAuthenticatedUser;
import org.apache.guacamole.auth.common.user.UserModelInterface;
import org.apache.guacamole.net.auth.Identifiable;
import org.apache.guacamole.net.auth.permission.ObjectPermission;
import org.apache.guacamole.net.auth.permission.ObjectPermission.Type;
import org.apache.guacamole.net.auth.permission.ObjectPermissionSet;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating objects within directories. This service will automatically
 * enforce the permissions of the current user.
 *
 * @param <InternalType>
 *            The specific internal implementation of the type of object this
 *            service provides access to.
 *
 * @param <ExternalType>
 *            The external interface or implementation of the type of object
 *            this service provides access to, as defined by the guacamole-ext
 *            API.
 *
 * @param <ModelType>
 *            The underlying model object used to represent InternalType in the
 *            database.
 */
/**
 * @author pirao
 *
 * @param <InternalType>
 * @param <ExternalType>
 * @param <ModelType>
 */
public abstract class ModeledDirectoryObjectServiceAbstract<InternalType extends ModeledDirectoryObject<ModelType>, ExternalType extends Identifiable, ModelType extends ObjectModelInterface>
        implements DirectoryObjectService<InternalType, ExternalType> {

    /**
     * All object permissions which are implicitly granted upon creation to the
     * creator of the object.
     */
    protected static final ObjectPermission.Type[] IMPLICIT_OBJECT_PERMISSIONS = {
            ObjectPermission.Type.READ, ObjectPermission.Type.UPDATE,
            ObjectPermission.Type.DELETE, ObjectPermission.Type.ADMINISTER };

    /**
     * Returns an instance of a mapper for the type of object used by this
     * service.
     *
     * @return A mapper which provides access to the model objects associated
     *         with the objects used by this service.
     */
    protected abstract ModeledDirectoryObjectMapperInterface<ModelType> getObjectMapper();

    /**
     * Returns an instance of a mapper for the type of permissions that affect
     * the type of object used by this service.
     *
     * @return A mapper which provides access to the model objects associated
     *         with the permissions that affect the objects used by this
     *         service.
     */
    protected abstract ObjectPermissionMapperInterface getPermissionMapper();

    /**
     * Returns an instance of an object which is backed by the given model
     * object.
     *
     * @param currentUser
     *            The user for whom this object is being created.
     *
     * @param model
     *            The model object to use to back the returned object.
     *
     * @return An object which is backed by the given model object.
     *
     * @throws GuacamoleException
     *             If the object instance cannot be created.
     */
    protected abstract InternalType getObjectInstance(
            ModeledAuthenticatedUser currentUser, ModelType model)
            throws GuacamoleException;

    /**
     * Returns an instance of a model object which is based on the given object.
     *
     * @param currentUser
     *            The user for whom this model object is being created.
     *
     * @param object
     *            The object to use to produce the returned model object.
     *
     * @return A model object which is based on the given object.
     *
     * @throws GuacamoleException
     *             If the model object instance cannot be created.
     */
    protected abstract ModelType getModelInstance(
            ModeledAuthenticatedUser currentUser, ExternalType object)
            throws GuacamoleException;

    /**
     * Returns whether the given user has permission to create the type of
     * objects that this directory object service manages, taking into account
     * permission inheritance through user groups.
     *
     * @param user
     *            The user being checked.
     *
     * @return true if the user has object creation permission relevant to this
     *         directory object service, false otherwise.
     * 
     * @throws GuacamoleException
     *             If permission to read the user's permissions is denied.
     */
    protected abstract boolean hasCreatePermission(
            ModeledAuthenticatedUser user) throws GuacamoleException;

    /**
     * Returns whether the given user has permission to perform a certain action
     * on a specific object managed by this directory object service,
     * taking into account permission inheritance through user groups.
     *
     * @param user
     *            The user being checked.
     *
     * @param identifier
     *            The identifier of the object to check.
     *
     * @param type
     *            The type of action that will be performed.
     *
     * @return true if the user has object permission relevant described, false
     *         otherwise.
     * 
     * @throws GuacamoleException
     *             If permission to read the user's permissions is denied.
     */
    protected boolean hasObjectPermission(ModeledAuthenticatedUser user,
            String identifier, ObjectPermission.Type type)
            throws GuacamoleException {

        // Get object permissions
        ObjectPermissionSet permissionSet = getEffectivePermissionSet(user);

        // Return whether permission is granted
        return user.getUser().isAdministrator()
                || permissionSet.hasPermission(type, identifier);

    }

    /**
     * Returns the permission set associated with the given user and related to
     * the type of objects handled by this directory object service, taking
     * into account permission inheritance via user groups.
     *
     * @param user
     *            The user whose permissions are being retrieved.
     *
     * @return A permission set which contains the permissions associated with
     *         the given user and related to the type of objects handled by this
     *         directory object service.
     * 
     * @throws GuacamoleException
     *             If permission to read the user's permissions is denied.
     */
    protected abstract ObjectPermissionSet getEffectivePermissionSet(
            ModeledAuthenticatedUser user) throws GuacamoleException;

    /**
     * Returns a collection of objects which are backed by the models in the
     * given collection.
     *
     * @param currentUser
     *            The user for whom these objects are being created.
     *
     * @param models
     *            The model objects to use to back the objects within the
     *            returned collection.
     *
     * @return A collection of objects which are backed by the models in the
     *         given collection.
     *
     * @throws GuacamoleException
     *             If any of the object instances cannot be created.
     */
    protected Collection<InternalType> getObjectInstances(
            ModeledAuthenticatedUser currentUser, Collection<ModelType> models)
            throws GuacamoleException {

        // Create new collection of objects by manually converting each model
        Collection<InternalType> objects = new ArrayList<InternalType>(
                models.size());
        for (ModelType model : models)
            objects.add(getObjectInstance(currentUser, model));

        return objects;

    }

    /**
     * Called before any object is created through this directory object
     * service. This function serves as a final point of validation before the
     * create operation occurs. In its default implementation, beforeCreate()
     * performs basic permissions checks.
     *
     * @param user
     *            The user creating the object.
     *
     * @param object
     *            The object being created.
     *
     * @param model
     *            The model of the object being created.
     *
     * @throws GuacamoleException
     *             If the object is invalid, or an error prevents validating the
     *             given object.
     */
    protected void beforeCreate(ModeledAuthenticatedUser user,
            ExternalType object, ModelType model) throws GuacamoleException {

        // Verify permission to create objects
        if (!user.getUser().isAdministrator() && !hasCreatePermission(user))
            throw new GuacamoleSecurityException("Permission denied.");

    }

    /**
     * Called before any object is updated through this directory object
     * service. This function serves as a final point of validation before the
     * update operation occurs. In its default implementation, beforeUpdate()
     * performs basic permissions checks.
     *
     * @param user
     *            The user updating the existing object.
     *
     * @param object
     *            The object being updated.
     *
     * @param model
     *            The model of the object being updated.
     *
     * @throws GuacamoleException
     *             If the object is invalid, or an error prevents validating the
     *             given object.
     */
    protected void beforeUpdate(ModeledAuthenticatedUser user,
            InternalType object, ModelType model) throws GuacamoleException {

        // By default, do nothing.
        if (!hasObjectPermission(user, model.getIdentifier(),
                ObjectPermission.Type.UPDATE))
            throw new GuacamoleSecurityException("Permission denied.");

    }

    /**
     * Called before any object is deleted through this directory object
     * service. This function serves as a final point of validation before the
     * delete operation occurs. In its default implementation, beforeDelete()
     * performs basic permissions checks.
     *
     * @param user
     *            The user deleting the existing object.
     *
     * @param identifier
     *            The identifier of the object being deleted.
     *
     * @throws GuacamoleException
     *             If the object is invalid, or an error prevents validating the
     *             given object.
     */
    protected void beforeDelete(ModeledAuthenticatedUser user,
            String identifier) throws GuacamoleException {

        // Verify permission to delete objects
        if (!hasObjectPermission(user, identifier,
                ObjectPermission.Type.DELETE))
            throw new GuacamoleSecurityException("Permission denied.");

    }

    /**
     * Returns whether the given string is a valid identifier within the Mongo
     * authentication extension. Invalid identifiers may result in SQL errors
     * from the underlying database when used in queries.
     *
     * @param identifier
     *            The string to check for validity.
     *
     * @return true if the given string is a valid identifier, false otherwise.
     */
    protected abstract boolean isValidIdentifier(String identifier);

    /**
     * Filters the given collection of strings, returning a new collection
     * containing only those strings which are valid identifiers. If no strings
     * within the collection are valid identifiers, the returned collection will
     * simply be empty.
     *
     * @param identifiers
     *            The collection of strings to filter.
     *
     * @return A new collection containing only the strings within the provided
     *         collection which are valid identifiers.
     */
    protected Collection<String> filterIdentifiers(
            Collection<String> identifiers) {

        // Obtain enough space for a full copy of the given identifiers
        Collection<String> validIdentifiers = new ArrayList<String>(
                identifiers.size());

        // Add only valid identifiers to the copy
        for (String identifier : identifiers) {
            if (isValidIdentifier(identifier))
                validIdentifiers.add(identifier);
        }

        return validIdentifiers;

    }

    @Override
    public InternalType retrieveObject(ModeledAuthenticatedUser user,
            String identifier) throws GuacamoleException {

        // Pull objects having given identifier
        Collection<InternalType> objects = retrieveObjects(user,
                Collections.singleton(identifier));

        // If no such object, return null
        if (objects.isEmpty())
            return null;

        // The object collection will have exactly one element unless the
        // database has seriously lost integrity
        assert (objects.size() == 1);

        // Return first and only object
        return objects.iterator().next();

    }

    @Override
    public Collection<InternalType> retrieveObjects(
            ModeledAuthenticatedUser user, Collection<String> identifiers)
            throws GuacamoleException {

        // Ignore invalid identifiers
        identifiers = filterIdentifiers(identifiers);

        // Do not query if no identifiers given
        if (identifiers.isEmpty())
            return Collections.<InternalType>emptyList();

        Collection<ModelType> objects;

        // Bypass permission checks if the user is a system admin
        if (user.getUser().isAdministrator())
            objects = getObjectMapper().select(identifiers);

        // Otherwise only return explicitly readable identifiers
        else
            objects = getObjectMapper()
                    .selectReadable(user.getUser().getModel(),
                    		identifiers, user.getEffectiveUserGroups());

        // Return collection of requested objects
        return getObjectInstances(user, objects);

    }

    /**
     * Returns a collection of permissions that should be granted due to the
     * creation of the given object. These permissions need not be granted
     * solely to the user creating the object.
     * 
     * @param user
     *            The user creating the object.
     * 
     * @param model
     *            The object being created.
     * 
     * @return The collection of implicit permissions that should be granted due
     *         to the creation of the given object.
     */
   	protected Collection<ObjectPermissionModelInterface> getImplicitPermissions(
               ModeledAuthenticatedUser user, ModelType model) {

           // Build list of implicit permissions
           Collection<ObjectPermissionModelInterface> implicitPermissions = new ArrayList<ObjectPermissionModelInterface>(
                   IMPLICIT_OBJECT_PERMISSIONS.length);
           
           UserModelInterface userModel = user.getUser().getModel();
           
           loadPermissions(userModel, model, implicitPermissions, IMPLICIT_OBJECT_PERMISSIONS);

           return implicitPermissions;

    }
   	
   	/**
   	 * Iterate the received permissions to create specifically the permissions of each type
   	 * 
   	 * @param userModel
   	 * @param model
   	 * @param implicitPermissionsLoaded
   	 * @param implicitUserPermissions
   	 */
   	protected void loadPermissions(UserModelInterface userModel,
   								ModelType model,
								 Collection<ObjectPermissionModelInterface> implicitPermissionsLoaded,
								 Type[] implicitUserPermissions) {
   		
		// Grant implicit permissions
        for (ObjectPermission.Type permission : implicitUserPermissions) {

        	createModelPermission(userModel, implicitPermissionsLoaded, model, permission);
        	
        }
	}

    /**
     * Create the specific permission type
     * 
     * @param userModel
     * @param implicitPermissions
     * @param model
     * @param permission
     */
    protected abstract void createModelPermission(UserModelInterface userModel,
			Collection<ObjectPermissionModelInterface> implicitPermissions, ModelType model,
			Type permission);

	@Override
    public InternalType createObject(ModeledAuthenticatedUser user,
            ExternalType object) throws GuacamoleException {

        ModelType model = getModelInstance(user, object);
        beforeCreate(user, object, model);

        // Create object
        getObjectMapper().insert(model);

        // Set identifier on original object
        object.setIdentifier(model.getIdentifier());

        // Add implicit permissions
        getPermissionMapper().insert(getImplicitPermissions(user, model));

        // Add any arbitrary attributes
        if (model.hasArbitraryAttributes())
            getObjectMapper().insertAttributes(model);
        
        return getObjectInstance(user, model);

    }

	@Override
    public void deleteObject(ModeledAuthenticatedUser user, String identifier)
            throws GuacamoleException {

        beforeDelete(user, identifier);

        // Delete object
        getObjectMapper().delete(identifier);

    }

    @Override
    public void updateObject(ModeledAuthenticatedUser user, InternalType object)
            throws GuacamoleException {

        ModelType model = object.getModel();
        beforeUpdate(user, object, model);

        // Update object
        getObjectMapper().update(model);

        // Replace any existing arbitrary attributes
        getObjectMapper().deleteAttributes(model);
        if (model.hasArbitraryAttributes())
            getObjectMapper().insertAttributes(model);
        
    }

    @Override
    public Set<String> getIdentifiers(ModeledAuthenticatedUser user)
            throws GuacamoleException {

        // Bypass permission checks if the user is a system admin
        if (user.getUser().isAdministrator())
            return getObjectMapper().selectIdentifiers();

        // Otherwise only return explicitly readable identifiers
        else
            return getObjectMapper()
                    .selectReadableIdentifiers(user.getUser().getModel(),
                            user.getEffectiveUserGroups());

    }

}
