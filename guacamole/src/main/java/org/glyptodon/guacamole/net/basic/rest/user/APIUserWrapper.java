package org.glyptodon.guacamole.net.basic.rest.user;

/*
 *  Guacamole - Clientless Remote Desktop
 *  Copyright (C) 2010  Michael Jumper
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
