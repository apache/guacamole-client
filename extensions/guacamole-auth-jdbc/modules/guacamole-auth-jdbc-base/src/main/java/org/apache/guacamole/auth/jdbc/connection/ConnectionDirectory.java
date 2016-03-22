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

package org.apache.guacamole.auth.jdbc.connection;


import com.google.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.jdbc.base.RestrictedObject;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.Directory;
import org.mybatis.guice.transactional.Transactional;

/**
 * Implementation of the Connection Directory which is driven by an underlying,
 * arbitrary database.
 *
 * @author James Muehlner
 * @author Michael Jumper
 */
public class ConnectionDirectory extends RestrictedObject
    implements Directory<Connection> {

    /**
     * Service for managing connection objects.
     */
    @Inject
    private ConnectionService connectionService;

    @Override
    public Connection get(String identifier) throws GuacamoleException {
        return connectionService.retrieveObject(getCurrentUser(), identifier);
    }

    @Override
    @Transactional
    public Collection<Connection> getAll(Collection<String> identifiers) throws GuacamoleException {
        Collection<ModeledConnection> objects = connectionService.retrieveObjects(getCurrentUser(), identifiers);
        return Collections.<Connection>unmodifiableCollection(objects);
    }

    @Override
    @Transactional
    public Set<String> getIdentifiers() throws GuacamoleException {
        return connectionService.getIdentifiers(getCurrentUser());
    }

    @Override
    @Transactional
    public void add(Connection object) throws GuacamoleException {
        connectionService.createObject(getCurrentUser(), object);
    }

    @Override
    @Transactional
    public void update(Connection object) throws GuacamoleException {
        ModeledConnection connection = (ModeledConnection) object;
        connectionService.updateObject(getCurrentUser(), connection);
    }

    @Override
    @Transactional
    public void remove(String identifier) throws GuacamoleException {
        connectionService.deleteObject(getCurrentUser(), identifier);
    }

}
