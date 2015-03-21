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

package org.glyptodon.guacamole.auth.jdbc.permission;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.glyptodon.guacamole.auth.jdbc.user.AuthenticatedUser;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.auth.jdbc.user.ModeledUser;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * deleting user permissions. This service will automatically enforce the
 * permissions of the current user.
 *
 * @author Michael Jumper
 */
public class UserPermissionService extends ModeledObjectPermissionService {

    /**
     * Mapper for user permissions.
     */
    @Inject
    private UserPermissionMapper userPermissionMapper;
    
    /**
     * Provider for user permission sets.
     */
    @Inject
    private Provider<UserPermissionSet> userPermissionSetProvider;
    
    @Override
    protected ObjectPermissionMapper getPermissionMapper() {
        return userPermissionMapper;
    }

    @Override
    public ObjectPermissionSet getPermissionSet(AuthenticatedUser user,
            ModeledUser targetUser) throws GuacamoleException {

        // Create permission set for requested user
        ObjectPermissionSet permissionSet = userPermissionSetProvider.get();
        permissionSet.init(user, targetUser);

        return permissionSet;
        
    }

}
