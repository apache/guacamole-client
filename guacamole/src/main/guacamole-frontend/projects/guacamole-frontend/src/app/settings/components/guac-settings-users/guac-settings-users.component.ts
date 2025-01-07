

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
import { TranslocoService } from '@ngneat/transloco';
import keys from 'lodash/keys';
import { BehaviorSubject, take } from 'rxjs';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import { GuacFilterComponent } from '../../../list/components/guac-filter/guac-filter.component';
import { GuacPagerComponent } from '../../../list/components/guac-pager/guac-pager.component';
import { DataSourceBuilderService } from '../../../list/services/data-source-builder.service';
import { SortService } from '../../../list/services/sort.service';
import { DataSource } from '../../../list/types/DataSource';
import { SortOrder } from '../../../list/types/SortOrder';
import { ManageableUser } from '../../../manage/types/ManageableUser';
import { DataSourceService } from '../../../rest/service/data-source-service.service';
import { PermissionService } from '../../../rest/service/permission.service';
import { RequestService } from '../../../rest/service/request.service';
import { UserService } from '../../../rest/service/user.service';
import { PermissionSet } from '../../../rest/types/PermissionSet';
import { User } from '../../../rest/types/User';

/**
 * A component for managing all users in the system.
 */
@Component({
    selector     : 'guac-settings-users',
    templateUrl  : './guac-settings-users.component.html',
    encapsulation: ViewEncapsulation.None
})
export class GuacSettingsUsersComponent implements OnInit {

    /**
     * Reference to the instance of the pager component.
     */
    @ViewChild(GuacPagerComponent, { static: true }) pager!: GuacPagerComponent;

    /**
     * Reference to the instance of the filter component.
     */
    @ViewChild(GuacFilterComponent, { static: true }) filter!: GuacFilterComponent;

    /**
     * Identifier of the current user
     */
    private currentUsername: string | null = this.authenticationService.getCurrentUsername();

    /**
     * The identifiers of all data sources accessible by the current
     * user.
     */
    private readonly dataSources: string[] = this.authenticationService.getAvailableDataSources();

    /**
     * TODO: document
     */
    dataSourceView: DataSource<ManageableUser> | null = null;

    /**
     * All visible users, along with their corresponding data sources.
     */
    manageableUsers: ManageableUser[] | null = null;

    /**
     * The name of the new user to create, if any, when user creation
     * is requested via newUser().
     */
    newUsername = '';

    /**
     * Map of data source identifiers to all permissions associated
     * with the current user within that data source, or null if the
     * user's permissions have not yet been loaded.
     */
    permissions: Record<string, PermissionSet> | null = null;

    /**
     * Array of all user properties that are filterable.
     */
    readonly filteredUserProperties: string[] = [
        'user.attributes["guac-full-name"]',
        'user.attributes["guac-organization"]',
        'user.lastActive',
        'user.username'
    ];

    /**
     * The date format for use for the last active date.
     */
    dateFormat: string | null = null;

    /**
     * SortOrder instance which stores the sort order of the listed
     * users.
     */
    private readonly initialOrder: SortOrder = new SortOrder([
        'user.username',
        '-user.lastActive',
        'user.attributes["guac-organization"]',
        'user.attributes["guac-full-name"]'
    ]);

    /**
     * Observable of the current SortOrder instance which stores the sort order of the listed
     * users. The value is updated by the GuacSortOrderDirective.
     */
    order: BehaviorSubject<SortOrder> = new BehaviorSubject(this.initialOrder);

    /**
     * Inject required services and initialize fields.
     */
    constructor(private authenticationService: AuthenticationService,
                private dataSourceService: DataSourceService,
                private permissionService: PermissionService,
                private requestService: RequestService,
                private userService: UserService,
                private translocoService: TranslocoService,
                private sortService: SortService,
                private dataSourceBuilderService: DataSourceBuilderService,
                private router: Router) {


        // Get session date format
        this.translocoService.selectTranslate<string>('SETTINGS_USERS.FORMAT_DATE')
            .pipe(take(1))
            .subscribe((retrievedDateFormat: string) => {
                // Store received date format
                this.dateFormat = retrievedDateFormat;
            });

    }

    ngOnInit(): void {

        // Build the data source for the users list entries.
        this.dataSourceView = this.dataSourceBuilderService.getBuilder<ManageableUser>()
            .source([])
            .filter(this.filter.searchStringChange, this.filteredUserProperties)
            .sort(this.order)
            .paginate(this.pager.page)
            .build();

        // Retrieve current permissions
        this.retrieveCurrentPermissions();
    }

    /**
     * Retrieves the current permissions.
     * @private
     */
    private retrieveCurrentPermissions(): void {

        this.dataSourceService.apply(
            (ds: string, username: string) => this.permissionService.getEffectivePermissions(ds, username),
            this.dataSources,
            this.currentUsername
        )
            .then(permissions => {

                // Store retrieved permissions
                this.permissions = permissions;

                // Return to home if there's nothing to do here
                if (!this.canManageUsers()) {
                    this.router.navigate(['/']);
                    return;
                }

                let userPromise: Promise<Record<string, Record<string, User>>>;

                // If users can be created, list all readable users
                if (this.canCreateUsers())
                    userPromise = this.dataSourceService.apply((ds: string) => this.userService.getUsers(ds), this.dataSources);

                // Otherwise, list only updatable/deletable users
                else
                    userPromise = this.dataSourceService.apply(this.userService.getUsers, this.dataSources, [
                        PermissionSet.ObjectPermissionType.UPDATE,
                        PermissionSet.ObjectPermissionType.DELETE
                    ]);

                userPromise.then((allUsers) => {

                    const addedUsers: Record<string, User> = {};
                    const manageableUsers: ManageableUser[] = [];

                    // For each user in each data source
                    this.dataSources.forEach(dataSource => {
                        for (const username in allUsers[dataSource]) {
                            const user = allUsers[dataSource][username];

                            // Do not add the same user twice
                            if (addedUsers[user.username])
                                return;

                            // Link to default creation data source if we cannot manage this user
                            if (!PermissionSet.hasSystemPermission(permissions[dataSource], PermissionSet.SystemPermissionType.ADMINISTER)
                                && !PermissionSet.hasUserPermission(permissions[dataSource], PermissionSet.ObjectPermissionType.UPDATE, user.username)
                                && !PermissionSet.hasUserPermission(permissions[dataSource], PermissionSet.ObjectPermissionType.DELETE, user.username))
                                dataSource = this.getDefaultDataSource() || '';

                            // Add user to overall list
                            addedUsers[user.username] = user;
                            manageableUsers.push(new ManageableUser({
                                dataSource: dataSource,
                                user      : user
                            }));

                        }

                    });

                    this.manageableUsers = manageableUsers;
                    this.dataSourceView?.updateSource(this.manageableUsers);

                }, this.requestService.PROMISE_DIE);

            }, this.requestService.PROMISE_DIE);
    }

    /**
     * Returns whether critical data has completed being loaded.
     *
     * @returns
     *     true if enough data has been loaded for the user interface
     *     to be useful, false otherwise.
     */
    isLoaded(): boolean {

        return this.dateFormat !== null
            && this.manageableUsers !== null
            && this.permissions !== null
            && this.dataSourceView !== null;

    }

    /**
     * Returns the identifier of the data source that should be used by
     * default when creating a new user.
     *
     * @return
     *     The identifier of the data source that should be used by
     *     default when creating a new user, or null if user creation
     *     is not allowed.
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
            const permissionSet = this.permissions[dataSource];

            // Can create users if administrator or have explicit permission
            if (PermissionSet.hasSystemPermission(permissionSet, PermissionSet.SystemPermissionType.ADMINISTER)
                || PermissionSet.hasSystemPermission(permissionSet, PermissionSet.SystemPermissionType.CREATE_USER))
                return dataSource;

        }

        // No data sources allow user creation
        return null;

    }

    /**
     * Returns whether the current user can create new users within at
     * least one data source.
     *
     * @return
     *     true if the current user can create new users within at
     *     least one data source, false otherwise.
     */
    canCreateUsers(): boolean {
        return this.getDefaultDataSource() !== null;
    }

    /**
     * Returns whether the current user can create new users or make
     * changes to existing users within at least one data source. The
     * user management interface as a whole is useless if this function
     * returns false.
     *
     * @return
     *     true if the current user can create new users or make
     *     changes to existing users within at least one data source,
     *     false otherwise.
     */
    canManageUsers(): boolean {

        // Abort if permissions have not yet loaded
        if (!this.permissions)
            return false;

        // Creating users counts as management
        if (this.canCreateUsers())
            return true;

        // For each data source
        for (const dataSource in this.permissions) {

            // Retrieve corresponding permission set
            const permissionSet = this.permissions[dataSource];

            // Can manage users if granted explicit update or delete
            if (PermissionSet.hasUserPermission(permissionSet, PermissionSet.ObjectPermissionType.UPDATE)
                || PermissionSet.hasUserPermission(permissionSet, PermissionSet.ObjectPermissionType.DELETE))
                return true;

        }

        // No data sources allow management of users
        return false;

    }

    /**
     * Track a user by their username. It is used in Angular's *ngFor
     * directive to optimize performance.
     *
     * @param index The index of the current item in the iterable.
     * @param manageableUser The current item being iterated.
     *
     * @returns The username of the manageable user.
     */
    trackByUsername(index: number, manageableUser: ManageableUser): string {
        return manageableUser.user.username;
    }

}
