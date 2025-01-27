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

import { ConnectionHistoryEntry } from '../../rest/types/ConnectionHistoryEntry';

/**
 * Wrapper for ConnectionHistoryEntry which adds display-specific
 * properties, such as the connection duration.
 */
export class HistoryEntryWrapper {

    /**
     * The wrapped ConnectionHistoryEntry.
     */
    entry: ConnectionHistoryEntry;

    /**
     * An object providing value and unit properties, denoting the duration
     * and its corresponding units.
     */
    duration: ConnectionHistoryEntry.Duration | null = null;

    /**
     * The string to display as the duration of this history entry. If a
     * duration is available, its value and unit will be exposed to any
     * given translation string as the VALUE and UNIT substitution
     * variables respectively.
     */
    durationText = 'MANAGE_CONNECTION.TEXT_HISTORY_DURATION';

    /**
     * Creates a new HistoryEntryWrapper.
     *
     * @param historyEntry
     *     The history entry to wrap.
     */
    constructor(historyEntry: ConnectionHistoryEntry) {
        this.entry = historyEntry;
        this.duration = null;

        // Notify if connection is active right now
        if (historyEntry.active)
            this.durationText = 'MANAGE_CONNECTION.INFO_CONNECTION_ACTIVE_NOW';

        // If connection is not active, inform user if end date is not known
        else if (!historyEntry.endDate)
            this.durationText = 'MANAGE_CONNECTION.INFO_CONNECTION_DURATION_UNKNOWN';

        // Set the duration if the necessary information is present
        if (historyEntry.endDate && historyEntry.startDate)
            this.duration = new ConnectionHistoryEntry.Duration(historyEntry.endDate - historyEntry.startDate);
    }
}
