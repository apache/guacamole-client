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

import { Component, Input, OnChanges, OnInit, SimpleChanges, ViewChild, ViewEncapsulation } from '@angular/core';
import {
    GuacGroupListFilterComponent
} from '../../../group-list/components/guac-group-list-filter/guac-group-list-filter.component';
import { ConnectionGroupDataSource } from '../../../group-list/types/ConnectionGroupDataSource';
import { GroupListItem } from '../../../group-list/types/GroupListItem';
import { FilterService } from '../../../list/services/filter.service';
import { ConnectionGroupService } from '../../../rest/service/connection-group.service';
import { DataSourceService } from '../../../rest/service/data-source-service.service';
import { RequestService } from '../../../rest/service/request.service';
import { ConnectionGroup } from '../../../rest/types/ConnectionGroup';
import { PermissionFlagSet } from '../../../rest/types/PermissionFlagSet';
import { PermissionSet } from '../../../rest/types/PermissionSet';
import { ConnectionListContext } from '../../types/ConnectionListContext';

/**
 * A component for manipulating the connection permissions granted within a
 * given {@link PermissionFlagSet}, tracking the specific permissions added or
 * removed within a separate pair of {@link PermissionSet} objects.
 */
@Component({
    selector: 'connection-permission-editor',
    templateUrl: './connection-permission-editor.component.html',
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ConnectionPermissionEditorComponent implements OnInit, OnChanges {

    /**
     * The unique identifier of the data source associated with the
     * permissions being manipulated.
     */
    @Input({ required: true }) dataSource!: string;

    /**
     * The current state of the permissions being manipulated. This
     * {@link PermissionFlagSet} will be modified as changes are made
     * through this permission editor.
     */
    @Input({ required: true }) permissionFlags: PermissionFlagSet | null = null;

    /**
     * The set of permissions that have been added, relative to the
     * initial state of the permissions being manipulated.
     */
    @Input({ required: true }) permissionsAdded!: PermissionSet;

    /**
     * The set of permissions that have been removed, relative to the
     * initial state of the permissions being manipulated.
     */
    @Input({ required: true }) permissionsRemoved!: PermissionSet;

    /**
     * Reference to the instance of the filter component.
     */
    @ViewChild(GuacGroupListFilterComponent, { static: true }) filter!: GuacGroupListFilterComponent;

    /**
     * Filtered view of the root connection groups which satisfy the current
     * search string.
     */
    rootGroupsDataSource: ConnectionGroupDataSource | null = null;

    /**
     * A map of data source identifiers to all root connection groups
     * within those data sources, regardless of the permissions granted for
     * the items within those groups. As only one data source is applicable
     * to any particular permission set being edited/created, this will only
     * contain a single key. If the data necessary to produce this map has
     * not yet been loaded, this will be null.
     */
    private allRootGroups: Record<string, GroupListItem> | null = null;

    /**
     * A map of data source identifiers to the root connection groups within
     * those data sources, excluding all items which are not explicitly
     * readable according to this.permissionFlags. As only one data
     * source is applicable to any particular permission set being
     * edited/created, this will only contain a single key. If the data
     * necessary to produce this map has not yet been loaded, this will be
     * null.
     */
    private readableRootGroups: Record<string, GroupListItem | undefined> | null = null;

    /**
     * A map of data source identifiers to the root connection groups within
     * those data sources for which we have ADMINISTER permission. If the data
     * necessary to produce this map has not yet been loaded, this will be
     * null.
     */
    private administeredRootGroups: Record<string, ConnectionGroup> | null = null;

    /**
     * The name of the tab within the connection permission editor which
     * displays currently selected (readable) connections only.
     *
     * @constant
     */
    private readonly CURRENT_CONNECTIONS: string = 'CURRENT_CONNECTIONS';

    /**
     * The name of the tab within the connection permission editor which
     * displays all connections, regardless of whether they are readable.
     *
     * @constant
     */
    private readonly ALL_CONNECTIONS: string = 'ALL_CONNECTIONS';

    /**
     * The names of all tabs which should be available within the
     * connection permission editor, in display order.
     */
    tabs: string[] = [
        this.CURRENT_CONNECTIONS,
        this.ALL_CONNECTIONS
    ];

    /**
     * The name of the currently selected tab.
     */
    currentTab: string = this.ALL_CONNECTIONS;

    /**
     * Array of all connection properties that are filterable.
     */
    filteredConnectionProperties: string[] = [
        'name',
        'protocol'
    ];

    /**
     * Array of all connection group properties that are filterable.
     */
    filteredConnectionGroupProperties: string[] = [
        'name'
    ];

    /**
     * Inject required services.
     */
    constructor(
        private connectionGroupService: ConnectionGroupService,
        private dataSourceService: DataSourceService,
        private requestService: RequestService,
        private filterService: FilterService
    ) {
    }

    ngOnInit(): void {

        // Create a new data source for the root connection groups
        this.rootGroupsDataSource = new ConnectionGroupDataSource(this.filterService,
            {},
            this.filter.searchStringChange,
            this.filteredConnectionProperties,
            this.filteredConnectionGroupProperties);

        // Retrieve all connections for which we have ADMINISTER permission
        this.dataSourceService.apply(
            (dataSource: string, connectionGroupID: string, permissionTypes: string[]) => this.connectionGroupService.getConnectionGroupTree(dataSource, connectionGroupID, permissionTypes),
            [this.dataSource],
            ConnectionGroup.ROOT_IDENTIFIER,
            [PermissionSet.ObjectPermissionType.ADMINISTER]
        )
            .then((rootGroups) => {

                this.administeredRootGroups = rootGroups;
                this.updateDefaultExpandedStates();
                this.rootGroupsDataSource?.updateSource(this.getRootGroups());

            }, this.requestService.DIE);

    }

    ngOnChanges(changes: SimpleChanges): void {

        // Update default expanded state and the all / readable-only views
        // when associated permissions change
        if (changes['permissionFlags']) {
            this.updateDefaultExpandedStates();
        }

    }

    /**
     * Update default expanded state and the all / readable-only views.
     */
    private updateDefaultExpandedStates(): void {

        if (!this.permissionFlags)
            return;

        this.allRootGroups = {};
        this.readableRootGroups = {};

        for (let dataSource in this.administeredRootGroups) {
            const rootGroup = this.administeredRootGroups[dataSource];

            // Convert all received ConnectionGroup objects into GroupListItems
            const item = GroupListItem.fromConnectionGroup(dataSource, rootGroup);
            this.allRootGroups[dataSource] = item;

            // Automatically expand all objects with any descendants for
            // which the permission set contains READ permission
            this.expandReadable(item, this.permissionFlags);

            // Create a duplicate view which contains only readable
            // items
            this.readableRootGroups[dataSource] = this.copyReadable(item, this.permissionFlags);

        }

        // Display only readable connections by default if at least one
        // readable connection exists
        this.currentTab = !!this.readableRootGroups[this.dataSource]?.children.length ? this.CURRENT_CONNECTIONS : this.ALL_CONNECTIONS;

    }

    /**
     * Update the list data source when the selected tab changes.
     */
    onCurrentTabChange(): void {
        this.rootGroupsDataSource?.updateSource(this.getRootGroups());
    }

    /**
     * Returns the root groups which should be displayed within the
     * connection permission editor.
     *
     * @returns
     *     The root groups which should be displayed within the connection
     *     permission editor as a map of data source identifiers to the
     *     root connection groups within those data sources.
     */
    getRootGroups(): Record<string, GroupListItem> {

        const rootGroups = this.currentTab === this.CURRENT_CONNECTIONS ? this.readableRootGroups : this.allRootGroups;

        if (rootGroups === null) {
            return {};
        }

        return rootGroups as Record<string, GroupListItem>;

    }

    /**
     * Returns whether the given PermissionFlagSet declares explicit READ
     * permission for the connection, connection group, or sharing profile
     * represented by the given GroupListItem.
     *
     * @param item
     *     The GroupListItem which should be checked against the
     *     PermissionFlagSet.
     *
     * @param flags
     *     The set of permissions which should be used to determine whether
     *     explicit READ permission is granted for the given item.
     *
     * @returns
     *     true if explicit READ permission is granted for the given item
     *     according to the given permission set, false otherwise.
     */
    private isReadable(item: GroupListItem, flags: PermissionFlagSet): boolean {

        switch (item.type) {

            case GroupListItem.Type.CONNECTION:
                return flags.connectionPermissions['READ'][item.identifier!];

            case GroupListItem.Type.CONNECTION_GROUP:
                return flags.connectionGroupPermissions['READ'][item.identifier!];

            case GroupListItem.Type.SHARING_PROFILE:
                return flags.sharingProfilePermissions['READ'][item.identifier!];

        }

        return false;

    }

    /**
     * Expands all items within the tree descending from the given
     * GroupListItem which have at least one descendant for which explicit
     * READ permission is granted. The expanded state of all other items is
     * left untouched.
     *
     * @param item
     *     The GroupListItem which should be conditionally expanded
     *     depending on whether READ permission is granted for any of its
     *     descendants.
     *
     * @param flags
     *     The set of permissions which should be used to determine whether
     *     the given item and its descendants are expanded.
     *
     * @returns
     *     true if the given item has been expanded, false otherwise.
     */
    private expandReadable(item: GroupListItem, flags: PermissionFlagSet): boolean {

        // If the current item is expandable and has defined children,
        // determine whether it should be expanded
        if (item.expandable && item.children) {
            for (let child of item.children) {

                // The parent should be expanded by default if the child is
                // expanded by default OR the permission set contains READ
                // permission on the child
                const childExpanded = this.expandReadable(child, flags) || this.isReadable(child, flags);
                item.expanded ||= childExpanded;

            }
        }

        return item.expanded;

    }

    /**
     * Creates a deep copy of all items within the tree descending from the
     * given GroupListItem which have at least one descendant for which
     * explicit READ permission is granted. Items which lack explicit READ
     * permission and which have no descendants having explicit READ
     * permission are omitted from the copy.
     *
     * @param item
     *     The GroupListItem which should be conditionally copied
     *     depending on whether READ permission is granted for any of its
     *     descendants.
     *
     * @param flags
     *     The set of permissions which should be used to determine whether
     *     the given item or any of its descendants are copied.
     *
     * @returns
     *     A new GroupListItem containing a deep copy of the given item,
     *     omitting any items which lack explicit READ permission and whose
     *     descendants also lack explicit READ permission, or null if even
     *     the given item would not be copied.
     */
    private copyReadable(item: GroupListItem, flags: PermissionFlagSet): GroupListItem {

        // Produce initial shallow copy of given item
        item = new GroupListItem(item);

        // Replace children array with an array containing only readable
        // children (or children with at least one readable descendant),
        // flagging the current item for copying if any such children exist
        if (item.children) {

            const children: GroupListItem[] = [];
            for (let child of item.children) {

                // Reduce child tree to only explicitly readable items and
                // their parents
                child = this.copyReadable(child, flags);

                // Include child only if they are explicitly readable, they
                // have explicitly readable descendants, or their parent is
                // readable (and thus all children are relevant)
                if ((child.children && child.children.length)
                    || this.isReadable(item, flags)
                    || this.isReadable(child, flags))
                    children.push(child);

            }

            item.children = children;

        }

        return item;

    }

    /**
     * Updates the permissionsAdded and permissionsRemoved permission sets
     * to reflect the addition of the given connection permission.
     *
     * @param identifier
     *     The identifier of the connection to add READ permission for.
     */
    private addConnectionPermission(identifier: string): void {

        // If permission was previously removed, simply un-remove it
        if (PermissionSet.hasConnectionPermission(this.permissionsRemoved, PermissionSet.ObjectPermissionType.READ, identifier))
            PermissionSet.removeConnectionPermission(this.permissionsRemoved, PermissionSet.ObjectPermissionType.READ, identifier);

        // Otherwise, explicitly add the permission
        else
            PermissionSet.addConnectionPermission(this.permissionsAdded, PermissionSet.ObjectPermissionType.READ, identifier);

    }

    /**
     * Updates the permissionsAdded and permissionsRemoved permission sets
     * to reflect the removal of the given connection permission.
     *
     * @param identifier
     *     The identifier of the connection to remove READ permission for.
     */
    private removeConnectionPermission(identifier: string): void {

        // If permission was previously added, simply un-add it
        if (PermissionSet.hasConnectionPermission(this.permissionsAdded, PermissionSet.ObjectPermissionType.READ, identifier))
            PermissionSet.removeConnectionPermission(this.permissionsAdded, PermissionSet.ObjectPermissionType.READ, identifier);

        // Otherwise, explicitly remove the permission
        else
            PermissionSet.addConnectionPermission(this.permissionsRemoved, PermissionSet.ObjectPermissionType.READ, identifier);

    }

    /**
     * Updates the permissionsAdded and permissionsRemoved permission sets
     * to reflect the addition of the given connection group permission.
     *
     * @param identifier
     *     The identifier of the connection group to add READ permission
     *     for.
     */
    private addConnectionGroupPermission(identifier: string): void {

        // If permission was previously removed, simply un-remove it
        if (PermissionSet.hasConnectionGroupPermission(this.permissionsRemoved, PermissionSet.ObjectPermissionType.READ, identifier))
            PermissionSet.removeConnectionGroupPermission(this.permissionsRemoved, PermissionSet.ObjectPermissionType.READ, identifier);

        // Otherwise, explicitly add the permission
        else
            PermissionSet.addConnectionGroupPermission(this.permissionsAdded, PermissionSet.ObjectPermissionType.READ, identifier);

    }

    /**
     * Updates the permissionsAdded and permissionsRemoved permission sets
     * to reflect the removal of the given connection group permission.
     *
     * @param identifier
     *     The identifier of the connection group to remove READ permission
     *     for.
     */
    private removeConnectionGroupPermission(identifier: string): void {

        // If permission was previously added, simply un-add it
        if (PermissionSet.hasConnectionGroupPermission(this.permissionsAdded, PermissionSet.ObjectPermissionType.READ, identifier))
            PermissionSet.removeConnectionGroupPermission(this.permissionsAdded, PermissionSet.ObjectPermissionType.READ, identifier);

        // Otherwise, explicitly remove the permission
        else
            PermissionSet.addConnectionGroupPermission(this.permissionsRemoved, PermissionSet.ObjectPermissionType.READ, identifier);

    }

    /**
     * Updates the permissionsAdded and permissionsRemoved permission sets
     * to reflect the addition of the given sharing profile permission.
     *
     * @param identifier
     *     The identifier of the sharing profile to add READ permission for.
     */
    private addSharingProfilePermission(identifier: string): void {

        // If permission was previously removed, simply un-remove it
        if (PermissionSet.hasSharingProfilePermission(this.permissionsRemoved, PermissionSet.ObjectPermissionType.READ, identifier))
            PermissionSet.removeSharingProfilePermission(this.permissionsRemoved, PermissionSet.ObjectPermissionType.READ, identifier);

        // Otherwise, explicitly add the permission
        else
            PermissionSet.addSharingProfilePermission(this.permissionsAdded, PermissionSet.ObjectPermissionType.READ, identifier);

    }

    /**
     * Updates the permissionsAdded and permissionsRemoved permission sets
     * to reflect the removal of the given sharing profile permission.
     *
     * @param identifier
     *     The identifier of the sharing profile to remove READ permission
     *     for.
     */
    private removeSharingProfilePermission(identifier: string): void {

        // If permission was previously added, simply un-add it
        if (PermissionSet.hasSharingProfilePermission(this.permissionsAdded, PermissionSet.ObjectPermissionType.READ, identifier))
            PermissionSet.removeSharingProfilePermission(this.permissionsAdded, PermissionSet.ObjectPermissionType.READ, identifier);

        // Otherwise, explicitly remove the permission
        else
            PermissionSet.addSharingProfilePermission(this.permissionsRemoved, PermissionSet.ObjectPermissionType.READ, identifier);

    }

    /**
     * Expose permission query and modification functions to group list template.
     */
    groupListContext: ConnectionListContext = {
        getPermissionFlags: () => {
            return this.permissionFlags!;
        },

        connectionPermissionChanged: (identifier: string) => {

            // Determine current permission setting
            const granted = this.permissionFlags!.connectionPermissions['READ'][identifier];

            // Add/remove permission depending on flag state
            if (granted)
                this.addConnectionPermission(identifier);
            else
                this.removeConnectionPermission(identifier);

        },

        connectionGroupPermissionChanged: (identifier: string) => {

            // Determine current permission setting
            const granted = this.permissionFlags!.connectionGroupPermissions['READ'][identifier];

            // Add/remove permission depending on flag state
            if (granted)
                this.addConnectionGroupPermission(identifier);
            else
                this.removeConnectionGroupPermission(identifier);

        },
        sharingProfilePermissionChanged : (identifier: string) => {

            // Determine current permission setting
            const granted = this.permissionFlags!.sharingProfilePermissions['READ'][identifier];

            // Add/remove permission depending on flag state
            if (granted)
                this.addSharingProfilePermission(identifier);
            else
                this.removeSharingProfilePermission(identifier);

        }
    };

}
