/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * A service for defining the ConnectionHistoryEntryWrapper class.
 */
angular.module('settings').factory('ConnectionHistoryEntryWrapper', ['$injector',
    function defineConnectionHistoryEntryWrapper($injector) {

    // Required types
    var ConnectionHistoryEntry = $injector.get('ConnectionHistoryEntry');

    /**
     * Wrapper for ConnectionHistoryEntry which adds display-specific
     * properties, such as a duration.
     *
     * @constructor
     * @param {ConnectionHistoryEntry} historyEntry
     *     The ConnectionHistoryEntry that should be wrapped.
     */
    var ConnectionHistoryEntryWrapper = function ConnectionHistoryEntryWrapper(historyEntry) {

        /**
         * The wrapped ConnectionHistoryEntry.
         *
         * @type ConnectionHistoryEntry
         */
        this.entry = historyEntry;

        /**
         * The total amount of time the connection associated with the wrapped
         * history record was open, in seconds.
         *
         * @type Number
         */
        this.duration = historyEntry.endDate - historyEntry.startDate;

        /**
         * An object providing value and unit properties, denoting the duration
         * and its corresponding units.
         *
         * @type ConnectionHistoryEntry.Duration
         */
        this.readableDuration = null;

        // Set the duration if the necessary information is present
        if (historyEntry.endDate && historyEntry.startDate)
            this.readableDuration = new ConnectionHistoryEntry.Duration(this.duration);

        /**
         * The string to display as the duration of this history entry. If a
         * duration is available, its value and unit will be exposed to any
         * given translation string as the VALUE and UNIT substitution
         * variables respectively.
         *
         * @type String
         */
        this.readableDurationText = 'SETTINGS_CONNECTION_HISTORY.TEXT_HISTORY_DURATION';

        // Inform user if end date is not known
        if (!historyEntry.endDate)
            this.readableDurationText = 'SETTINGS_CONNECTION_HISTORY.INFO_CONNECTION_DURATION_UNKNOWN';

    };

    return ConnectionHistoryEntryWrapper;

}]);
