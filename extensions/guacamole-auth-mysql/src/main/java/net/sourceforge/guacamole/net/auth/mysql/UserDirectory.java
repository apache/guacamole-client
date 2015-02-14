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
import net.sourceforge.guacamole.net.auth.mysql.service.UserService;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.net.auth.Directory;
import org.glyptodon.guacamole.net.auth.User;
import org.mybatis.guice.transactional.Transactional;

/**
 * A MySQL based implementation of the User Directory.
 *
 * @author James Muehlner
 * @author Michael Jumper
 */
public class UserDirectory implements Directory<User> {

    /**
     * The user this user directory belongs to. Access is based on his/her
     * permission settings.
     */
    private AuthenticatedUser currentUser;
    
    /**
     * Service for managing user objects.
     */
    @Inject
    private UserService userService;

    /**
     * Set the user for this directory.
     *
     * @param currentUser
     *     The user whose permissions define the visibility of other users in
     *     this directory.
     */
    public void init(AuthenticatedUser currentUser) {
        this.currentUser = currentUser;
    }
    
    @Override
    public void move(String identifier, Directory<User> groupIdentifier) 
            throws GuacamoleException {
        throw new GuacamoleSecurityException("Permission denied.");
    }

    @Override
    public User get(String identifier) throws GuacamoleException {
        return userService.retrieveObject(currentUser, identifier);
    }

    @Override
    @Transactional
    public Collection<User> getAll(Collection<String> identifiers) throws GuacamoleException {
        Collection<MySQLUser> objects = userService.retrieveObjects(currentUser, identifiers);
        return Collections.<User>unmodifiableCollection(objects);
    }

    @Override
    @Transactional
    public Set<String> getIdentifiers() throws GuacamoleException {
        return userService.getIdentifiers(currentUser);
    }

    @Override
    @Transactional
    public void add(User object) throws GuacamoleException {
        userService.createObject(currentUser, object);
    }

    @Override
    @Transactional
    public void update(User object) throws GuacamoleException {
        MySQLUser user = (MySQLUser) object;
        userService.updateObject(currentUser, user);
    }

    @Override
    @Transactional
    public void remove(String identifier) throws GuacamoleException {
        userService.deleteObject(currentUser, identifier);
    }

}
