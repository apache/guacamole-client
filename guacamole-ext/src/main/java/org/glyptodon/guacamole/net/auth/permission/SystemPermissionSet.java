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

package org.glyptodon.guacamole.net.auth.permission;

import org.glyptodon.guacamole.GuacamoleException;


/**
 * A set of permissions which affects the system as a whole.
 *
 * @author Michael Jumper
 */
public interface SystemPermissionSet {

    /**
     * Tests whether the permission of the given type is granted.
     *
     * @param permission
     *     The permission to check.
     *
     * @return
     *     true if the permission is granted, false otherwise.
     *
     * @throws GuacamoleException
     *     If an error occurs while checking permissions, or if permissions
     *     cannot be checked due to lack of permissions to do so.
     */
    boolean hasPermission(SystemPermission.Type permission)
            throws GuacamoleException;

    /**
     * Adds the specified permission.
     *
     * @param permission
     *     The permission to add.
     *
     * @throws GuacamoleException
     *     If an error occurs while adding the permission, or if permission to
     *     add permissions is denied.
     */
    void addPermission(SystemPermission.Type permission)
            throws GuacamoleException;

    /**
     * Removes the specified permission.
     *
     * @param permission
     *     The permission to remove.
     *
     * @throws GuacamoleException
     *     If an error occurs while removing the permission, or if permission
     *     to remove permissions is denied.
     */
    void removePermission(SystemPermission.Type permission)
            throws GuacamoleException;

}
