package org.glyptodon.guacamole.net.basic.rest.permission;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.permission.Permission;

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

/**
 * A service for performing useful manipulations on REST Permissions.
 * 
 * @author James Muehlner
 */
public class PermissionService {
    
    /**
     * Converts a list of Permission to a list of APIPermission objects for 
     * exposing with the REST endpoints.
     * 
     * @param permissions The Connections to convert for REST endpoint use.
     * @return A List of APIPermission objects for use with the REST endpoint.
     */
    public List<APIPermission> convertPermissionList(Iterable<? extends Permission> permissions) {
        List<APIPermission> restPermissions = new ArrayList<APIPermission>();
        
        for(Permission permission : permissions) {
            restPermissions.add(new APIPermission(permission));
        }
            
        return restPermissions;
    }
    
    /**
     * Converts a list of APIPermission to a set of Permission objects for internal
     * Guacamole use.
     * 
     * @param restPermissions The APIPermission objects from the REST endpoints.
     * @return a List of Permission objects for internal Guacamole use.
     */
    public Set<Permission> convertAPIPermissionList(Iterable<APIPermission> restPermissions) {
        Set<Permission> permissions = new HashSet<Permission>();
        
        for(APIPermission restPermission : restPermissions) {
            permissions.add(restPermission.toPermission());
        }
        
        return permissions;
    }
}
