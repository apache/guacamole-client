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
            
    // Get required services
    var authenticationService  = $injector.get("authenticationService");
    var connectionGroupService = $injector.get("connectionGroupService");
    var permissionService      = $injector.get("permissionService");
    
    /**
     * The root connection group, or null if the connection group hierarchy has
     * not yet been loaded.
     *
     * @type ConnectionGroup
     */
    $scope.rootConnectionGroup = null;

    /**
     * All permissions associated with the current user, or null if the user's
     * permissions have not yet been loaded.
     *
     * @type PermissionSet
     */
    $scope.permissions = null;

    /**
     * Returns whether critical data has completed being loaded.
     *
     * @returns {Boolean}
     *     true if enough data has been loaded for the user interface to be
     *     useful, false otherwise.
     */
    $scope.isLoaded = function isLoaded() {

        return $scope.rootConnectionGroup !== null
            && $scope.permissions         !== null;

    };

    // Retrieve root group and all descendants
    connectionGroupService.getConnectionGroupTree(ConnectionGroup.ROOT_IDENTIFIER)
    .success(function rootGroupRetrieved(rootConnectionGroup) {
        $scope.rootConnectionGroup = rootConnectionGroup;
    });
    
    // Retrieve current permissions
    permissionService.getPermissions(authenticationService.getCurrentUserID())
    .success(function permissionsRetrieved(permissions) {
        $scope.permissions = permissions;
    });
    
}]);
