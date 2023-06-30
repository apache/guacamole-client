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

import { Injectable } from '@angular/core';
import { AuthenticationService } from '../../auth/service/authentication.service';
import { ConnectionGroupService } from '../../rest/service/connection-group.service';
import { DataSourceService } from '../../rest/service/data-source-service.service';
import { PermissionService } from '../../rest/service/permission.service';
import { RequestService } from '../../rest/service/request.service';
import { PageDefinition } from '../../navigation/types/PageDefinition';
import { ConnectionGroup } from '../../rest/types/ConnectionGroup';
import { PermissionSet } from '../../rest/types/PermissionSet';
import { ClientIdentifier } from '../../navigation/types/ClientIdentifier';
import { catchError, forkJoin, from, map, Observable } from 'rxjs';
import cloneDeep from 'lodash/cloneDeep';
import { ClientIdentifierService } from '../../navigation/service/client-identifier.service';
import { canonicalize } from '../../locale/service/translation.service';

/**
 * A service for generating all the important pages a user can visit.
 */
@Injectable({
    providedIn: 'root'
})
export class UserPageService {

    /**
     * The home page to assign to a user if they can navigate to more than one
     * page.
     */
    private readonly SYSTEM_HOME_PAGE: PageDefinition = new PageDefinition({
        name: 'USER_MENU.ACTION_NAVIGATE_HOME',
        url: '/'
    });

    /**
     * Inject required services.
     */
    constructor(
        private authenticationService: AuthenticationService,
        private connectionGroupService: ConnectionGroupService,
        private dataSourceService: DataSourceService,
        private permissionService: PermissionService,
        private requestService: RequestService,
        private clientIdentifierService: ClientIdentifierService
    ) {
    }

    /**
     * Returns an appropriate home page for the current user.
     *
     * @param rootGroups
     *     A map of all root connection groups visible to the current user,
     *     where each key is the identifier of the corresponding data source.
     *
     * @param permissions
     *     A map of all permissions granted to the current user, where each
     *     key is the identifier of the corresponding data source.
     *
     * @returns
     *     The user's home page.
     */
    private generateHomePage(rootGroups: Record<string, ConnectionGroup>, permissions: Record<string, PermissionSet>): PageDefinition {

        const settingsPages = this.generateSettingsPages(permissions);

        // If user has access to settings pages, return home page and skip
        // evaluation for automatic connections.  The Preferences page is
        // a Settings page and is always visible, and the Session management
        // page is also available to all users so that they can kill their
        // own session.  We look for more than those two pages to determine
        // if we should go to the home page.
        if (settingsPages.length > 2)
            return this.SYSTEM_HOME_PAGE;

        // If exactly one connection or balancing group is available, use
        // that as the home page
        const clientPages = this.getClientPages(rootGroups);
        return (clientPages.length === 1) ? clientPages[0] : this.SYSTEM_HOME_PAGE;

    }

    /**
     * Adds to the given array all pages that the current user may use to
     * access connections or balancing groups that are descendants of the given
     * connection group.
     *
     * @param clientPages
     *     The array that pages should be added to.
     *
     * @param dataSource
     *     The data source containing the given connection group.
     *
     * @param connectionGroup
     *     The connection group ancestor of the connection or balancing group
     *     descendants whose pages should be added to the given array.
     */
    private addClientPages(clientPages: PageDefinition[], dataSource: string, connectionGroup: ConnectionGroup): void {

        // Add pages for all child connections
        connectionGroup.childConnections?.forEach((connection) => {
            clientPages.push(new PageDefinition({
                name: connection.name!,
                url: '/client/' + this.clientIdentifierService.getString({
                    dataSource: dataSource,
                    type: ClientIdentifier.Types.CONNECTION,
                    id: connection.identifier
                })
            }));
        });

        // Add pages for all child balancing groups, as well as the connectable
        // descendants of all balancing groups of any type
        connectionGroup.childConnectionGroups?.forEach((connectionGroup) => {
            if (connectionGroup.type === ConnectionGroup.Type.BALANCING) {
                clientPages.push(new PageDefinition({
                    name: connectionGroup.name,
                    url: '/client/' + this.clientIdentifierService.getString({
                        dataSource: dataSource,
                        type: ClientIdentifier.Types.CONNECTION_GROUP,
                        id: connectionGroup.identifier
                    })
                }));
            }

            this.addClientPages(clientPages, dataSource, connectionGroup);

        });

    }

    /**
     * Returns a full list of all pages that the current user may use to access
     * a connection or balancing group, regardless of the depth of those
     * connections/groups within the connection hierarchy.
     *
     * @param rootGroups
     *     A map of all root connection groups visible to the current user,
     *     where each key is the identifier of the corresponding data source.
     *
     * @returns
     *     A list of all pages that the current user may use to access a
     *     connection or balancing group.
     */
    getClientPages(rootGroups: Record<string, ConnectionGroup>): PageDefinition[] {

        const clientPages: PageDefinition[] = [];

        // Determine whether a connection or balancing group should serve as
        // the home page
        for (const dataSource in rootGroups) {
            this.addClientPages(clientPages, dataSource, rootGroups[dataSource]);
        }

        return clientPages;

    }

    /**
     * Returns an observable which emits an appropriate home page for the
     * current user.
     *
     * @returns
     *     An observable which emits the user's default home page.
     */
    getHomePage(): Observable<PageDefinition> {

        // Resolve promise using home page derived from root connection groups
        const rootGroups = this.dataSourceService.apply(
            (dataSource: string, connectionGroupID: string) => this.connectionGroupService.getConnectionGroupTree(dataSource, connectionGroupID),
            this.authenticationService.getAvailableDataSources(),
            ConnectionGroup.ROOT_IDENTIFIER
        );
        const permissionsSets = this.dataSourceService.apply(
            (dataSource: string, userID: string) => this.permissionService.getEffectivePermissions(dataSource, userID),
            this.authenticationService.getAvailableDataSources(),
            this.authenticationService.getCurrentUsername()
        );

        return forkJoin([rootGroups, permissionsSets])
            .pipe(
                map(([rootGroups, permissionsSets]) => {
                    return this.generateHomePage(rootGroups, permissionsSets);
                }),
                catchError(this.requestService.DIE)
            );

    }

    /**
     * Returns all settings pages that the current user can visit. This can
     * include any of the various manage pages.
     *
     * @param permissionSets
     *     A map of all permissions granted to the current user, where each
     *     key is the identifier of the corresponding data source.
     *
     * @returns
     *     An array of all settings pages that the current user can visit.
     */
    private generateSettingsPages(permissionSets: Record<string, PermissionSet>): PageDefinition[] {

        const pages: PageDefinition[] = [];

        const canManageUsers: string[] = [];
        const canManageUserGroups: string[] = [];
        const canManageConnections: string[] = [];
        const canViewConnectionRecords: string[] = [];

        // Inspect the contents of each provided permission set
        this.authenticationService.getAvailableDataSources().forEach((dataSource) => {

            // Get permissions for current data source, skipping if non-existent
            let permissions: PermissionSet = permissionSets[dataSource];
            if (!permissions)
                return;

            // Do not modify original object
            permissions = cloneDeep(permissions);

            // Ignore permission to update root group
            PermissionSet.removeConnectionGroupPermission(permissions,
                PermissionSet.ObjectPermissionType.UPDATE,
                ConnectionGroup.ROOT_IDENTIFIER);

            // Ignore permission to update self
            PermissionSet.removeUserPermission(permissions,
                PermissionSet.ObjectPermissionType.UPDATE,
                this.authenticationService.getCurrentUsername()!);

            // Determine whether the current user needs access to the user management UI
            if (
                // System permissions
                PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.ADMINISTER)
                || PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.CREATE_USER)

                // Permission to update users
                || PermissionSet.hasUserPermission(permissions, PermissionSet.ObjectPermissionType.UPDATE)

                // Permission to delete users
                || PermissionSet.hasUserPermission(permissions, PermissionSet.ObjectPermissionType.DELETE)

                // Permission to administer users
                || PermissionSet.hasUserPermission(permissions, PermissionSet.ObjectPermissionType.ADMINISTER)
            ) {
                canManageUsers.push(dataSource);
            }

            // Determine whether the current user needs access to the group management UI
            if (
                // System permissions
                PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.ADMINISTER)
                || PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.CREATE_USER_GROUP)

                // Permission to update user groups
                || PermissionSet.hasUserGroupPermission(permissions, PermissionSet.ObjectPermissionType.UPDATE)

                // Permission to delete user groups
                || PermissionSet.hasUserGroupPermission(permissions, PermissionSet.ObjectPermissionType.DELETE)

                // Permission to administer user groups
                || PermissionSet.hasUserGroupPermission(permissions, PermissionSet.ObjectPermissionType.ADMINISTER)
            ) {
                canManageUserGroups.push(dataSource);
            }

            // Determine whether the current user needs access to the connection management UI
            if (
                // System permissions
                PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.ADMINISTER)
                || PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.CREATE_CONNECTION)
                || PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.CREATE_CONNECTION_GROUP)

                // Permission to update connections or connection groups
                || PermissionSet.hasConnectionPermission(permissions, PermissionSet.ObjectPermissionType.UPDATE)
                || PermissionSet.hasConnectionGroupPermission(permissions, PermissionSet.ObjectPermissionType.UPDATE)

                // Permission to delete connections or connection groups
                || PermissionSet.hasConnectionPermission(permissions, PermissionSet.ObjectPermissionType.DELETE)
                || PermissionSet.hasConnectionGroupPermission(permissions, PermissionSet.ObjectPermissionType.DELETE)

                // Permission to administer connections or connection groups
                || PermissionSet.hasConnectionPermission(permissions, PermissionSet.ObjectPermissionType.ADMINISTER)
                || PermissionSet.hasConnectionGroupPermission(permissions, PermissionSet.ObjectPermissionType.ADMINISTER)
            ) {
                canManageConnections.push(dataSource);
            }

            // Determine whether the current user needs access to view connection history
            if (
                // A user must be a system administrator to view connection records
                PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.ADMINISTER)
            ) {
                canViewConnectionRecords.push(dataSource);
            }

        });

        // Add link to Session management (always accessible)
        pages.push(new PageDefinition({
            name: 'USER_MENU.ACTION_MANAGE_SESSIONS',
            url: '/settings/sessions'
        }));

        // If user can manage connections, add links for connection management pages
        canViewConnectionRecords.forEach((dataSource) => {
            pages.push(new PageDefinition({
                name: [
                    'USER_MENU.ACTION_VIEW_HISTORY',
                    canonicalize('DATA_SOURCE_' + dataSource) + '.NAME'
                ],
                url: '/settings/' + encodeURIComponent(dataSource) + '/history'
            }));
        });

        // If user can manage users, add link to user management page
        if (canManageUsers.length) {
            pages.push(new PageDefinition({
                name: 'USER_MENU.ACTION_MANAGE_USERS',
                url: '/settings/users'
            }));
        }

        // If user can manage user groups, add link to group management page
        if (canManageUserGroups.length) {
            pages.push(new PageDefinition({
                name: 'USER_MENU.ACTION_MANAGE_USER_GROUPS',
                url: '/settings/userGroups'
            }));
        }

        // If user can manage connections, add links for connection management pages
        canManageConnections.forEach((dataSource) => {
            pages.push(new PageDefinition({
                name: [
                    'USER_MENU.ACTION_MANAGE_CONNECTIONS',
                    canonicalize('DATA_SOURCE_' + dataSource) + '.NAME'
                ],
                url: '/settings/' + encodeURIComponent(dataSource) + '/connections'
            }));
        });

        // Add link to user preferences (always accessible)
        pages.push(new PageDefinition({
            name: 'USER_MENU.ACTION_MANAGE_PREFERENCES',
            url: '/settings/preferences'
        }));

        return pages;
    }

    /**
     * Returns an observable which emits an array of all settings pages that
     * the current user can visit. This can include any of the various manage
     * pages. The promise will not be rejected.
     *
     * @returns
     *     An observable which emits an array of all settings pages that the
     *     current user can visit.
     */
    getSettingsPages(): Observable<PageDefinition[]> {

        // Retrieve current permissions
        return from(this.dataSourceService.apply(
            (dataSource: string, userID: string) => this.permissionService.getEffectivePermissions(dataSource, userID),
            this.authenticationService.getAvailableDataSources(),
            this.authenticationService.getCurrentUsername()
        ))

            // Resolve promise using settings pages derived from permissions
            .pipe(map(permissions => {
                    return this.generateSettingsPages(permissions);
                }),
                catchError(this.requestService.DIE)
            );

    }

    /**
     * Returns all the main pages that the current user can visit. This can
     * include the home page, manage pages, etc. In the case that there are no
     * applicable pages of this sort, it may return a client page.
     *
     * @param rootGroups
     *     A map of all root connection groups visible to the current user,
     *     where each key is the identifier of the corresponding data source.
     *
     * @param permissions
     *     A map of all permissions granted to the current user, where each
     *     key is the identifier of the corresponding data source.
     *
     * @returns
     *     An array of all main pages that the current user can visit.
     */
    generateMainPages(rootGroups: Record<string, ConnectionGroup>, permissions: Record<string, PermissionSet>): PageDefinition[] {

        const pages: PageDefinition[] = [];

        // Get home page and settings pages
        const homePage = this.generateHomePage(rootGroups, permissions);
        const settingsPages = this.generateSettingsPages(permissions);

        // Only include the home page in the list of main pages if the user
        // can navigate elsewhere.
        if (homePage === this.SYSTEM_HOME_PAGE || settingsPages.length)
            pages.push(homePage);

        // Add generic link to the first-available settings page
        if (settingsPages.length) {
            pages.push(new PageDefinition({
                name: 'USER_MENU.ACTION_MANAGE_SETTINGS',
                url: settingsPages[0].url
            }));
        }

        return pages;
    }

    /**
     * Returns a promise which resolves to an array of all main pages that the
     * current user can visit. This can include the home page, manage pages,
     * etc. In the case that there are no applicable pages of this sort, it may
     * return a client page. The promise will not be rejected.
     *
     * @returns
     *     A promise which resolves to an array of all main pages that the
     *     current user can visit.
     */
    getMainPages(): Observable<PageDefinition[]> {

        const promise: Promise<PageDefinition[]> = new Promise<PageDefinition[]>((resolve, reject) => {

            let rootGroups: Record<string, ConnectionGroup> | null = null;
            let permissions: Record<string, PermissionSet> | null = null;

            /**
             * Resolves the main pages retrieval promise, if possible. If
             * insufficient data is available, this function does nothing.
             */
            const resolveMainPages = () => {
                if (rootGroups && permissions)
                    resolve(this.generateMainPages(rootGroups, permissions));
            };

            // Retrieve root group, resolving main pages if possible
            this.dataSourceService.apply(
                (dataSource: string, connectionGroupID: string) => this.connectionGroupService.getConnectionGroupTree(dataSource, connectionGroupID),
                this.authenticationService.getAvailableDataSources(),
                ConnectionGroup.ROOT_IDENTIFIER
            )
                .then(function rootConnectionGroupsRetrieved(retrievedRootGroups) {
                    rootGroups = retrievedRootGroups;
                    resolveMainPages();
                }, this.requestService.PROMISE_DIE);


            // Retrieve current permissions
            this.dataSourceService.apply(
                (dataSource: string, userID: string) => this.permissionService.getEffectivePermissions(dataSource, userID),
                this.authenticationService.getAvailableDataSources(),
                this.authenticationService.getCurrentUsername()
            )

                // Resolving main pages if possible
                .then(function permissionsRetrieved(retrievedPermissions) {
                    permissions = retrievedPermissions;
                    resolveMainPages();
                }, this.requestService.PROMISE_DIE);

        });

        return from(promise);

    }

}
