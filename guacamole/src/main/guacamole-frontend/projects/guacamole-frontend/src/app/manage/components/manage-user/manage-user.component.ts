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

import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, Input, OnInit, ViewEncapsulation } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, forkJoin, map, Observable, of, switchMap, throwError } from 'rxjs';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import { FormService } from '../../../form/service/form.service';
import { DataSourceService } from '../../../rest/service/data-source-service.service';
import { MembershipService } from '../../../rest/service/membership.service';
import { PermissionService } from '../../../rest/service/permission.service';
import { RequestService } from '../../../rest/service/request.service';
import { SchemaService } from '../../../rest/service/schema.service';
import { UserGroupService } from '../../../rest/service/user-group.service';
import { UserService } from '../../../rest/service/user.service';
import { Error } from '../../../rest/types/Error';
import { Form } from '../../../rest/types/Form';
import { PermissionFlagSet } from '../../../rest/types/PermissionFlagSet';
import { PermissionSet } from '../../../rest/types/PermissionSet';
import { User } from '../../../rest/types/User';
import { ManagementPermissions } from '../../types/ManagementPermissions';

@Component({
    selector     : 'guac-manage-user',
    templateUrl  : './manage-user.component.html',
    encapsulation: ViewEncapsulation.None
})
export class ManageUserComponent implements OnInit {


    /**
     * The unique identifier of the data source containing the user being
     * edited.
     */
    @Input() dataSource!: string;

    /**
     * The username of the user being edited. If a new user is
     * being created, this will not be defined.
     */
    @Input('id') username?: string

    /**
     * The identifiers of all data sources currently available to the
     * authenticated user.
     */
    private dataSources: string[] = [];

    /**
     * The username of the current, authenticated user.
     */
    private currentUsername: string | null = null;

    /**
     * The username of the original user from which this user is
     * being cloned. Only valid if this is a new user.
     */
    cloneSourceUsername: string | null = null;

    /**
     * The string value representing the user currently being edited within the
     * permission flag set. Note that his may not match the user's actual
     * username - it is a marker that is (1) guaranteed to be associated with
     * the current user's permissions in the permission set and (2) guaranteed
     * not to collide with any user that does not represent the current user
     * within the permission set.
     */
    selfUsername = '';

    /**
     * All user accounts associated with the same username as the account being
     * created or edited, as a map of data source identifier to the User object
     * within that data source.
     */
    users: Record<string, User> | null = null;

    /**
     * The user being modified.
     */
    user: User | null = null;

    /**
     * The form group describing the attributes of the user being modified.
     */
    userAttributes: FormGroup = new FormGroup({});

    /**
     * All permissions associated with the user being modified.
     */
    permissionFlags: PermissionFlagSet | null = null;

    /**
     * The set of permissions that will be added to the user when the user is
     * saved. Permissions will only be present in this set if they are
     * manually added, and not later manually removed before saving.
     */
    permissionsAdded: PermissionSet = new PermissionSet();

    /**
     * The set of permissions that will be removed from the user when the user
     * is saved. Permissions will only be present in this set if they are
     * manually removed, and not later manually added before saving.
     */
    permissionsRemoved: PermissionSet = new PermissionSet();

    /**
     * The identifiers of all user groups which can be manipulated (all groups
     * for which the user accessing this interface has UPDATE permission),
     * either through adding the current user as a member or removing the
     * current user from that group. If this information has not yet been
     * retrieved, this will be null.
     */
    availableGroups: string[] | null = null;

    /**
     * The identifiers of all user groups of which the user is a member,
     * taking into account any user groups which will be added/removed when
     * saved. If this information has not yet been retrieved, this will be
     * null.
     */
    parentGroups: string[] | null = null;

    /**
     * The set of identifiers of all parent user groups to which the user will
     * be added when saved. Parent groups will only be present in this set if
     * they are manually added, and not later manually removed before saving.
     */
    parentGroupsAdded: string[] = [];

    /**
     * The set of identifiers of all parent user groups from which the user
     * will be removed when saved. Parent groups will only be present in this
     * set if they are manually removed, and not later manually added before
     * saving.
     */
    parentGroupsRemoved: string[] = [];

    /**
     * For each applicable data source, the management-related actions that the
     * current user may perform on the user account currently being created
     * or modified, as a map of data source identifier to the
     * {@link ManagementPermissions} object describing the actions available
     * within that data source, or null if the current user's permissions have
     * not yet been loaded.
     */
    managementPermissions: Record<string, ManagementPermissions> | null = null;

    /**
     * All available user attributes. This is only the set of attribute
     * definitions, organized as logical groupings of attributes, not attribute
     * values.
     */
    attributes: Form[] | null = null;

    /**
     * The password match for the user.
     */
    passwordMatch?: string = undefined;

    constructor(private router: Router,
                private route: ActivatedRoute,
                private http: HttpClient,
                private authenticationService: AuthenticationService,
                private dataSourceService: DataSourceService,
                private membershipService: MembershipService,
                private permissionService: PermissionService,
                private requestService: RequestService,
                private schemaService: SchemaService,
                private userGroupService: UserGroupService,
                private userService: UserService,
                private formService: FormService,
                private destroyRef: DestroyRef
    ) {
    }

    ngOnInit(): void {
        this.dataSources = this.authenticationService.getAvailableDataSources();
        this.currentUsername = this.authenticationService.getCurrentUsername();
        this.cloneSourceUsername = this.route.snapshot.queryParamMap.get('clone');

        // Populate interface with requested data
        const userData = this.loadRequestedUser();
        const permissions = this.dataSourceService.apply(
            (ds: string, username: string) => this.permissionService.getEffectivePermissions(ds, username),
            this.dataSources,
            this.currentUsername
        );
        const userGroups = this.userGroupService.getUserGroups(this.dataSource, [PermissionSet.ObjectPermissionType.UPDATE]);
        const attributes = this.schemaService.getUserAttributes(this.dataSource);

        forkJoin([userData, permissions, userGroups, attributes])
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next    : ([userData, permissions, userGroups, attributes]) => {

                    this.attributes = attributes;

                    // Populate form with user attributes
                    this.createUserAttributesFormGroup();

                    this.managementPermissions = {};

                    // add account page
                    this.dataSources.forEach(dataSource => {

                        // Determine whether data source contains this user
                        const exists = (dataSource in (this.users || {}));

                        // Add the identifiers of all modifiable user groups
                        const availableGroups: string[] = [];
                        for (const groupIdentifier in userGroups) {
                            const userGroup = userGroups[groupIdentifier];
                            availableGroups.push(userGroup.identifier!);
                        }
                        this.availableGroups = availableGroups;

                        // Calculate management actions available for this specific account
                        if (this.managementPermissions)
                            this.managementPermissions[dataSource] = ManagementPermissions.fromPermissionSet(
                                permissions[dataSource],
                                PermissionSet.SystemPermissionType.CREATE_USER,
                                PermissionSet.hasUserPermission,
                                exists ? this.username : undefined);

                    });

                }, error: this.requestService.DIE
            });
    }

    /**
     * Creates the form group that is passed to the guac-form component to allow the
     * editing of the user's attributes. This form group is populated with the user's
     * current attribute values. The user's attributes are updated when the form group
     * values change.
     */
    private createUserAttributesFormGroup() {
        if (!this.attributes)
            return

        // Get a form group which allows editing of the user's attributes
        this.userAttributes = this.formService.getFormGroup(this.attributes);

        // Populate the form group with the user's current attribute values
        this.userAttributes.patchValue(this.user!.attributes);

        // Update the user's attributes when the form group is updated
        this.userAttributes.valueChanges
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(value => {
                if (this.user)
                    this.user.attributes = value;
            });
    }

    /**
     * Returns whether critical data has completed being loaded.
     *
     * @returns
     *     true if enough data has been loaded for the user interface to be
     *     useful, false otherwise.
     */
    isLoaded(): boolean {

        return this.users !== null
            && this.permissionFlags !== null
            && this.managementPermissions !== null
            && this.availableGroups !== null
            && this.parentGroups !== null
            && this.attributes !== null;

    }

    /**
     * Returns whether the current user can edit the username of the user being
     * edited within the given data source.
     *
     * @param dataSource
     *     The identifier of the data source to check. If omitted, this will
     *     default to the currently-selected data source.
     *
     * @returns
     *     true if the current user can edit the username of the user being
     *     edited, false otherwise.
     */
    canEditUsername(dataSource?: string): boolean {
        return !this.username;
    }

    /**
     * Loads the data associated with the user having the given username,
     * preparing the interface for making modifications to that existing user.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the user to
     *     load.
     *
     * @param username
     *     The username of the user to load.
     *
     * @returns
     *     An observable that completes when the interface has been prepared for
     *     editing the given user.
     */
    private loadExistingUser(dataSource: string, username: string): Observable<void> {

        const users = this.dataSourceService.apply(
            (ds: string, username: string) => this.userService.getUser(ds, username),
            this.dataSources,
            username
        );

        // Use empty permission set if user cannot be found
        const permissions = this.permissionService.getPermissions(dataSource, username)
            .pipe(
                catchError(this.requestService.defaultValue(new PermissionSet()))
            );

        // Assume no parent groups if user cannot be found
        const parentGroups = this.membershipService.getUserGroups(dataSource, username)
            .pipe(
                catchError(this.requestService.defaultValue([]))
            );


        return forkJoin([users, permissions, parentGroups])
            .pipe(
                map(([users, permissions, parentGroups]) => {

                    this.users = users;
                    this.parentGroups = parentGroups;

                    // Create skeleton user if uster does not exist
                    this.user = users[dataSource] || new User({
                        'username': username
                    });

                    // The current user will be associated with username of the existing
                    // user in the retrieved permission set
                    this.selfUsername = username;
                    this.permissionFlags = PermissionFlagSet.fromPermissionSet(permissions);

                })
            );
    }

    /**
     * Loads the data associated with the user having the given username,
     * preparing the interface for cloning that existing user.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the user to
     *     be cloned.
     *
     * @param username
     *     The username of the user being cloned.
     *
     * @returns
     *     An observable that completes when the interface has been prepared for
     *     cloning the given user.
     */
    private loadClonedUser(dataSource: string, username: string): Observable<void> {

        const users = this.dataSourceService.apply(
            (ds: string, username: string) => this.userService.getUser(ds, username),
            [dataSource],
            username
        );

        const permissions = this.permissionService.getPermissions(dataSource, username);
        const parentGroups = this.membershipService.getUserGroups(dataSource, username);


        return forkJoin([users, permissions, parentGroups])
            .pipe(
                map(([users, permissions, parentGroups]) => {

                    this.users = {};
                    this.user = users[dataSource];
                    this.parentGroups = parentGroups;
                    this.parentGroupsAdded = parentGroups;

                    // The current user will be associated with cloneSourceUsername in the
                    // retrieved permission set
                    this.selfUsername = username;
                    this.permissionFlags = PermissionFlagSet.fromPermissionSet(permissions);
                    this.permissionsAdded = permissions;

                })
            );
    }

    /**
     * Loads skeleton user data, preparing the interface for creating a new
     * user.
     *
     * @returns
     *     An observable that completes when the interface has been prepared for
     *     creating a new user.
     */
    private loadSkeletonUser(): Observable<void> {

        // No users exist regardless of data source if there is no username
        this.users = {};

        // Use skeleton user object with no associated permissions
        this.user = new User();
        this.parentGroups = [];
        this.permissionFlags = new PermissionFlagSet();

        // As no permissions are yet associated with the user, it is safe to
        // use any non-empty username as a placeholder for self-referential
        // permissions
        this.selfUsername = 'SELF';

        return of(void (0));

    }

    /**
     * Loads the data required for performing the management task requested
     * through the route parameters given at load time, automatically preparing
     * the interface for editing an existing user, cloning an existing user, or
     * creating an entirely new user.
     *
     * @returns
     *     An observable that completes when the interface has been prepared
     *     for performing the requested management task.
     */
    loadRequestedUser(): Observable<void> {

        // Pull user data and permissions if we are editing an existing user
        if (this.username)
            return this.loadExistingUser(this.dataSource, this.username);

        // If we are cloning an existing user, pull his/her data instead
        if (this.cloneSourceUsername)
            return this.loadClonedUser(this.dataSource, this.cloneSourceUsername);

        // If we are creating a new user, populate skeleton user data
        return this.loadSkeletonUser();

    }

    /**
     * Returns the URL for the page which manages the user account currently
     * being edited under the given data source. The given data source need not
     * be the same as the data source currently selected.
     *
     * @param dataSource
     *     The unique identifier of the data source that the URL is being
     *     generated for.
     *
     * @returns
     *     The URL for the page which manages the user account currently being
     *     edited under the given data source.
     */
    getUserURL(dataSource: string): string {
        return '/manage/' + encodeURIComponent(dataSource) + '/users/' + encodeURIComponent(this.username || '');
    }

    /**
     * Cancels all pending edits, returning to the main list of users.
     */
    returnToUserList(): void {
        this.router.navigate(['/settings/users']);
    }

    /**
     * Cancels all pending edits, opening an edit page for a new user
     * which is prepopulated with the data from the user currently being edited.
     */
    cloneUser(): void {
        this.router.navigate(
            ['manage', encodeURIComponent(this.dataSource), 'users'],
            { queryParams: { clone: this.username } }
        ).then(() => window.scrollTo(0, 0));
    }

    /**
     * Saves the current user, creating a new user or updating the existing
     * user depending on context, returning an observable that completes if the
     * save operation succeeds and rejected if the save operation fails.
     *
     * @returns
     *     An observable that completes if the save operation succeeds and fails
     *     with an {@link Error} if the save operation fails.
     */
    saveUser(): Observable<void> {

        // Verify passwords match
        if (this.passwordMatch !== this.user?.password) {
            return throwError(() => new Error({
                translatableMessage: {
                    key: 'MANAGE_USER.ERROR_PASSWORD_MISMATCH'
                }
            }));
        }

        // Save or create the user, depending on whether the user exists
        let saveUserPromise;
        if (this.dataSource in (this.users || {}))
            saveUserPromise = this.userService.saveUser(this.dataSource, this.user!);
        else
            saveUserPromise = this.userService.createUser(this.dataSource, this.user!);

        return saveUserPromise.pipe(
            switchMap(() => {

                // Move permission flags if username differs from marker
                if (this.selfUsername !== this.user!.username) {

                    // Rename added permission
                    if (this.permissionsAdded.userPermissions[this.selfUsername]) {
                        this.permissionsAdded.userPermissions[this.user!.username] = this.permissionsAdded.userPermissions[this.selfUsername];
                        delete this.permissionsAdded.userPermissions[this.selfUsername];
                    }

                    // Rename removed permission
                    if (this.permissionsRemoved.userPermissions[this.selfUsername]) {
                        this.permissionsRemoved.userPermissions[this.user!.username] = this.permissionsRemoved.userPermissions[this.selfUsername];
                        delete this.permissionsRemoved.userPermissions[this.selfUsername];
                    }

                }

                // Upon success, save any changed permissions/groups
                return forkJoin([
                    this.permissionService.patchPermissions(this.dataSource, this.user!.username, this.permissionsAdded, this.permissionsRemoved),
                    this.membershipService.patchUserGroups(this.dataSource, this.user!.username, this.parentGroupsAdded, this.parentGroupsRemoved)
                ]);

            }),

            // Map [void, void] from forkJoin to a simple void
            map(() => void (0))
        );

    }

    /**
     * Deletes the current user, returning an observable that completes if the
     * delete operation succeeds and rejected if the delete operation fails.
     *
     * @returns
     *     An observable that completes if the delete operation succeeds and is
     *     rejected with an {@link Error} if the delete operation fails.
     */
    deleteUser(): Observable<void> {

        if (this.user !== null) {
            return this.userService.deleteUser(this.dataSource, this.user);
        }

        return of(void (0));
    }

    modelOnly(): boolean {
        return !this.managementPermissions?.[this.dataSource].canChangeAllAttributes;
    }
}
