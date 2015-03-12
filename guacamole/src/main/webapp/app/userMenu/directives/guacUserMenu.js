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
 * A directive which provides a user-oriented menu containing options for
 * navigation and configuration.
 */
angular.module('userMenu').directive('guacUserMenu', [function guacUserMenu() {

    return {
        restrict: 'E',
        replace: true,
        scope: {

            /**
             * The permissions associated with the user for whom this menu is
             * being displayed.
             *
             * @type PermissionSet
             */
            permissions : '='

        },

        templateUrl: 'app/userMenu/templates/guacUserMenu.html',
        controller: ['$scope', '$injector', function guacUserMenuController($scope, $injector) {

            // Get required types
            var ConnectionGroup = $injector.get("ConnectionGroup");
            var PermissionSet   = $injector.get("PermissionSet");
            
            // Get required services
            var $location             = $injector.get("$location");
            var authenticationService = $injector.get("authenticationService");
            var userService           = $injector.get("userService");

            /**
             * An action to be provided along with the object sent to
             * showStatus which closes the currently-shown status dialog.
             */
            var ACKNOWLEDGE_ACTION = {
                name        : "HOME.ACTION_ACKNOWLEDGE",
                // Handle action
                callback    : function acknowledgeCallback() {
                    $scope.showStatus(false);
                }
            };
            
            /**
             * Whether the current user has sufficient permissions to use the
             * management interface. If permissions have not yet been loaded,
             * this will be null.
             *
             * @type Boolean
             */
            $scope.canManageGuacamole = null;

            /**
             * Whether the current user has sufficient permissions to change
             * his/her own password. If permissions have not yet been loaded,
             * this will be null.
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
            $scope.newPassword = null;

            /**
             * The password match for the user. The update password action will
             * fail if $scope.newPassword !== $scope.passwordMatch.
             *
             * @type String
             */
            $scope.newPasswordMatch = null;

            /**
             * Whether the contents of the user menu are currently shown.
             *
             * @type Boolean
             */
            $scope.menuShown = false;

            /**
             * The username of the current user.
             *
             * @type String
             */
            $scope.username = authenticationService.getCurrentUserID();

            // Update available menu options when permissions are changed
            $scope.$watch('permissions', function permissionsChanged(permissions) {

                // Permissions are unknown if no permissions are provided
                if (!permissions) {
                    $scope.canChangePassword = null;
                    $scope.canManageGuacamole = null;
                    return;
                }

                // Determine whether the current user can change his/her own password
                $scope.canChangePassword = 
                        PermissionSet.hasUserPermission(permissions, PermissionSet.ObjectPermissionType.UPDATE, $scope.username)
                     && PermissionSet.hasUserPermission(permissions, PermissionSet.ObjectPermissionType.READ,   $scope.username);

                // Ignore permission to update root group
                PermissionSet.removeConnectionGroupPermission(permissions, PermissionSet.ObjectPermissionType.UPDATE, ConnectionGroup.ROOT_IDENTIFIER);
                
                // Ignore permission to update self
                PermissionSet.removeUserPermission(permissions, PermissionSet.ObjectPermissionType.UPDATE, $scope.username);

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
             * Toggles visibility of the user menu.
             */
            $scope.toggleMenu = function toggleMenu() {
                $scope.menuShown = !$scope.menuShown;
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
                $scope.oldPassword        = null;
                $scope.newPassword        = null;
                $scope.newPasswordMatch   = null;
                $scope.showPasswordDialog = false;
            };
            
            /**
             * Update the current user's password to the password currently set within
             * the password change dialog.
             */
            $scope.updatePassword = function updatePassword() {

                // Verify passwords match
                if ($scope.newPasswordMatch !== $scope.newPassword) {
                    $scope.showStatus({
                        className  : 'error',
                        title      : 'HOME.DIALOG_HEADER_ERROR',
                        text       : 'HOME.ERROR_PASSWORD_MISMATCH',
                        actions    : [ ACKNOWLEDGE_ACTION ]
                    });
                    return;
                }
                
                // Verify that the new password is not blank
                if (!$scope.newPassword) {
                    $scope.showStatus({
                        className  : 'error',
                        title      : 'HOME.DIALOG_HEADER_ERROR',
                        text       : 'HOME.ERROR_PASSWORD_BLANK',
                        actions    : [ ACKNOWLEDGE_ACTION ]
                    });
                    return;
                }
                
                // Save the user with the new password
                userService.updateUserPassword($scope.username, $scope.oldPassword, $scope.newPassword)
                .success(function passwordUpdated() {
                
                    // Close the password update dialog
                    $scope.closePasswordUpdate();

                    // Indicate that the password has been changed
                    $scope.showStatus({
                        text    : 'HOME.PASSWORD_CHANGED',
                        actions : [ ACKNOWLEDGE_ACTION ]
                    });
                })
                
                // Notify of any errors
                .error(function passwordUpdateFailed(error) {
                    $scope.showStatus({
                        className  : 'error',
                        title      : 'HOME.DIALOG_HEADER_ERROR',
                        'text'       : error.message,
                        actions    : [ ACKNOWLEDGE_ACTION ]
                    });
                });
                
            };

            /**
             * Logs out the current user, redirecting them to back to the login
             * screen after logout completes.
             */
            $scope.logout = function logout() {
                authenticationService.logout()['finally'](function logoutComplete() {
                    $location.path('/login');
                });
            };

        }] // end controller

    };
}]);
