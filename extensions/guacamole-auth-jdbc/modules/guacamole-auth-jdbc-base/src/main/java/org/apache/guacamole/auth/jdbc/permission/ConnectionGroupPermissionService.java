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

package org.apache.guacamole.auth.jdbc.permission;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.guacamole.auth.jdbc.user.AuthenticatedUser;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.jdbc.user.ModeledUser;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * deleting connection group permissions. This service will automatically
 * enforce the permissions of the current user.
 *
 * @author Michael Jumper
 */
public class ConnectionGroupPermissionService extends ModeledObjectPermissionService {

    /**
     * Mapper for connection group permissions.
     */
    @Inject
    private ConnectionGroupPermissionMapper connectionGroupPermissionMapper;
    
    /**
     * Provider for connection group permission sets.
     */
    @Inject
    private Provider<ConnectionGroupPermissionSet> connectionGroupPermissionSetProvider;
    
    @Override
    protected ObjectPermissionMapper getPermissionMapper() {
        return connectionGroupPermissionMapper;
    }

    @Override
    public ObjectPermissionSet getPermissionSet(AuthenticatedUser user,
            ModeledUser targetUser) throws GuacamoleException {

        // Create permission set for requested user
        ObjectPermissionSet permissionSet = connectionGroupPermissionSetProvider.get();
        permissionSet.init(user, targetUser);

        return permissionSet;
        
    }

}
