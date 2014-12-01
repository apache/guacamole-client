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
 * Provides the Notification class definition.
 */
angular.module('notification').factory('Notification', [function defineNotification() {

    /**
     * Creates a new Notification, initializing the properties of that
     * Notification with the corresponding properties of the given template.
     *
     * @constructor
     * @param {Notification|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     Notification.
     */
    var Notification = function Notification(template) {

        // Use empty object by default
        template = template || {};

        /**
         * The CSS class to associate with the notification, if any.
         *
         * @type String
         */
        this.className = template.className;

        /**
         * The title of the notification.
         *
         * @type String
         */
        this.title = template.title;

        /**
         * The body text of the notification.
         *
         * @type String
         */
        this.text = template.text;

        /**
         * An array of all actions available to the user in response to this
         * notification.
         *
         * @type NotificationAction[]
         */
        this.actions = template.actions || [];

        /**
         * The current progress state of the ongoing action associated with this
         * notification.
         *
         * @type NotificationProgress
         */
        this.progress = template.progress;

        /**
         * The countdown and corresponding default action which applies to
         * this notification, if any.
         *
         * @type NotificationCountdown
         */
        this.countdown = template.countdown;

    };

    return Notification;

}]);
