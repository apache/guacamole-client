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

import java.util.Collection;
import net.sourceforge.guacamole.net.auth.mysql.model.UserModel;
import org.apache.ibatis.annotations.Param;

/**
 * Generic base for mappers which handle permissions.
 *
 * @author Michael Jumper
 * @param <PermissionType>
 *     The type of permission model object handled by this mapper.
 */
public interface PermissionMapper<PermissionType> {

    /**
     * Retrieves all permissions associated with the given user.
     *
     * @param user
     *     The user to retrieve permissions for.
     *
     * @return
     *     All permissions associated with the given user.
     */
    Collection<PermissionType> select(@Param("user") UserModel user);

    /**
     * Inserts the given permissions into the database. If any permissions
     * already exist, they will be ignored.
     *
     * @param permissions 
     *     The permissions to insert.
     *
     * @return
     *     The number of rows inserted.
     */
    int insert(@Param("permissions") Collection<PermissionType> permissions);

    /**
     * Deletes the given permissions from the database. If any permissions do
     * not exist, they will be ignored.
     *
     * @param permissions
     *     The permissions to delete.
     *
     * @return
     *     The number of rows deleted.
     */
    int delete(@Param("permissions") Collection<PermissionType> permissions);

}