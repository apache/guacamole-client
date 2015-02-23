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

package net.sourceforge.guacamole.net.auth.mysql;


import com.google.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import net.sourceforge.guacamole.net.auth.mysql.service.ConnectionService;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.Connection;
import org.glyptodon.guacamole.net.auth.Directory;
import org.mybatis.guice.transactional.Transactional;

/**
 * A MySQL based implementation of the Connection Directory.
 *
 * @author James Muehlner
 * @author Michael Jumper
 */
public class ConnectionDirectory implements Directory<Connection> {

    /**
     * The user this user directory belongs to. Access is based on his/her
     * permission settings.
     */
    private AuthenticatedUser currentUser;
    
    /**
     * Service for managing connection objects.
     */
    @Inject
    private ConnectionService connectionService;

    /**
     * Set the user for this directory.
     *
     * @param currentUser
     *     The user whose permissions define the visibility of connections in
     *     this directory.
     */
    public void init(AuthenticatedUser currentUser) {
        this.currentUser = currentUser;
    }
    
    @Override
    public Connection get(String identifier) throws GuacamoleException {
        return connectionService.retrieveObject(currentUser, identifier);
    }

    @Override
    @Transactional
    public Collection<Connection> getAll(Collection<String> identifiers) throws GuacamoleException {
        Collection<MySQLConnection> objects = connectionService.retrieveObjects(currentUser, identifiers);
        return Collections.<Connection>unmodifiableCollection(objects);
    }

    @Override
    @Transactional
    public Set<String> getIdentifiers() throws GuacamoleException {
        return connectionService.getIdentifiers(currentUser);
    }

    @Override
    @Transactional
    public void add(Connection object) throws GuacamoleException {
        connectionService.createObject(currentUser, object);
    }

    @Override
    @Transactional
    public void update(Connection object) throws GuacamoleException {
        MySQLConnection connection = (MySQLConnection) object;
        connectionService.updateObject(currentUser, connection);
    }

    @Override
    @Transactional
    public void remove(String identifier) throws GuacamoleException {
        connectionService.deleteObject(currentUser, identifier);
    }

}
