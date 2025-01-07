

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

import { Component, Input, OnChanges, OnInit, SimpleChanges, ViewEncapsulation } from '@angular/core';
import { ConnectionGroup } from '../../../rest/types/ConnectionGroup';

/**
 * A component for choosing the location of a connection or connection group.
 */
@Component({
    selector     : 'guac-location-chooser',
    templateUrl  : './location-chooser.component.html',
    encapsulation: ViewEncapsulation.None
})
export class LocationChooserComponent implements OnInit, OnChanges {

    /**
     * The identifier of the data source from which the given root
     * connection group was retrieved.
     */
    @Input() dataSource?: string;

    /**
     * The root connection group of the connection group hierarchy to
     * display.
     */
    @Input({ required: true }) rootGroup!: ConnectionGroup;

    /**
     * The unique identifier of the currently-selected connection
     * group. If not specified, the root group will be used.
     */
    @Input() value?: string;

    /**
     * Map of unique identifiers to their corresponding connection
     * groups.
     */
    private connectionGroups: Record<string, ConnectionGroup> = {};

    /**
     * Whether the group list menu is currently open.
     */
    menuOpen = false;

    /**
     * The human-readable name of the currently-chosen connection
     * group.
     */
    chosenConnectionGroupName: string | null = null;

    /**
     * Map of the data source identifier to the root connection group.
     */
    rootGroups: Record<string, ConnectionGroup> = {};

    /**
     * Recursively traverses the given connection group and all
     * children, storing each encountered connection group within the
     * connectionGroups map by its identifier.
     *
     * @param group
     *     The connection group to traverse.
     */
    private mapConnectionGroups(group: ConnectionGroup): void {

        // Map given group
        (this.connectionGroups)[group.identifier!] = group;

        // Map all child groups
        if (group.childConnectionGroups)
            group.childConnectionGroups.forEach((childGroup: ConnectionGroup) => this.mapConnectionGroups(childGroup));

    }

    /**
     * Toggle the current state of the menu listing connection groups.
     * If the menu is currently open, it will be closed. If currently
     * closed, it will be opened.
     */
    toggleMenu(): void {
        this.menuOpen = !this.menuOpen;
    }

    ngOnInit(): void {
        this.onDataSourceChange();
    }

    ngOnChanges(changes: SimpleChanges): void {

        // Update the root group map when data source or root group change
        if (changes['dataSource'] || changes['rootGroup']) {

            // Abort if the root group is not set
            if (this.dataSource && this.rootGroup) {

                // Wrap root group in map
                this.rootGroups = {};
                this.rootGroups[this.dataSource] = this.rootGroup;
            }

        }

        if (changes['dataSource']) {
            this.onDataSourceChange();
        }

    }

    onDataSourceChange(): void {
        this.connectionGroups = {};

        if (this.rootGroup) {

            // Map all known groups
            this.mapConnectionGroups(this.rootGroup);

            // If no value is specified, default to the root identifier
            if (!this.value || !(this.value in this.connectionGroups))
                this.value = this.rootGroup.identifier!;

            this.chosenConnectionGroupName = this.connectionGroups[this.value].name;
        }
    }


    /**
     * Expose selection function to group list template.
     */
    groupListContext = {

        /**
         * Selects the given group item.
         *
         * @param item
         *     The chosen item.
         */
        chooseGroup: (item: ConnectionGroup) => {

            // Record new parent
            this.value = item.identifier;
            this.chosenConnectionGroupName = item.name;

            // Close menu
            this.menuOpen = false;

        }

    };

}
