/*
 * Copyright (C) 2013 Glyptodon LLC
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

package net.sourceforge.guacamole.net.auth.mysql.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import net.sourceforge.guacamole.net.auth.mysql.AuthenticatedUser;
import net.sourceforge.guacamole.net.auth.mysql.DirectoryObject;
import net.sourceforge.guacamole.net.auth.mysql.dao.DirectoryObjectMapper;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermission;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermissionSet;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating users.
 *
 * @author Michael Jumper
 * @param <ObjectType>
 *     The type of object this service provides access to.
 *
 * @param <ModelType>
 *     The underlying model object used to represent ObjectType in the
 *     database.
 */
public abstract class DirectoryObjectService<ObjectType extends DirectoryObject<ModelType>, ModelType> {

    /**
     * Returns an instance of a mapper for the type of object used by this
     * service.
     *
     * @return
     *     A mapper which provides access to the model objects associated with
     *     the objects used by this service.
     */
    protected abstract DirectoryObjectMapper<ModelType> getObjectMapper();

    /**
     * Returns an instance of an object which is backed by the given model
     * object.
     *
     * @param model
     *     The model object to use to back the returned object.
     *
     * @return
     *     An object which is backed by the given model object.
     */
    protected abstract ObjectType getObjectInstance(ModelType model);

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
     * @param models
     *     The model objects to use to back the objects within the returned
     *     collection.
     *
     * @return
     *     A collection of objects which are backed by the models in the given
     *     collection.
     */
    protected Collection<ObjectType> getObjectInstances(Collection<ModelType> models) {

        // Create new collection of objects by manually converting each model
        Collection<ObjectType> objects = new ArrayList<ObjectType>(models.size());
        for (ModelType model : models)
            objects.add(getObjectInstance(model));

        return objects;
        
    }

    /**
     * Retrieves the single object that has the given identifier, if it exists
     * and the user has permission to read it.
     *
     * @param user
     *     The user retrieving the object.
     *
     * @param identifier
     *     The identifier of the object to retrieve.
     *
     * @return
     *     The object having the given identifier, or null if no such object
     *     exists.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the requested object.
     */
    public ObjectType retrieveObject(AuthenticatedUser user,
            String identifier) throws GuacamoleException {

        // Pull objects having given identifier
        Collection<ObjectType> objects = retrieveObjects(user, Collections.singleton(identifier));

        // If no such object, return null
        if (objects.isEmpty())
            return null;

        // The object collection will have exactly one element unless the
        // database has seriously lost integrity
        assert(objects.size() == 1);

        // Return first and only object 
        return objects.iterator().next();

    }
    
    /**
     * Retrieves all objects that have the identifiers in the given collection.
     * Only objects that the user has permission to read will be returned.
     *
     * @param user
     *     The user retrieving the objects.
     *
     * @param identifiers
     *     The identifiers of the objects to retrieve.
     *
     * @return
     *     The objects having the given identifiers.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the requested objects.
     */
    public Collection<ObjectType> retrieveObjects(AuthenticatedUser user,
            Collection<String> identifiers) throws GuacamoleException {

        // Do not query if no identifiers given
        if (identifiers.isEmpty())
            return Collections.EMPTY_LIST;

        Collection<ModelType> objects;

        // Bypass permission checks if the user is a system admin
        if (user.getUser().isAdministrator())
            objects = getObjectMapper().select(identifiers);

        // Otherwise only return explicitly readable identifiers
        else
            objects = getObjectMapper().selectReadable(user.getUser().getModel(), identifiers);
        
        // Return collection of requested objects
        return getObjectInstances(objects);
        
    }

    /**
     * Creates the given object within the database. If the object already
     * exists, an error will be thrown. The internal model object will be
     * updated appropriately to contain the new database ID.
     *
     * @param user
     *     The user creating the object.
     *
     * @param object
     *     The object to create.
     *
     * @throws GuacamoleException
     *     If the user lacks permission to create the object, or an error
     *     occurs while creating the object.
     */
    public void createObject(AuthenticatedUser user, ObjectType object)
        throws GuacamoleException {

        // Only create object if user has permission to do so
        if (user.getUser().isAdministrator() || hasCreatePermission(user))
            getObjectMapper().insert(object.getModel());

        // User lacks permission to create 
        throw new GuacamoleSecurityException("Permission denied.");

    }

    /**
     * Deletes the object having the given identifier. If no such object
     * exists, this function has no effect.
     *
     * @param user
     *     The user deleting the object.
     *
     * @param identifier
     *     The identifier of the object to delete.
     *
     * @throws GuacamoleException
     *     If the user lacks permission to delete the object, or an error
     *     occurs while deleting the object.
     */
    public void deleteObject(AuthenticatedUser user, String identifier)
        throws GuacamoleException {

        // Get object permissions
        ObjectPermissionSet permissionSet = getPermissionSet(user);
        
        // Only delete object if user has permission to do so
        if (user.getUser().isAdministrator()
                || permissionSet.hasPermission(ObjectPermission.Type.DELETE, identifier))
            getObjectMapper().delete(identifier);

        // User lacks permission to delete 
        throw new GuacamoleSecurityException("Permission denied.");

    }

    /**
     * Updates the given object in the database, applying any changes that have
     * been made. If no such object exists, this function has no effect.
     *
     * @param user
     *     The user updating the object.
     *
     * @param object
     *     The object to update.
     *
     * @throws GuacamoleException
     *     If the user lacks permission to update the object, or an error
     *     occurs while updating the object.
     */
    public void updateObject(AuthenticatedUser user, ObjectType object)
        throws GuacamoleException {

        // Get object permissions
        ObjectPermissionSet permissionSet = getPermissionSet(user);
        
        // Only update object if user has permission to do so
        if (user.getUser().isAdministrator()
                || permissionSet.hasPermission(ObjectPermission.Type.UPDATE, object.getIdentifier()))
            getObjectMapper().update(object.getModel());

        // User lacks permission to update
        throw new GuacamoleSecurityException("Permission denied.");

    }

    /**
     * Returns the set of all identifiers for all objects in the database that
     * the user has read access to.
     *
     * @param user
     *     The user retrieving the identifiers.
     *
     * @return
     *     The set of all identifiers for all objects in the database.
     *
     * @throws GuacamoleException
     *     If an error occurs while reading identifiers.
     */
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
