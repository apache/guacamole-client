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
import { forkJoin, map, Observable, switchMap, tap } from 'rxjs';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import { FormService } from '../../../form/service/form.service';
import { ConnectionService } from '../../../rest/service/connection.service';
import { PermissionService } from '../../../rest/service/permission.service';
import { RequestService } from '../../../rest/service/request.service';
import { SchemaService } from '../../../rest/service/schema.service';
import { SharingProfileService } from '../../../rest/service/sharing-profile.service';
import { Connection } from '../../../rest/types/Connection';
import { Form } from '../../../rest/types/Form';
import { PermissionSet } from '../../../rest/types/PermissionSet';
import { Protocol } from '../../../rest/types/Protocol';
import { SharingProfile } from '../../../rest/types/SharingProfile';
import { NonNullableAllProperties } from '../../../util/utility-types';
import { ManagementPermissions } from '../../types/ManagementPermissions';

/**
 * Component for editing or creating sharing profiles.
 */
@Component({
    selector     : 'guac-manage-sharing-profile',
    templateUrl  : './manage-sharing-profile.component.html',
    encapsulation: ViewEncapsulation.None
})
export class ManageSharingProfileComponent implements OnInit {

    /**
     * The unique identifier of the data source containing the sharing profile
     * being edited.
     */
    @Input('dataSource') selectedDataSource!: string;

    /**
     * The identifier of the original sharing profile from which this sharing
     * profile is being cloned. Only valid if this is a new sharing profile.
     */
    private cloneSourceIdentifier: string | null = null;

    /**
     * The identifier of the sharing profile being edited. If a new sharing
     * profile is being created, this will not be defined.
     */
    @Input('id') private identifier?: string;

    /**
     * Map of protocol name to corresponding Protocol object.
     */
    protocols: Record<string, Protocol> | null = null;

    /**
     * The sharing profile being modified.
     */
    sharingProfile: SharingProfile | null = null;

    /**
     * The connection associated with the sharing profile being modified.
     */
    primaryConnection: Connection | null = null

    /**
     * The parameter name/value pairs associated with the sharing profile being
     * modified.
     */
    parameters: Record<string, string> | null = null;

    /**
     * The form group for editing sharing profile parameters.
     */
    parametersFormGroup: FormGroup = new FormGroup({});

    /**
     * The management-related actions that the current user may perform on the
     * sharing profile currently being created/modified, or null if the current
     * user's permissions have not yet been loaded.
     */
    managementPermissions: ManagementPermissions | null = null;

    /**
     * All available sharing profile attributes. This is only the set of
     * attribute definitions, organized as logical groupings of attributes, not
     * attribute values.
     */
    attributes?: Form[];

    /**
     * Form group for editing sharing profile attributes.
     */
    sharingProfileAttributesFormGroup: FormGroup = new FormGroup({});

    /**
     * Inject required services.
     */
    constructor(private router: Router,
                private route: ActivatedRoute,
                private authenticationService: AuthenticationService,
                private connectionService: ConnectionService,
                private permissionService: PermissionService,
                private requestService: RequestService,
                private schemaService: SchemaService,
                private sharingProfileService: SharingProfileService,
                private formService: FormService,
                private destroyRef: DestroyRef) {
    }

    /**
     * Query the user's permissions for the current sharing profile.
     */
    ngOnInit(): void {

        this.cloneSourceIdentifier = this.route.snapshot.queryParamMap.get('clone');

        forkJoin([
            this.loadRequestedSharingProfile(),
            this.schemaService.getSharingProfileAttributes(this.selectedDataSource),
            this.schemaService.getProtocols(this.selectedDataSource),
            this.permissionService.getEffectivePermissions(this.selectedDataSource, this.authenticationService.getCurrentUsername()!)
        ]).subscribe({
            next : ([_, attributes, protocols, permissions]) => {

                this.attributes = attributes;
                this.protocols = protocols;

                // Create form group for editing sharing profile attributes
                this.sharingProfileAttributesFormGroup = this.formService.getFormGroup(attributes);
                this.sharingProfileAttributesFormGroup.patchValue(this.sharingProfile?.attributes || {})
                this.sharingProfileAttributesFormGroup.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((value) => {
                    this.sharingProfile!.attributes = value;
                });

                // Create form group for editing sharing profile parameters
                this.parametersFormGroup = this.formService.getFormGroup(this.protocols[this.primaryConnection!.protocol].sharingProfileForms);
                this.parametersFormGroup.patchValue(this.parameters || {});
                this.parametersFormGroup.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((value) => {
                    this.parameters = value;
                });

                this.managementPermissions = ManagementPermissions.fromPermissionSet(
                    permissions,
                    PermissionSet.SystemPermissionType.CREATE_CONNECTION,
                    PermissionSet.hasConnectionPermission,
                    this.identifier);

            },
            error: this.requestService.DIE
        });

    }


    /**
     * Returns whether critical data has completed being loaded.
     *
     * @returns
     *     true if enough data has been loaded for the user interface to be
     *     useful, false otherwise.
     */
    isLoaded(): this is NonNullableAllProperties<ManageSharingProfileComponent> {

        return this.protocols !== null
            && this.sharingProfile !== null
            && this.primaryConnection !== null
            && this.parameters !== null
            && this.managementPermissions !== null
            && this.attributes !== null;

    }

    /**
     * Loads the data required for performing the management task requested
     * through the route parameters given at load time, automatically preparing
     * the interface for editing an existing sharing profile, cloning an
     * existing sharing profile, or creating an entirely new sharing profile.
     *
     * @returns
     *     An observable which completes when the interface has been prepared
     *     for performing the requested management task.
     */
    loadRequestedSharingProfile(): Observable<void> {

        // If we are editing an existing sharing profile, pull its data
        if (this.identifier)
            return this.loadExistingSharingProfile(this.selectedDataSource, this.identifier);

        // If we are cloning an existing sharing profile, pull its data instead
        if (this.cloneSourceIdentifier)
            return this.loadClonedSharingProfile(this.selectedDataSource, this.cloneSourceIdentifier);

        // If we are creating a new sharing profile, populate skeleton sharing
        // profile data
        return this.loadSkeletonSharingProfile(this.selectedDataSource);

    }

    /**
     * Loads the data associated with the sharing profile having the given
     * identifier, preparing the interface for making modifications to that
     * existing sharing profile.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the sharing
     *     profile to load.
     *
     * @param identifier
     *     The identifier of the sharing profile to load.
     *
     * @returns
     *     An observable which completes when the interface has been prepared for
     *     editing the given sharing profile.
     */
    private loadExistingSharingProfile(dataSource: string, identifier: string): Observable<void> {
        return forkJoin([
            this.sharingProfileService.getSharingProfile(dataSource, identifier),
            this.sharingProfileService.getSharingProfileParameters(dataSource, identifier)
        ]).pipe(
            // Store sharing profile and parameters
            tap(([sharingProfile, parameters]) => {
                this.sharingProfile = sharingProfile;
                this.parameters = parameters;
            }),

            // Load connection object for associated primary connection
            switchMap(([sharingProfile]) => {
                return this.connectionService.getConnection(
                    dataSource,
                    sharingProfile.primaryConnectionIdentifier!
                );
            }),

            // Store connection object and map observable to void
            map(connection => {
                this.primaryConnection = connection;
            })
        );
    }

    /**
     * Loads the data associated with the sharing profile having the given
     * identifier, preparing the interface for cloning that existing
     * sharing profile.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the sharing
     *     profile to be cloned.
     *
     * @param identifier
     *     The identifier of the sharing profile being cloned.
     *
     * @returns
     *     An observable which completes when the interface has been prepared for
     *     cloning the given sharing profile.
     */
    private loadClonedSharingProfile(dataSource: string, identifier: string): Observable<void> {
        return forkJoin([
            this.sharingProfileService.getSharingProfile(dataSource, identifier),
            this.sharingProfileService.getSharingProfileParameters(dataSource, identifier)
        ]).pipe(
            tap(([sharingProfile, parameters]) => {
                this.sharingProfile = sharingProfile;
                this.parameters = parameters;

                // Clear the identifier field because this sharing profile is new
                delete this.sharingProfile.identifier;
            }),

            // Load connection object for associated primary connection
            switchMap(([sharingProfile]) => {
                return this.connectionService.getConnection(
                    dataSource,
                    sharingProfile.primaryConnectionIdentifier!
                );
            }),

            // Store connection object and map observable to void
            map(connection => {
                this.primaryConnection = connection;
            })
        );
    }

    /**
     * Loads skeleton sharing profile data, preparing the interface for
     * creating a new sharing profile.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the sharing
     *     profile to be created.
     *
     * @returns
     *     An observable which completes when the interface has been prepared for
     *     creating a new sharing profile.
     */
    private loadSkeletonSharingProfile(dataSource: string): Observable<void> {

        // Use skeleton sharing profile object with no associated parameters
        this.sharingProfile = new SharingProfile({
            primaryConnectionIdentifier: this.route.snapshot.queryParamMap.get('parent')!
        });
        this.parameters = {};

        // Load connection object for associated primary connection
        return this.connectionService.getConnection(
            dataSource,
            this.sharingProfile.primaryConnectionIdentifier!
        ).pipe(
            // Store connection object and map observable to void
            map(connection => {
                this.primaryConnection = connection;
            })
        );
    }

    /**
     * @borrows Protocol.getNamespace
     */
    getNamespace = Protocol.getNamespace;

    /**
     * Cancels all pending edits, returning to the main list of connections
     * within the selected data source.
     */
    returnToConnectionList(): void {
        this.router.navigate(['settings', encodeURIComponent(this.selectedDataSource), 'connections']);
    }

    /**
     * Cancels all pending edits, opening an edit page for a new sharing profile
     * which is prepopulated with the data from the sharing profile currently
     * being edited.
     */
    cloneSharingProfile(): void {
        this.router.navigate(
            ['manage', encodeURIComponent(this.selectedDataSource), 'sharingProfiles'],
            { queryParams: { clone: this.identifier } }
        );
    }

    /**
     * Saves the current sharing profile, creating a new sharing profile or
     * updating the existing sharing profile, returning a promise which is
     * resolved if the save operation succeeds and rejected if the save
     * operation fails.
     *
     * @returns
     *     An observable which completes if the save operation succeeds and is
     *     rejected with an {@link Error} if the save operation fails.
     */
    saveSharingProfile(): Observable<void> {

        this.sharingProfile!.parameters = this.parameters || undefined;

        // Save the sharing profile
        return this.sharingProfileService.saveSharingProfile(this.selectedDataSource, this.sharingProfile!);

    }

    /**
     * Deletes the current sharing profile, returning a promise which is
     * resolved if the delete operation succeeds and rejected if the delete
     * operation fails.
     *
     * @returns
     *     An observable which completes if the delete operation succeeds and is
     *     rejected with an {@link Error} if the delete operation fails.
     */
    deleteSharingProfile(): Observable<void> {
        return this.sharingProfileService.deleteSharingProfile(this.selectedDataSource, this.sharingProfile!);
    }

}
