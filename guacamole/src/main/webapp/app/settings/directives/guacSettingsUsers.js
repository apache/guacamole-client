/*
 * Copyright (C) 2015 Glyptodon LLC
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
 * A directive for managing all users in the system.
 */
angular.module('settings').directive('guacSettingsUsers', [function guacSettingsUsers() {
    
    return {
        // Element only
        restrict: 'E',
        replace: true,

        scope: {
        },

        templateUrl: 'app/settings/templates/settingsUsers.html',
        controller: ['$scope', '$injector', function settingsUsersController($scope, $injector) {

            // Required types
            var ManageableUser  = $injector.get('ManageableUser');
            var PermissionSet   = $injector.get('PermissionSet');

            // Required services
            var $location              = $injector.get('$location');
            var authenticationService  = $injector.get('authenticationService');
            var dataSourceService      = $injector.get('dataSourceService');
            var guacNotification       = $injector.get('guacNotification');
            var permissionService      = $injector.get('permissionService');
            var userService            = $injector.get('userService');

            // Identifier of the current user
            var currentUsername = authenticationService.getCurrentUsername();

            /**
             * An action to be provided along with the object sent to
             * showStatus which closes the currently-shown status dialog.
             */
            var ACKNOWLEDGE_ACTION = {
                name        : "SETTINGS_USERS.ACTION_ACKNOWLEDGE",
                // Handle action
                callback    : function acknowledgeCallback() {
                    guacNotification.showStatus(false);
                }
            };

            /**
             * The identifiers of all data sources accessible by the current
             * user.
             *
             * @type String[]
             */
            var dataSources = authenticationService.getAvailableDataSources();

            /**
             * All visible users, along with their corresponding data sources.
             *
             * @type ManageableUser[]
             */
            $scope.manageableUsers = null;

            /**
             * The name of the new user to create, if any, when user creation
             * is requested via newUser().
             *
             * @type String
             */
            $scope.newUsername = "";

            /**
             * Map of data source identifiers to all permissions associated
             * with the current user within that data source, or null if the
             * user's permissions have not yet been loaded.
             *
             * @type Object.<String, PermissionSet>
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

                return $scope.manageableUsers !== null
                    && $scope.permissions     !== null;

            };

            /**
             * Returns the identifier of the data source that should be used by
             * default when creating a new user.
             *
             * @return {String}
             *     The identifier of the data source that should be used by
             *     default when creating a new user, or null if user creation
             *     is not allowed.
             */
            var getDefaultDataSource = function getDefaultDataSource() {

                // Abort if permissions have not yet loaded
                if (!$scope.permissions)
                    return null;

                // For each data source
                for (var dataSource in $scope.permissions) {

                    // Retrieve corresponding permission set
                    var permissionSet = $scope.permissions[dataSource];

                    // Can create users if adminstrator or have explicit permission
                    if (PermissionSet.hasSystemPermission(permissionSet, PermissionSet.SystemPermissionType.ADMINISTER)
                     || PermissionSet.hasSystemPermission(permissionSet, PermissionSet.SystemPermissionType.CREATE_USER))
                        return dataSource;

                }

                // No data sources allow user creation
                return null;

            };

            /**
             * Returns whether the current user can create new users within at
             * least one data source.
             *
             * @return {Boolean}
             *     true if the current user can create new users within at
             *     least one data source, false otherwise.
             */
            $scope.canCreateUsers = function canCreateUsers() {
                return getDefaultDataSource() !== null;
            };

            /**
             * Returns whether the current user can create new users or make
             * changes to existing users within at least one data source. The
             * user management interface as a whole is useless if this function
             * returns false.
             *
             * @return {Boolean}
             *     true if the current user can create new users or make
             *     changes to existing users within at least one data source,
             *     false otherwise.
             */
            var canManageUsers = function canManageUsers() {

                // Abort if permissions have not yet loaded
                if (!$scope.permissions)
                    return false;

                // Creating users counts as management
                if ($scope.canCreateUsers())
                    return true;

                // For each data source
                for (var dataSource in $scope.permissions) {

                    // Retrieve corresponding permission set
                    var permissionSet = $scope.permissions[dataSource];

                    // Can manage users if granted explicit update or delete
                    if (PermissionSet.hasUserPermission(permissionSet, PermissionSet.ObjectPermissionType.UPDATE)
                     || PermissionSet.hasUserPermission(permissionSet, PermissionSet.ObjectPermissionType.DELETE))
                        return true;

                }

                // No data sources allow management of users
                return false;

            };

            // Retrieve current permissions
            dataSourceService.apply(permissionService.getPermissions, dataSources, currentUsername)
            .then(function permissionsRetrieved(permissions) {

                // Store retrieved permissions
                $scope.permissions = permissions;

                // Return to home if there's nothing to do here
                if (!canManageUsers())
                    $location.path('/');

            });

            // Retrieve all users for whom we have UPDATE or DELETE permission
            dataSourceService.apply(userService.getUsers, dataSources, [
                PermissionSet.ObjectPermissionType.UPDATE,
                PermissionSet.ObjectPermissionType.DELETE
            ])
            .then(function usersReceived(userArrays) {

                var addedUsers = {};
                $scope.manageableUsers = [];

                // For each user in each data source
                angular.forEach(dataSources, function addUserList(dataSource) {
                    angular.forEach(userArrays[dataSource], function addUser(user) {

                        // Do not add the same user twice
                        if (addedUsers[user.username])
                            return;

                        // Add user to overall list
                        addedUsers[user.username] = user;
                        $scope.manageableUsers.push(new ManageableUser ({
                            'dataSource' : dataSource,
                            'user'       : user
                        }));

                    });
                });

            });

            /**
             * Navigates to an interface for creating a new user having the
             * username specified.
             */
            $scope.newUser = function newUser() {
                $location.url('/manage/' + encodeURIComponent(getDefaultDataSource()) + '/users/' + encodeURIComponent($scope.newUsername));
            };
            
        }]
    };
    
}]);
