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
import net.sourceforge.guacamole.net.auth.mysql.DirectoryObject;
import net.sourceforge.guacamole.net.auth.mysql.dao.DirectoryObjectMapper;

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
     * Retrieves the single object that has the given identifier, if it exists.
     *
     * @param identifier
     *     The identifier of the object to retrieve.
     *
     * @return
     *     The object having the given identifier, or null if no such object
     *     exists.
     */
    public ObjectType retrieveObject(String identifier) {

        // Pull objects having given identifier
        Collection<ObjectType> objects = retrieveObjects(Collections.singleton(identifier));

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
     *
     * @param identifiers
     *     The identifiers of the objects to retrieve.
     *
     * @return
     *     The objects having the given identifiers.
     */
    public Collection<ObjectType> retrieveObjects(Collection<String> identifiers) {

        // Do not query if no identifiers given
        if (identifiers.isEmpty())
            return Collections.EMPTY_LIST;

        // Return collection of requested objects
        return getObjectInstances(getObjectMapper().select(identifiers));
        
    }

    /**
     * Creates the given object within the database. If the object already
     * exists, an error will be thrown. The internal model object will be
     * updated appropriately to contain the new database ID.
     *
     * @param object
     *     The object to create.
     */
    public void createObject(ObjectType object) {
        getObjectMapper().insert(object.getModel());
    }

    /**
     * Deletes the object having the given identifier. If no such object
     * exists, this function has no effect.
     *
     * @param identifier
     *     The identifier of the object to delete.
     */
    public void deleteObject(String identifier) {
        getObjectMapper().delete(identifier);
    }

    /**
     * Updates the given object in the database, applying any changes that have
     * been made. If no such object exists, this function has no effect.
     *
     * @param object
     *     The object to update.
     */
    public void updateObject(ObjectType object) {
        getObjectMapper().update(object.getModel());
    }

    /**
     * Returns the set of all identifiers for all objects in the database.
     *
     * @return
     *     The set of all identifiers for all objects in the database.
     */
    public Set<String> getIdentifiers() {
        return getObjectMapper().selectIdentifiers();
    }

}
