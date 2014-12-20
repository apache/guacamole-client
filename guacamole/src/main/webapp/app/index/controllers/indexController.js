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

    // Get class dependencies
    var PermissionSet = $injector.get("PermissionSet");

    // Get services
    var permissionService       = $injector.get("permissionService"),
        authenticationService   = $injector.get("authenticationService"),
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

    /**
     * The current status notification, or false if no status is currently
     * shown.
     * 
     * @type Notification|Boolean
     */
    $scope.status = false;

    /**
     * All currently-visible notifications.
     * 
     * @type Notification[]
     */
    $scope.notifications = [];

    // Put some useful variables in the top level scope
    $scope.page = {
        title: '',
        bodyClassName: ''
    };
    $scope.currentUserID = null;
    $scope.currentUserIsAdmin = false;
    $scope.currentUserHasUpdate = false;
    $scope.currentUserPermissions = null;
    var notificationUniqueID = 0;
    
    // A promise to be fulfilled when all basic user permissions are loaded.
    var permissionsLoaded= $q.defer();
    $scope.basicPermissionsLoaded = permissionsLoaded.promise;
    
    $scope.currentUserID = authenticationService.getCurrentUserID();
    
    // If the user is unknown, force a login
    if(!$scope.currentUserID)
        $location.path('/login');

    /**
     * Shows or hides the given notification as a modal status. If a status
     * notification is currently shown, no further statuses will be shown
     * until the current status is hidden.
     *
     * @param {Notification|Boolean|Object} status
     *     The status notification to show.
     *
     * @example
     * 
     * // To show a status message with actions
     * $scope.showStatus({
     *     'title'      : 'Disconnected',
     *     'text'       : 'You have been disconnected!',
     *     'actions'    : {
     *         'name'       : 'reconnect',
     *         'callback'   : function () {
     *             // Reconnection code goes here
     *         }
     *     }
     * });
     * 
     * // To hide the status message
     * $scope.showStatus(false);
     */
    $scope.showStatus = function showStatus(status) {
        if (!$scope.status || !status)
            $scope.status = status;
    };
    
    /**
     * Adds a notification to the the list of notifications shown.
     * 
     * @param {Notification|Object} notification The notification to add.
     *
     * @returns {Number}
     *     A unique ID for the notification that's just been added.
     * 
     * @example
     * 
     * var id = $scope.addNotification({
     *     'title'      : 'Download',
     *     'text'       : 'You have a file ready for download!',
     *     'actions'    : {
     *         'name'       : 'download',
     *         'callback'   : function () {
     *             // download the file and remove the notification here
     *         }
     *     }
     * });
     */
    $scope.addNotification = function addNotification(notification) {
        var id = ++notificationUniqueID;

        $scope.notifications.push({
            notification    : notification,
            id              : id
        });
        
        return id;
    };
    
    /**
     * Remove a notification by unique ID.
     * 
     * @param {type} id The unique ID of the notification to remove. This ID is
     *                  retrieved from the initial call to addNotification.
     */
    $scope.removeNotification = function removeNotification(id) {
        for(var i = 0; i < $scope.notifications.length; i++) {
            if($scope.notifications[i].id === id) {
                $scope.notifications.splice(i, 1);
                return;
            }
        }
    };
           
    // Allow the permissions to be reloaded elsewhere if needed
    $scope.loadBasicPermissions = function loadBasicPermissions() {
        
        permissionService.getPermissions($scope.currentUserID).success(function fetchCurrentUserPermissions(permissions) {
            $scope.currentUserPermissions = permissions;

            // Whether the user has system-wide admin permission
            $scope.currentUserIsAdmin = PermissionSet.hasSystemPermission($scope.currentUserPermissions, PermissionSet.SystemPermissionType.ADMINISTER);

            // Whether the user can update at least one object
            $scope.currentUserHasUpdate = $scope.currentUserIsAdmin
                                        || PermissionSet.hasConnectionPermission($scope.currentUserPermissions, "UPDATE")
                                        || PermissionSet.hasConnectionGroupPermission($scope.currentUserPermissions, "UPDATE")
                                        || PermissionSet.hasUserPermission($scope.currentUserPermissions, "UPDATE");

            permissionsLoaded.resolve();
        });
    };

    // Provide simple mechanism for logging out the current user
    $scope.logout = function logout() {
        authenticationService.logout()['finally'](function logoutComplete() {
            $location.path('/login');
        });
    };
    
    // Try to load them now
    $scope.loadBasicPermissions();

    // Create event listeners at the global level
    var keyboard = new Guacamole.Keyboard($document[0]);

    // Broadcast keydown events
    keyboard.onkeydown = function onkeydown(keysym) {

        // Warn of pending keydown
        var guacBeforeKeydownEvent = $scope.$broadcast('guacBeforeKeydown', keysym, keyboard);
        if (guacBeforeKeydownEvent.defaultPrevented)
            return true;

        // If not prevented via guacBeforeKeydown, fire corresponding keydown event
        var guacKeydownEvent = $scope.$broadcast('guacKeydown', keysym, keyboard);
        return !guacKeydownEvent.defaultPrevented;

    };
    
    // Broadcast keyup events
    keyboard.onkeyup = function onkeyup(keysym) {

        // Warn of pending keyup
        var guacBeforeKeydownEvent = $scope.$broadcast('guacBeforeKeyup', keysym, keyboard);
        if (guacBeforeKeydownEvent.defaultPrevented)
            return;

        // If not prevented via guacBeforeKeyup, fire corresponding keydown event
        $scope.$broadcast('guacKeyup', keysym, keyboard);

    };

    // Release all keys when window loses focus
    $window.onblur = function () {
        keyboard.reset();
    };

    // Update title and CSS class upon navigation
    $scope.$on('$routeChangeSuccess', function(event, current, previous) {

        var title = current.$$route.title;
        if (title)
            $scope.page.title = title;

        $scope.page.bodyClassName = current.$$route.bodyClassName || '';

    });

}]);
