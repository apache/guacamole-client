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
 * A service for generating new guacClient properties objects.
 */
angular.module('manage').factory('HistoryEntryWrapper', [function defineHistoryEntryWrapper() {

    /**
     * Given a number of milliseconds, returns an object containing a unit and value
     * for that history entry duration.
     * 
     * @param {Number} milliseconds The number of milliseconds.
     * @return {Object} A unit and value pair representing a history entry duration.
     */
    var formatMilliseconds = function formatMilliseconds(milliseconds) {

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

        // Seconds
        if (seconds < 60) {
            return {
                value : round(seconds),
                unit  : "second"
            };
        }

        // Minutes
        if (seconds < 3600) {
            return {
                value : round(seconds / 60 ),
                unit  : "minute"
            };
        }
        
        // Hours
        if (seconds < 86400) {
            return {
                value : round(seconds / 3600),
                unit  : "hour"
            };
        }
        
        // Days
        return {
            value : round(seconds / 86400),
            unit  : "day"
        };

    };
        
    /**
     * Wrapper for ConnectionHistoryEntry which adds display-specific
     * properties, such as the connection duration.
     * 
     * @constructor
     * @param {ConnectionHistoryEntry} historyEntry
     *     The history entry to wrap.
     */
    var HistoryEntryWrapper = function HistoryEntryWrapper(historyEntry) {

        /**
         * The wrapped ConnectionHistoryEntry.
         *
         * @type ConnectionHistoryEntry
         */
        this.entry = historyEntry;

        /**
         * An object providing value and unit properties, denoting the duration
         * and its corresponding units.
         *
         * @type Object
         */
        this.duration = null;

        // Set the duration if the necessary information is present
        if (historyEntry.endDate && historyEntry.startDate)
            this.duration = formatMilliseconds(historyEntry.endDate - historyEntry.startDate);

    };

    return HistoryEntryWrapper;

}]);