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

package org.glyptodon.guacamole.auth.jdbc.permission;

import org.glyptodon.guacamole.auth.jdbc.user.ModeledUser;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.glyptodon.guacamole.auth.jdbc.user.AuthenticatedUser;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.auth.jdbc.base.RestrictedObject;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermission;

/**
 * A database implementation of ObjectPermissionSet which uses an injected
 * service to query and manipulate the object-level permissions associated with
 * a particular user.
 *
 * @author Michael Jumper
 */
public abstract class ObjectPermissionSet extends RestrictedObject
    implements org.glyptodon.guacamole.net.auth.permission.ObjectPermissionSet {

    /**
     * The user associated with this permission set. Each of the permissions in
     * this permission set is granted to this user.
     */
    private ModeledUser user;

    /**
     * Creates a new ObjectPermissionSet. The resulting permission set
     * must still be initialized by a call to init(), or the information
     * necessary to read and modify this set will be missing.
     */
    public ObjectPermissionSet() {
    }

    /**
     * Initializes this permission set with the current user and the user
     * to whom the permissions in this set are granted.
     *
     * @param currentUser
     *     The user who queried this permission set, and whose permissions
     *     dictate the access level of all operations performed on this set.
     *
     * @param user
     *     The user to whom the permissions in this set are granted.
     */
    public void init(AuthenticatedUser currentUser, ModeledUser user) {
        super.init(currentUser);
        this.user = user;
    }

    /**
     * Returns an ObjectPermissionService implementation for manipulating the
     * type of permissions contained within this permission set.
     *
     * @return
     *     An object permission service for manipulating the type of
     *     permissions contained within this permission set.
     */
    protected abstract ObjectPermissionService getObjectPermissionService();
 
    @Override
    public Set<ObjectPermission> getPermissions() throws GuacamoleException {
        return getObjectPermissionService().retrievePermissions(getCurrentUser(), user);
    }

    @Override
    public boolean hasPermission(ObjectPermission.Type permission,
            String identifier) throws GuacamoleException {
        return getObjectPermissionService().retrievePermission(getCurrentUser(), user, permission, identifier) != null;
    }

    @Override
    public void addPermission(ObjectPermission.Type permission,
            String identifier) throws GuacamoleException {
        addPermissions(Collections.singleton(new ObjectPermission(permission, identifier)));
    }

    @Override
    public void removePermission(ObjectPermission.Type permission,
            String identifier) throws GuacamoleException {
        removePermissions(Collections.singleton(new ObjectPermission(permission, identifier)));
    }

    @Override
    public Collection<String> getAccessibleObjects(Collection<ObjectPermission.Type> permissions,
            Collection<String> identifiers) throws GuacamoleException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addPermissions(Set<ObjectPermission> permissions)
            throws GuacamoleException {
        getObjectPermissionService().createPermissions(getCurrentUser(), user, permissions);
    }

    @Override
    public void removePermissions(Set<ObjectPermission> permissions)
            throws GuacamoleException {
        getObjectPermissionService().deletePermissions(getCurrentUser(), user, permissions);
    }

}
