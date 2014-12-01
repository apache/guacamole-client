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
 * Provides the NotificationProgress class definition.
 */
angular.module('notification').factory('NotificationProgress', [function defineNotificationProgress() {

    /**
     * Creates a new NotificationProgress which describes the current status
     * of an operation, and how much of that operation remains to be performed.
     *
     * @constructor
     * @param {String} text The text describing the operation progress.
     *
     * @param {Number} value
     *     The current state of operation progress, as an arbitrary number
     *     which increases as the operation continues.
     *
     * @param {String} [unit]
     *     The unit of the arbitrary value, if that value has an associated
     *     unit.
     *
     * @param {Number} [ratio]
     *     If known, the current status of the operation as a value between 0
     *     and 1 inclusive, where 0 is not yet started, and 1 is complete.
     */
    var NotificationProgress = function NotificationProgress(text, value, unit, ratio) {

        /**
         * The text describing the operation progress. For the sake of i18n,
         * the variable PROGRESS should be applied within the translation
         * string for formatting plurals, etc., while UNIT should be used
         * for the progress unit, if any.
         *
         * @type String
         */
        this.text = text;

        /**
         * The current state of operation progress, as an arbitrary number which
         * increases as the operation continues.
         *
         * @type Number
         */
        this.value = value;

        /**
         * The unit of the arbitrary value, if that value has an associated
         * unit.
         *
         * @type String
         */
        this.unit = unit;

        /**
         * If known, the current status of the operation as a value between 0
         * and 1 inclusive, where 0 is not yet started, and 1 is complete.
         *
         * @type String
         */
        this.ratio = ratio;

    };

    return NotificationProgress;

}]);
