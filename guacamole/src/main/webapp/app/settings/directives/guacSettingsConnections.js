/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
            var $routeParams           = $injector.get('$routeParams');
            var authenticationService  = $injector.get('authenticationService');
            var connectionGroupService = $injector.get('connectionGroupService');
            var dataSourceService      = $injector.get('dataSourceService');
            var guacNotification       = $injector.get('guacNotification');
            var permissionService      = $injector.get('permissionService');

            /**
             * The identifier of the current user.
             *
             * @type String
             */
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
             * The identifier of the currently-selected data source.
             *
             * @type String
             */
            $scope.dataSource = $routeParams.dataSource;

            /**
             * The root connection group of the connection group hierarchy.
             *
             * @type Object.<String, ConnectionGroup>
             */
            $scope.rootGroups = null;

            /**
             * All permissions associated with the current user, or null if the
             * user's permissions have not yet been loaded.
             *
             * @type PermissionSet
             */
            $scope.permissions = null;

            /**
             * Array of all connection properties that are filterable.
             *
             * @type String[]
             */
            $scope.filteredConnectionProperties = [
                'name',
                'protocol'
            ];

            /**
             * Array of all connection group properties that are filterable.
             *
             * @type String[]
             */
            $scope.filteredConnectionGroupProperties = [
                'name'
            ];

            /**
             * Returns whether critical data has completed being loaded.
             *
             * @returns {Boolean}
             *     true if enough data has been loaded for the user interface
             *     to be useful, false otherwise.
             */
            $scope.isLoaded = function isLoaded() {

                return $scope.rootGroup   !== null
                    && $scope.permissions !== null;

            };

            /**
             * Returns whether the current user can create new connections
             * within the current data source.
             *
             * @return {Boolean}
             *     true if the current user can create new connections within
             *     the current data source, false otherwise.
             */
            $scope.canCreateConnections = function canCreateConnections() {

                // Abort if permissions have not yet loaded
                if (!$scope.permissions)
                    return false;

                // Can create connections if adminstrator or have explicit permission
                if (PermissionSet.hasSystemPermission($scope.permissions, PermissionSet.SystemPermissionType.ADMINISTER)
                 || PermissionSet.hasSystemPermission($scope.permissions, PermissionSet.SystemPermissionType.CREATE_CONNECTION))
                     return true;

                // No data sources allow connection creation
                return false;

            };

            /**
             * Returns whether the current user can create new connection
             * groups within the current data source.
             *
             * @return {Boolean}
             *     true if the current user can create new connection groups
             *     within the current data source, false otherwise.
             */
            $scope.canCreateConnectionGroups = function canCreateConnectionGroups() {

                // Abort if permissions have not yet loaded
                if (!$scope.permissions)
                    return false;

                // Can create connections groups if adminstrator or have explicit permission
                if (PermissionSet.hasSystemPermission($scope.permissions, PermissionSet.SystemPermissionType.ADMINISTER)
                 || PermissionSet.hasSystemPermission($scope.permissions, PermissionSet.SystemPermissionType.CREATE_CONNECTION_GROUP))
                     return true;

                // No data sources allow connection group creation
                return false;

            };

            /**
             * Returns whether the current user can create new connections or
             * connection groups or make changes to existing connections or
             * connection groups within the current data source. The
             * connection management interface as a whole is useless if this
             * function returns false.
             *
             * @return {Boolean}
             *     true if the current user can create new connections/groups
             *     or make changes to existing connections/groups within the
             *     current data source, false otherwise.
             */
            $scope.canManageConnections = function canManageConnections() {

                // Abort if permissions have not yet loaded
                if (!$scope.permissions)
                    return false;

                // Creating connections/groups counts as management
                if ($scope.canCreateConnections() || $scope.canCreateConnectionGroups())
                    return true;

                // Can manage connections if granted explicit update or delete
                if (PermissionSet.hasConnectionPermission($scope.permissions, PermissionSet.ObjectPermissionType.UPDATE)
                 || PermissionSet.hasConnectionPermission($scope.permissions, PermissionSet.ObjectPermissionType.DELETE))
                    return true;

                // Can manage connections groups if granted explicit update or delete
                if (PermissionSet.hasConnectionGroupPermission($scope.permissions, PermissionSet.ObjectPermissionType.UPDATE)
                 || PermissionSet.hasConnectionGroupPermission($scope.permissions, PermissionSet.ObjectPermissionType.DELETE))
                    return true;

                // No data sources allow management of connections or groups
                return false;

            };

            // Retrieve current permissions
            permissionService.getPermissions($scope.dataSource, currentUsername)
            .success(function permissionsRetrieved(permissions) {

                // Store retrieved permissions
                $scope.permissions = permissions;

                // Ignore permission to update root group
                PermissionSet.removeConnectionGroupPermission($scope.permissions, PermissionSet.ObjectPermissionType.UPDATE, ConnectionGroup.ROOT_IDENTIFIER);

                // Return to home if there's nothing to do here
                if (!$scope.canManageConnections())
                    $location.path('/');

            });
            
            // Retrieve all connections for which we have UPDATE or DELETE permission
            dataSourceService.apply(
                connectionGroupService.getConnectionGroupTree,
                [$scope.dataSource],
                ConnectionGroup.ROOT_IDENTIFIER,
                [PermissionSet.ObjectPermissionType.UPDATE, PermissionSet.ObjectPermissionType.DELETE]
            )
            .then(function connectionGroupsReceived(rootGroups) {
                $scope.rootGroups = rootGroups;
            });
            
        }]
    };
    
}]);
