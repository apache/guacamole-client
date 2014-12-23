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
    var connectionGroupService = $injector.get('connectionGroupService');
    var userService            = $injector.get('userService');

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
     * The name of the new user to create, if any, when user creation is
     * requested via newUser().
     *
     * @type String
     */
    $scope.newUsername = "";
    
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
                'title'      : 'manage.error.title',
                'text'       : error.message,
                'actions'    : [ ACKNOWLEDGE_ACTION ]
            });
        });

        // Reset username
        $scope.newUsername = "";

    };
    
}]);
