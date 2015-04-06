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
angular.module('navigation').directive('guacUserMenu', [function guacUserMenu() {

    return {
        restrict: 'E',
        replace: true,
        scope: {
        },

        templateUrl: 'app/navigation/templates/guacUserMenu.html',
        controller: ['$scope', '$injector', '$element', function guacUserMenuController($scope, $injector, $element) {

            // Get required types
            var ConnectionGroup = $injector.get('ConnectionGroup');
            var PermissionSet   = $injector.get('PermissionSet');
            
            // Get required services
            var $document              = $injector.get('$document');
            var $location              = $injector.get('$location');
            var authenticationService  = $injector.get('authenticationService');
            var connectionGroupService = $injector.get("connectionGroupService");
            var guacNotification       = $injector.get('guacNotification');
            var permissionService      = $injector.get("permissionService");
            var userService            = $injector.get('userService');
            var userPageService        = $injector.get('userPageService');

            /**
             * An action to be provided along with the object sent to
             * showStatus which closes the currently-shown status dialog.
             */
            var ACKNOWLEDGE_ACTION = {
                name        : 'USER_MENU.ACTION_ACKNOWLEDGE',
                // Handle action
                callback    : function acknowledgeCallback() {
                    guacNotification.showStatus(false);
                }
            };

            /**
             * The outermost element of the user menu directive.
             *
             * @type Element
             */
            var element = $element[0];

            /**
             * The main document object.
             *
             * @type Document
             */
            var document = $document[0];

            /**
             * The root connection group, or null if the connection group hierarchy has
             * not yet been loaded.
             *
             * @type ConnectionGroup
             */
            var rootConnectionGroup = null;

            /**
             * All permissions associated with the current user, or null if the user's
             * permissions have not yet been loaded.
             *
             * @type PermissionSet
             */
            var permissions = null;

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
            
            /**
             * The available main pages for the current user.
             * 
             * @type Page[]
             */
            $scope.pages = null;

            /**
             * Updates the visible menu items based on the permissions and root
             * group on the scope, if available. If either the permissions or
             * the root group are not yet available, this function has no
             * effect.
             */
            var updateMenuItems = function updateMenuItems() {
                
                // Menu items are unknown until permissions and rootConnectionGroup are both available
                if (!permissions || !rootConnectionGroup) {
                    $scope.canChangePassword = null;
                    $scope.pages = [];
                    return;
                }
    
                // Retrieve the main pages from the user page service
                $scope.pages = userPageService.getMainPages(rootConnectionGroup, permissions);
                
                // Check whether the current user can change their own password
                $scope.canChangePassword = PermissionSet.hasUserPermission(
                        permissions, PermissionSet.ObjectPermissionType.UPDATE, 
                        authenticationService.getCurrentUserID()
                );
            };

            // Retrieve root group and all descendants
            connectionGroupService.getConnectionGroupTree(ConnectionGroup.ROOT_IDENTIFIER)
            .success(function rootConnectionGroupRetrieved(retrievedRootConnectionGroup) {
                
                rootConnectionGroup = retrievedRootConnectionGroup;

                // Navigate to home page, if not already there
                var homePage = userPageService.getHomePage(rootConnectionGroup);
                $location.url(homePage.url);
                
                updateMenuItems();
            });
            
            // Retrieve current permissions
            permissionService.getPermissions(authenticationService.getCurrentUserID())
            .success(function permissionsRetrieved(retrievedPermissions) {
                permissions = retrievedPermissions;
                updateMenuItems();
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
                    guacNotification.showStatus({
                        className  : 'error',
                        title      : 'USER_MENU.DIALOG_HEADER_ERROR',
                        text       : 'USER_MENU.ERROR_PASSWORD_MISMATCH',
                        actions    : [ ACKNOWLEDGE_ACTION ]
                    });
                    return;
                }
                
                // Verify that the new password is not blank
                if (!$scope.newPassword) {
                    guacNotification.showStatus({
                        className  : 'error',
                        title      : 'USER_MENU.DIALOG_HEADER_ERROR',
                        text       : 'USER_MENU.ERROR_PASSWORD_BLANK',
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
                    guacNotification.showStatus({
                        text    : 'USER_MENU.PASSWORD_CHANGED',
                        actions : [ ACKNOWLEDGE_ACTION ]
                    });
                })
                
                // Notify of any errors
                .error(function passwordUpdateFailed(error) {
                    guacNotification.showStatus({
                        className  : 'error',
                        title      : 'USER_MENU.DIALOG_HEADER_ERROR',
                        'text'       : error.message,
                        actions    : [ ACKNOWLEDGE_ACTION ]
                    });
                });
                
            };

            /**
             * Navigate to the given page.
             * 
             * @param {Page} page
             *     The page to navigate to.
             */
            $scope.navigateToPage = function navigateToPage(page) {
                $location.path(page.url);
            };
            
            /**
             * Tests whether the given page should be disabled.
             *
             * @param {Page} page
             *     The page to test.
             *
             * @returns {Boolean}
             *     true if the given page should be disabled, false otherwise.
             */
            $scope.isPageDisabled = function isPageDisabled(page) {
                return $location.url() === page.url;
            };
            
            /**
             * Logs out the current user, redirecting them to back to the login
             * screen after logout completes.
             */
            $scope.logout = function logout() {
                authenticationService.logout()['finally'](function logoutComplete() {
                    $location.path('/login/');
                });
            };

            // Close menu when use clicks anywhere else
            document.body.addEventListener('click', function clickOutsideMenu() {
                $scope.$apply(function closeMenu() {
                    $scope.menuShown = false;
                });
            }, false);

            // Prevent click within menu from triggering the outside-menu handler
            element.addEventListener('click', function clickInsideMenu(e) {
                e.stopPropagation();
            }, false);

        }] // end controller

    };
}]);
