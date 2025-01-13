

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

import { Component, Input, OnInit, ViewEncapsulation } from '@angular/core';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import { DataSourceService } from '../../../rest/service/data-source-service.service';
import { PermissionService } from '../../../rest/service/permission.service';
import { RequestService } from '../../../rest/service/request.service';
import { PermissionFlagSet } from '../../../rest/types/PermissionFlagSet';
import { PermissionSet } from '../../../rest/types/PermissionSet';

/**
 * A directive for manipulating the system permissions granted within a given
 * {@link PermissionFlagSet}, tracking the specific permissions added or
 * removed within a separate pair of {@link PermissionSet} objects. Optionally,
 * the permission for a particular user to update themselves (change their own
 * password/attributes) may also be manipulated.
 */
@Component({
    selector     : 'system-permission-editor',
    templateUrl  : './system-permission-editor.component.html',
    encapsulation: ViewEncapsulation.None
})
export class SystemPermissionEditorComponent implements OnInit {

    /**
     * The unique identifier of the data source associated with the
     * permissions being manipulated.
     */
    @Input({ required: true }) dataSource!: string;

    /**
     * The username of the user whose self-update permission (whether
     * the user has permission to update their own user account) should
     * be additionally controlled by this editor. If no such user
     * permissions should be controlled, this should be left undefined.
     */
    @Input() username?: string;

    /**
     * The current state of the permissions being manipulated. This
     * {@link PermissionFlagSet} will be modified as changes are made
     * through this permission editor.
     */
    @Input({ required: true }) permissionFlags!: PermissionFlagSet;

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
     * The identifiers of all data sources currently available to the
     * authenticated user.
     */
    private dataSources: string[] = [];

    /**
     * The username of the current, authenticated user.
     */
    private currentUsername: string | null = null;

    /**
     * The permissions granted to the currently-authenticated user.
     */
    private permissions: Record<string, PermissionSet> | null = null;


    /**
     * Available system permission types, as translation string / internal
     * value pairs.
     */
    systemPermissionTypes: { label: string; value: PermissionSet.SystemPermissionType }[] = [
        {
            label: 'MANAGE_USER.FIELD_HEADER_ADMINISTER_SYSTEM',
            value: PermissionSet.SystemPermissionType.ADMINISTER
        },
        {
            label: "MANAGE_USER.FIELD_HEADER_AUDIT_SYSTEM",
            value: PermissionSet.SystemPermissionType.AUDIT
        },
        {
            label: 'MANAGE_USER.FIELD_HEADER_CREATE_NEW_USERS',
            value: PermissionSet.SystemPermissionType.CREATE_USER
        },
        {
            label: 'MANAGE_USER.FIELD_HEADER_CREATE_NEW_USER_GROUPS',
            value: PermissionSet.SystemPermissionType.CREATE_USER_GROUP
        },
        {
            label: 'MANAGE_USER.FIELD_HEADER_CREATE_NEW_CONNECTIONS',
            value: PermissionSet.SystemPermissionType.CREATE_CONNECTION
        },
        {
            label: 'MANAGE_USER.FIELD_HEADER_CREATE_NEW_CONNECTION_GROUPS',
            value: PermissionSet.SystemPermissionType.CREATE_CONNECTION_GROUP
        },
        {
            label: 'MANAGE_USER.FIELD_HEADER_CREATE_NEW_SHARING_PROFILES',
            value: PermissionSet.SystemPermissionType.CREATE_SHARING_PROFILE
        }
    ];


    constructor(private authenticationService: AuthenticationService,
                private dataSourceService: DataSourceService,
                private permissionService: PermissionService,
                private requestService: RequestService) {
    }

    ngOnInit(): void {
        this.dataSources = this.authenticationService.getAvailableDataSources();
        this.currentUsername = this.authenticationService.getCurrentUsername();

        // Query the permissions granted to the currently-authenticated user
        this.dataSourceService.apply(
            (dataSource: string, userID: string) => this.permissionService.getEffectivePermissions(dataSource, userID),
            this.dataSources,
            this.currentUsername
        )
            .then(permissions => {
                this.permissions = permissions;
            }, this.requestService.PROMISE_DIE);


    }

    /**
     * Returns whether the current user has permission to change the system
     * permissions granted to users.
     *
     * @returns
     *     true if the current user can grant or revoke system permissions
     *     to the permission set being edited, false otherwise.
     */
    canChangeSystemPermissions(): boolean {

        // Do not check if permissions are not yet loaded
        if (!this.permissions)
            return false;

        // Only the administrator can modify system permissions
        return PermissionSet.hasSystemPermission(this.permissions[this.dataSource],
            PermissionSet.SystemPermissionType.ADMINISTER);

    }

    /**
     * Updates the permissionsAdded and permissionsRemoved permission sets
     * to reflect the addition of the given system permission.
     *
     * @param type
     *     The system permission to add, as defined by
     *     PermissionSet.SystemPermissionType.
     */
    addSystemPermission(type: PermissionSet.SystemPermissionType): void {

        // If permission was previously removed, simply un-remove it
        if (PermissionSet.hasSystemPermission(this.permissionsRemoved, type))
            PermissionSet.removeSystemPermission(this.permissionsRemoved, type);

        // Otherwise, explicitly add the permission
        else
            PermissionSet.addSystemPermission(this.permissionsAdded, type);

    }

    /**
     * Updates the permissionsAdded and permissionsRemoved permission sets
     * to reflect the removal of the given system permission.
     *
     * @param type
     *     The system permission to remove, as defined by
     *     PermissionSet.SystemPermissionType.
     */
    removeSystemPermission(type: PermissionSet.SystemPermissionType): void {

        // If permission was previously added, simply un-add it
        if (PermissionSet.hasSystemPermission(this.permissionsAdded, type))
            PermissionSet.removeSystemPermission(this.permissionsAdded, type);

        // Otherwise, explicitly remove the permission
        else
            PermissionSet.addSystemPermission(this.permissionsRemoved, type);

    }

    /**
     * Notifies the controller that a change has been made to the given
     * system permission for the permission set being edited.
     *
     * @param type
     *     The system permission that was changed, as defined by
     *     PermissionSet.SystemPermissionType.
     */
    systemPermissionChanged(type: PermissionSet.SystemPermissionType): void {

        // Determine current permission setting
        const granted = this.permissionFlags.systemPermissions[type];

        // Add/remove permission depending on flag state
        if (granted)
            this.addSystemPermission(type);
        else
            this.removeSystemPermission(type);

    }

    /**
     * Updates the permissionsAdded and permissionsRemoved permission sets
     * to reflect the addition of the given user permission.
     *
     * @param type
     *     The user permission to add, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param identifier
     *     The identifier of the user affected by the permission being added.
     */
    addUserPermission(type: PermissionSet.ObjectPermissionType, identifier: string): void {

        // If permission was previously removed, simply un-remove it
        if (PermissionSet.hasUserPermission(this.permissionsRemoved, type, identifier))
            PermissionSet.removeUserPermission(this.permissionsRemoved, type, identifier);

        // Otherwise, explicitly add the permission
        else
            PermissionSet.addUserPermission(this.permissionsAdded, type, identifier);

    }

    /**
     * Updates the permissionsAdded and permissionsRemoved permission sets
     * to reflect the removal of the given user permission.
     *
     * @param type
     *     The user permission to remove, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param identifier
     *     The identifier of the user affected by the permission being
     *     removed.
     */
    removeUserPermission(type: PermissionSet.ObjectPermissionType, identifier: string): void {

        // If permission was previously added, simply un-add it
        if (PermissionSet.hasUserPermission(this.permissionsAdded, type, identifier))
            PermissionSet.removeUserPermission(this.permissionsAdded, type, identifier);

        // Otherwise, explicitly remove the permission
        else
            PermissionSet.addUserPermission(this.permissionsRemoved, type, identifier);

    }

    /**
     * Notifies the controller that a change has been made to the given user
     * permission for the permission set being edited.
     *
     * @param type
     *     The user permission that was changed, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param identifier
     *     The identifier of the user affected by the changed permission.
     */
    userPermissionChanged(type: PermissionSet.ObjectPermissionType, identifier: string): void {

        // Determine current permission setting
        const granted = this.permissionFlags.userPermissions[type][identifier];

        // Add/remove permission depending on flag state
        if (granted)
            this.addUserPermission(type, identifier);
        else
            this.removeUserPermission(type, identifier);

    }

    /**
     * Make the ObjectPermissionType enum available to the template.
     */
    protected readonly ObjectPermissionType = PermissionSet.ObjectPermissionType;
}
