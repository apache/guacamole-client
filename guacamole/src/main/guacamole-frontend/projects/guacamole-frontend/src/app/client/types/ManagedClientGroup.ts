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

import filter from 'lodash/filter';
import findIndex from 'lodash/findIndex';
import isArray from 'lodash/isArray';
import isString from 'lodash/isString';
import map from 'lodash/map';
import { ManagedClient } from './ManagedClient';

export namespace ManagedClientGroup {

    /**
     * A callback that is invoked for a ManagedClient within a ManagedClientGroup.
     *
     * @param client
     *     The relevant ManagedClient.
     *
     * @param row
     *     The row number of the client within the tiled grid, where 0 is the
     *     first row.
     *
     * @param column
     *     The column number of the client within the tiled grid, where 0 is
     *     the first column.
     *
     * @param index
     *     The index of the client within the relevant
     *     {@link ManagedClientGroup#clients} array.
     */
    export type ClientCallback = (client: ManagedClient, row: number, column: number, index: number) => void;

}

/**
 * Serves as a grouping of ManagedClients. Each
 * ManagedClientGroup may be attached, detached, and reattached dynamically
 * from different client views, with its contents automatically displayed
 * in a tiled arrangement if needed.
 *
 * Used by the guacClientManager service.
 */
export class ManagedClientGroup {

    /**
     * The time that this group was last brought to the foreground of
     * the current tab, as the number of milliseconds elapsed since
     * midnight of January 1, 1970 UTC. If the group has not yet been
     * viewed, this will be 0.
     */
    lastUsed: number;

    /**
     * Whether this ManagedClientGroup is currently attached to the client
     * interface (true) or is running in the background (false).
     *
     * @default false
     */
    attached: boolean;

    /**
     * The clients that should be displayed within the client interface
     * when this group is attached.
     *
     * @default []
     */
    clients: ManagedClient[];

    /**
     * The number of rows that should be used when arranging the clients
     * within this group in a grid. By default, this value is automatically
     * calculated from the number of clients.
     */
    rows: number;

    /**
     * The number of columns that should be used when arranging the clients
     * within this group in a grid. By default, this value is automatically
     * calculated from the number of clients.
     */
    columns: number;

    /**
     * Creates a new ManagedClientGroup. This constructor initializes the properties of the
     * new ManagedClientGroup with the corresponding properties of the given template.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     ManagedClientGroup.
     */
    constructor(template: Partial<ManagedClientGroup> = {}) {
        this.lastUsed = template.lastUsed || 0;
        this.attached = template.attached || false;
        this.clients = template.clients || [];
        this.rows = template.rows || ManagedClientGroup.getRows(this);
        this.columns = template.columns || ManagedClientGroup.getColumns(this);
    }

    /**
     * Updates the number of rows and columns stored within the given
     * ManagedClientGroup such that the clients within the group are evenly
     * distributed. This function should be called whenever the size of a
     * group changes.
     *
     * @param group
     *     The ManagedClientGroup that should be updated.
     */
    static recalculateTiles(group: ManagedClientGroup): void {

        const recalculated = new ManagedClientGroup({
            clients: group.clients
        });

        group.rows = recalculated.rows;
        group.columns = recalculated.columns;

    }

    /**
     * Returns the unique ID representing the given ManagedClientGroup or set
     * of client IDs. The ID of a ManagedClientGroup consists simply of the
     * IDs of all its ManagedClients, separated by periods.
     *
     * @param group
     *     The ManagedClientGroup or array of client IDs to determine the
     *     ManagedClientGroup ID of.
     *
     * @returns
     *     The unique ID representing the given ManagedClientGroup, or the
     *     unique ID that would represent a ManagedClientGroup containing the
     *     clients with the given IDs.
     */
    static getIdentifier(group: ManagedClientGroup | string[]): string {

        if (!isArray(group))
            group = map(group.clients, client => client.id);

        return group.join('.');

    }

    /**
     * Returns an array of client identifiers for all clients contained within
     * the given ManagedClientGroup. Order of the identifiers is preserved
     * with respect to the order of the clients within the group.
     *
     * @param group
     *     The ManagedClientGroup to retrieve the client identifiers from,
     *     or its ID.
     *
     * @returns
     *     The client identifiers of all clients contained within the given
     *     ManagedClientGroup.
     */
    static getClientIdentifiers(group: ManagedClientGroup | string): string[] {

        if (isString(group))
            return group.split(/\./);

        return group.clients.map(client => client.id);

    }

    /**
     * Returns the number of columns that should be used to evenly arrange
     * all provided clients in a tiled grid.
     *
     * @param group
     *     The ManagedClientGroup to calculate the number of columns for.
     *
     * @returns
     *     The number of columns that should be used for the grid of
     *     clients.
     */
    static getColumns(group: ManagedClientGroup): number {

        if (!group.clients.length)
            return 0;

        return Math.ceil(Math.sqrt(group.clients.length));

    }

    /**
     * Returns the number of rows that should be used to evenly arrange all
     * provided clients in a tiled grid.
     *
     * @param group
     *     The ManagedClientGroup to calculate the number of rows for.
     *
     * @returns
     *     The number of rows that should be used for the grid of clients.
     */
    static getRows(group: ManagedClientGroup): number {

        if (!group.clients.length)
            return 0;

        return Math.ceil(group.clients.length / ManagedClientGroup.getColumns(group));

    }

    /**
     * Returns the title which should be displayed as the page title if the
     * given client group is attached to the interface.
     *
     * @param group
     *     The ManagedClientGroup to determine the title of.
     *
     * @returns
     *     The title of the given ManagedClientGroup.
     */
    static getTitle(group: ManagedClientGroup): string | undefined {

        // Use client-specific title if only one client
        if (group.clients.length === 1)
            return group.clients[0].title;

        // With multiple clients, somehow combining multiple page titles would
        // be confusing. Instead, use the combined names.
        return ManagedClientGroup.getName(group);

    }

    /**
     * Returns the combined names of all clients within the given
     * ManagedClientGroup, as determined by the names of the associated
     * connections or connection groups.
     *
     * @param group
     *     The ManagedClientGroup to determine the name of.
     *
     * @returns
     *     The combined names of all clients within the given
     *     ManagedClientGroup.
     */
    static getName(group: ManagedClientGroup): string {

        // Generate a name from ONLY the focused clients, unless there are no
        // focused clients
        let relevantClients = filter(group.clients, client => client.clientProperties.focused);
        if (!relevantClients.length)
            relevantClients = group.clients;

        return filter(relevantClients, (client => !!client.name)).map(client => client.name).join(', ') || '...';

    }

    /**
     * Loops through each of the clients associated with the given
     * ManagedClientGroup, invoking the given callback for each client.
     *
     * @param group
     *     The ManagedClientGroup to loop through.
     *
     * @param callback
     *     The callback to invoke for each of the clients within the given
     *     ManagedClientGroup.
     */
    static forEach(group: ManagedClientGroup, callback: ManagedClientGroup.ClientCallback): void {
        let current = 0;
        for (let row = 0; row < group.rows; row++) {
            for (let column = 0; column < group.columns; column++) {

                callback(group.clients[current], row, column, current);
                current++;

                if (current >= group.clients.length)
                    return;

            }
        }
    }

    /**
     * Returns whether the given ManagedClientGroup contains more than one
     * client.
     *
     * @param group
     *     The ManagedClientGroup to test.
     *
     * @returns
     *     true if two or more clients are currently present in the given
     *     group, false otherwise.
     */
    static hasMultipleClients(group: ManagedClientGroup | null): boolean {
        return !!group && group.clients.length > 1;
    }

    /**
     * Returns a two-dimensional array of all ManagedClients within the given
     * group, arranged in the grid defined by {@link ManagedClientGroup#rows}
     * and {@link ManagedClientGroup#columns}. If any grid cell lacks a
     * corresponding client (because the number of clients does not divide
     * evenly into a grid), that cell will be null.
     *
     * For the sake of AngularJS scope watches, the results of calling this
     * function are cached and will always favor modifying an existing array
     * over creating a new array, even for nested arrays.
     *
     * @param group
     *     The ManagedClientGroup defining the tiled grid arrangement of
     *     ManagedClients.
     *
     * @returns
     *     A two-dimensional array of all ManagedClients within the given
     *     group.
     */
    static getClientGrid(group: ManagedClientGroup): ManagedClient[][] {

        let index = 0;

        // Operate on cached copy of grid
        const clientGrid = (group as any)._grid || ((group as any)._grid = []);

        // Delete any rows in excess of the required size
        clientGrid.splice(group.rows);

        for (let row = 0; row < group.rows; row++) {

            // Prefer to use existing column arrays, deleting any columns in
            // excess of the required size
            const currentRow = clientGrid[row] || (clientGrid[row] = []);
            currentRow.splice(group.columns);

            for (let column = 0; column < group.columns; column++) {
                currentRow[column] = group.clients[index++] || null;
            }

        }

        return clientGrid;

    }

    /**
     * Verifies that focus is assigned to at least one client in the given
     * group. If no client has focus, focus is assigned to the first client in
     * the group.
     *
     * @param group
     *     The group to verify.
     */
    static verifyFocus(group: ManagedClientGroup | null): void {

        // Focus the first client if there are no clients focused
        if (group && group.clients.length >= 1 && findIndex(group.clients, client => client.clientProperties.focused) === -1) {
            group.clients[0].clientProperties.focused = true;
        }

    }
}
