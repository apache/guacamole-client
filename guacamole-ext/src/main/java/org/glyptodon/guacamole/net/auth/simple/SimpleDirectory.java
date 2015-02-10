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

package org.glyptodon.guacamole.net.auth.simple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.net.auth.Directory;

/**
 * An extremely simple read-only implementation of a Directory which provides
 * access to a pre-defined Map of arbitrary objects. Any changes to the Map
 * will affect the available contents of this SimpleDirectory.
 *
 * @author Michael Jumper
 * @param <IdentifierType>
 *     The type of identifier used to identify objects stored within this
 *     SimpleDirectory.
 *
 * @param <ObjectType>
 *     The type of objects stored within this SimpleDirectory.
 */
public class SimpleDirectory<IdentifierType, ObjectType>
    implements Directory<IdentifierType, ObjectType> {

    /**
     * The Map of objects to provide access to.
     */
    private Map<IdentifierType, ObjectType> objects = Collections.EMPTY_MAP;

    /**
     * Creates a new empty SimpleDirectory which does not provide access to
     * any objects.
     */
    public SimpleDirectory() {
    }

    /**
     * Creates a new SimpleDirectory which provides access to the objects
     * contained within the given Map.
     *
     * @param objects
     *     The Map of objects to provide access to.
     */
    public SimpleDirectory(Map<IdentifierType, ObjectType> objects) {
        this.objects = objects;
    }

    /**
     * Sets the Map which backs this SimpleDirectory. Future function calls
     * which retrieve objects from this SimpleDirectory will use the provided
     * Map.
     *
     * @param objects
     *     The Map of objects to provide access to.
     */
    protected void setObjects(Map<IdentifierType, ObjectType> objects) {
        this.objects = objects;
    }

    /**
     * Returns the Map which currently backs this SimpleDirectory. Changes to
     * this Map will affect future function calls that retrieve objects from
     * this SimpleDirectory.
     *
     * @return
     *     The Map of objects which currently backs this SimpleDirectory.
     */
    protected Map<IdentifierType, ObjectType> getObjects() {
        return objects;
    }

    @Override
    public ObjectType get(IdentifierType identifier)
            throws GuacamoleException {
        return objects.get(identifier);
    }

    @Override
    public Collection<ObjectType> getAll(Collection<IdentifierType> identifiers)
            throws GuacamoleException {

        // Create collection which has an appropriate initial size
        Collection<ObjectType> foundObjects = new ArrayList<ObjectType>(identifiers.size());

        // Populate collection with matching objects
        for (IdentifierType identifier : identifiers) {

            // Add the object which has the current identifier, if any
            ObjectType object = objects.get(identifier);
            if (object != null)
                foundObjects.add(object);

        }

        return foundObjects;

    }

    @Override
    public Set<IdentifierType> getIdentifiers() throws GuacamoleException {
        return objects.keySet();
    }

    @Override
    public void add(ObjectType connection)
            throws GuacamoleException {
        throw new GuacamoleSecurityException("Permission denied.");
    }

    @Override
    public void update(ObjectType connection)
            throws GuacamoleException {
        throw new GuacamoleSecurityException("Permission denied.");
    }

    @Override
    public void remove(IdentifierType identifier) throws GuacamoleException {
        throw new GuacamoleSecurityException("Permission denied.");
    }

    @Override
    public void move(IdentifierType identifier,
            Directory<IdentifierType, ObjectType> directory)
            throws GuacamoleException {
        throw new GuacamoleSecurityException("Permission denied.");
    }

}
