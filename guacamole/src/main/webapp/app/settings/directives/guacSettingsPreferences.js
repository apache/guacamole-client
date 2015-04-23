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
 * A directive for managing preferences local to the current user.
 */
angular.module('settings').directive('guacSettingsPreferences', [function guacSettingsPreferences() {
    
    return {
        // Element only
        restrict: 'E',
        replace: true,

        scope: {},

        templateUrl: 'app/settings/templates/settingsPreferences.html',
        controller: ['$scope', '$injector', function settingsPreferencesController($scope, $injector) {

            // Get required types
            var PermissionSet = $injector.get('PermissionSet');

            // Required services
            var $translate            = $injector.get('$translate');
            var authenticationService = $injector.get('authenticationService');
            var guacNotification      = $injector.get('guacNotification');
            var languageService       = $injector.get('languageService');
            var permissionService     = $injector.get('permissionService');
            var preferenceService     = $injector.get('preferenceService');
            var userService           = $injector.get('userService');

            /**
             * An action to be provided along with the object sent to
             * showStatus which closes the currently-shown status dialog.
             */
            var ACKNOWLEDGE_ACTION = {
                name        : 'SETTINGS_PREFERENCES.ACTION_ACKNOWLEDGE',
                // Handle action
                callback    : function acknowledgeCallback() {
                    guacNotification.showStatus(false);
                }
            };

            /**
             * The username of the current user.
             *
             * @type String
             */
            var username = authenticationService.getCurrentUserID();

            /**
             * All currently-set preferences, or their defaults if not yet set.
             *
             * @type Object.<String, Object>
             */
            $scope.preferences = preferenceService.preferences;
            
            /**
             * A map of all available language keys to their human-readable
             * names.
             * 
             * @type Object.<String, String>
             */
            $scope.languages = null;
            
            /**
             * Switches the active display langugae to the chosen language.
             */
            $scope.changeLanguage = function changeLanguage() {
                $translate.use($scope.preferences.language);
            };

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
             * Whether the current user can change their own password, or null
             * if this is not yet known.
             *
             * @type Boolean
             */
            $scope.canChangePassword = null;

            /**
             * Update the current user's password to the password currently set within
             * the password change dialog.
             */
            $scope.updatePassword = function updatePassword() {

                // Verify passwords match
                if ($scope.newPasswordMatch !== $scope.newPassword) {
                    guacNotification.showStatus({
                        className  : 'error',
                        title      : 'SETTINGS_PREFERENCES.DIALOG_HEADER_ERROR',
                        text       : 'SETTINGS_PREFERENCES.ERROR_PASSWORD_MISMATCH',
                        actions    : [ ACKNOWLEDGE_ACTION ]
                    });
                    return;
                }
                
                // Verify that the new password is not blank
                if (!$scope.newPassword) {
                    guacNotification.showStatus({
                        className  : 'error',
                        title      : 'SETTINGS_PREFERENCES.DIALOG_HEADER_ERROR',
                        text       : 'SETTINGS_PREFERENCES.ERROR_PASSWORD_BLANK',
                        actions    : [ ACKNOWLEDGE_ACTION ]
                    });
                    return;
                }
                
                // Save the user with the new password
                userService.updateUserPassword(username, $scope.oldPassword, $scope.newPassword)
                .success(function passwordUpdated() {
                
                    // Clear the password fields
                    $scope.oldPassword      = null;
                    $scope.newPassword      = null;
                    $scope.newPasswordMatch = null;

                    // Indicate that the password has been changed
                    guacNotification.showStatus({
                        text    : 'SETTINGS_PREFERENCES.INFO_PASSWORD_CHANGED',
                        actions : [ ACKNOWLEDGE_ACTION ]
                    });
                })
                
                // Notify of any errors
                .error(function passwordUpdateFailed(error) {
                    guacNotification.showStatus({
                        className  : 'error',
                        title      : 'SETTINGS_PREFERENCES.DIALOG_HEADER_ERROR',
                        'text'       : error.message,
                        actions    : [ ACKNOWLEDGE_ACTION ]
                    });
                });
                
            };

            // Retrieve defined languages
            languageService.getLanguages()
            .success(function languagesRetrieved(languages) {
                $scope.languages = languages;
            });

            // Retrieve current permissions
            permissionService.getPermissions(username)
            .success(function permissionsRetrieved(permissions) {

                // Add action for changing password if permission is granted
                $scope.canChangePassword = PermissionSet.hasUserPermission(permissions,
                        PermissionSet.ObjectPermissionType.UPDATE, username);
                        
            });

            /**
             * Returns whether critical data has completed being loaded.
             *
             * @returns {Boolean}
             *     true if enough data has been loaded for the user interface to be
             *     useful, false otherwise.
             */
            $scope.isLoaded = function isLoaded() {

                return $scope.canChangePassword !== null
                    && $scope.languages         !== null;

            };

        }]
    };
    
    
    
}]);
