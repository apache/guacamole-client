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
import { GuacEventService } from 'guacamole-frontend-lib';
import { GuacFrontendEventArguments } from '../../events/types/GuacFrontendEventArguments';
import { SessionStorageEntry, SessionStorageFactory } from '../../storage/session-storage-factory.service';
import { ClipboardData } from '../types/ClipboardData';

/**
 * A service for maintaining and accessing clipboard data. If possible, this
 * service will leverage the local clipboard. If the local clipboard is not
 * available, an internal in-memory clipboard will be used instead.
 */
@Injectable({
    providedIn: 'root'
})
export class ClipboardService {

    /**
     * Getter/setter which retrieves or sets the current stored clipboard
     * contents. The stored clipboard contents are strictly internal to
     * Guacamole, and may not reflect the local clipboard if local clipboard
     * access is unavailable.
     */
    private readonly storedClipboardData: SessionStorageEntry<ClipboardData> = this.sessionStorageFactory.create(new ClipboardData());

    /**
     * The promise associated with the current pending clipboard read attempt.
     * If no clipboard read is active, this will be null.
     */
    private pendingRead: Promise<ClipboardData> | null = null;


    constructor(private readonly sessionStorageFactory: SessionStorageFactory,
                private guacEventService: GuacEventService<GuacFrontendEventArguments>) {
    }

    /**
     * Sets the local clipboard, if possible, to the given text.
     *
     * @param data
     *     The data to assign to the local clipboard.
     *
     * @return
     *     A promise that resolves if setting the clipboard was successful,
     *     and rejects if it failed.
     */
    private setLocalClipboard(data: ClipboardData): Promise<void> {

        // Attempt to read the clipboard using the Asynchronous Clipboard
        // API, if it's available
        if (navigator.clipboard && navigator.clipboard.writeText) {
            if (data.type === 'text/plain' && typeof data.data === 'string') {
                return navigator.clipboard.writeText(data.data);
            }
        }

        return Promise.reject();
    }

    /**
     * Get the current value of the local clipboard.
     *
     * @return
     *     A promise that resolves with the contents of the local clipboard
     *     if successful, and rejects if it fails.
     */
    private getLocalClipboard(): Promise<ClipboardData> {

        // If the clipboard is already being read, do not overlap the read
        // attempts; instead share the result across all requests
        if (this.pendingRead)
            return this.pendingRead;

        // Create and store the pending promise
        this.pendingRead = navigator.clipboard.readText()
            .then(text => new ClipboardData({ type: 'text/plain', data: text }))
            .finally(() => {
                // Clear pending promise after completion
                this.pendingRead = null;
            });

        return this.pendingRead;
    }

    /**
     * Returns the current value of the internal clipboard shared across all
     * active Guacamole connections running within the current browser tab. If
     * access to the local clipboard is available, the internal clipboard is
     * first synchronized with the current local clipboard contents. If access
     * to the local clipboard is unavailable, only the internal clipboard will
     * be used.
     *
     * @return
     *     A promise that will resolve with the contents of the internal
     *     clipboard, first retrieving those contents from the local clipboard
     *     if permission to do so has been granted. This promise is always
     *     resolved.
     */
    getClipboard(): Promise<ClipboardData> {
        return this.getLocalClipboard()
            .then((data) => this.storedClipboardData(data), () => this.storedClipboardData());
    }

    /**
     * Sets the content of the internal clipboard shared across all active
     * Guacamole connections running within the current browser tab. If
     * access to the local clipboard is available, the local clipboard is
     * first set to the provided clipboard content. If access to the local
     * clipboard is unavailable, only the internal clipboard will be used. A
     * "guacClipboard" event will be broadcast with the assigned data once the
     * operation has completed.
     *
     * @param data
     *     The data to assign to the clipboard.
     *
     * @return
     *     A promise that will resolve after the clipboard content has been
     *     set. This promise is always resolved.
     */
    setClipboard(data: ClipboardData): Promise<void> {
        return this.setLocalClipboard(data).finally(() => {

            // Update internal clipboard and broadcast event notifying of
            // updated contents
            this.storedClipboardData(data);
            this.guacEventService.broadcast('guacClipboard', { data });

        });
    }

    /**
     * Resynchronizes the local and internal clipboards, setting the contents
     * of the internal clipboard to that of the local clipboard (if local
     * clipboard access is granted) and broadcasting a "guacClipboard" event
     * with the current internal clipboard contents for consumption by external
     * components like the "guacClient" directive.
     */
    resyncClipboard = () => {
        this.getLocalClipboard().then(data => this.setClipboard(data));
    }

}
