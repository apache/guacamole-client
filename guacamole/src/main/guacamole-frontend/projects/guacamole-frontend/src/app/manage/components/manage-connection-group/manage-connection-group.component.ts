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
import { PermissionService } from '../../../rest/service/permission.service';
import { RequestService } from '../../../rest/service/request.service';
import { SchemaService } from '../../../rest/service/schema.service';
import { ActivatedRoute, Router } from '@angular/router';
import { ManagementPermissions } from '../../types/ManagementPermissions';
import { Form } from '../../../rest/types/Form';
import { NonNullableProperties, Optional } from '../../../util/utility-types';
import { ConnectionGroup } from '../../../rest/types/ConnectionGroup';
import { forkJoin, map, Observable, of } from 'rxjs';
import { ConnectionGroupService } from '../../../rest/service/connection-group.service';
import { PermissionSet } from '../../../rest/types/PermissionSet';
import { FormService } from '../../../form/service/form.service';
import { FormGroup } from '@angular/forms';

/**
 * Type of {@link ConnectionGroup} which has been cloned from another connection
 * group. For a cloned connection group, the identifier is optional.
 */
type ClonedConnectionGroup = Optional<ConnectionGroup, 'identifier'>;

/**
 * The component for editing or creating connection groups.
 */
@Component({
    selector: 'guac-manage-connection-group',
    templateUrl: './manage-connection-group.component.html',
    encapsulation: ViewEncapsulation.None
})
export class ManageConnectionGroupComponent implements OnInit {

    /**
     * The identifier of the user group being edited. If a new user group is
     * being created, this will not be defined.
     */
    @Input({alias: 'id'}) private identifier?: string;

    /**
     * The unique identifier of the data source containing the connection group
     * being edited.
     */
    @Input({alias: 'dataSource'}) selectedDataSource!: string;

    /**
     * The identifier of the original connection group from which this
     * connection group is being cloned. Only valid if this is a new
     * connection group.
     */
    private cloneSourceIdentifier: string | null = null;

    /**
     * Available connection group types, as translation string / internal value
     * pairs.
     */
    types: { label: string; value: string }[] = [
        {
            label: 'MANAGE_CONNECTION_GROUP.NAME_TYPE_ORGANIZATIONAL',
            value: ConnectionGroup.Type.ORGANIZATIONAL
        },
        {
            label: 'MANAGE_CONNECTION_GROUP.NAME_TYPE_BALANCING',
            value: ConnectionGroup.Type.BALANCING
        }
    ];

    /**
     * The root connection group of the connection group hierarchy.
     */
    rootGroup: ConnectionGroup | null = null;

    /**
     * The connection group being modified.
     */
    connectionGroup: ConnectionGroup | ClonedConnectionGroup | null = null;

    /**
     * The management-related actions that the current user may perform on the
     * connection group currently being created/modified, or null if the current
     * user's permissions have not yet been loaded.
     */
    managementPermissions: ManagementPermissions | null = null;

    /**
     * All available connection group attributes. This is only the set of
     * attribute definitions, organized as logical groupings of attributes, not
     * attribute values.
     */
    attributes: Form[] | null = null;

    /**
     * Form group for editing connection group attributes.
     */
    connectionGroupAttributesFormGroup: FormGroup = new FormGroup({});

    /**
     * Inject required services.
     */
    constructor(private authenticationService: AuthenticationService,
                private connectionGroupService: ConnectionGroupService,
                private permissionService: PermissionService,
                private requestService: RequestService,
                private schemaService: SchemaService,
                private formService: FormService,
                private router: Router,
                private route: ActivatedRoute) {
    }

    ngOnInit(): void {
        this.cloneSourceIdentifier = this.route.snapshot.queryParamMap.get('clone');

        // Query the user's permissions for the current connection group
        const connectionGroupData = this.loadRequestedConnectionGroup();
        const attributes = this.schemaService.getConnectionGroupAttributes(this.selectedDataSource);
        const permissions = this.permissionService.getEffectivePermissions(this.selectedDataSource, this.authenticationService.getCurrentUsername()!);
        const rootGroup = this.connectionGroupService.getConnectionGroupTree(this.selectedDataSource, ConnectionGroup.ROOT_IDENTIFIER, [PermissionSet.ObjectPermissionType.ADMINISTER]);


        forkJoin([connectionGroupData, attributes, permissions, rootGroup])
            .subscribe({

                next: ([_, attributes, permissions, rootGroup]) => {

                    this.attributes = attributes;
                    this.rootGroup = rootGroup;

                    this.connectionGroupAttributesFormGroup = this.formService.getFormGroup(attributes);
                    this.connectionGroupAttributesFormGroup.patchValue(this.connectionGroup?.attributes || {});
                    this.connectionGroupAttributesFormGroup.valueChanges.subscribe((value) => {
                        if (this.connectionGroup)
                            this.connectionGroup.attributes = value;
                    });

                    this.managementPermissions = ManagementPermissions.fromPermissionSet(
                        permissions,
                        PermissionSet.SystemPermissionType.CREATE_CONNECTION,
                        PermissionSet.hasConnectionGroupPermission,
                        this.identifier);

                }, error: this.requestService.DIE

            });

    }

    /**
     * Returns whether critical data has completed being loaded.
     *
     * @returns
     *     true if enough data has been loaded for the user interface to be
     *     useful, false otherwise.
     */
    isLoaded(): this is NonNullableProperties<ManageConnectionGroupComponent,
        'rootGroup' | 'connectionGroup' | 'managementPermissions' | 'attributes'> {

        return this.rootGroup !== null
            && this.connectionGroup !== null
            && this.managementPermissions !== null
            && this.attributes !== null;

    }

    /**
     * Loads the data associated with the connection group having the given
     * identifier, preparing the interface for making modifications to that
     * existing connection group.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the connection
     *     group to load.
     *
     * @param identifier
     *     The identifier of the connection group to load.
     *
     * @returns
     *     An observable which emits when the interface has been prepared for
     *     editing the given connection group.
     */
    private loadExistingConnectionGroup(dataSource: string, identifier: string): Observable<void> {
        return this.connectionGroupService.getConnectionGroup(
            dataSource,
            identifier
        )
            .pipe(map(connectionGroup => {
                this.connectionGroup = connectionGroup;
            }));
    }

    /**
     * Loads the data associated with the connection group having the given
     * identifier, preparing the interface for cloning that existing
     * connection group.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the connection
     *     group to be cloned.
     *
     * @param identifier
     *     The identifier of the connection group being cloned.
     *
     * @returns
     *     An observable which emits when the interface has been prepared for
     *     cloning the given connection group.
     */
    private loadClonedConnectionGroup(dataSource: string, identifier: string): Observable<void> {
        return this.connectionGroupService.getConnectionGroup(
            dataSource,
            identifier
        )
            .pipe(map((connectionGroup: ClonedConnectionGroup) => {
                this.connectionGroup = connectionGroup;
                delete this.connectionGroup.identifier;
            }));
    }

    /**
     * Loads skeleton connection group data, preparing the interface for
     * creating a new connection group.
     *
     * @returns
     *     An observable which emits when the interface has been prepared for
     *     creating a new connection group.
     */
    private loadSkeletonConnectionGroup(): Observable<void> {

        // Use skeleton connection group object with specified parent
        this.connectionGroup = new ConnectionGroup({
            identifier: '',
            name: '',
            type: '',
            activeConnections: 0,
            parentIdentifier: this.route.snapshot.queryParamMap.get('parent') || undefined
        });

        return of(void (0));

    }

    /**
     * Loads the data required for performing the management task requested
     * through the route parameters given at load time, automatically preparing
     * the interface for editing an existing connection group, cloning an
     * existing connection group, or creating an entirely new connection group.
     *
     * @returns
     *     An observable which emits when the interface has been prepared
     *     for performing the requested management task.
     */
    private loadRequestedConnectionGroup(): Observable<void> {

        // If we are editing an existing connection group, pull its data
        if (this.identifier)
            return this.loadExistingConnectionGroup(this.selectedDataSource, this.identifier);

        // If we are cloning an existing connection group, pull its data
        // instead
        if (this.cloneSourceIdentifier)
            return this.loadClonedConnectionGroup(this.selectedDataSource, this.cloneSourceIdentifier);

        // If we are creating a new connection group, populate skeleton
        // connection group data
        return this.loadSkeletonConnectionGroup();

    }

    /**
     * Cancels all pending edits, returning to the main list of connections
     * within the selected data source.
     */
    returnToConnectionList() {
        this.router.navigate(['settings', encodeURIComponent(this.selectedDataSource), 'connections']);
    }

    /**
     * Cancels all pending edits, opening an edit page for a new connection
     * group which is prepopulated with the data from the connection group
     * currently being edited.
     */
    cloneConnectionGroup() {
        this.router.navigate(['manage', encodeURIComponent(this.selectedDataSource), 'connectionGroups'],
            {queryParams: {clone: this.identifier}});
    }

    /**
     * Saves the current connection group, creating a new connection group or
     * updating the existing connection group, returning a promise which is
     * resolved if the save operation succeeds and rejected if the save
     * operation fails.
     *
     * @returns
     *     An observable which completes if the save operation succeeds and is
     *     fails with an {@link Error} if the save operation fails.
     */
    saveConnectionGroup(): Observable<void> {
        return this.connectionGroupService.saveConnectionGroup(this.selectedDataSource, this.connectionGroup as ConnectionGroup);
    }

    /**
     * Deletes the current connection group, returning a promise which is
     * resolved if the delete operation succeeds and rejected if the delete
     * operation fails.
     *
     * @returns
     *     An observable which completes if the delete operation succeeds and is
     *     fails with an {@link Error} if the delete operation fails.
     */
    deleteConnectionGroup(): Observable<void> {
        return this.connectionGroupService.deleteConnectionGroup(this.selectedDataSource, this.connectionGroup as ConnectionGroup);
    }
}
