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
 * Provides the NotificationCountdown class definition.
 */
angular.module('notification').factory('NotificationCountdown', [function defineNotificationCountdown() {

    /**
     * Creates a new NotificationCountdown which describes an action that
     * should be performed after a specific number of seconds has elapsed.
     *
     * @constructor
     * @param {String} text The body text of the notification countdown.
     *
     * @param {Number} remaining
     *     The number of seconds remaining in the countdown.
     *
     * @param {Function} [callback]
     *     The callback to call when the countdown elapses.
     */
    var NotificationCountdown = function NotificationCountdown(text, remaining, callback) {

        /**
         * Reference to this NotificationCountdown.
         *
         * @type NotificationCountdown
         */
        var countdown = this;

        /**
         * The body text of the notification countdown. For the sake of i18n,
         * the variable REMAINING should be applied within the translation
         * string for formatting plurals, etc.
         *
         * @type String
         */
        this.text = text;

        /**
         * The number of seconds remaining in the countdown. After this number
         * of seconds elapses, the callback associated with this
         * NotificationCountdown will be called.
         *
         * @type Number
         */
        this.remaining = remaining;

        /**
         * The callback to call when this countdown expires.
         *
         * @type Function
         */
        this.callback = callback;

    };

    return NotificationCountdown;

}]);
