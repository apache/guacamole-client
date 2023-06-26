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

import {
    Component,
    Input,
    OnChanges,
    OnInit,
    SimpleChanges,
    TemplateRef,
    ViewChild,
    ViewEncapsulation
} from '@angular/core';
import { ConnectionGroup } from '../../../rest/types/ConnectionGroup';
import { GroupListItem } from '../../types/GroupListItem';
import { ActiveConnectionService } from '../../../rest/service/active-connection.service';
import { DataSourceService } from '../../../rest/service/data-source-service.service';
import { RequestService } from '../../../rest/service/request.service';
import { Connection } from '../../../rest/types/Connection';
import { SortService } from '../../../list/services/sort.service';
import { GuacPagerComponent } from '../../../list/components/guac-pager/guac-pager.component';
import { DataSourceBuilderService } from "../../../list/services/data-source-builder.service";
import { DataSource } from "../../../list/types/DataSource";
import { SortOrder } from "../../../list/types/SortOrder";
import { of } from "rxjs";

/**
 * A component which displays the contents of a connection group within an
 * automatically-paginated view.
 */
@Component({
    selector: 'guac-group-list',
    templateUrl: './guac-group-list.component.html',
    encapsulation: ViewEncapsulation.None
})
export class GuacGroupListComponent implements OnInit, OnChanges {

    /**
     * The connection groups to display as a map of data source
     * identifier to corresponding root group.
     */
    @Input() connectionGroups: Record<string, ConnectionGroup | GroupListItem> | null = null;

    /**
     * Arbitrary object which shall be made available to the connection
     * and connection group templates within the scope as
     * <code>context</code>.
     */
    @Input() context: any = {};

    /**
     * The map of @link{GroupListItem} type to the URL or ID of the
     * Angular template to use when rendering a @link{GroupListItem} of
     * that type. The @link{GroupListItem} itself will be within the
     * scope of the template as <code>item</code>, and the arbitrary
     * context object, if any, will be exposed as <code>context</code>.
     * If the template for a type is omitted, items of that type will
     * not be rendered. All standard types are defined by
     * @link{GroupListItem.Type}, but use of custom types is legal.
     */
    @Input({required: true}) templates!: Record<string, TemplateRef<any>>;

    /**
     * Whether the root of the connection group hierarchy given should
     * be shown. If false (the default), only the descendants of the
     * given connection group will be listed.
     */
    @Input() showRootGroup = false;

    /**
     * The maximum number of connections or groups to show per page.
     */
    @Input() pageSize?: number;

    /**
     * A callback which accepts an array of GroupListItems as its sole
     * parameter. If provided, the callback will be invoked whenever an
     * array of root-level GroupListItems is about to be rendered.
     * Changes may be made by this function to that array or to the
     * GroupListItems themselves.
     */
    @Input() decorator?: (items: GroupListItem[]) => void;

    /**
     * Reference to the instance of the pager component.
     */
    @ViewChild(GuacPagerComponent, {static: true}) pager!: GuacPagerComponent;

    /**
     * Map of data source identifier to the number of active
     * connections associated with a given connection identifier.
     * If this information is unknown, or there are no active
     * connections for a given identifier, no number will be stored.
     */
    private connectionCount: Record<string, Record<string, number>> = {};

    /**
     * A list of all items which should appear at the root level. As
     * connections and connection groups from multiple data sources may
     * be included in a guacGroupList, there may be multiple root
     * items, even if the root connection group is shown.
     */
    rootItems: GroupListItem[] = [];

    /**
     * A data source which provides a sorted, paginated list of
     * all root-level items.
     */
    connectionGroupsDataSource: DataSource<GroupListItem> | null = null;

    /**
     * The sort order to apply to the root items.
     */
    private readonly sortOrder = new SortOrder(['weight', 'name']);

    /**
     * Inject required services.
     */
    constructor(private activeConnectionService: ActiveConnectionService,
                private dataSourceService: DataSourceService,
                private requestService: RequestService,
                private dataSourceBuilderService: DataSourceBuilderService,
                protected sortService: SortService) {
    }

    /**
     * Creates a new data source for the root items.
     */
    ngOnInit(): void {

        this.connectionGroupsDataSource = this.dataSourceBuilderService
            .getBuilder<GroupListItem>()
            // Start with an empty list
            .source(this.rootItems)
            // Sort according to the specified sort order
            .sort(of(this.sortOrder))
            // Paginate using the GuacPagerComponent
            .paginate(this.pager.page)
            .build();

    }

    /**
     * Returns the number of active usages of a given connection.
     *
     * @param dataSource
     *     The identifier of the data source containing the given
     *     connection.
     *
     * @param connection
     *     The connection whose active connections should be counted.
     *
     * @returns
     *     The number of currently-active usages of the given
     *     connection.
     */
    private countActiveConnections(dataSource: string, connection: Connection): number {
        if (this.connectionCount[dataSource] === undefined
            || connection.identifier === undefined
            || this.connectionCount[dataSource][connection.identifier] === undefined)
            return 0;

        return (this.connectionCount)[dataSource][connection.identifier];
    }

    /**
     * Returns whether a {@link GroupListItem} of the given type can be
     * displayed. If there is no template associated with the given
     * type, then a {@link GroupListItem} of that type cannot be
     * displayed.
     *
     * @param type
     *     The type to check.
     *
     * @returns
     *     true if the given {@link GroupListItem} type can be displayed,
     *     false otherwise.
     */
    isVisible(type: string): boolean {
        return !!this.templates[type];
    }

    /**
     * Toggle the open/closed status of a group list item.
     *
     * @param groupListItem
     *     The list item to expand, which should represent a
     *     connection group.
     */
    toggleExpanded(groupListItem: GroupListItem): void {
        groupListItem.expanded = !groupListItem.expanded;
    }

    /**
     * Set contents whenever the connection group is assigned or changed.
     */
    ngOnChanges(changes: SimpleChanges): void {
        if (!changes['connectionGroups'])
            return;

        // Reset stored data
        const dataSources: string[] = [];
        this.rootItems = [];
        this.connectionCount = {};

        // If connection groups are given, add them to the interface
        if (this.connectionGroups) {

            // Add each provided connection group
            for (const dataSource in this.connectionGroups) {
                const connectionGroup = this.connectionGroups[dataSource];

                let rootItem: GroupListItem;

                // Prepare data source for active connection counting
                dataSources.push(dataSource);
                this.connectionCount[dataSource] = {};

                // If the provided connection group is already a
                // GroupListItem, no need to create a new item
                if (connectionGroup instanceof GroupListItem)
                    rootItem = connectionGroup;

                // Create root item for current connection group
                else
                    rootItem = GroupListItem.fromConnectionGroup(dataSource, connectionGroup,
                        this.isVisible(GroupListItem.Type.CONNECTION),
                        this.isVisible(GroupListItem.Type.SHARING_PROFILE),
                        this.countActiveConnections.bind(this));

                // If root group is to be shown, add it as a root item
                if (this.showRootGroup)
                    this.rootItems.push(rootItem);

                // Otherwise, add its children as root items
                else {
                    rootItem.children.forEach(child => {
                        this.rootItems.push(child);
                    });
                }

            }

            // Count active connections by connection identifier
            this.dataSourceService.apply(
                (dataSource: string) => this.activeConnectionService.getActiveConnections(dataSource),
                dataSources
            )
                .then((activeConnectionMap) => {

                    // Within each data source, count each active connection by identifier
                    for (const dataSource in activeConnectionMap) {
                        const activeConnections = activeConnectionMap[dataSource];

                        for (const connectionID in activeConnections) {
                            const activeConnection = activeConnections[connectionID];

                            // If counter already exists, increment
                            const identifier = activeConnection.connectionIdentifier!;
                            if (this.connectionCount[dataSource][identifier])
                                this.connectionCount[dataSource][identifier]++;

                            // Otherwise, initialize counter to 1
                            else
                                this.connectionCount[dataSource][identifier] = 1;

                        }
                    }

                }, this.requestService.PROMISE_DIE);

        }

        // Invoke item decorator, if provided
        if (this.decorator)
            this.decorator(this.rootItems);

        // Update the data source with the new root items
        this.connectionGroupsDataSource?.updateSource(this.rootItems);

    }


}
