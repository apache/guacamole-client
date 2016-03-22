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

package org.apache.guacamole.auth.jdbc.connectiongroup;


import com.google.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.jdbc.base.RestrictedObject;
import org.apache.guacamole.net.auth.ConnectionGroup;
import org.apache.guacamole.net.auth.Directory;
import org.mybatis.guice.transactional.Transactional;

/**
 * Implementation of the ConnectionGroup Directory which is driven by an
 * underlying, arbitrary database.
 *
 * @author James Muehlner
 * @author Michael Jumper
 */
public class ConnectionGroupDirectory extends RestrictedObject
    implements Directory<ConnectionGroup> {

    /**
     * Service for managing connection group objects.
     */
    @Inject
    private ConnectionGroupService connectionGroupService;

    @Override
    public ConnectionGroup get(String identifier) throws GuacamoleException {
        return connectionGroupService.retrieveObject(getCurrentUser(), identifier);
    }

    @Override
    @Transactional
    public Collection<ConnectionGroup> getAll(Collection<String> identifiers) throws GuacamoleException {
        Collection<ModeledConnectionGroup> objects = connectionGroupService.retrieveObjects(getCurrentUser(), identifiers);
        return Collections.<ConnectionGroup>unmodifiableCollection(objects);
    }

    @Override
    @Transactional
    public Set<String> getIdentifiers() throws GuacamoleException {
        return connectionGroupService.getIdentifiers(getCurrentUser());
    }

    @Override
    @Transactional
    public void add(ConnectionGroup object) throws GuacamoleException {
        connectionGroupService.createObject(getCurrentUser(), object);
    }

    @Override
    @Transactional
    public void update(ConnectionGroup object) throws GuacamoleException {
        ModeledConnectionGroup connectionGroup = (ModeledConnectionGroup) object;
        connectionGroupService.updateObject(getCurrentUser(), connectionGroup);
    }

    @Override
    @Transactional
    public void remove(String identifier) throws GuacamoleException {
        connectionGroupService.deleteObject(getCurrentUser(), identifier);
    }

}
