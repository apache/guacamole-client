/*
 * Copyright (C) 2014 Glyptodon LLC
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

package org.glyptodon.guacamole.net.basic.rest.user;

import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleUnsupportedException;
import org.glyptodon.guacamole.net.auth.User;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermissionSet;
import org.glyptodon.guacamole.net.auth.permission.SystemPermissionSet;

/**
 * A wrapper to make an APIUser look like a User. Useful where an
 * org.glyptodon.guacamole.net.auth.User is required. As a simple wrapper for
 * APIUser, access to permissions is not provided. Any attempt to access or
 * manipulate permissions on an APIUserWrapper will result in an exception.
 * 
 * @author James Muehlner
 */
public class APIUserWrapper implements User {
    
    /**
     * The wrapped APIUser.
     */
    private final APIUser apiUser;
    
    /**
     * Wrap a given APIUser to expose as a User.
     * @param apiUser The APIUser to wrap.
     */
    public APIUserWrapper(APIUser apiUser) {
        this.apiUser = apiUser;
    }
    
    @Override
    public String getUsername() {
        return apiUser.getUsername();
    }

    @Override
    public void setUsername(String username) {
        apiUser.setUsername(username);
    }

    @Override
    public String getPassword() {
        return apiUser.getPassword();
    }

    @Override
    public void setPassword(String password) {
        apiUser.setPassword(password);
    }

    @Override
    public SystemPermissionSet getSystemPermissions()
            throws GuacamoleException {
        throw new GuacamoleUnsupportedException("APIUserWrapper does not provide permission access.");
    }

    @Override
    public ObjectPermissionSet<String> getConnectionPermissions()
            throws GuacamoleException {
        throw new GuacamoleUnsupportedException("APIUserWrapper does not provide permission access.");
    }

    @Override
    public ObjectPermissionSet<String> getConnectionGroupPermissions()
            throws GuacamoleException {
        throw new GuacamoleUnsupportedException("APIUserWrapper does not provide permission access.");
    }

    @Override
    public ObjectPermissionSet<String> getUserPermissions()
            throws GuacamoleException {
        throw new GuacamoleUnsupportedException("APIUserWrapper does not provide permission access.");
    }

}
