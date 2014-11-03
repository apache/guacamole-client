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
 * The controller for the root of the application.
 */
angular.module('index').controller('indexController', ['$scope', '$injector',
        function indexController($scope, $injector) {
            
    // Get the dependencies commonJS style
    var permissionDAO           = $injector.get("permissionDAO"),
        permissionCheckService  = $injector.get("permissionCheckService"),
        localStorageUtility     = $injector.get("localStorageUtility"),
        $q                      = $injector.get("$q"),
        $document               = $injector.get("$document"),
        $window                 = $injector.get("$window"),
        $location               = $injector.get("$location");
    
    /*
     * Safe $apply implementation from Alex Vanston:
     * https://coderwall.com/p/ngisma
     */
    $scope.safeApply = function(fn) {
        var phase = this.$root.$$phase;
        if(phase === '$apply' || phase === '$digest') {
            if(fn && (typeof(fn) === 'function')) {
                fn();
            }
        } else {
            this.$apply(fn);
        }
    };

    // Put some useful variables in the top level scope
    $scope.page = { title: '' };
    $scope.currentUserID = null;
    $scope.currentUserIsAdmin = false;
    $scope.currentUserHasUpdate = false;
    $scope.currentUserPermissions = null;
    
    // A promise to be fulfilled when all basic user permissions are loaded.
    var permissionsLoaded= $q.defer();
    $scope.basicPermissionsLoaded = permissionsLoaded.promise;
    
    $scope.currentUserID = localStorageUtility.get('userID');
    
    // If the user is unknown, force a login
    if(!$scope.currentUserID)
        $location.path('/login');
    
    // Allow the permissions to be reloaded elsewhere if needed
    $scope.loadBasicPermissions = function loadBasicPermissions() {
        
        permissionDAO.getPermissions($scope.currentUserID).success(function fetchCurrentUserPermissions(permissions) {
            $scope.currentUserPermissions = permissions;

            // Will be true if the user is an admin
            $scope.currentUserIsAdmin = permissionCheckService.checkPermission($scope.currentUserPermissions, "SYSTEM", undefined, "ADMINISTER");

            // Will be true if the user is an admin or has update access to any object               
            $scope.currentUserHasUpdate = $scope.currentUserIsAdmin || 
                    permissionCheckService.checkPermission($scope.currentUserPermissions, undefined, undefined, "UPDATE");
            
            permissionsLoaded.resolve();
        });
    };
    
    // Try to load them now
    $scope.loadBasicPermissions();
    
    // Create event listeners at the global level
    var keyboard = new Guacamole.Keyboard($document[0]);

    // Broadcast keydown events down the scope heirarchy
    keyboard.onkeydown = function onkeydown(keysym) {
        var guacKeydownEvent = $scope.$broadcast('guacKeydown', keysym, keyboard);
        return !guacKeydownEvent.defaultPrevented;
    };
    
    // Broadcast keyup events down the scope heirarchy
    keyboard.onkeyup = function onkeyup(keysym) {
        $scope.$broadcast('guacKeyup', keysym, keyboard);
    };

    // Release all keys when window loses focus
    $window.onblur = function () {
        keyboard.reset();
    };

    // Update title upon navigation
    $scope.$on('$routeChangeSuccess', function(event, current, previous) {
        var title = current.$$route.title;
        if (title)
            $scope.page.title = title;
    });

}]);
