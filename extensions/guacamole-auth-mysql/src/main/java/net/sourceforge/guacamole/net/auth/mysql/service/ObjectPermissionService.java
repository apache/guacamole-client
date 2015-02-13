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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import net.sourceforge.guacamole.net.auth.mysql.AuthenticatedUser;
import net.sourceforge.guacamole.net.auth.mysql.MySQLUser;
import net.sourceforge.guacamole.net.auth.mysql.dao.PermissionMapper;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermission;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermissionSet;
import org.glyptodon.guacamole.net.auth.permission.Permission;
import org.glyptodon.guacamole.net.auth.permission.PermissionSet;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * deleting object permissions.
 *
 * @author Michael Jumper
 * @param <ObjectPermissionType>
 *     The type of object permission this service provides access to.
 *
 * @param <ModelType>
 *     The underlying model object used to represent PermissionType in the
 *     database.
 */
public abstract class ObjectPermissionService<ObjectPermissionType extends ObjectPermission, ModelType>
    extends PermissionService<ObjectPermissionType, ModelType> {

    /**
     * Returns the permission set associated with the given user and related
     * to the type of objects affected the permissions handled by this
     * permission service.
     *
     * @param user
     *     The user whose permissions are being retrieved.
     *
     * @return
     *     A permission set which contains the permissions associated with the
     *     given user and related to the type of objects handled by this
     *     directory object service.
     * 
     * @throws GuacamoleException
     *     If permission to read the user's permissions is denied.
     */
    protected abstract PermissionSet<ObjectPermissionType> getAffectedPermissionSet(AuthenticatedUser user)
            throws GuacamoleException;

    /* TODO: Override create/delete testing permissions for affected objects */

}
