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

import { Component, DoCheck, Input, ViewEncapsulation } from '@angular/core';

import { GuacClickCallback, GuacEventService } from 'guacamole-frontend-lib';
import _ from 'lodash';
import { GuacFrontendEventArguments } from '../../../events/types/GuacFrontendEventArguments';
import { ManagedClientService } from '../../services/managed-client.service';
import { ManagedArgument } from '../../types/ManagedArgument';
import { ManagedClient } from '../../types/ManagedClient';
import { ManagedClientGroup } from '../../types/ManagedClientGroup';

/**
 * A component which displays one or more Guacamole clients in an evenly-tiled
 * view. The number of rows and columns used for the arrangement of tiles is
 * automatically determined by the number of clients present.
 */
@Component({
    selector: 'guac-tiled-clients',
    templateUrl: './guac-tiled-clients.component.html',
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class GuacTiledClientsComponent implements DoCheck {

    /**
     * The function to invoke when the "close" button in the header of a
     * client tile is clicked. The ManagedClient that is closed will be
     * supplied as the function argument.
     */
    @Input({ required: true }) onClose!: (client: ManagedClient) => void;

    /**
     * The group of Guacamole clients that should be displayed in an
     * evenly-tiled grid arrangement.
     */
    @Input({ required: true }) clientGroup!: ManagedClientGroup | null;

    /**
     * Whether translation of touch to mouse events should emulate an
     * absolute pointer device, or a relative pointer device.
     */
    @Input({ required: true }) emulateAbsoluteMouse!: boolean;

    /**
     * TODO
     * @private
     */
    private lastFocusedClient?: ManagedClient | null;

    /**
     * TODO
     * @private
     */
    private lastFocusedClientArguments?: Record<string, ManagedArgument>;

    /**
     * TODO
     * @private
     */
    private lastProtocol: string | undefined | null;

    /**
     * Inject required services.
     */
    constructor(private guacEventService: GuacEventService<GuacFrontendEventArguments>,
                private managedClientService: ManagedClientService) {
    }

    /**
     * Returns the currently-focused ManagedClient. If there is no such
     * client, or multiple clients are focused, null is returned.
     *
     * @returns
     *     The currently-focused client, or null if there are no focused
     *     clients or if multiple clients are focused.
     */
    private getFocusedClient(): ManagedClient | null {

        const managedClientGroup = this.clientGroup;
        if (managedClientGroup) {
            const focusedClients = _.filter(managedClientGroup.clients, client => client.clientProperties.focused);
            if (focusedClients.length === 1)
                return focusedClients[0];
        }

        return null;

    }

    /**
     * Custom change detection logic for the component.
     */
    ngDoCheck(): void {

        // Notify whenever identify of currently-focused client changes
        const newFocusedClient: ManagedClient | null = this.getFocusedClient();

        if (this.lastFocusedClient !== newFocusedClient) {

            this.guacEventService.broadcast('guacClientFocused', { newFocusedClient });

            // Remember the new focused Client
            this.lastFocusedClient = newFocusedClient;

        }

        // Notify whenever arguments of currently-focused client changes
        const newArguments: Record<string, ManagedArgument> | undefined = this.getFocusedClient()?.arguments;

        if (!_.isEqual(this.lastFocusedClientArguments, newArguments)) {

            this.guacEventService.broadcast('guacClientArgumentsUpdated', { focusedClient: newFocusedClient });

            // Remember the new arguments
            this.lastFocusedClientArguments = newArguments;
        }

        // Notify whenever protocol of currently-focused client changes
        const newProtocol: string | undefined | null = this.getFocusedClient()?.protocol;

        if (this.lastProtocol !== newProtocol) {

            this.guacEventService.broadcast('guacClientProtocolUpdated', { focusedClient: newFocusedClient });

            // Remember the new protocol
            this.lastProtocol = newProtocol;
        }

    }

    /**
     * Returns a callback for guacClick that assigns or updates keyboard
     * focus to the given client, allowing that client to receive and
     * handle keyboard events. Multiple clients may have keyboard focus
     * simultaneously.
     *
     * @param client
     *     The client that should receive keyboard focus.
     *
     * @return
     *     The callback that guacClient should invoke when the given client
     *     has been clicked.
     */
    getFocusAssignmentCallback(client: ManagedClient): GuacClickCallback {
        return (shift, ctrl) => {

            // Clear focus of all other clients if not selecting multiple
            if (!shift && !ctrl) {
                this.clientGroup?.clients.forEach(client => {
                    client.clientProperties.focused = false;
                });
            }

            client.clientProperties.focused = true;

            // Fill in any gaps if performing rectangular multi-selection
            // via shift-click
            if (shift && this.clientGroup) {

                let minRow = this.clientGroup.rows - 1;
                let minColumn = this.clientGroup.columns - 1;
                let maxRow = 0;
                let maxColumn = 0;

                // Determine extents of selected area
                ManagedClientGroup.forEach(this.clientGroup, (client, row, column) => {
                    if (client.clientProperties.focused) {
                        minRow = Math.min(minRow, row);
                        minColumn = Math.min(minColumn, column);
                        maxRow = Math.max(maxRow, row);
                        maxColumn = Math.max(maxColumn, column);
                    }
                });

                ManagedClientGroup.forEach(this.clientGroup, (client, row, column) => {
                    client.clientProperties.focused =
                        row >= minRow
                        && row <= maxRow
                        && column >= minColumn
                        && column <= maxColumn;
                });

            }

        };
    }

    /**
     * @borrows ManagedClientGroup.hasMultipleClients
     */
    hasMultipleClients(group: ManagedClientGroup | null): boolean {
        return ManagedClientGroup.hasMultipleClients(group);
    }

    /**
     * @borrows ManagedClientGroup.getClientGrid
     */
    getClientGrid(group: ManagedClientGroup | null): ManagedClient[][] {
        if (group === null)
            return [];

        return ManagedClientGroup.getClientGrid(group);
    }

    /**
     * @borrows ManagedClientService~isShared
     */
    isShared(client: ManagedClient): boolean {
        return this.managedClientService.isShared(client);
    }

}
