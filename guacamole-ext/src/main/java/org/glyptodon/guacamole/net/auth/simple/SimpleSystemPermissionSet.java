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

import java.util.Collections;
import java.util.Set;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.net.auth.permission.SystemPermission;
import org.glyptodon.guacamole.net.auth.permission.SystemPermissionSet;

/**
 * A read-only implementation of SystemPermissionSet which uses a backing Set
 * of Permissions to determine which permissions are present.
 *
 * @author Michael Jumper
 */
public class SimpleSystemPermissionSet implements SystemPermissionSet {

    /**
     * The set of all permissions currently granted.
     */
    private Set<SystemPermission> permissions = Collections.EMPTY_SET;

    /**
     * Creates a new empty SimpleSystemPermissionSet.
     */
    public SimpleSystemPermissionSet() {
    }

    /**
     * Creates a new SimpleSystemPermissionSet which contains the permissions
     * within the given Set.
     *
     * @param permissions 
     *     The Set of permissions this SimpleSystemPermissionSet should
     *     contain.
     */
    public SimpleSystemPermissionSet(Set<SystemPermission> permissions) {
        this.permissions = permissions;
    }

    /**
     * Sets the Set which backs this SimpleSystemPermissionSet. Future function
     * calls on this SimpleSystemPermissionSet will use the provided Set.
     *
     * @param permissions 
     *     The Set of permissions this SimpleSystemPermissionSet should
     *     contain.
     */
    protected void setPermissions(Set<SystemPermission> permissions) {
        this.permissions = permissions;
    }

    @Override
    public Set<SystemPermission> getPermissions() {
        return permissions;
    }

    @Override
    public boolean hasPermission(SystemPermission.Type permission)
            throws GuacamoleException {

        SystemPermission systemPermission = new SystemPermission(permission);
        return permissions.contains(systemPermission);

    }

    @Override
    public void addPermission(SystemPermission.Type permission)
            throws GuacamoleException {
        throw new GuacamoleSecurityException("Permission denied.");
    }

    @Override
    public void removePermission(SystemPermission.Type permission)
            throws GuacamoleException {
        throw new GuacamoleSecurityException("Permission denied.");
    }

    @Override
    public void addPermissions(Set<SystemPermission> permissions)
            throws GuacamoleException {
        throw new GuacamoleSecurityException("Permission denied.");
    }

    @Override
    public void removePermissions(Set<SystemPermission> permissions)
            throws GuacamoleException {
        throw new GuacamoleSecurityException("Permission denied.");
    }

}
