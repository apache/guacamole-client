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

import { Component, Input, OnInit, signal, ViewEncapsulation } from '@angular/core';
import { RequestService } from '../../../rest/service/request.service';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import { ConnectionGroupService } from '../../../rest/service/connection-group.service';
import { DataSourceService } from '../../../rest/service/data-source-service.service';
import { PermissionService } from '../../../rest/service/permission.service';
import { GuacNotificationService } from '../../../notification/services/guac-notification.service';
import { Router } from '@angular/router';
import { ConnectionGroup } from '../../../rest/types/ConnectionGroup';
import { PermissionSet } from '../../../rest/types/PermissionSet';
import { NonNullableProperties } from '../../../util/utility-types';
import { GroupListItem } from '../../../group-list/types/GroupListItem';

/**
 * A component for managing all connections and connection groups in the system.
 */
@Component({
    selector: 'guac-settings-connections',
    templateUrl: './guac-settings-connections.component.html',
    encapsulation: ViewEncapsulation.None
})
export class GuacSettingsConnectionsComponent implements OnInit {

    /**
     * The identifier of the current user.
     */
    private currentUsername: string | null = this.authenticationService.getCurrentUsername();

    /**
     * The identifier of the currently-selected data source.
     */
    @Input({required: true}) dataSource!: string;

    /**
     * The root connection group of the connection group hierarchy.
     */
    rootGroups: Record<string, ConnectionGroup> | null = null;

    /**
     * Signal that contains the result of filtering the root groups.
     */
    filteredRootGroups = signal<Record<string, ConnectionGroup>>({})

    /**
     * All permissions associated with the current user, or null if the
     * user's permissions have not yet been loaded.
     */
    permissions: PermissionSet | null = null;

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

    constructor(private authenticationService: AuthenticationService,
                private connectionGroupService: ConnectionGroupService,
                private dataSourceService: DataSourceService,
                private guacNotification: GuacNotificationService,
                private permissionService: PermissionService,
                private requestService: RequestService,
                private router: Router) {
    }

    ngOnInit(): void {
        // Retrieve current permissions
        this.permissionService.getEffectivePermissions(this.dataSource, this.currentUsername!)
            .subscribe({
                next: permissions => {

                    // Store retrieved permissions
                    this.permissions = permissions;

                    // Ignore permission to update root group
                    PermissionSet.removeConnectionGroupPermission(this.permissions, PermissionSet.ObjectPermissionType.UPDATE, ConnectionGroup.ROOT_IDENTIFIER);

                    // Return to home if there's nothing to do here
                    if (!this.canManageConnections())
                        this.router.navigate(['/']);

                    // Retrieve all connections for which we have UPDATE or DELETE permission
                    this.dataSourceService.apply(
                        (dataSource: string, connectionGroupID?: string, permissionTypes?: string[] | undefined) =>
                            this.connectionGroupService.getConnectionGroupTree(dataSource, connectionGroupID, permissionTypes),
                        [this.dataSource],
                        ConnectionGroup.ROOT_IDENTIFIER,
                        [PermissionSet.ObjectPermissionType.UPDATE, PermissionSet.ObjectPermissionType.DELETE]
                    )
                        .then(rootGroups => {
                            this.rootGroups = rootGroups;
                            // TODO: Remove the next line once the filter component sets the filteredRootGroups
                            this.filteredRootGroups.set(rootGroups);
                        }, this.requestService.PROMISE_DIE);

                }, error: this.requestService.DIE
            }); // end retrieve permissions
    }

    /**
     * Returns whether critical data has completed being loaded.
     *
     * @returns
     *     true if enough data has been loaded for the user interface
     *     to be useful, false otherwise.
     */
    isLoaded(): this is NonNullableProperties<GuacSettingsConnectionsComponent, 'rootGroups' | 'permissions'> {

        return this.rootGroups !== null
            && this.permissions !== null;

    }

    /**
     * Returns whether the current user has the ADMINISTER system
     * permission (i.e. they are an administrator).
     *
     * @return
     *     true if the current user is an administrator.
     */
    canAdminister(): boolean {

        // Abort if permissions have not yet loaded
        if (!this.permissions)
            return false;

        // Return whether the current user is an administrator
        return PermissionSet.hasSystemPermission(
            this.permissions, PermissionSet.SystemPermissionType.ADMINISTER);
    }

    /**
     * Returns whether the current user can create new connections
     * within the current data source.
     *
     * @return
     *     true if the current user can create new connections within
     *     the current data source, false otherwise.
     */
    canCreateConnections(): boolean {

        // Abort if permissions have not yet loaded
        if (!this.permissions)
            return false;

        // Can create connections if adminstrator or have explicit permission
        if (PermissionSet.hasSystemPermission(this.permissions, PermissionSet.SystemPermissionType.ADMINISTER)
            || PermissionSet.hasSystemPermission(this.permissions, PermissionSet.SystemPermissionType.CREATE_CONNECTION))
            return true;

        // No data sources allow connection creation
        return false;

    }

    /**
     * Returns whether the current user can create new connection
     * groups within the current data source.
     *
     * @return
     *     true if the current user can create new connection groups
     *     within the current data source, false otherwise.
     */
    canCreateConnectionGroups(): boolean {

        // Abort if permissions have not yet loaded
        if (!this.permissions)
            return false;

        // Can create connections groups if adminstrator or have explicit permission
        if (PermissionSet.hasSystemPermission(this.permissions, PermissionSet.SystemPermissionType.ADMINISTER)
            || PermissionSet.hasSystemPermission(this.permissions, PermissionSet.SystemPermissionType.CREATE_CONNECTION_GROUP))
            return true;

        // No data sources allow connection group creation
        return false;

    }

    /**
     * Returns whether the current user can create new sharing profiles
     * within the current data source.
     *
     * @return
     *     true if the current user can create new sharing profiles
     *     within the current data source, false otherwise.
     */
    canCreateSharingProfiles(): boolean {

        // Abort if permissions have not yet loaded
        if (!this.permissions)
            return false;

        // Can create sharing profiles if adminstrator or have explicit permission
        if (PermissionSet.hasSystemPermission(this.permissions, PermissionSet.SystemPermissionType.ADMINISTER)
            || PermissionSet.hasSystemPermission(this.permissions, PermissionSet.SystemPermissionType.CREATE_SHARING_PROFILE))
            return true;

        // Current data source does not allow sharing profile creation
        return false;

    }

    /**
     * Returns whether the current user can create new connections or
     * connection groups or make changes to existing connections or
     * connection groups within the current data source. The
     * connection management interface as a whole is useless if this
     * function returns false.
     *
     * @return
     *     true if the current user can create new connections/groups
     *     or make changes to existing connections/groups within the
     *     current data source, false otherwise.
     */
    canManageConnections(): boolean {

        // Abort if permissions have not yet loaded
        if (!this.permissions)
            return false;

        // Creating connections/groups counts as management
        if (this.canCreateConnections()
            || this.canCreateConnectionGroups()
            || this.canCreateSharingProfiles())
            return true;

        // Can manage connections if granted explicit update or delete
        if (PermissionSet.hasConnectionPermission(this.permissions, PermissionSet.ObjectPermissionType.UPDATE)
            || PermissionSet.hasConnectionPermission(this.permissions, PermissionSet.ObjectPermissionType.DELETE))
            return true;

        // Can manage connections groups if granted explicit update or delete
        if (PermissionSet.hasConnectionGroupPermission(this.permissions, PermissionSet.ObjectPermissionType.UPDATE)
            || PermissionSet.hasConnectionGroupPermission(this.permissions, PermissionSet.ObjectPermissionType.DELETE))
            return true;

        // No data sources allow management of connections or groups
        return false;

    }

    /**
     * Returns whether the current user can update the connection having
     * the given identifier within the current data source.
     *
     * @param identifier
     *     The identifier of the connection to check.
     *
     * @return
     *     true if the current user can update the connection having the
     *     given identifier within the current data source, false
     *     otherwise.
     */
    canUpdateConnection(identifier: string): boolean {

        // Abort if permissions have not yet loaded
        if (!this.permissions)
            return false;

        // Can update the connection if adminstrator or have explicit permission
        if (PermissionSet.hasSystemPermission(this.permissions, PermissionSet.SystemPermissionType.ADMINISTER)
            || PermissionSet.hasConnectionPermission(this.permissions, PermissionSet.ObjectPermissionType.UPDATE, identifier))
            return true;

        // Current data sources does not allow the connection to be updated
        return false;

    }

    /**
     * Returns whether the current user can update the connection group
     * having the given identifier within the current data source.
     *
     * @param identifier
     *     The identifier of the connection group to check.
     *
     * @return
     *     true if the current user can update the connection group
     *     having the given identifier within the current data source,
     *     false otherwise.
     */
    canUpdateConnectionGroup(identifier: string): boolean {

        // Abort if permissions have not yet loaded
        if (!this.permissions)
            return false;

        // Can update the connection if adminstrator or have explicit permission
        if (PermissionSet.hasSystemPermission(this.permissions, PermissionSet.SystemPermissionType.ADMINISTER)
            || PermissionSet.hasConnectionGroupPermission(this.permissions, PermissionSet.ObjectPermissionType.UPDATE, identifier))
            return true;

        // Current data sources does not allow the connection group to be updated
        return false;

    }

    /**
     * Adds connection-group-specific contextual actions to the given
     * array of GroupListItems. Each contextual action will be
     * represented by a new GroupListItem.
     *
     * @param items
     *     The array of GroupListItems to which new GroupListItems
     *     representing connection-group-specific contextual actions
     *     should be added.
     *
     * @param parent
     *     The GroupListItem representing the connection group which
     *     contains the given array of GroupListItems, if known.
     */
    private addConnectionGroupActions(items: GroupListItem[], parent?: GroupListItem): void {

        // Do nothing if we lack permission to modify the parent at all
        if (parent && !this.canUpdateConnectionGroup(parent.identifier!))
            return;

        // Add action for creating a child connection, if the user has
        // permission to do so
        if (this.canCreateConnections())
            items.push(new GroupListItem({
                type: 'new-connection',
                dataSource: this.dataSource,
                weight: 1,
                wrappedItem: parent
            }));

        // Add action for creating a child connection group, if the user
        // has permission to do so
        if (this.canCreateConnectionGroups())
            items.push(new GroupListItem({
                type: 'new-connection-group',
                dataSource: this.dataSource,
                weight: 1,
                wrappedItem: parent
            }));

    }

    /**
     * Adds connection-specific contextual actions to the given array of
     * GroupListItems. Each contextual action will be represented by a
     * new GroupListItem.
     *
     * @param items
     *     The array of GroupListItems to which new GroupListItems
     *     representing connection-specific contextual actions should
     *     be added.
     *
     * @param parent
     *     The GroupListItem representing the connection which contains
     *     the given array of GroupListItems, if known.
     */
    private addConnectionActions(items: GroupListItem[], parent?: GroupListItem): void {

        // Do nothing if we lack permission to modify the parent at all
        if (parent && !this.canUpdateConnection(parent.identifier!))
            return;

        // Add action for creating a child sharing profile, if the user
        // has permission to do so
        if (this.canCreateSharingProfiles())
            items.push(new GroupListItem({
                type: 'new-sharing-profile',
                dataSource: this.dataSource,
                weight: 1,
                wrappedItem: parent
            }));

    }

    /**
     * Decorates the given GroupListItem, including all descendants,
     * adding contextual actions.
     *
     * @param item
     *     The GroupListItem which should be decorated with additional
     *     GroupListItems representing contextual actions.
     */
    private decorateItem(item: GroupListItem): void {

        // If the item is a connection group, add actions specific to
        // connection groups
        if (item.type === GroupListItem.Type.CONNECTION_GROUP)
            this.addConnectionGroupActions(item.children, item);

            // If the item is a connection, add actions specific to
        // connections
        else if (item.type === GroupListItem.Type.CONNECTION) {
            this.addConnectionActions(item.children, item);
        }

        // Decorate all children
        item.children.forEach(child => this.decorateItem(child));

    }

    /**
     * Callback which decorates all items within the given array of
     * GroupListItems, including their descendants, adding contextual
     * actions.
     *
     * @param items
     *     The array of GroupListItems which should be decorated with
     *     additional GroupListItems representing contextual actions.
     */
    rootItemDecorator(items: GroupListItem[]): void {

        // Decorate each root-level item
        items.forEach(item => this.decorateItem(item));

    }
}
