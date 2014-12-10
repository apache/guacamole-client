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
 * A service for defining the HistoryEntryDuration class.
 */
angular.module('manage').factory('HistoryEntryDuration', [function defineHistoryEntryDuration() {

    /**
     * Value/unit pair representing the length of time that a connection was
     * used.
     * 
     * @constructor
     * @param {Number} milliseconds
     *     The number of milliseconds that the associated connection was used.
     */
    var HistoryEntryDuration = function HistoryEntryDuration(milliseconds) {

        var seconds = milliseconds / 1000;

        /**
         * Rounds the given value to the nearest tenth.
         *
         * @param {Number} value The value to round.
         * @returns {Number} The given value, rounded to the nearest tenth.
         */
        var round = function round(value) {
            return Math.round(value * 10) / 10;
        };

        // Days
        if (seconds >= 86400) {
            this.value = round(seconds / 86400);
            this.unit  = 'day';
        }

        // Hours
        else if (seconds >= 3600) {
            this.value = round(seconds / 3600);
            this.unit  = 'hour';
        }

        // Minutes
        else if (seconds >= 60) {
            this.value = round(seconds / 60);
            this.unit  = 'minute';
        }
        
        // Seconds
        else {

            /**
             * The number of seconds (or minutes, or hours, etc.) that the
             * connection was used. The units associated with this value are
             * represented by the unit property.
             *
             * @type Number
             */
            this.value = round(seconds);

            /**
             * The units associated with the value of this duration. Valid
             * units are 'second', 'minute', 'hour', and 'day'.
             *
             * @type String
             */
            this.unit = 'second';

        }

    };

    return HistoryEntryDuration;

}]);