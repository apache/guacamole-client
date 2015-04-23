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
 * Service for displaying notifications and modal status dialogs.
 */
angular.module('notification').factory('guacNotification', ['$injector',
        function guacNotification($injector) {

    // Required services
    var $rootScope            = $injector.get('$rootScope');
    var sessionStorageFactory = $injector.get('sessionStorageFactory');

    var service = {};

    /**
     * Getter/setter which retrieves or sets the current status notification,
     * which may simply be false if no status is currently shown.
     * 
     * @type Function
     */
    var storedStatus = sessionStorageFactory.create(false);

    /**
     * Getter/setter which retrieves or sets an array of all currently-visible
     * notifications.
     * 
     * @type Function
     */
    var storedNotifications = sessionStorageFactory.create([]);

    /**
     * Getter/setter which retrieves or sets the ID of the most recently shown
     * notification, or 0 if no notifications have yet been shown.
     *
     * @type Function
     */
    var storedNotificationUniqueID = sessionStorageFactory.create(0);

    /**
     * Retrieves the current status notification, which may simply be false if
     * no status is currently shown.
     * 
     * @type Notification|Boolean
     */
    service.getStatus = function getStatus() {
        return storedStatus();
    };

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
     * guacNotification.showStatus({
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
     * guacNotification.showStatus(false);
     */
    service.showStatus = function showStatus(status) {
        if (!storedStatus() || !status)
            storedStatus(status);
    };

    /**
     * Returns an array of all currently-visible notifications.
     *
     * @returns {Notification[]}
     *     An array of all currently-visible notifications.
     */
    service.getNotifications = function getNotifications() {
        return storedNotifications();
    };

    /**
     * Adds a notification to the the list of notifications shown.
     * 
     * @param {Notification|Object} notification
     *     The notification to add.
     *
     * @returns {Number}
     *     A unique ID for the notification that's just been added.
     * 
     * @example
     * 
     * var id = guacNotification.addNotification({
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
    service.addNotification = function addNotification(notification) {
        var id = storedNotificationUniqueID(storedNotificationUniqueID() + 1);

        storedNotifications().push({
            notification    : notification,
            id              : id
        });
        
        return id;
    };
    
    /**
     * Remove a notification by unique ID.
     * 
     * @param {Number} id
     *     The unique ID of the notification to remove. This ID is retrieved
     *     from the initial call to addNotification.
     */
    service.removeNotification = function removeNotification(id) {

        var notifications = storedNotifications();

        for (var i = 0; i < notifications.length; i++) {
            if (notifications[i].id === id) {
                notifications.splice(i, 1);
                return;
            }
        }

    };
           
    // Hide status upon navigation
    $rootScope.$on('$routeChangeSuccess', function() {
        service.showStatus(false);
    });

    return service;

}]);
