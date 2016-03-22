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

package org.apache.guacamole.auth.jdbc.user;


import com.google.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.jdbc.base.RestrictedObject;
import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.net.auth.User;
import org.mybatis.guice.transactional.Transactional;

/**
 * Implementation of the User Directory which is driven by an underlying,
 * arbitrary database.
 *
 * @author James Muehlner
 * @author Michael Jumper
 */
public class UserDirectory extends RestrictedObject
    implements Directory<User> {

    /**
     * Service for managing user objects.
     */
    @Inject
    private UserService userService;

    @Override
    public User get(String identifier) throws GuacamoleException {
        return userService.retrieveObject(getCurrentUser(), identifier);
    }

    @Override
    @Transactional
    public Collection<User> getAll(Collection<String> identifiers) throws GuacamoleException {
        Collection<ModeledUser> objects = userService.retrieveObjects(getCurrentUser(), identifiers);
        return Collections.<User>unmodifiableCollection(objects);
    }

    @Override
    @Transactional
    public Set<String> getIdentifiers() throws GuacamoleException {
        return userService.getIdentifiers(getCurrentUser());
    }

    @Override
    @Transactional
    public void add(User object) throws GuacamoleException {
        userService.createObject(getCurrentUser(), object);
    }

    @Override
    @Transactional
    public void update(User object) throws GuacamoleException {
        ModeledUser user = (ModeledUser) object;
        userService.updateObject(getCurrentUser(), user);
    }

    @Override
    @Transactional
    public void remove(String identifier) throws GuacamoleException {
        userService.deleteObject(getCurrentUser(), identifier);
    }

}
