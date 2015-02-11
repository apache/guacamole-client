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

package org.glyptodon.guacamole.net.auth.simple;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.AbstractUser;
import org.glyptodon.guacamole.net.auth.Connection;
import org.glyptodon.guacamole.net.auth.ConnectionGroup;
import org.glyptodon.guacamole.net.auth.User;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermission;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermissionSet;
import org.glyptodon.guacamole.net.auth.permission.SystemPermissionSet;
import org.glyptodon.guacamole.protocol.GuacamoleConfiguration;

/**
 * An extremely basic User implementation.
 *
 * @author Michael Jumper
 */
public class SimpleUser extends AbstractUser {

    /**
     * All connection permissions granted to this user.
     */
    private final Set<ObjectPermission<String>> connectionPermissions =
            new HashSet<ObjectPermission<String>>();
    
    /**
     * All connection group permissions granted to this user.
     */
    private final Set<ObjectPermission<String>> connectionGroupPermissions =
            new HashSet<ObjectPermission<String>>();

    /**
     * Creates a completely uninitialized SimpleUser.
     */
    public SimpleUser() {
    }

    /**
     * Creates a new SimpleUser having the given username.
     *
     * @param username The username to assign to this SimpleUser.
     * @param configs All configurations this user has read access to.
     * @param groups All groups this user has read access to.
     */
    public SimpleUser(String username,
            Map<String, GuacamoleConfiguration> configs,
            Collection<ConnectionGroup> groups) {

        // Set username
        setUsername(username);

        // Add connection permissions
        for (String identifier : configs.keySet()) {

            // Create permission
            ObjectPermission permission = new ObjectPermission(
                ObjectPermission.Type.READ,
                identifier
            );

            // Add to set
            connectionPermissions.add(permission);

        }

        // Add group permissions
        for (ConnectionGroup group : groups) {

            // Create permission
            ObjectPermission permission = new ObjectPermission(
                ObjectPermission.Type.READ,
                group.getIdentifier()
            );

            // Add to set
            connectionGroupPermissions.add(permission);

        }

    }

    @Override
    public SystemPermissionSet getSystemPermissions()
            throws GuacamoleException {
        return new SimpleSystemPermissionSet();
    }

    @Override
    public ObjectPermissionSet<String, Connection> getConnectionPermissions()
            throws GuacamoleException {
        return new SimpleObjectPermissionSet<String, Connection>(connectionPermissions);
    }

    @Override
    public ObjectPermissionSet<String, ConnectionGroup> getConnectionGroupPermissions()
            throws GuacamoleException {
        return new SimpleObjectPermissionSet<String, ConnectionGroup>(connectionGroupPermissions);
    }

    @Override
    public ObjectPermissionSet<String, User> getUserPermissions()
            throws GuacamoleException {
        return new SimpleObjectPermissionSet<String, User>();
    }

}
