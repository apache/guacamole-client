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

package org.apache.guacamole.auth.jdbc.activeconnection;


import com.google.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.jdbc.base.RestrictedObject;
import org.apache.guacamole.net.auth.ActiveConnection;
import org.apache.guacamole.net.auth.Directory;

/**
 * Implementation of a Directory which contains all currently-active
 * connections.
 *
 * @author Michael Jumper
 */
public class ActiveConnectionDirectory extends RestrictedObject
    implements Directory<ActiveConnection> {

    /**
     * Service for retrieving and manipulating active connections.
     */
    @Inject
    private ActiveConnectionService activeConnectionService;

    @Override
    public ActiveConnection get(String identifier) throws GuacamoleException {
        return activeConnectionService.retrieveObject(getCurrentUser(), identifier);
    }

    @Override
    public Collection<ActiveConnection> getAll(Collection<String> identifiers)
            throws GuacamoleException {
        Collection<TrackedActiveConnection> objects = activeConnectionService.retrieveObjects(getCurrentUser(), identifiers);
        return Collections.<ActiveConnection>unmodifiableCollection(objects);
    }

    @Override
    public Set<String> getIdentifiers() throws GuacamoleException {
        return activeConnectionService.getIdentifiers(getCurrentUser());
    }

    @Override
    public void add(ActiveConnection object) throws GuacamoleException {
        activeConnectionService.createObject(getCurrentUser(), object);
    }

    @Override
    public void update(ActiveConnection object) throws GuacamoleException {
        TrackedActiveConnection connection = (TrackedActiveConnection) object;
        activeConnectionService.updateObject(getCurrentUser(), connection);
    }

    @Override
    public void remove(String identifier) throws GuacamoleException {
        activeConnectionService.deleteObject(getCurrentUser(), identifier);
    }

}
