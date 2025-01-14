

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

import { DOCUMENT } from '@angular/common';
import { Inject, Injectable } from '@angular/core';
import find from 'lodash/find';
import findIndex from 'lodash/findIndex';
import remove from 'lodash/remove';
import { SessionStorageEntry, SessionStorageFactory } from '../../storage/session-storage-factory.service';
import { ManagedClient } from '../types/ManagedClient';
import { ManagedClientGroup } from '../types/ManagedClientGroup';
import { ManagedClientService } from './managed-client.service';

/**
 * A service for managing several active Guacamole clients.
 */
@Injectable({
    providedIn: 'root'
})
export class GuacClientManagerService {

    /**
     * Getter/setter which retrieves or sets the map of all active managed
     * clients. Each key is the ID of the connection used by that client.
     */
    private readonly storedManagedClients: SessionStorageEntry<Record<string, ManagedClient>> = this.sessionStorageFactory.create({}, () => {

        // Disconnect all clients when storage is destroyed
        this.clear();

    });

    /**
     * Getter/setter which retrieves or sets the array of all active managed
     * client groups.
     */
    private readonly storedManagedClientGroups: SessionStorageEntry<ManagedClientGroup[]> = this.sessionStorageFactory.create([] as ManagedClientGroup[], () => {

        // Disconnect all clients when storage is destroyed
        this.clear();

    });

    /**
     * Inject required services.
     */
    constructor(private sessionStorageFactory: SessionStorageFactory,
                private managedClientService: ManagedClientService) {

        // Disconnect all clients when window is unloaded
        window.addEventListener('unload', () => this.clear());

    }

    /**
     * Returns a map of all active managed clients. Each key is the ID of the
     * connection used by that client.
     *
     * @returns
     *     A map of all active managed clients.
     */
    getManagedClients(): Record<string, ManagedClient> {
        return this.storedManagedClients();
    }

    /**
     * Returns an array of all managed client groups.
     *
     * @returns
     *     An array of all active managed client groups.
     */
    getManagedClientGroups(): ManagedClientGroup[] {
        return this.storedManagedClientGroups();
    }

    /**
     * Removes the ManagedClient with the given ID from all
     * ManagedClientGroups, automatically adjusting the tile size of the
     * clients that remain in each group. All client groups that are empty
     * after the client is removed will also be removed.
     *
     * @param id
     *     The ID of the ManagedClient to remove.
     */
    private ungroupManagedClient(id: string): void {

        const managedClientGroups: ManagedClientGroup[] = this.storedManagedClientGroups();

        // Remove client from all groups
        managedClientGroups.forEach(group => {
            const removed = remove(group.clients, client => (client.id === id));
            if (removed.length) {

                // Reset focus state if client is being removed from a group
                // that isn't currently attached (focus may otherwise be
                // retained and result in a newly added connection unexpectedly
                // sharing focus)
                if (!group.attached)
                    removed.forEach(client => {
                        client.clientProperties.focused = false;
                    });

                // Recalculate group grid if number of clients is changing
                ManagedClientGroup.recalculateTiles(group);

            }
        });

        // Remove any groups that are now empty
        remove(managedClientGroups, group => !group.clients.length);

    }

    /**
     * Removes the existing ManagedClient associated with the connection having
     * the given ID, if any. If no such a ManagedClient already exists, this
     * function has no effect.
     *
     * @param id
     *     The ID of the connection whose ManagedClient should be removed.
     *
     * @returns
     *     true if an existing client was removed, false otherwise.
     */
    removeManagedClient(id: string): boolean {

        const managedClients: Record<string, ManagedClient> = this.storedManagedClients();

        // Remove client if it exists
        if (id in managedClients) {

            // Pull client out of any containing groups
            this.ungroupManagedClient(id);

            // Disconnect and remove
            managedClients[id].client.disconnect();
            delete managedClients[id];

            // A client was removed
            return true;

        }

        // No client was removed
        return false;

    }

    /**
     * Creates a new ManagedClient associated with the connection having the
     * given ID. If such a ManagedClient already exists, it is disconnected and
     * replaced.
     *
     * @param id
     *     The ID of the connection whose ManagedClient should be retrieved.
     *
     * @returns
     *     The ManagedClient associated with the connection having the given
     *     ID.
     */
    replaceManagedClient(id: string): ManagedClient {

        const managedClients: Record<string, ManagedClient> = this.storedManagedClients();
        const managedClientGroups: ManagedClientGroup[] = this.storedManagedClientGroups();

        // Remove client if it exists
        if (id in managedClients) {

            const hadFocus = managedClients[id].clientProperties.focused;
            managedClients[id].client.disconnect();
            delete managedClients[id];

            // Remove client from all groups
            managedClientGroups.forEach(group => {

                const index = findIndex(group.clients, client => (client.id === id));
                if (index === -1)
                    return;

                group.clients[index] = managedClients[id] = this.managedClientService.getInstance(id);
                managedClients[id].clientProperties.focused = hadFocus;

            });

        }

        return managedClients[id];

    }

    /**
     * Returns the ManagedClient associated with the connection having the
     * given ID. If no such ManagedClient exists, a new ManagedClient is
     * created.
     *
     * @param id
     *     The ID of the connection whose ManagedClient should be retrieved.
     *
     * @returns
     *     The ManagedClient associated with the connection having the given
     *     ID.
     */
    getManagedClient(id: string): ManagedClient {

        const managedClients = this.storedManagedClients();

        // Ensure any existing client is removed from its containing group
        // prior to being returned
        this.ungroupManagedClient(id);

        // Create new managed client if it doesn't already exist
        if (!(id in managedClients))
            managedClients[id] = this.managedClientService.getInstance(id);

        // Return existing client
        return managedClients[id];

    }

    /**
     * Returns the ManagedClientGroup having the given ID. If no such
     * ManagedClientGroup exists, a new ManagedClientGroup is created by
     * extracting the relevant connections from the ID.
     *
     * @param id
     *     The ID of the ManagedClientGroup to retrieve or create.
     *
     * @returns
     *     The ManagedClientGroup having the given ID.
     */
    getManagedClientGroup(id: string): ManagedClientGroup {

        const managedClientGroups: ManagedClientGroup[] = this.storedManagedClientGroups();
        const existingGroup = find(managedClientGroups, (group) => {
            return id === ManagedClientGroup.getIdentifier(group);
        });

        // Prefer to return the existing group if it exactly matches
        if (existingGroup)
            return existingGroup;

        const clients: ManagedClient[] = [];
        const clientIds = ManagedClientGroup.getClientIdentifiers(id);

        // Separate active clients by whether they should be displayed within
        // the current view
        clientIds.forEach(id => {
            clients.push(this.getManagedClient(id));
        });

        const group = new ManagedClientGroup({
            clients: clients
        });

        // Focus the first client if there are no clients focused
        ManagedClientGroup.verifyFocus(group);

        managedClientGroups.push(group);
        return group;

    }

    /**
     * Removes the existing ManagedClientGroup having the given ID, if any,
     * disconnecting and removing all ManagedClients associated with that
     * group. If no such a ManagedClientGroup currently exists, this function
     * has no effect.
     *
     * @param id
     *     The ID of the ManagedClientGroup to remove.
     *
     * @returns
     *     true if a ManagedClientGroup was removed, false otherwise.
     */
    removeManagedClientGroup(id: string): boolean {

        const managedClients: Record<string, ManagedClient> = this.storedManagedClients();
        const managedClientGroups: ManagedClientGroup[] = this.storedManagedClientGroups();

        // Remove all matching groups (there SHOULD only be one)
        const removed = remove(managedClientGroups, (group) => ManagedClientGroup.getIdentifier(group) === id);

        // Disconnect all clients associated with the removed group(s)
        removed.forEach((group) => {
            group.clients.forEach((client) => {

                const id = client.id;
                if (managedClients[id]) {
                    managedClients[id].client.disconnect();
                    delete managedClients[id];
                }

            });
        });

        return !!removed.length;

    }

    /**
     * Disconnects and removes all currently-connected clients and client
     * groups.
     */
    clear(): void {

        const managedClients: Record<string, ManagedClient> = this.storedManagedClients();

        // Disconnect each managed client
        for (const id in managedClients)
            managedClients[id].client.disconnect();

        // Clear managed clients and client groups
        this.storedManagedClients({});
        this.storedManagedClientGroups([]);

    }

}
