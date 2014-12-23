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
 * The controller for editing users.
 */
angular.module('manage').controller('manageUserController', ['$scope', '$injector', 
        function manageUserController($scope, $injector) {
            
    // Required types
    var ConnectionGroup   = $injector.get('ConnectionGroup');
    var PermissionFlagSet = $injector.get('PermissionFlagSet');
    var PermissionSet     = $injector.get('PermissionSet');

    // Required services
    var $location              = $injector.get('$location');
    var $routeParams           = $injector.get('$routeParams');
    var connectionGroupService = $injector.get('connectionGroupService');
    var userService            = $injector.get('userService');
    var permissionService      = $injector.get('permissionService');

    /**
     * An action to be provided along with the object sent to showStatus which
     * closes the currently-shown status dialog.
     */
    var ACKNOWLEDGE_ACTION = {
        name        : "manage.error.action.acknowledge",
        // Handle action
        callback    : function acknowledgeCallback() {
            $scope.showStatus(false);
        }
    };

    /**
     * The username of the user being edited.
     *
     * @type String
     */
    var username = $routeParams.id;

    // Pull user data
    userService.getUser(username).success(function userReceived(user) {
        $scope.user = user;
    });

    // Pull user permissions
    permissionService.getPermissions(username).success(function gotPermissions(permissions) {
        $scope.permissionFlags = PermissionFlagSet.fromPermissionSet(permissions);
    });

    // Retrieve all connections for which we have UPDATE permission
    connectionGroupService.getConnectionGroupTree(ConnectionGroup.ROOT_IDENTIFIER, PermissionSet.ObjectPermissionType.ADMINISTER)
    .success(function connectionGroupReceived(rootGroup) {
        $scope.rootGroup = rootGroup;
    });

    /**
     * Available system permission types, as translation string / internal
     * value pairs.
     * 
     * @type Object[]
     */
    $scope.systemPermissionTypes = [
        {
            label: "manage.edit.user.administerSystem",
            value: PermissionSet.SystemPermissionType.ADMINISTER
        },
        {
            label: "manage.edit.user.createUser",
            value: PermissionSet.SystemPermissionType.CREATE_USER
        },
        {
            label: "manage.edit.user.createConnection",
            value: PermissionSet.SystemPermissionType.CREATE_CONNECTION
        },
        {
            label: "manage.edit.user.createConnectionGroup",
            value: PermissionSet.SystemPermissionType.CREATE_CONNECTION_GROUP
        }
    ];

    // Expose permission query and modification functions to group list template
    $scope.groupListContext = {

        /**
         * Returns the PermissionFlagSet that contains the current state of
         * granted permissions.
         *
         * @returns {PermissionFlagSet}
         *     The PermissionFlagSet describing the current state of granted
         *     permissions for the user being edited.
         */
        getPermissionFlags : function getPermissionFlags() {
            return $scope.permissionFlags;
        }

    };

    /**
     * Cancels all pending edits, returning to the management page.
     */
    $scope.cancel = function cancel() {
        $location.path('/manage/');
    };
            
    /**
     * Saves the user, updating the existing user only.
     */
    $scope.saveUser = function saveUser() {

        // Verify passwords match
        if ($scope.passwordMatch !== $scope.user.password) {
            $scope.showStatus({
                'className'  : 'error',
                'title'      : 'manage.error.title',
                'text'       : 'manage.edit.user.passwordMismatch',
                'actions'    : [ ACKNOWLEDGE_ACTION ]
            });
            return;
        }

        // Save the user
        userService.saveUser($scope.user)
        .success(function savedUser() {
            $location.path('/manage/');

            // TODO: Save permissions
        })

        // Notify of any errors
        .error(function userSaveFailed(error) {
            $scope.showStatus({
                'className'  : 'error',
                'title'      : 'manage.error.title',
                'text'       : error.message,
                'actions'    : [ ACKNOWLEDGE_ACTION ]
            });
        });

    };
    
    /**
     * An action to be provided along with the object sent to showStatus which
     * immediately deletes the current user.
     */
    var DELETE_ACTION = {
        name        : "manage.edit.user.delete",
        className   : "danger",
        // Handle action
        callback    : function deleteCallback() {
            deleteUserImmediately();
            $scope.showStatus(false);
        }
    };

    /**
     * An action to be provided along with the object sent to showStatus which
     * closes the currently-shown status dialog.
     */
    var CANCEL_ACTION = {
        name        : "manage.edit.user.cancel",
        // Handle action
        callback    : function cancelCallback() {
            $scope.showStatus(false);
        }
    };

    /**
     * Immediately deletes the current user, without prompting the user for
     * confirmation.
     */
    var deleteUserImmediately = function deleteUserImmediately() {

        // Delete the user 
        userService.deleteUser($scope.user)
        .success(function deletedUser() {
            $location.path('/manage/');
        })

        // Notify of any errors
        .error(function userDeletionFailed(error) {
            $scope.showStatus({
                'className'  : 'error',
                'title'      : 'manage.error.title',
                'text'       : error.message,
                'actions'    : [ ACKNOWLEDGE_ACTION ]
            });
        });

    };

    /**
     * Deletes the user, prompting the user first to confirm that deletion is
     * desired.
     */
    $scope.deleteUser = function deleteUser() {

        // Confirm deletion request
        $scope.showStatus({
            'title'      : 'manage.edit.user.confirmDelete.title',
            'text'       : 'manage.edit.user.confirmDelete.text',
            'actions'    : [ DELETE_ACTION, CANCEL_ACTION]
        });

    };

}]);
