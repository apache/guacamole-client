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

package net.sourceforge.guacamole.net.auth.mysql.service;

import com.google.inject.Inject;
import java.util.Collection;
import net.sourceforge.guacamole.net.auth.mysql.AuthenticatedUser;
import net.sourceforge.guacamole.net.auth.mysql.dao.SystemPermissionMapper;
import net.sourceforge.guacamole.net.auth.mysql.model.SystemPermissionModel;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.permission.SystemPermission;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * deleting system permissions.
 *
 * @author Michael Jumper
 */
public class SystemPermissionService
    extends PermissionService<SystemPermission, SystemPermissionModel> {

    /**
     * Mapper for system-level permissions.
     */
    @Inject
    private SystemPermissionMapper systemPermissionMapper;
    
    @Override
    protected SystemPermissionMapper getPermissionMapper() {
        return systemPermissionMapper;
    }

    @Override
    protected SystemPermission getPermissionInstance(SystemPermissionModel model) {
        // TODO: Will need an implementation-specific SystemPermission, but this
        // will suffice for testing
        return new SystemPermission(model.getType());
    }

    @Override
    public void createPermissions(AuthenticatedUser user,
            Collection<SystemPermission> permissions) throws GuacamoleException {
        // TODO: Implement, including perm checks
    }

    @Override
    public void deletePermissions(AuthenticatedUser user,
            Collection<SystemPermission> permissions) throws GuacamoleException {
        // TODO: Implement, including perm checks
    }

}
