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
import com.google.inject.Provider;
import java.util.Collection;
import net.sourceforge.guacamole.net.auth.mysql.AuthenticatedUser;
import net.sourceforge.guacamole.net.auth.mysql.MySQLSystemPermissionSet;
import net.sourceforge.guacamole.net.auth.mysql.MySQLUser;
import net.sourceforge.guacamole.net.auth.mysql.dao.SystemPermissionMapper;
import net.sourceforge.guacamole.net.auth.mysql.model.SystemPermissionModel;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.net.auth.permission.SystemPermission;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * deleting system permissions. This service will automatically enforce
 * the permissions of the current user.
 *
 * @author Michael Jumper
 */
public class SystemPermissionService
    extends PermissionService<MySQLSystemPermissionSet, SystemPermission, SystemPermissionModel> {

    /**
     * Mapper for system-level permissions.
     */
    @Inject
    private SystemPermissionMapper systemPermissionMapper;

    /**
     * Provider for creating system permission sets.
     */
    @Inject
    private Provider<MySQLSystemPermissionSet> systemPermissionSetProvider;

    @Override
    protected SystemPermissionMapper getPermissionMapper() {
        return systemPermissionMapper;
    }
    
    @Override
    protected SystemPermission getPermissionInstance(SystemPermissionModel model) {
        return new SystemPermission(model.getType());
    }

    @Override
    protected SystemPermissionModel getModelInstance(final MySQLUser targetUser,
            final SystemPermission permission) {

        // Populate and return model object
        return new SystemPermissionModel() {

            /**
             * The ID of the user to whom this permission is granted.
             */
            private Integer userID = targetUser.getModel().getUserID();

            /**
             * The username of the user to whom this permission is granted.
             */
            private String username = targetUser.getModel().getUsername();

            /**
             * The type of action granted by this permission.
             */
            private SystemPermission.Type type = permission.getType();
            
            @Override
            public Integer getUserID() {
                return userID;
            }

            @Override
            public void setUserID(Integer userID) {
                this.userID = userID;
            }

            @Override
            public String getUsername() {
                return username;
            }

            @Override
            public void setUsername(String username) {
                this.username = username;
            }

            @Override
            public SystemPermission.Type getType() {
                return type;
            }

            @Override
            public void setType(SystemPermission.Type type) {
                this.type = type;
            }

        };
        
    }

    @Override
    public MySQLSystemPermissionSet getPermissionSet(AuthenticatedUser user,
            MySQLUser targetUser) throws GuacamoleException {

        // Create permission set for requested user
        MySQLSystemPermissionSet permissionSet = systemPermissionSetProvider.get();
        permissionSet.init(user, targetUser);

        return permissionSet;
        
    }
    
    @Override
    public void createPermissions(AuthenticatedUser user, MySQLUser targetUser,
            Collection<SystemPermission> permissions) throws GuacamoleException {

        // Only an admin can create system permissions
        if (user.getUser().isAdministrator()) {
            Collection<SystemPermissionModel> models = getModelInstances(targetUser, permissions);
            systemPermissionMapper.insert(models);
            return;
        }

        // User lacks permission to create system permissions
        throw new GuacamoleSecurityException("Permission denied.");
        
    }

    @Override
    public void deletePermissions(AuthenticatedUser user, MySQLUser targetUser,
            Collection<SystemPermission> permissions) throws GuacamoleException {

        // Only an admin can delete system permissions
        if (user.getUser().isAdministrator()) {
            Collection<SystemPermissionModel> models = getModelInstances(targetUser, permissions);
            systemPermissionMapper.delete(models);
            return;
        }

        // User lacks permission to delete system permissions
        throw new GuacamoleSecurityException("Permission denied.");
        
    }

    /**
     * Retrieves the permission of the given type associated with the given
     * user, if it exists. If no such permission exists, null is returned.
     *
     * @param user
     *     The user retrieving the permission.
     *
     * @param targetUser
     *     The user associated with the permission to be retrieved.
     * 
     * @param type
     *     The type of permission to retrieve.
     *
     * @return
     *     The permission of the given type associated with the given user, or
     *     null if no such permission exists.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the requested permission.
     */
    public SystemPermission retrievePermission(AuthenticatedUser user,
            MySQLUser targetUser, SystemPermission.Type type) throws GuacamoleException {

        // Only an admin can read permissions that aren't his own
        if (user.getUser().getIdentifier().equals(targetUser.getIdentifier())
                || user.getUser().isAdministrator())
            return getPermissionInstance(getPermissionMapper().selectOne(targetUser.getModel(), type));

        // User cannot read this user's permissions
        throw new GuacamoleSecurityException("Permission denied.");
        
    }

}
