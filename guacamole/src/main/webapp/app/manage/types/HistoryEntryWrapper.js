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
 * A service for defining the HistoryEntryWrapper class.
 */
angular.module('manage').factory('HistoryEntryWrapper', ['$injector',
    function defineHistoryEntryWrapper($injector) {

    // Required types
    var ConnectionHistoryEntry = $injector.get('ConnectionHistoryEntry');

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
         * @type ConnectionHistoryEntry.Duration
         */
        this.duration = null;

        /**
         * The string to display as the duration of this history entry. If a
         * duration is available, its value and unit will be exposed to any
         * given translation string as the VALUE and UNIT substitution
         * variables respectively.
         * 
         * @type String
         */
        this.durationText = 'MANAGE_CONNECTION.TEXT_HISTORY_DURATION';

        // Notify if connection is active right now
        if (historyEntry.active)
            this.durationText = 'MANAGE_CONNECTION.INFO_CONNECTION_ACTIVE_NOW';

        // If connection is not active, inform use if end date is not known
        else if (!historyEntry.endDate)
            this.durationText = 'MANAGE_CONNECTION.INFO_CONNECTION_DURATION_UNKNOWN';

        // Set the duration if the necessary information is present
        if (historyEntry.endDate && historyEntry.startDate)
            this.duration = new ConnectionHistoryEntry.Duration(historyEntry.endDate - historyEntry.startDate);

    };

    return HistoryEntryWrapper;

}]);