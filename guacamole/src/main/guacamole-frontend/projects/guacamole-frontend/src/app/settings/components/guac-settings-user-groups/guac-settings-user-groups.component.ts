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
import { Router } from '@angular/router';
import keys from 'lodash/keys';
import { BehaviorSubject } from 'rxjs';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import { GuacFilterComponent } from '../../../list/components/guac-filter/guac-filter.component';
import { GuacPagerComponent } from '../../../list/components/guac-pager/guac-pager.component';
import { DataSourceBuilderService } from '../../../list/services/data-source-builder.service';
import { DataSource } from '../../../list/types/DataSource';
import { SortOrder } from '../../../list/types/SortOrder';
import { ManageableUserGroup } from '../../../manage/types/ManageableUserGroup';
import { DataSourceService } from '../../../rest/service/data-source-service.service';
import { PermissionService } from '../../../rest/service/permission.service';
import { RequestService } from '../../../rest/service/request.service';
import { UserGroupService } from '../../../rest/service/user-group.service';
import { PermissionSet } from '../../../rest/types/PermissionSet';
import { UserGroup } from '../../../rest/types/UserGroup';
import { NonNullableProperties } from '../../../util/utility-types';

/**
 * A component for managing all user groups in the system.
 */
@Component({
    selector     : 'guac-settings-user-groups',
    templateUrl  : './guac-settings-user-groups.component.html',
    encapsulation: ViewEncapsulation.None
})
export class GuacSettingsUserGroupsComponent implements OnInit {

    /**
     * Reference to the instance of the pager component.
     */
    @ViewChild(GuacPagerComponent, { static: true }) pager!: GuacPagerComponent;

    /**
     * Reference to the instance of the filter component.
     */
    @ViewChild(GuacFilterComponent, { static: true }) filter!: GuacFilterComponent;

    /**
     * Identifier of the current user.
     */
    private currentUsername: string | null = this.authenticationService.getCurrentUsername();

    /**
     * The identifiers of all data sources accessible by the current
     * user.
     */
    private dataSources: string[] = this.authenticationService.getAvailableDataSources();

    /**
     * TODO: document
     */
    dataSourceView: DataSource<ManageableUserGroup> | null = null;

    /**
     * All visible user groups, along with their corresponding data
     * sources.
     */
    manageableUserGroups: ManageableUserGroup[] | null = null;

    /**
     * Array of all user group properties that are filterable.
     */
    filteredUserGroupProperties: string[] = [
        'userGroup.identifier'
    ];

    /**
     * The initial SortOrder which stores the sort order of the listed
     * user groups.
     */
    private readonly initialOrder: SortOrder = new SortOrder([
        'userGroup.identifier'
    ]);

    /**
     * Observable of the current SortOrder instance which stores the sort order of the listed
     * user groups. The value is updated by the GuacSortOrderDirective.
     */
    order: BehaviorSubject<SortOrder> = new BehaviorSubject(this.initialOrder);

    /**
     * Map of data source identifiers to all permissions associated
     * with the current user within that data source, or null if the
     * user's permissions have not yet been loaded.
     */
    private permissions: Record<string, PermissionSet> | null = null;

    /**
     * Inject required services.
     */
    constructor(private authenticationService: AuthenticationService,
                private dataSourceService: DataSourceService,
                private permissionService: PermissionService,
                private requestService: RequestService,
                private userGroupService: UserGroupService,
                private dataSourceBuilderService: DataSourceBuilderService,
                private router: Router) {
    }

    ngOnInit(): void {

        // Build the data source for the user group list entries.
        this.dataSourceView = this.dataSourceBuilderService.getBuilder<ManageableUserGroup>()
            .source([])
            .filter(this.filter.searchStringChange, this.filteredUserGroupProperties)
            .sort(this.order)
            .paginate(this.pager.page)
            .build();

        // Retrieve current permissions
        this.dataSourceService.apply(
            (dataSource: string, userID: string) => this.permissionService.getEffectivePermissions(dataSource, userID),
            this.dataSources,
            this.currentUsername
        )
            .then(retrievedPermissions => {

                // Store retrieved permissions
                this.permissions = retrievedPermissions;

                // Return to home if there's nothing to do here
                if (!this.canManageUserGroups())
                    this.router.navigate(['/']);

                // If user groups can be created, list all readable user groups
                if (this.canCreateUserGroups()) {
                    return this.dataSourceService.apply(
                        (dataSource: string) => this.userGroupService.getUserGroups(dataSource),
                        this.dataSources
                    );
                }

                // Otherwise, list only updateable/deletable users
                return this.dataSourceService.apply(
                    (dataSource: string, permissionTypes: string[]) => this.userGroupService.getUserGroups(dataSource, permissionTypes),
                    this.dataSources,
                    [
                        PermissionSet.ObjectPermissionType.UPDATE,
                        PermissionSet.ObjectPermissionType.DELETE
                    ]
                );

            })
            .then(userGroups => {
                this.setDisplayedUserGroups(this.permissions!, userGroups);
            }, this.requestService.PROMISE_WARN);
    }


    /**
     * Returns whether critical data has completed being loaded.
     *
     * @returns
     *     true if enough data has been loaded for the user group
     *     interface to be useful, false otherwise.
     */
    isLoaded(): this is NonNullableProperties<GuacSettingsUserGroupsComponent, 'manageableUserGroups' | 'dataSourceView'> {
        return this.manageableUserGroups !== null
            && this.dataSourceView !== null;
    }

    /**
     * Returns the identifier of the data source that should be used by
     * default when creating a new user group.
     *
     * @return
     *     The identifier of the data source that should be used by
     *     default when creating a new user group, or null if user group
     *     creation is not allowed.
     */
    getDefaultDataSource(): string | null {

        // Abort if permissions have not yet loaded
        if (!this.permissions)
            return null;

        // For each data source
        const dataSources = keys(this.permissions).sort();
        for (let i = 0; i < dataSources.length; i++) {

            // Retrieve corresponding permission set
            const dataSource = dataSources[i];
            const permissionSet = (this.permissions)[dataSource];

            // Can create user groups if adminstrator or have explicit permission
            if (PermissionSet.hasSystemPermission(permissionSet, PermissionSet.SystemPermissionType.ADMINISTER)
                || PermissionSet.hasSystemPermission(permissionSet, PermissionSet.SystemPermissionType.CREATE_USER_GROUP))
                return dataSource;

        }

        // No data sources allow user group creation
        return null;

    }

    /**
     * Returns whether the current user can create new user groups
     * within at least one data source.
     *
     * @return
     *     true if the current user can create new user groups within at
     *     least one data source, false otherwise.
     */
    canCreateUserGroups(): boolean {
        return this.getDefaultDataSource() !== null;
    }

    /**
     * Returns whether the current user can create new user groups or
     * make changes to existing user groups within at least one data
     * source. The user group management interface as a whole is useless
     * if this function returns false.
     *
     * @return {Boolean}
     *     true if the current user can create new user groups or make
     *     changes to existing user groups within at least one data
     *     source, false otherwise.
     */
    private canManageUserGroups(): boolean {

        // Abort if permissions have not yet loaded
        if (!this.permissions)
            return false;

        // Creating user groups counts as management
        if (this.canCreateUserGroups())
            return true;

        // For each data source
        for (const dataSource in this.permissions) {

            // Retrieve corresponding permission set
            const permissionSet = (this.permissions)[dataSource];

            // Can manage user groups if granted explicit update or delete
            if (PermissionSet.hasUserGroupPermission(permissionSet, PermissionSet.ObjectPermissionType.UPDATE)
                || PermissionSet.hasUserGroupPermission(permissionSet, PermissionSet.ObjectPermissionType.DELETE))
                return true;

        }

        // No data sources allow management of user groups
        return false;

    }

    /**
     * Sets the displayed list of user groups. If any user groups are
     * already shown within the interface, those user groups are replaced
     * with the given user groups.
     *
     * @param permissions
     *     A map of data source identifiers to all permissions associated
     *     with the current user within that data source.
     *
     * @param userGroups
     *     A map of all user groups which should be displayed, where each
     *     key is the data source identifier from which the user groups
     *     were retrieved and each value is a map of user group identifiers
     *     to their corresponding @link{UserGroup} objects.
     */
    setDisplayedUserGroups(permissions: Record<string, PermissionSet>, userGroups: Record<string, Record<string, UserGroup>>): void {

        const addedUserGroups: Record<string, UserGroup> = {};
        this.manageableUserGroups = [];

        // For each user group in each data source
        this.dataSources.forEach(dataSource => {
            for (const userGroupIdentifier in userGroups[dataSource]) {
                const userGroup = userGroups[dataSource][userGroupIdentifier];

                // Do not add the same user group twice
                if (addedUserGroups[userGroup.identifier!])
                    return;

                // Link to default creation data source if we cannot manage this user
                if (!PermissionSet.hasSystemPermission(permissions[dataSource], PermissionSet.SystemPermissionType.ADMINISTER)
                    && !PermissionSet.hasUserGroupPermission(permissions[dataSource], PermissionSet.ObjectPermissionType.UPDATE, userGroup.identifier)
                    && !PermissionSet.hasUserGroupPermission(permissions[dataSource], PermissionSet.ObjectPermissionType.DELETE, userGroup.identifier))
                    dataSource = this.getDefaultDataSource()!;

                // Add user group to overall list
                addedUserGroups[userGroup.identifier!] = userGroup;
                this.manageableUserGroups!.push(new ManageableUserGroup({
                    'dataSource': dataSource,
                    'userGroup' : userGroup
                }));

            }

            this.dataSourceView?.updateSource(this.manageableUserGroups!);
        });

    }

}
