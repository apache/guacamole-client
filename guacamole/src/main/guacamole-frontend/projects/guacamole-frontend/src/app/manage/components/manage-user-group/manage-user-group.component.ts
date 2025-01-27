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

import { Component, DestroyRef, Input, OnInit, ViewEncapsulation } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, forkJoin, from, map, Observable, of, switchMap } from 'rxjs';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import { FormService } from '../../../form/service/form.service';
import { DataSourceService } from '../../../rest/service/data-source-service.service';
import { MembershipService } from '../../../rest/service/membership.service';
import { PermissionService } from '../../../rest/service/permission.service';
import { RequestService } from '../../../rest/service/request.service';
import { SchemaService } from '../../../rest/service/schema.service';
import { UserGroupService } from '../../../rest/service/user-group.service';
import { UserService } from '../../../rest/service/user.service';
import { Form } from '../../../rest/types/Form';
import { PermissionFlagSet } from '../../../rest/types/PermissionFlagSet';
import { PermissionSet } from '../../../rest/types/PermissionSet';
import { UserGroup } from '../../../rest/types/UserGroup';
import { NonNullableProperties } from '../../../util/utility-types';
import { ManagementPermissions } from '../../types/ManagementPermissions';

/**
 * The component for editing user groups.
 */
@Component({
    selector: 'guac-manage-user-group',
    templateUrl: './manage-user-group.component.html',
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ManageUserGroupComponent implements OnInit {

    /**
     * The identifier of the user group being edited. If a new user group is
     * being created, this will not be defined.
     */
    @Input({ alias: 'id' }) private identifier?: string;

    /**
     * The unique identifier of the data source containing the user group being
     * edited.
     */
    @Input({ required: true }) dataSource!: string;

    /**
     * The identifiers of all data sources currently available to the
     * authenticated user.
     */
    private dataSources: string[] = this.authenticationService.getAvailableDataSources();

    /**
     * The username of the current, authenticated user.
     */
    private currentUsername: string | null = this.authenticationService.getCurrentUsername();

    /**
     * The identifier of the original user group from which this user group is
     * being cloned. Only valid if this is a new user group.
     */
    private cloneSourceIdentifier: string | null = null;

    /**
     * All user groups associated with the same identifier as the group being
     * created or edited, as a map of data source identifier to the UserGroup
     * object within that data source.
     */
    userGroups: Record<string, UserGroup> | null = null;

    /**
     * The user group being modified.
     */
    userGroup: UserGroup | null = null;

    /**
     * All permissions associated with the user group being modified.
     */
    permissionFlags: PermissionFlagSet | null = null;

    /**
     * The set of permissions that will be added to the user group when the
     * user group is saved. Permissions will only be present in this set if they
     * are manually added, and not later manually removed before saving.
     */
    permissionsAdded: PermissionSet = new PermissionSet();

    /**
     * The set of permissions that will be removed from the user group when the
     * user group is saved. Permissions will only be present in this set if they
     * are manually removed, and not later manually added before saving.
     */
    permissionsRemoved: PermissionSet = new PermissionSet();

    /**
     * The identifiers of all user groups which can be manipulated (all groups
     * for which the user accessing this interface has UPDATE permission),
     * whether that means changing the members of those groups or changing the
     * groups of which those groups are members. If this information has not
     * yet been retrieved, this will be null.
     */
    availableGroups: string[] | null = null;

    /**
     * The identifiers of all users which can be manipulated (all users for
     * which the user accessing this interface has UPDATE permission), either
     * through adding those users as a member of the current group or removing
     * those users from the current group. If this information has not yet been
     * retrieved, this will be null.
     */
    availableUsers: string[] | null = null;

    /**
     * The identifiers of all user groups of which this group is a member,
     * taking into account any user groups which will be added/removed when
     * saved. If this information has not yet been retrieved, this will be
     * null.
     */
    parentGroups: string[] | null = null;

    /**
     * The set of identifiers of all parent user groups to which this group
     * will be added when saved. Parent groups will only be present in this set
     * if they are manually added, and not later manually removed before
     * saving.
     */
    parentGroupsAdded: string[] = [];

    /**
     * The set of identifiers of all parent user groups from which this group
     * will be removed when saved. Parent groups will only be present in this
     * set if they are manually removed, and not later manually added before
     * saving.
     */
    parentGroupsRemoved: string[] = [];

    /**
     * The identifiers of all user groups which are members of this group,
     * taking into account any user groups which will be added/removed when
     * saved. If this information has not yet been retrieved, this will be
     * null.
     */
    memberGroups: string[] | null = null;

    /**
     * The set of identifiers of all member user groups which will be added to
     * this group when saved. Member groups will only be present in this set if
     * they are manually added, and not later manually removed before saving.
     */
    memberGroupsAdded: string[] = [];

    /**
     * The set of identifiers of all member user groups which will be removed
     * from this group when saved. Member groups will only be present in this
     * set if they are manually removed, and not later manually added before
     * saving.
     */
    memberGroupsRemoved: string[] = [];

    /**
     * The identifiers of all users which are members of this group, taking
     * into account any users which will be added/removed when saved. If this
     * information has not yet been retrieved, this will be null.
     */
    memberUsers: string[] | null = null;

    /**
     * The set of identifiers of all member users which will be added to this
     * group when saved. Member users will only be present in this set if they
     * are manually added, and not later manually removed before saving.
     */
    memberUsersAdded: string[] = [];

    /**
     * The set of identifiers of all member users which will be removed from
     * this group when saved. Member users will only be present in this set if
     * they are manually removed, and not later manually added before saving.
     */
    memberUsersRemoved: string[] = [];

    /**
     * For each applicable data source, the management-related actions that the
     * current user may perform on the user group currently being created
     * or modified, as a map of data source identifier to the
     * {@link ManagementPermissions} object describing the actions available
     * within that data source, or null if the current user's permissions have
     * not yet been loaded.
     */
    managementPermissions: Record<string, ManagementPermissions> | null = null;

    /**
     * All available user group attributes. This is only the set of attribute
     * definitions, organized as logical groupings of attributes, not attribute
     * values.
     */
    attributes: Form[] | null = null;

    /**
     * The form group for editing user group attributes.
     */
    attributesFormGroup: FormGroup = new FormGroup({});

    /**
     * Inject required services.
     */
    constructor(private authenticationService: AuthenticationService,
                private dataSourceService: DataSourceService,
                private membershipService: MembershipService,
                private permissionService: PermissionService,
                private requestService: RequestService,
                private schemaService: SchemaService,
                private userGroupService: UserGroupService,
                private userService: UserService,
                private formService: FormService,
                private router: Router,
                private route: ActivatedRoute,
                private destroyRef: DestroyRef) {
    }

    ngOnInit(): void {
        this.cloneSourceIdentifier = this.route.snapshot.queryParamMap.get('clone');

        // Populate interface with requested data
        forkJoin([
            this.loadRequestedUserGroup(),
            this.dataSourceService.apply((dataSource: string, userID: string) => this.permissionService.getEffectivePermissions(dataSource, userID), this.dataSources, this.currentUsername),
            this.userGroupService.getUserGroups(this.dataSource, [PermissionSet.ObjectPermissionType.UPDATE]),
            this.userService.getUsers(this.dataSource, [PermissionSet.ObjectPermissionType.UPDATE]),
            this.schemaService.getUserGroupAttributes(this.dataSource)
        ])
            .subscribe({
                next    : ([userGroupData, permissions, userGroups, users, attributes]) => {

                    this.attributes = attributes;

                    this.attributesFormGroup = this.formService.getFormGroup(this.attributes);
                    this.attributesFormGroup.valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
                        .subscribe((value) => {
                            this.userGroup!.attributes = value;
                        });

                    this.managementPermissions = {};
                    this.dataSources.forEach(dataSource => {

                        // Determine whether data source contains this user group
                        const exists = (dataSource in (this.userGroups || {}));

                        // Add the identifiers of all modifiable user groups
                        this.availableGroups = [];
                        for (const groupIdentifier in userGroups) {
                            const userGroup = userGroups[groupIdentifier];
                            this.availableGroups.push(userGroup.identifier!);
                        }

                        // Add the identifiers of all modifiable users
                        this.availableUsers = [];
                        for (const username in users) {
                            const user = users[username];
                            this.availableUsers.push(user.username);
                        }

                        // Calculate management actions available for this specific group
                        this.managementPermissions![dataSource] = ManagementPermissions.fromPermissionSet(
                            permissions[dataSource],
                            PermissionSet.SystemPermissionType.CREATE_USER_GROUP,
                            PermissionSet.hasUserGroupPermission,
                            exists ? this.identifier : undefined);

                    });

                }, error: this.requestService.WARN
            });

    }

    /**
     * Returns whether critical data has completed being loaded.
     *
     * @returns
     *     true if enough data has been loaded for the user group interface to
     *     be useful, false otherwise.
     */
    isLoaded(): this is NonNullableProperties<ManageUserGroupComponent, 'userGroups' | 'userGroup' | 'permissionFlags'
        | 'managementPermissions' | 'availableGroups' | 'availableUsers' | 'parentGroups'
        | 'memberGroups' | 'memberUsers' | 'attributes'> {

        return this.userGroups !== null
            && this.userGroup !== null
            && this.permissionFlags !== null
            && this.managementPermissions !== null
            && this.availableGroups !== null
            && this.availableUsers !== null
            && this.parentGroups !== null
            && this.memberGroups !== null
            && this.memberUsers !== null
            && this.attributes !== null;

    }

    /**
     * Returns whether the current user can edit the identifier of the user
     * group being edited.
     *
     * @returns
     *     true if the current user can edit the identifier of the user group
     *     being edited, false otherwise.
     */
    canEditIdentifier(): boolean {
        return !this.identifier;
    }

    /**
     * Loads the data required for performing the management task requested
     * through the route parameters given at load time, automatically preparing
     * the interface for editing an existing user group, cloning an existing
     * user group, or creating an entirely new user group.
     *
     * @returns
     *     An observable which completes when the interface has been prepared
     *     for performing the requested management task.
     */
    loadRequestedUserGroup(): Observable<void> {

        // Pull user group data and permissions if we are editing an existing
        // user group
        if (this.identifier)
            return this.loadExistingUserGroup(this.dataSource, this.identifier);

        // If we are cloning an existing user group, pull its data instead
        if (this.cloneSourceIdentifier)
            return this.loadClonedUserGroup(this.dataSource, this.cloneSourceIdentifier);

        // If we are creating a new user group, populate skeleton user group data
        return this.loadSkeletonUserGroup();

    }

    /**
     * Loads the data associated with the user group having the given
     * identifier, preparing the interface for making modifications to that
     * existing user group.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the user group
     *     to load.
     *
     * @param identifier
     *     The unique identifier of the user group to load.
     *
     * @returns
     *     An observable which completes when the interface has been prepared for
     *     editing the given user group.
     */
    private loadExistingUserGroup(dataSource: string, identifier: string): Observable<void> {
        const userGroups = from(this.dataSourceService.apply(
            (dataSource: string, identifier: string) => this.userGroupService.getUserGroup(dataSource, identifier),
            this.dataSources,
            identifier));

        // Use empty permission set if group cannot be found
        const permissions =
                  this.permissionService.getPermissions(this.dataSource, this.identifier!, true)
                      .pipe(catchError(this.requestService.defaultValue(new PermissionSet())));

        // Assume no parent groups if group cannot be found
        const parentGroups =
                  this.membershipService.getUserGroups(this.dataSource, this.identifier!, true)
                      .pipe(catchError(this.requestService.defaultValue([])));

        // Assume no member groups if group cannot be found
        const memberGroups =
                  this.membershipService.getMemberUserGroups(this.dataSource, this.identifier!)
                      .pipe(catchError(this.requestService.defaultValue([])));

        // Assume no member users if group cannot be found
        const memberUsers =
                  this.membershipService.getMemberUsers(this.dataSource, this.identifier!)
                      .pipe(catchError(this.requestService.defaultValue([])));

        return forkJoin([userGroups, permissions, parentGroups, memberGroups, memberUsers])
            .pipe(
                map(([userGroups, permissions, parentGroups, memberGroups, memberUsers]) => {

                    this.userGroups = userGroups;
                    this.parentGroups = parentGroups;
                    this.memberGroups = memberGroups;
                    this.memberUsers = memberUsers;

                    // Create skeleton user group if user group does not exist
                    this.userGroup = userGroups[dataSource] || new UserGroup({
                        'identifier': identifier
                    });

                    this.permissionFlags = PermissionFlagSet.fromPermissionSet(permissions);

                })
            );
    }

    /**
     * Loads the data associated with the user group having the given
     * identifier, preparing the interface for cloning that existing user
     * group.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the user group to
     *     be cloned.
     *
     * @param identifier
     *     The unique identifier of the user group being cloned.
     *
     * @returns {Promise}
     *     An observable which completes when the interface has been prepared for
     *     cloning the given user group.
     */
    private loadClonedUserGroup(dataSource: string, identifier: string): Observable<void> {
        return forkJoin([
            this.dataSourceService.apply((dataSource: string, identifier: string) =>
                this.userGroupService.getUserGroup(dataSource, identifier), [dataSource], identifier),
            this.permissionService.getPermissions(dataSource, identifier, true),
            this.membershipService.getUserGroups(dataSource, identifier, true),
            this.membershipService.getMemberUserGroups(dataSource, identifier),
            this.membershipService.getMemberUsers(dataSource, identifier)
        ])
            .pipe(
                map(([userGroups, permissions, parentGroups, memberGroups, memberUsers]) => {
                    this.userGroups = {};
                    this.userGroup = userGroups[dataSource];
                    this.parentGroups = parentGroups;
                    this.parentGroupsAdded = parentGroups;
                    this.memberGroups = memberGroups;
                    this.memberGroupsAdded = memberGroups;
                    this.memberUsers = memberUsers;
                    this.memberUsersAdded = memberUsers;

                    this.permissionFlags = PermissionFlagSet.fromPermissionSet(permissions);
                    this.permissionsAdded = permissions;

                })
            );
    }

    /**
     * Loads skeleton user group data, preparing the interface for creating a
     * new user group.
     *
     * @returns
     *     An observable which completes when the interface has been prepared for
     *     creating a new user group.
     */
    private loadSkeletonUserGroup(): Observable<void> {

        // No user groups exist regardless of data source if the user group is
        // being created
        this.userGroups = {};

        // Use skeleton user group object with no associated permissions
        this.userGroup = new UserGroup();
        this.parentGroups = [];
        this.memberGroups = [];
        this.memberUsers = [];
        this.permissionFlags = new PermissionFlagSet();

        return of(void (0));

    }

    /**
     * Returns the URL for the page which manages the user group currently
     * being edited under the given data source. The given data source need not
     * be the same as the data source currently selected.
     *
     * @param dataSource
     *     The unique identifier of the data source that the URL is being
     *     generated for.
     *
     * @returns
     *     The URL for the page which manages the user group currently being
     *     edited under the given data source.
     */
    getUserGroupURL(dataSource: string): string {
        return '/manage/' + encodeURIComponent(dataSource) + '/userGroups/' + encodeURIComponent(this.identifier || '');
    }

    /**
     * Cancels all pending edits, returning to the main list of user groups.
     */
    returnToUserGroupList(): void {
        this.router.navigate(['/settings/userGroups']);
    }

    /**
     * Cancels all pending edits, opening an edit page for a new user group
     * which is prepopulated with the data from the user currently being edited.
     */
    cloneUserGroup(): void {
        this.router.navigate(
            ['manage', encodeURIComponent(this.dataSource), 'userGroups'],
            { queryParams: { clone: this.identifier } }
        ).then(() => window.scrollTo(0, 0));
    }

    /**
     * Saves the current user group, creating a new user group or updating the
     * existing user group depending on context, returning a promise which is
     * resolved if the save operation succeeds and rejected if the save
     * operation fails.
     *
     * @returns
     *     An observable which completes if the save operation succeeds and is
     *     fails with an {@link Error} if the save operation fails.
     */
    saveUserGroup(): Observable<void> {

        // Save or create the user group, depending on whether the user group exists
        let saveUserGroup$: Observable<void>;
        if (this.dataSource in (this.userGroups || {}))
            saveUserGroup$ = this.userGroupService.saveUserGroup(this.dataSource, this.userGroup!);
        else
            saveUserGroup$ = this.userGroupService.createUserGroup(this.dataSource, this.userGroup!);

        return saveUserGroup$
            .pipe(
                switchMap(() => {

                    return forkJoin([
                        this.permissionService.patchPermissions(this.dataSource, this.userGroup!.identifier!, this.permissionsAdded, this.permissionsRemoved, true),
                        this.membershipService.patchUserGroups(this.dataSource, this.userGroup!.identifier!, this.parentGroupsAdded, this.parentGroupsRemoved, true),
                        this.membershipService.patchMemberUserGroups(this.dataSource, this.userGroup!.identifier!, this.memberGroupsAdded, this.memberGroupsRemoved),
                        this.membershipService.patchMemberUsers(this.dataSource, this.userGroup!.identifier!, this.memberUsersAdded, this.memberUsersRemoved)
                    ])
                        .pipe(map(() => void (0)));
                })
            );

    }

    /**
     * Deletes the current user group, returning a promise which is resolved if
     * the delete operation succeeds and rejected if the delete operation
     * fails.
     *
     * @returns
     *     An observable which completes if the delete operation succeeds and fails
     *     with an {@link Error} if the delete operation fails.
     */
    deleteUserGroup(): Observable<void> {
        return this.userGroupService.deleteUserGroup(this.dataSource, this.userGroup!);
    }

}
