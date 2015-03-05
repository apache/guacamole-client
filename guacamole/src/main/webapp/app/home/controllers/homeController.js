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

/**
 * The controller for the home page.
 */
angular.module('home').controller('homeController', ['$scope', '$injector', 
        function homeController($scope, $injector) {

    // Get required types
    var ConnectionGroup = $injector.get("ConnectionGroup");
    var PermissionSet   = $injector.get("PermissionSet");
            
    // Get required services
    var authenticationService  = $injector.get("authenticationService");
    var connectionGroupService = $injector.get("connectionGroupService");
    var permissionService      = $injector.get("permissionService");
    var userService            = $injector.get("userService");
    
    /**
     * The root connection group, or null if the connection group hierarchy has
     * not yet been loaded.
     *
     * @type ConnectionGroup
     */
    $scope.rootConnectionGroup = null;

    /**
     * Whether the current user has sufficient permissions to use the
     * management interface. If permissions have not yet been loaded, this will
     * be null.
     *
     * @type Boolean
     */
    $scope.canManageGuacamole = null;

    /**
     * Whether the current user has sufficient permissions to change
     * his/her own password. If permissions have not yet been loaded, this will
     * be null.
     *
     * @type Boolean
     */
    $scope.canChangePassword = null;

    /**
     * Whether the password edit dialog should be shown.
     *
     * @type Boolean
     */
    $scope.showPasswordDialog = false;

    /**
     * The new password for the user.
     *
     * @type String
     */
    $scope.password = null;

    /**
     * The password match for the user. The update password action will fail if
     * $scope.password !== $scope.passwordMatch.
     *
     * @type String
     */
    $scope.passwordMatch = null;

    /**
     * Returns whether critical data has completed being loaded.
     *
     * @returns {Boolean}
     *     true if enough data has been loaded for the user interface to be
     *     useful, false otherwise.
     */
    $scope.isLoaded = function isLoaded() {

        return $scope.rootConnectionGroup !== null
            && $scope.canManageGuacamole  !== null
            && $scope.canChangePassword   !== null;

    };

    // Retrieve root group and all descendants
    connectionGroupService.getConnectionGroupTree(ConnectionGroup.ROOT_IDENTIFIER)
    .success(function rootGroupRetrieved(rootConnectionGroup) {
        $scope.rootConnectionGroup = rootConnectionGroup;
    });
    
    // Identifier of the current user
    var currentUserID = authenticationService.getCurrentUserID();

    // Retrieve current permissions
    permissionService.getPermissions(currentUserID)
    .success(function permissionsRetrieved(permissions) {
        
        // Determine whether the current user can change his/her own password
        $scope.canChangePassword = 
                PermissionSet.hasUserPermission(permissions, PermissionSet.ObjectPermissionType.UPDATE, currentUserID)
             && PermissionSet.hasUserPermission(permissions, PermissionSet.ObjectPermissionType.READ,   currentUserID);

        // Ignore permission to update root group
        PermissionSet.removeConnectionGroupPermission(permissions, PermissionSet.ObjectPermissionType.UPDATE, ConnectionGroup.ROOT_IDENTIFIER);
        
        // Ignore permission to update self
        PermissionSet.removeUserPermission(permissions, PermissionSet.ObjectPermissionType.UPDATE, currentUserID);

        // Determine whether the current user needs access to the management UI
        $scope.canManageGuacamole =

                // System permissions
                   PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.ADMINISTER)
                || PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.CREATE_USER)
                || PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.CREATE_CONNECTION)
                || PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.CREATE_CONNECTION_GROUP)

                // Permission to update objects
                || PermissionSet.hasConnectionPermission(permissions,      PermissionSet.ObjectPermissionType.UPDATE)
                || PermissionSet.hasConnectionGroupPermission(permissions, PermissionSet.ObjectPermissionType.UPDATE)
                || PermissionSet.hasUserPermission(permissions,            PermissionSet.ObjectPermissionType.UPDATE)

                // Permission to delete objects
                || PermissionSet.hasConnectionPermission(permissions,      PermissionSet.ObjectPermissionType.DELETE)
                || PermissionSet.hasConnectionGroupPermission(permissions, PermissionSet.ObjectPermissionType.DELETE)
                || PermissionSet.hasUserPermission(permissions,            PermissionSet.ObjectPermissionType.DELETE)

                // Permission to administer objects
                || PermissionSet.hasConnectionPermission(permissions,      PermissionSet.ObjectPermissionType.ADMINISTER)
                || PermissionSet.hasConnectionGroupPermission(permissions, PermissionSet.ObjectPermissionType.ADMINISTER)
                || PermissionSet.hasUserPermission(permissions,            PermissionSet.ObjectPermissionType.ADMINISTER);
        
    });
    
    /**
     * An action to be provided along with the object sent to showStatus which
     * closes the currently-shown status dialog.
     */
    var ACKNOWLEDGE_ACTION = {
        name        : "MANAGE_USER.ACTION_ACKNOWLEDGE",
        // Handle action
        callback    : function acknowledgeCallback() {
            $scope.showStatus(false);
        }
    };
    
    /**
     * Show the password update dialog.
     */
    $scope.showPasswordUpdate = function showPasswordUpdate() {
        
        // Show the dialog
        $scope.showPasswordDialog = true;
    };
    
    /**
     * Close the password update dialog.
     */
    $scope.closePasswordUpdate = function closePasswordUpdate() {
        
        // Clear the password fields and close the dialog
        $scope.password = null;
        $scope.passwordMatch = null;
        $scope.showPasswordDialog = false;
    };
    
    /**
     * Update the current user's password to the password currently set within
     * the password change dialog.
     */
    $scope.updatePassword = function updatePassword() {

        // Verify passwords match
        if ($scope.passwordMatch !== $scope.password) {
            $scope.showStatus({
                'className'  : 'error',
                'title'      : 'HOME.DIALOG_HEADER_ERROR',
                'text'       : 'HOME.ERROR_PASSWORD_MISMATCH',
                'actions'    : [ ACKNOWLEDGE_ACTION ]
            });
            return;
        }
        
        // Save the user with the new password
        userService.saveUser({
            username: currentUserID, 
            password: $scope.password
        });
        
        $scope.closePasswordUpdate();
        
        // Indicate that the password has been changed
        $scope.showStatus({
            'text'       : 'HOME.PASSWORD_CHANGED',
            'actions'    : [ ACKNOWLEDGE_ACTION ]
        });
    };
    
}]);
