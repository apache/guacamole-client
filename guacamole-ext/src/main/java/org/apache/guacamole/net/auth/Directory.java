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

package org.apache.guacamole.net.auth;

import java.util.Collection;
import java.util.Set;
import org.apache.guacamole.GuacamoleException;

/**
 * Provides access to a collection of all objects with associated identifiers,
 * and allows user manipulation and removal. Objects returned by a Directory
 * are not necessarily backed by the stored objects, thus updating an object
 * always requires calling the update() function.
 *
 * @param <ObjectType>
 *     The type of objects stored within this Directory.
 */
public interface Directory<ObjectType extends Identifiable> {

    /**
     * Returns the object having the given identifier. Note that changes to
     * the object returned will not necessarily affect the object stored within
     * the Directory. To update an object stored within an
     * Directory such that future calls to get() will return the updated
     * object, you must call update() on the object after modification.
     *
     * @param identifier The identifier to use when locating the object to
     *                   return.
     * @return The object having the given identifier, or null if no such object
     *         exists.
     *
     * @throws GuacamoleException If an error occurs while retrieving the
     *                            object, or if permission for retrieving the
     *                            object is denied.
     */
    ObjectType get(String identifier) throws GuacamoleException;

    /**
     * Returns the objects having the given identifiers. Note that changes to
     * any object returned will not necessarily affect the object stored within
     * the Directory. To update an object stored within a
     * Directory such that future calls to get() will return the updated
     * object, you must call update() on the object after modification.
     *
     * @param identifiers
     *     The identifiers to use when locating the objects to return.
     *
     * @return
     *     The objects having the given identifiers. If any identifiers do not
     *     correspond to accessible objects, those identifiers will be ignored.
     *     If no objects correspond to any of the given identifiers, the
     *     returned collection will be empty.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the objects, or if permission
     *     to retrieve the requested objects is denied.
     */
    Collection<ObjectType> getAll(Collection<String> identifiers)
            throws GuacamoleException;

    /**
     * Returns a Set containing all identifiers for all objects within this
     * Directory.
     *
     * @return A Set of all identifiers.
     * @throws GuacamoleException If an error occurs while retrieving
     *                            the identifiers.
     */
    Set<String> getIdentifiers() throws GuacamoleException;

    /**
     * Adds the given object to the overall set. If a new identifier is
     * created for the added object, that identifier will be automatically
     * assigned via setIdentifier().
     *
     * @param object
     *     The object to add.
     *
     * @throws GuacamoleException
     *     If an error occurs while adding the object, or if adding the object
     *     is not allowed.
     */
    void add(ObjectType object)
            throws GuacamoleException;

    /**
     * Updates the stored object with the data contained in the given object.
     *
     * @param object The object which will supply the data for the update.
     *
     * @throws GuacamoleException If an error occurs while updating the object,
     *                            or if updating the object is not allowed.
     */
    void update(ObjectType object)
            throws GuacamoleException;

    /**
     * Removes the object with the given identifier from the overall set.
     *
     * @param identifier The identifier of the object to remove.
     *
     * @throws GuacamoleException If an error occurs while removing the object,
     *                            or if removing object is not allowed.
     */
    void remove(String identifier) throws GuacamoleException;

}
