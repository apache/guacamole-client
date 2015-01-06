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
 * The controller for the administration page.
 */
angular.module('manage').controller('manageController', ['$scope', '$injector', 
        function manageController($scope, $injector) {

    // Required types
    var ConnectionGroup = $injector.get('ConnectionGroup');
    var PermissionSet   = $injector.get('PermissionSet');
    var User            = $injector.get('User');

    // Required services
    var $location              = $injector.get('$location');
    var authenticationService  = $injector.get('authenticationService');
    var connectionGroupService = $injector.get('connectionGroupService');
    var permissionService      = $injector.get('permissionService');
    var userService            = $injector.get('userService');

    /**
     * An action to be provided along with the object sent to showStatus which
     * closes the currently-shown status dialog.
     */
    var ACKNOWLEDGE_ACTION = {
        name        : "MANAGE.ACTION_ACKNOWLEDGE",
        // Handle action
        callback    : function acknowledgeCallback() {
            $scope.showStatus(false);
        }
    };

    /**
     * All visible users.
     *
     * @type User[]
     */
    $scope.users = null;

    /**
     * The root connection group of the connection group hierarchy.
     *
     * @type ConnectionGroup
     */
    $scope.rootGroup = null;

    /**
     * Whether the current user can manage users. If the current permissions
     * have not yet been loaded, this will be null.
     *
     * @type Boolean
     */
    $scope.canManageUsers = null;

    /**
     * Whether the current user can manage connections. If the current
     * permissions have not yet been loaded, this will be null.
     *
     * @type Boolean
     */
    $scope.canManageConnections = null;

    /**
     * Whether the current user can create new users. If the current
     * permissions have not yet been loaded, this will be null.
     *
     * @type Boolean
     */
    $scope.canCreateUsers = null;

    /**
     * Whether the current user can create new connections. If the current
     * permissions have not yet been loaded, this will be null.
     *
     * @type Boolean
     */
    $scope.canCreateConnections = null;

    /**
     * Whether the current user can create new connection groups. If the
     * current permissions have not yet been loaded, this will be null.
     *
     * @type Boolean
     */
    $scope.canCreateConnectionGroups = null;

    /**
     * The name of the new user to create, if any, when user creation is
     * requested via newUser().
     *
     * @type String
     */
    $scope.newUsername = "";

    /**
     * Returns whether critical data has completed being loaded.
     *
     * @returns {Boolean}
     *     true if enough data has been loaded for the user interface to be
     *     useful, false otherwise.
     */
    $scope.isLoaded = function isLoaded() {

        return $scope.users                     !== null
            && $scope.rootGroup                 !== null
            && $scope.canManageUsers            !== null
            && $scope.canManageConnections      !== null
            && $scope.canCreateUsers            !== null
            && $scope.canCreateConnections      !== null
            && $scope.canCreateConnectionGroups !== null;

    };

    // Retrieve current permissions
    permissionService.getPermissions(authenticationService.getCurrentUserID())
    .success(function permissionsRetrieved(permissions) {

        // Ignore permission to update root group
        PermissionSet.removeConnectionGroupPermission(permissions, PermissionSet.ObjectPermissionType.UPDATE, ConnectionGroup.ROOT_IDENTIFIER);

        // Determine whether the current user can create new users
        $scope.canCreateUsers =
               PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.ADMINISTER)
            || PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.CREATE_USER);

        // Determine whether the current user can create new users
        $scope.canCreateConnections =
               PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.ADMINISTER)
            || PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.CREATE_CONNECTION);

        // Determine whether the current user can create new users
        $scope.canCreateConnectionGroups =
               PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.ADMINISTER)
            || PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.CREATE_CONNECTION_GROUP);

        // Determine whether the current user can manage other users
        $scope.canManageUsers =
               $scope.canCreateUsers
            || PermissionSet.hasUserPermission(permissions, PermissionSet.ObjectPermissionType.UPDATE)
            || PermissionSet.hasUserPermission(permissions, PermissionSet.ObjectPermissionType.DELETE);

        // Determine whether the current user can manage other connections or groups
        $scope.canManageConnections =

            // Permission to manage connections
               $scope.canCreateConnections
            || PermissionSet.hasConnectionPermission(permissions, PermissionSet.ObjectPermissionType.UPDATE)
            || PermissionSet.hasConnectionPermission(permissions, PermissionSet.ObjectPermissionType.DELETE)

            // Permission to manage groups
            || $scope.canCreateConnectionGroups
            || PermissionSet.hasConnectionGroupPermission(permissions, PermissionSet.ObjectPermissionType.UPDATE)
            || PermissionSet.hasConnectionGroupPermission(permissions, PermissionSet.ObjectPermissionType.DELETE);

        // Return to home if there's nothing to do here
        if (!$scope.canManageUsers && !$scope.canManageConnections)
            $location.path('/');
        
    });
    
    // Retrieve all connections for which we have UPDATE permission
    connectionGroupService.getConnectionGroupTree(ConnectionGroup.ROOT_IDENTIFIER, PermissionSet.ObjectPermissionType.UPDATE)
    .success(function connectionGroupReceived(rootGroup) {
        $scope.rootGroup = rootGroup;
    });

    // Retrieve all users for whom we have UPDATE permission
    userService.getUsers(PermissionSet.ObjectPermissionType.UPDATE)
    .success(function usersReceived(users) {
        $scope.users = users;
    });

    /**
     * Creates a new user having the username specified in the user creation
     * interface.
     */
    $scope.newUser = function newUser() {

        // Create user skeleton
        var user = new User({
            username: $scope.newUsername || ''
        });

        // Create specified user
        userService.createUser(user)

        // Add user to visible list upon success
        .success(function userCreated() {
            $scope.users.push(user);
        })

        // Notify of any errors
        .error(function userCreationFailed(error) {
            $scope.showStatus({
                'className'  : 'error',
                'title'      : 'MANAGE.DIALOG_HEADER_ERROR',
                'text'       : error.message,
                'actions'    : [ ACKNOWLEDGE_ACTION ]
            });
        });

        // Reset username
        $scope.newUsername = "";

    };
    
}]);
