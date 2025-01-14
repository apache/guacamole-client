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

import { Injectable } from '@angular/core';
import remove from 'lodash/remove';
import { PreferenceService } from '../settings/services/preference.service';
import { LocalStorageService } from '../storage/local-storage.service';
import { HistoryEntry } from './HistoryEntry';

// The parameter name for getting the history from local storage
const GUAC_HISTORY_STORAGE_KEY = 'GUAC_HISTORY';

/**
 * A service for reading and manipulating the Guacamole connection history.
 */
@Injectable({
    providedIn: 'root'
})
export class GuacHistoryService {

    /**
     * The top few recent connections, sorted in order of most recent access.
     */
    recentConnections: HistoryEntry[] = [];

    /**
     * Inject required services.
     */
    constructor(private readonly localStorageService: LocalStorageService,
                private readonly preferenceService: PreferenceService) {
        // Init stored connection history from localStorage
        const storedHistory = localStorageService.getItem(GUAC_HISTORY_STORAGE_KEY) || [];
        if (storedHistory instanceof Array)
            this.recentConnections = storedHistory;
    }

    /**
     * Remove from the list of connection history the item having the given
     * identfier.
     *
     * @param id
     *     The identifier of the item to remove from the history list.
     *
     * @returns
     *     True if the removal was successful, otherwise false.
     */
    removeEntry(id: string): boolean {

        return remove(this.recentConnections, entry => entry.id === id).length > 0;

    }

    /**
     * Updates the thumbnail and access time of the history entry for the
     * connection with the given ID.
     *
     * @param id
     *     The ID of the connection whose history entry should be updated.
     *
     * @param thumbnail
     *     The URL of the thumbnail image to associate with the history entry.
     */
    updateThumbnail(id: string, thumbnail: string) {

        let i;

        // Remove any existing entry for this connection
        for (i = 0; i < this.recentConnections.length; i++) {
            if (this.recentConnections[i].id === id) {
                this.recentConnections.splice(i, 1);
                break;
            }
        }

        // Store new entry in history
        this.recentConnections.unshift(new HistoryEntry(
            id,
            thumbnail
        ));

        // Truncate history to ideal length
        if (this.recentConnections.length > this.preferenceService.preferences.numberOfRecentConnections)
            this.recentConnections.length = this.preferenceService.preferences.numberOfRecentConnections;

        // Save updated history
        this.localStorageService.setItem(GUAC_HISTORY_STORAGE_KEY, this.recentConnections);

    }

}
