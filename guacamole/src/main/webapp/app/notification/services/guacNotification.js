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
angular.module('notification').factory('guacNotification', ['$rootScope',
        function guacNotification($rootScope) {

    var service = {};

    /**
     * The current status notification, or false if no status is currently
     * shown.
     * 
     * @type Notification|Boolean
     */
    service.status = false;

    /**
     * All currently-visible notifications.
     * 
     * @type Notification[]
     */
    service.notifications = [];

    /**
     * The ID of the most recently shown notification, or 0 if no notifications
     * have yet been shown.
     *
     * @type Number
     */
    var notificationUniqueID = 0;
    
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
        if (!service.status || !status)
            service.status = status;
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
        var id = ++notificationUniqueID;

        service.notifications.push({
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
        for (var i = 0; i < service.notifications.length; i++) {
            if (service.notifications[i].id === id) {
                service.notifications.splice(i, 1);
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
