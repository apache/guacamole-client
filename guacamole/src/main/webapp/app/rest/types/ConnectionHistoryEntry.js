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
 * Service which defines the ConnectionHistoryEntry class.
 */
angular.module('rest').factory('ConnectionHistoryEntry', [function defineConnectionHistoryEntry() {
            
    /**
     * The object returned by REST API calls when representing the data
     * associated with an entry in a connection's usage history. Each history
     * entry represents the time at which a particular started using a
     * connection and, if applicable, the time that usage stopped.
     * 
     * @constructor
     * @param {ConnectionHistoryEntry|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     ConnectionHistoryEntry.
     */
    var ConnectionHistoryEntry = function ConnectionHistoryEntry(template) {

        // Use empty object by default
        template = template || {};

        /**
         * The time that usage began, in seconds since 1970-01-01 00:00:00 UTC.
         *
         * @type Number 
         */
        this.startDate = template.startDate;

        /**
         * The time that usage ended, in seconds since 1970-01-01 00:00:00 UTC.
         * The absence of an endDate does NOT necessarily indicate that the
         * connection is still in use, particularly if the server was shutdown
         * or restarted before the history entry could be updated. To determine
         * whether a connection is still active, check the active property of
         * this history entry.
         * 
         * @type Number 
         */
        this.endDate = template.endDate;

        /**
         * The username of the user associated with this particular usage of
         * the connection.
         * 
         * @type String
         */
        this.username = template.username;

        /**
         * Whether this usage of the connection is still active. Note that this
         * is the only accurate way to check for connection activity; the
         * absence of endDate does not necessarily imply the connection is
         * active, as the history entry may simply be incomplete.
         * 
         * @type Boolean
         */
        this.active = template.active;

    };

    return ConnectionHistoryEntry;

}]);