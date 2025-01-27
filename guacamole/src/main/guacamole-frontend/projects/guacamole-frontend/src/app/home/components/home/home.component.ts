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

import { Component, OnInit, ViewChild, ViewEncapsulation } from '@angular/core';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import {
    GuacGroupListFilterComponent
} from '../../../group-list/components/guac-group-list-filter/guac-group-list-filter.component';
import { ConnectionGroupDataSource } from '../../../group-list/types/ConnectionGroupDataSource';
import { FilterService } from '../../../list/services/filter.service';
import { ConnectionGroupService } from '../../../rest/service/connection-group.service';
import { DataSourceService } from '../../../rest/service/data-source-service.service';
import { RequestService } from '../../../rest/service/request.service';
import { ConnectionGroup } from '../../../rest/types/ConnectionGroup';
import { PreferenceService } from '../../../settings/services/preference.service';
import { NonNullableProperties } from '../../../util/utility-types';

/**
 * The component for the home page.
 */
@Component({
    selector: 'guac-home',
    templateUrl: './home.component.html',
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class HomeComponent implements OnInit {

    /**
     * Map of data source identifier to the root connection group of that data
     * source, or null if the connection group hierarchy has not yet been
     * loaded.
     */
    rootConnectionGroups: Record<string, ConnectionGroup> | null = null;

    /**
     * Reference to the instance of the filter component.
     */
    @ViewChild(GuacGroupListFilterComponent, { static: true }) filter!: GuacGroupListFilterComponent;

    /**
     * TODO
     */
    rootConnectionGroupsDataSource: ConnectionGroupDataSource | null = null;

    /**
     * Array of all connection properties that are filterable.
     */
    readonly filteredConnectionProperties: string[] = [
        'name'
    ];

    /**
     * Array of all connection group properties that are filterable.
     */
    readonly filteredConnectionGroupProperties: string[] = [
        'name'
    ];

    /**
     * Inject required services.
     */
    constructor(private authenticationService: AuthenticationService,
                private connectionGroupService: ConnectionGroupService,
                private dataSourceService: DataSourceService,
                private requestService: RequestService,
                private preferenceService: PreferenceService,
                private filterService: FilterService) {
    }

    ngOnInit(): void {

        // Create a new data source for the root connection groups
        this.rootConnectionGroupsDataSource = new ConnectionGroupDataSource(this.filterService,
            {},
            this.filter.searchStringChange,
            this.filteredConnectionProperties,
            this.filteredConnectionGroupProperties);

        // Retrieve root groups and all descendants
        this.dataSourceService.apply(
            (dataSource: string, connectionGroupID: string) => this.connectionGroupService.getConnectionGroupTree(dataSource, connectionGroupID),
            this.authenticationService.getAvailableDataSources(),
            ConnectionGroup.ROOT_IDENTIFIER
        )
            .then(rootConnectionGroups => {
                this.rootConnectionGroups = rootConnectionGroups;
                this.rootConnectionGroupsDataSource?.updateSource(this.rootConnectionGroups);
            }, this.requestService.PROMISE_DIE);
    }


    /**
     * Returns whether the "Recent Connections" section should be displayed on
     * the home screen.
     *
     * @returns
     *     true if recent connections should be displayed on the home screen,
     *     false otherwise.
     */
    isRecentConnectionsVisible(): boolean {
        return this.preferenceService.preferences.showRecentConnections;
    }

    /**
     * Returns whether critical data has completed being loaded.
     *
     * @returns
     *     true if enough data has been loaded for the user interface to be
     *     useful, false otherwise.
     */
    isLoaded(): this is NonNullableProperties<HomeComponent, 'rootConnectionGroups' | 'rootConnectionGroupsDataSource'> {

        return this.rootConnectionGroups !== null
            && this.rootConnectionGroupsDataSource !== null;

    }
}
