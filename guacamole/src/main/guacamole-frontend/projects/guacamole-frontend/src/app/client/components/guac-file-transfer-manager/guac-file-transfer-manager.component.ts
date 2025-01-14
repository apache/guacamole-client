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

import { Component, Input, ViewEncapsulation } from '@angular/core';
import { ManagedClientService } from '../../services/managed-client.service';
import { ManagedClient } from '../../types/ManagedClient';
import { ManagedClientGroup } from '../../types/ManagedClientGroup';
import { ManagedFileTransferState } from '../../types/ManagedFileTransferState';

/**
 * Component which displays all active file transfers.
 */
@Component({
    selector     : 'guac-file-transfer-manager',
    templateUrl  : './guac-file-transfer-manager.component.html',
    encapsulation: ViewEncapsulation.None
})
export class GuacFileTransferManagerComponent {

    /**
     * The client group whose file transfers should be managed by this
     * directive.
     */
    @Input({ required: true }) clientGroup!: ManagedClientGroup | null;

    /**
     * Inject required services.
     */
    constructor(private managedClientService: ManagedClientService) {
    }

    /**
     * Determines whether the given file transfer state indicates an
     * in-progress transfer.
     *
     * @param transferState
     *     The file transfer state to check.
     *
     * @returns
     *     true if the given file transfer state indicates an in-
     *     progress transfer, false otherwise.
     */
    private isInProgress(transferState: ManagedFileTransferState): boolean {
        switch (transferState.streamState) {

            // IDLE or OPEN file transfers are active
            case ManagedFileTransferState.StreamState.IDLE:
            case ManagedFileTransferState.StreamState.OPEN:
                return true;

            // All others are not active
            default:
                return false;

        }
    }

    /**
     * Removes all file transfers which are not currently in-progress.
     */
    clearCompletedTransfers(): void {

        // Nothing to clear if no client group attached
        if (!this.clientGroup)
            return;

        // Remove completed uploads
        this.clientGroup.clients.forEach(client => {
            client.uploads = client.uploads.filter(upload => this.isInProgress(upload.transferState));
        });

    }

    /**
     * @borrows ManagedClientGroup.hasMultipleClients
     */
    hasMultipleClients(group: ManagedClientGroup | null): boolean {
        return ManagedClientGroup.hasMultipleClients(group);
    }

    /**
     * @borrows ManagedClientService.hasTransfers
     */
    hasTransfers(client: ManagedClient): boolean {
        return this.managedClientService.hasTransfers(client);
    }

}
