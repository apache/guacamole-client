

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

import { ActivityLog } from './ActivityLog';

export declare namespace ConnectionHistoryEntry {

    /**
     * Value/unit pair representing the length of time that a connection was
     * used.
     */
    type Duration = typeof ConnectionHistoryEntry.Duration.prototype;
}

/**
 * Returned by REST API calls when representing the data
 * associated with an entry in a connection's usage history. Each history
 * entry represents the time at which a particular started using a
 * connection and, if applicable, the time that usage stopped.
 */
export class ConnectionHistoryEntry {

    /**
     * An arbitrary identifier that uniquely identifies this record
     * relative to other records in the same set, or null if no such unique
     * identifier exists.
     */
    identifier?: string;

    /**
     * A UUID that uniquely identifies this record, or null if no such
     * unique identifier exists.
     */
    uuid?: string;

    /**
     * The identifier of the connection associated with this history entry.
     */
    connectionIdentifier?: string;

    /**
     * The name of the connection associated with this history entry.
     */
    connectionName?: string;

    /**
     * The time that usage began, in milliseconds since 1970-01-01 00:00:00 UTC.
     */
    startDate?: number;

    /**
     * The time that usage ended, in milliseconds since 1970-01-01 00:00:00 UTC.
     * The absence of an endDate does NOT necessarily indicate that the
     * connection is still in use, particularly if the server was shutdown
     * or restarted before the history entry could be updated. To determine
     * whether a connection is still active, check the active property of
     * this history entry.
     */
    endDate?: number;

    /**
     * The remote host that initiated this connection, if known.
     */
    remoteHost?: number;

    /**
     * The username of the user associated with this particular usage of
     * the connection.
     */
    username?: string;

    /**
     * Whether this usage of the connection is still active. Note that this
     *? is the only accurate way to check for connection activity; the
     * absence of endDate does not necessarily imply the connection is
     * active, as the history entry may simply be incomplete.
     */
    active?: boolean;

    /**
     * Arbitrary name/value pairs which further describe this history
     * entry. The semantics and validity of these attributes are dictated
     * by the extension which defines them.
     */
    attributes?: Record<string, string>;

    /**
     * All logs associated and accessible via this record, stored by their
     * corresponding unique names.
     */
    logs?: Record<string, ActivityLog>;

    /**
     * Creates a new ConnectionHistoryEntry.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     ConnectionHistoryEntry.
     */
    constructor(template: Partial<ConnectionHistoryEntry> = {}) {
        this.identifier = template.identifier;
        this.uuid = template.uuid;
        this.connectionIdentifier = template.connectionIdentifier;
        this.connectionName = template.connectionName;
        this.startDate = template.startDate;
        this.endDate = template.endDate;
        this.remoteHost = template.remoteHost;
        this.username = template.username;
        this.active = template.active;
        this.attributes = template.attributes;
        this.logs = template.logs;
    }

    /**
     * All possible predicates for sorting ConnectionHistoryEntry objects using
     * the REST API. By default, each predicate indicates ascending order. To
     * indicate descending order, add "-" to the beginning of the predicate.
     */
    static SortPredicate = {
        /**
         * The date and time that the connection associated with the history
         * entry began (connected).
         */
        START_DATE: 'startDate'

    };

    /**
     * Value/unit pair representing the length of time that a connection was
     * used.
     */
    static Duration = class Duration {

        /**
         * The number of seconds (or minutes, or hours, etc.) that the
         * connection was used. The units associated with this value are
         * represented by the unit property.
         */
        value: number;

        /**
         * The units associated with the value of this duration. Valid
         * units are 'second', 'minute', 'hour', and 'day'.
         */
        unit: string;

        /**
         * Creates a new Duration.
         *
         * @param milliseconds
         *     The number of milliseconds that the associated connection was used.
         */
        constructor(milliseconds: number) {

            /**
             * The provided duration in seconds.
             */
            const seconds = milliseconds / 1000;

            /**
             * Rounds the given value to the nearest tenth.
             *
             * @param value The value to round.
             * @returns The given value, rounded to the nearest tenth.
             */
            const round = (value: number): number => {
                return Math.round(value * 10) / 10;
            }

            // Days
            if (seconds >= 86400) {
                this.value = round(seconds / 86400);
                this.unit = 'day';
            }

            // Hours
            else if (seconds >= 3600) {
                this.value = round(seconds / 3600);
                this.unit = 'hour';
            }

            // Minutes
            else if (seconds >= 60) {
                this.value = round(seconds / 60);
                this.unit = 'minute';
            }

            // Seconds
            else {
                this.value = round(seconds);
                this.unit = 'second';
            }
        }

    };

}
