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

package net.sourceforge.guacamole.net.auth.mysql.dao;

import net.sourceforge.guacamole.net.auth.mysql.model.SystemPermissionModel;
import net.sourceforge.guacamole.net.auth.mysql.model.UserModel;
import org.apache.ibatis.annotations.Param;
import org.glyptodon.guacamole.net.auth.permission.SystemPermission;

/**
 * Mapper for system-level permissions.
 *
 * @author Michael Jumper
 */
public interface SystemPermissionMapper extends PermissionMapper<SystemPermissionModel> {

    /**
     * Retrieve the permission of the given type associated with the given
     * user, if it exists. If no such permission exists, null is returned.
     *
     * @param user
     *     The user to retrieve permissions for.
     * 
     * @param type
     *     The type of permission to return.
     *
     * @return
     *     The requested permission, or null if no such permission is granted
     *     to the given user.
     */
    SystemPermissionModel selectOne(@Param("user") UserModel user,
            @Param("type") SystemPermission.Type type);

}