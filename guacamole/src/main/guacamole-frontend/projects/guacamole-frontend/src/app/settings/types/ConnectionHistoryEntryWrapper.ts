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

import { translate } from '@ngneat/transloco';
import _ from 'lodash';
import { ActivityLog } from '../../rest/types/ActivityLog';
import { ConnectionHistoryEntry } from '../../rest/types/ConnectionHistoryEntry';

declare namespace ConnectionHistoryEntryWrapper {
    type Log = typeof ConnectionHistoryEntryWrapper.Log.prototype;
}

/**
 * Wrapper for ConnectionHistoryEntry which adds display-specific
 * properties, such as a duration.
 */
export class ConnectionHistoryEntryWrapper {

    /**
     * The data source associated with the wrapped history record.
     */
    dataSource: string;

    /**
     * The wrapped ConnectionHistoryEntry.
     */
    entry: ConnectionHistoryEntry;

    /**
     * The total amount of time the connection associated with the wrapped
     * history record was open, in seconds.
     */
    duration: number;

    /**
     * An object providing value and unit properties, denoting the duration
     * and its corresponding units.
     */
    readableDuration: ConnectionHistoryEntry.Duration | null = null;

    /**
     * The string to display as the duration of this history entry. If a
     * duration is available, its value and unit will be exposed to any
     * given translation string as the VALUE and UNIT substitution
     * variables respectively.
     */
    readableDurationText: string;

    /**
     * The graphical session recording associated with this history entry,
     * if any. If no session recordings are associated with the entry, this
     * will be null. If there are multiple session recordings, this will be
     * the first such recording.
     */
    sessionRecording: ConnectionHistoryEntryWrapper.Log | null = null;

    /**
     * Creates a new ConnectionHistoryEntryWrapper. This constructor initializes the properties of the
     * new ConnectionHistoryEntryWrapper with the corresponding properties of the given template.
     *
     * @param historyEntry
     *     The ConnectionHistoryEntry that should be wrapped.
     */
    constructor(dataSource: string, historyEntry: ConnectionHistoryEntry) {
        this.dataSource = dataSource;
        this.entry = historyEntry;
        this.duration = historyEntry.endDate! - historyEntry.startDate!;

        // Set the duration if the necessary information is present
        if (historyEntry.endDate && historyEntry.startDate)
            this.readableDuration = new ConnectionHistoryEntry.Duration(this.duration);

        this.readableDurationText = 'SETTINGS_CONNECTION_HISTORY.TEXT_HISTORY_DURATION';

        // Inform user if end date is not known
        if (!historyEntry.endDate)
            this.readableDurationText = 'SETTINGS_CONNECTION_HISTORY.INFO_CONNECTION_DURATION_UNKNOWN';

        this.sessionRecording = this.getSessionRecording();
    }

    private getSessionRecording(): ConnectionHistoryEntryWrapper.Log | null {

        const identifier = this.entry.identifier;
        if (!identifier)
            return null;

        const name: string | undefined = _.findKey(this.entry.logs, log => log.type === ActivityLog.Type.GUACAMOLE_SESSION_RECORDING);
        if (!name)
            return null;

        const log: ActivityLog = (this.entry.logs as Record<string, ActivityLog>)[name];

        const description = log.description && log.description.key ? translate(log.description.key, log.description.variables) : '';

        return new ConnectionHistoryEntryWrapper.Log({

            url: '/settings/' + encodeURIComponent(this.dataSource)
                + '/recording/' + encodeURIComponent(identifier)
                + '/' + encodeURIComponent(name),

            description

        });

    }

    /**
     * Representation of the ActivityLog of a ConnectionHistoryEntry which adds
     * display-specific properties, such as a URL for viewing the log.
     */
    static Log = class {

        /**
         * The relative URL for a session recording player that loads the
         * session recording represented by this log.
         */
        url: string;

        /**
         * A promise that resolves with a human-readable description of the log.
         */
        description: Promise<string>;

        /**
         * Creates a new ConnectionHistoryEntryWrapper.Log. This constructor initializes the properties of the
         * new Log with the corresponding properties of the given template.
         *
         * @param template
         *     The object whose properties should be copied within the new
         *     ConnectionHistoryEntryWrapper.Log.
         */
        constructor(template: ConnectionHistoryEntryWrapper.Log | any = {}) {
            this.url = template.url;
            this.description = template.description;
        }
    };
}
