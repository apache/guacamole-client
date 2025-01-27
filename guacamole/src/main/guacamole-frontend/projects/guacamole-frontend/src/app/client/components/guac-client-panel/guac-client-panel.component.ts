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

import { AfterViewInit, Component, ElementRef, Input, ViewChild, ViewEncapsulation } from '@angular/core';
import _ from 'lodash';
import { SortService } from '../../../list/services/sort.service';
import { SessionStorageEntry, SessionStorageFactory } from '../../../storage/session-storage-factory.service';
import { GuacClientManagerService } from '../../services/guac-client-manager.service';
import { ManagedClientGroup } from '../../types/ManagedClientGroup';
import { ManagedClientState } from '../../types/ManagedClientState';

/**
 * A toolbar/panel which displays a list of active Guacamole connections. The
 * panel is fixed to the bottom-right corner of its container and can be
 * manually hidden/exposed by the user.
 */
@Component({
    selector: 'guac-client-panel',
    templateUrl: './guac-client-panel.component.html',
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class GuacClientPanelComponent implements AfterViewInit {

    /**
     * The ManagedClientGroup instances associated with the active
     * connections to be displayed within this panel.
     */
    @Input({ required: true }) clientGroups!: ManagedClientGroup[];

    /**
     * Getter/setter for the boolean flag controlling whether the client panel
     * is currently hidden. This flag is maintained in session-local storage to
     * allow the state of the panel to persist despite navigation within the
     * same tab. When hidden, the panel will be collapsed against the right
     * side of the container. By default, the panel is visible.
     */
    panelHidden: SessionStorageEntry<boolean> = this.sessionStorageFactory.create(false);

    /**
     * Reference to the DOM element containing the scrollable portion of the client
     * panel.
     */
    @ViewChild('clientPanelConnectionList') private scrollableAreaRef?: ElementRef<HTMLUListElement>;

    /**
     * The DOM element containing the scrollable portion of the client
     * panel.
     */
    scrollableArea?: HTMLUListElement;

    /**
     * Inject required services.
     */
    constructor(private sessionStorageFactory: SessionStorageFactory,
                private guacClientManager: GuacClientManagerService,
                protected sortService: SortService) {
    }

    ngAfterViewInit(): void {
        this.scrollableArea = this.scrollableAreaRef?.nativeElement;

        const scrollableArea = this.scrollableArea;

        // Override vertical scrolling, scrolling horizontally instead
        scrollableArea?.addEventListener('wheel', function reorientVerticalScroll(e) {

            const deltaMultiplier = {
                /* DOM_DELTA_PIXEL */ 0x00: 1,
                /* DOM_DELTA_LINE  */ 0x01: 15,
                /* DOM_DELTA_PAGE  */ 0x02: scrollableArea.offsetWidth
            } as any;

            if (e.deltaY) {
                this.scrollLeft += e.deltaY * (deltaMultiplier[e.deltaMode] || deltaMultiplier(0x01));
                e.preventDefault();
            }

        });
    }

    /**
     * Returns whether this panel currently has any client groups
     * associated with it.
     *
     * @return
     *     true if at least one client group is associated with this
     *     panel, false otherwise.
     */
    hasClientGroups(): boolean {
        return this.clientGroups && this.clientGroups.length > 0;
    }

    /**
     * @borrows ManagedClientGroup.getIdentifier
     */
    getIdentifier(group: ManagedClientGroup | string[]): string {
        return ManagedClientGroup.getIdentifier(group);
    }

    /**
     * @borrows ManagedClientGroup.getTitle
     */
    getTitle(group: ManagedClientGroup): string | undefined {
        return ManagedClientGroup.getTitle(group);
    }

    /**
     * Returns whether the status of any client within the given client
     * group has changed in a way that requires the user's attention.
     * This may be due to an error, or due to a server-initiated
     * disconnect.
     *
     * @param clientGroup
     *     The client group to test.
     *
     * @returns
     *     true if the given client requires the user's attention,
     *     false otherwise.
     */
    hasStatusUpdate(clientGroup: ManagedClientGroup): boolean {
        return _.findIndex(clientGroup.clients, (client) => {

            // Test whether the client has encountered an error
            switch (client.clientState.connectionState) {
                // TODO: case ManagedClientState.ConnectionState.CONNECTION_ERROR:
                // I cant find a reference to the above state. Should this be something else or should the state be
                // added?
                case ManagedClientState.ConnectionState.TUNNEL_ERROR:
                case ManagedClientState.ConnectionState.DISCONNECTED:
                    return true;
            }

            return false;

        }) !== -1;
    }

    /**
     * Initiates an orderly disconnect of all clients within the given
     * group. The clients are removed from management such that
     * attempting to connect to any of the same connections will result
     * in new connections being established, rather than displaying a
     * notification that the connection has ended.
     *
     * @param clientGroup
     *     The group of clients to disconnect.
     */
    disconnect(clientGroup: ManagedClientGroup): void {
        this.guacClientManager.removeManagedClientGroup(ManagedClientGroup.getIdentifier(clientGroup));
    }

    /**
     * Toggles whether the client panel is currently hidden.
     */
    togglePanel(): void {
        this.panelHidden(!this.panelHidden());
    }
}
