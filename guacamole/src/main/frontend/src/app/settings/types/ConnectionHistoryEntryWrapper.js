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
         * The identifier of the connection associated with this history entry.
         *
         * @type String
         */
        this.connectionIdentifier = historyEntry.connectionIdentifier;

        /**
         * The name of the connection associated with this history entry.
         *
         * @type String
         */
        this.connectionName = historyEntry.connectionName;

        /**
         * The remote host associated with this history entry.
         *
         * @type String
         */
        this.remoteHost = historyEntry.remoteHost;

        /**
         * The username of the user associated with this particular usage of
         * the connection.
         *
         * @type String
         */
        this.username = historyEntry.username;

        /**
         * The time that usage began, in seconds since 1970-01-01 00:00:00 UTC.
         *
         * @type Number
         */
        this.startDate = historyEntry.startDate;

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
        this.endDate = historyEntry.endDate;

        /**
         * The total amount of time the connection associated with the wrapped
         * history record was open, in seconds.
         *
         * @type Number
         */
        this.duration = this.endDate - this.startDate;

        /**
         * An object providing value and unit properties, denoting the duration
         * and its corresponding units.
         *
         * @type ConnectionHistoryEntry.Duration
         */
        this.readableDuration = null;

        // Set the duration if the necessary information is present
        if (this.endDate && this.startDate)
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
        if (!this.endDate)
            this.readableDurationText = 'SETTINGS_CONNECTION_HISTORY.INFO_CONNECTION_DURATION_UNKNOWN';

    };

    return ConnectionHistoryEntryWrapper;

}]);
