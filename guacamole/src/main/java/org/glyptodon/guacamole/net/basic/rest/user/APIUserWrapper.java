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

package org.glyptodon.guacamole.net.basic.rest.user;

import java.util.Collections;
import java.util.Set;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.User;
import org.glyptodon.guacamole.net.auth.permission.Permission;

/**
 * A wrapper to make an APIConnection look like a User. Useful where a
 * org.glyptodon.guacamole.net.auth.User is required.
 * 
 * @author James Muehlner
 */
public class APIUserWrapper implements User {
    
    /**
     * The wrapped APIUser.
     */
    private APIUser apiUser;
    
    /**
     * The set of permissions for this user. 
     * NOTE: Not exposed by the REST endpoints.
     */
    private Set<Permission> permissionSet = Collections.EMPTY_SET;
    
    /**
     * Wrap a given APIUser to expose as a User.
     * @param apiUser The APIUser to wrap.
     */
    public APIUserWrapper(APIUser apiUser) {
        this.apiUser = apiUser;
    }
    
    /**
     * Wrap a given APIUser to expose as a User, with the given permission set.
     * @param apiUser The APIUser to wrap.
     * @param permissionSet The set of permissions for the wrapped user.
     */
    public APIUserWrapper(APIUser apiUser, Set<Permission> permissionSet) {
        this.apiUser = apiUser;
        this.permissionSet = permissionSet;
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
    public Set<Permission> getPermissions() throws GuacamoleException {
        return permissionSet;
    }

    @Override
    public boolean hasPermission(Permission permission) throws GuacamoleException {
        return permissionSet.contains(permission);
    }

    @Override
    public void addPermission(Permission permission) throws GuacamoleException {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    @Override
    public void removePermission(Permission permission) throws GuacamoleException {
        throw new UnsupportedOperationException("Operation not supported.");
    }
}
