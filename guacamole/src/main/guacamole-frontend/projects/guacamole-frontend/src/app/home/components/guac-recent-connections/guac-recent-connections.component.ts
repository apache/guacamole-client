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

import { Component, Input, OnChanges, SimpleChanges, ViewEncapsulation } from '@angular/core';
import { GuacHistoryService } from '../../../history/guac-history.service';
import { ConnectionGroup } from '../../../rest/types/ConnectionGroup';
import { RecentConnection } from '../../types/RecentConnection';
import { Connection } from '../../../rest/types/Connection';
import { ClientIdentifier } from '../../../navigation/types/ClientIdentifier';
import { ClientIdentifierService } from '../../../navigation/service/client-identifier.service';

/**
 * A component which displays the recently-accessed connections nested beneath
 * each of the given connection groups.
 */
@Component({
    selector: 'guac-recent-connections',
    templateUrl: './guac-recent-connections.component.html',
    encapsulation: ViewEncapsulation.None
})
export class GuacRecentConnectionsComponent implements OnChanges {

    /**
     * The root connection groups to display, and all visible
     * descendants, as a map of data source identifier to the root
     * connection group within that data source. Recent connections
     * will only be shown if they exist within this hierarchy,
     * regardless of their existence within the history.
     */
    @Input() rootGroups: Record<string, ConnectionGroup> = {};

    /**
     * Array of all known and visible recently-used connections.
     */
    recentConnections: RecentConnection[] = [];

    /**
     * Map of all visible objects, connections or connection groups, by
     * object identifier.
     */
    visibleObjects: Record<string, Connection | ConnectionGroup> = {};

    /**
     * Inject required services.
     */
    constructor(private guacHistoryService: GuacHistoryService,
                private clientIdentifierService: ClientIdentifierService) {
    }

    /**
     * Returns whether recent connections are available for display.
     *
     * @returns
     *     true if recent connections are present, false otherwise.
     */
    hasRecentConnections(): boolean {
        return !!this.recentConnections.length;
    }

    /**
     * Adds the given connection to the internal set of visible
     * objects.
     *
     * @param dataSource
     *     The identifier of the data source associated with the
     *     given connection group.
     *
     * @param connection
     *     The connection to add to the internal set of visible objects.
     */
    addVisibleConnection(dataSource: string, connection: Connection): void {

        // Add given connection to set of visible objects
        this.visibleObjects[this.clientIdentifierService.getString({
            dataSource: dataSource,
            type: ClientIdentifier.Types.CONNECTION,
            id: connection.identifier
        })] = connection;

    }

    /**
     * Adds the given connection group to the internal set of visible
     * objects, along with any descendants.
     *
     * @param dataSource
     *     The identifier of the data source associated with the
     *     given connection group.
     *
     * @param connectionGroup
     *     The connection group to add to the internal set of visible
     *     objects, along with any descendants.
     */
    addVisibleConnectionGroup(dataSource: string, connectionGroup: ConnectionGroup): void {

        // Add given connection group to set of visible objects
        (this.visibleObjects)[this.clientIdentifierService.getString({
            dataSource: dataSource,
            type: ClientIdentifier.Types.CONNECTION_GROUP,
            id: connectionGroup.identifier
        })] = connectionGroup;

        // Add all child connections
        if (connectionGroup.childConnections)
            connectionGroup.childConnections.forEach(child => {
                this.addVisibleConnection(dataSource, child);
            });

        // Add all child connection groups
        if (connectionGroup.childConnectionGroups)
            connectionGroup.childConnectionGroups.forEach(child => {
                this.addVisibleConnectionGroup(dataSource, child);
            });

    }

    /**
     * Update visible objects when root groups are set
     */
    ngOnChanges(changes: SimpleChanges): void {

        if (changes['rootGroups']) {
            const rootGroups = changes['rootGroups'].currentValue as Record<string, ConnectionGroup>;

            // Clear connection arrays
            this.recentConnections = [];

            // Produce collection of visible objects
            this.visibleObjects = {};
            if (rootGroups) {

                for (const dataSource in rootGroups) {
                    const rootGroup = rootGroups[dataSource];
                    this.addVisibleConnectionGroup(dataSource, rootGroup);
                }

            }

            // Add any recent connections that are visible
            this.guacHistoryService.recentConnections.forEach(historyEntry => {

                // Add recent connections for history entries with associated visible objects
                if (historyEntry.id in this.visibleObjects) {

                    const object = this.visibleObjects[historyEntry.id];
                    this.recentConnections.push(new RecentConnection(object.name, historyEntry));

                }

            });
        }

    }


}
