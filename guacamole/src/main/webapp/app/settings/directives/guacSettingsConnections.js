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
 * A directive for managing all connections and connection groups in the system.
 */
angular.module('settings').directive('guacSettingsConnections', [function guacSettingsConnections() {
    
    return {
        // Element only
        restrict: 'E',
        replace: true,

        scope: {
        },

        templateUrl: 'app/settings/templates/settingsConnections.html',
        controller: ['$scope', '$injector', function settingsConnectionsController($scope, $injector) {

            // Required types
            var ConnectionGroup = $injector.get('ConnectionGroup');
            var PermissionSet   = $injector.get('PermissionSet');

            // Required services
            var $location              = $injector.get('$location');
            var authenticationService  = $injector.get('authenticationService');
            var connectionGroupService = $injector.get('connectionGroupService');
            var guacNotification       = $injector.get('guacNotification');
            var permissionService      = $injector.get('permissionService');

            // Identifier of the current user
            var currentUsername = authenticationService.getCurrentUsername();

            /**
             * An action to be provided along with the object sent to
             * showStatus which closes the currently-shown status dialog.
             */
            var ACKNOWLEDGE_ACTION = {
                name        : "SETTINGS_CONNECTIONS.ACTION_ACKNOWLEDGE",
                // Handle action
                callback    : function acknowledgeCallback() {
                    guacNotification.showStatus(false);
                }
            };

            /**
             * The root connection group of the connection group hierarchy.
             *
             * @type ConnectionGroup
             */
            $scope.rootGroup = null;

            /**
             * Whether the current user can manage connections. If the current
             * permissions have not yet been loaded, this will be null.
             *
             * @type Boolean
             */
            $scope.canManageConnections = null;

            /**
             * Whether the current user can create new connections. If the
             * current permissions have not yet been loaded, this will be null.
             *
             * @type Boolean
             */
            $scope.canCreateConnections = null;

            /**
             * Whether the current user can create new connection groups. If
             * the current permissions have not yet been loaded, this will be
             * null.
             *
             * @type Boolean
             */
            $scope.canCreateConnectionGroups = null;

            /**
             * All permissions associated with the current user, or null if the
             * user's permissions have not yet been loaded.
             *
             * @type PermissionSet
             */
            $scope.permissions = null;

            /**
             * Returns whether critical data has completed being loaded.
             *
             * @returns {Boolean}
             *     true if enough data has been loaded for the user interface
             *     to be useful, false otherwise.
             */
            $scope.isLoaded = function isLoaded() {

                return $scope.rootGroup                 !== null
                    && $scope.permissions               !== null
                    && $scope.canManageConnections      !== null
                    && $scope.canCreateConnections      !== null
                    && $scope.canCreateConnectionGroups !== null;

            };

            // Retrieve current permissions
            permissionService.getPermissions(currentUsername)
            .success(function permissionsRetrieved(permissions) {

                $scope.permissions = permissions;
                                
                // Ignore permission to update root group
                PermissionSet.removeConnectionGroupPermission(permissions, PermissionSet.ObjectPermissionType.UPDATE, ConnectionGroup.ROOT_IDENTIFIER);

                // Determine whether the current user can create new users
                $scope.canCreateConnections =
                       PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.ADMINISTER)
                    || PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.CREATE_CONNECTION);

                // Determine whether the current user can create new users
                $scope.canCreateConnectionGroups =
                       PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.ADMINISTER)
                    || PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.CREATE_CONNECTION_GROUP);

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
                if (!$scope.canManageConnections)
                    $location.path('/');
                
            });
            
            // Retrieve all connections for which we have UPDATE or DELETE permission
            connectionGroupService.getConnectionGroupTree(ConnectionGroup.ROOT_IDENTIFIER, 
                [PermissionSet.ObjectPermissionType.UPDATE, PermissionSet.ObjectPermissionType.DELETE])
            .success(function connectionGroupReceived(rootGroup) {
                $scope.rootGroup = rootGroup;
            });
            
        }]
    };
    
}]);
