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

package org.glyptodon.guacamole.net.basic.rest.permission;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.glyptodon.guacamole.net.auth.permission.Permission;

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
