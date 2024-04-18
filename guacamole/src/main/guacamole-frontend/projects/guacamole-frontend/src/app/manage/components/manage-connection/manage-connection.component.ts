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

import { Component, DestroyRef, Input, OnInit, ViewChild, ViewEncapsulation } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslocoService } from '@ngneat/transloco';
import { forkJoin, map, Observable, of } from 'rxjs';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import { FormService } from '../../../form/service/form.service';
import { GuacPagerComponent } from '../../../list/components/guac-pager/guac-pager.component';
import { DataSourceBuilderService } from '../../../list/services/data-source-builder.service';
import { DataSource } from '../../../list/types/DataSource';
import { ConnectionGroupService } from '../../../rest/service/connection-group.service';
import { ConnectionService } from '../../../rest/service/connection.service';
import { PermissionService } from '../../../rest/service/permission.service';
import { RequestService } from '../../../rest/service/request.service';
import { SchemaService } from '../../../rest/service/schema.service';
import { Connection } from '../../../rest/types/Connection';
import { ConnectionGroup } from '../../../rest/types/ConnectionGroup';
import { Form } from '../../../rest/types/Form';
import { PermissionSet } from '../../../rest/types/PermissionSet';
import { Protocol } from '../../../rest/types/Protocol';
import { HistoryEntryWrapper } from '../../types/HistoryEntryWrapper';
import { ManagementPermissions } from '../../types/ManagementPermissions';

/**
 * Component for editing or creating connections.
 */
@Component({
    selector     : 'guac-manage-connection',
    templateUrl  : './manage-connection.component.html',
    encapsulation: ViewEncapsulation.None
})
export class ManageConnectionComponent implements OnInit {

    /**
     * The unique identifier of the data source containing the connection being
     * edited.
     */
    @Input('dataSource') selectedDataSource!: string;

    /**
     * Reference to the instance of the pager component.
     */
    @ViewChild(GuacPagerComponent, { static: true }) pager!: GuacPagerComponent;


    /**
     * The identifier of the original connection from which this connection is
     * being cloned. Only valid if this is a new connection.
     */
    private cloneSourceIdentifier: string | null = null;

    /**
     * The identifier of the connection being edited. If a new connection is
     * being created, this will not be defined.
     */
    @Input('id') private identifier?: string;

    /**
     * All known protocols.
     */
    protocols?: Record<string, Protocol>;

    /**
     * The root connection group of the connection group hierarchy.
     */
    rootGroup?: ConnectionGroup;

    /**
     * The connection being modified.
     */
    connection?: Connection;

    /**
     * The parameter name/value pairs associated with the connection being
     * modified.
     */
    parameters?: Record<string, string>;

    /**
     * The form group for editing connection parameters.
     */
    parametersFormGroup: FormGroup = new FormGroup({});

    /**
     * The date format for use within the connection history.
     */
    historyDateFormat?: string;

    /**
     * The usage history of the connection being modified.
     */
    historyEntryWrappers?: HistoryEntryWrapper[];

    /**
     * Paginated view of the usage history of the connection being modified.
     */
    dataSourceView: DataSource<HistoryEntryWrapper> | null = null;

    /**
     * The management-related actions that the current user may perform on the
     * connection currently being created/modified, or undefined if the current
     * user's permissions have not yet been loaded.
     */
    managementPermissions?: ManagementPermissions;

    /**
     * All available connection attributes. This is only the set of attribute
     * definitions, organized as logical groupings of attributes, not attribute
     * values.
     */
    attributes?: Form[];

    /**
     * Form group for editing connection attributes.
     */
    connectionAttributesFormGroup: FormGroup = new FormGroup({});

    /**
     * Inject required services.
     */
    constructor(private router: Router,
                private route: ActivatedRoute,
                private authenticationService: AuthenticationService,
                private connectionService: ConnectionService,
                private connectionGroupService: ConnectionGroupService,
                private permissionService: PermissionService,
                private requestService: RequestService,
                private schemaService: SchemaService,
                private translocoService: TranslocoService,
                private formService: FormService,
                private dataSourceBuilderService: DataSourceBuilderService,
                private destroyRef: DestroyRef) {
    }

    ngOnInit(): void {
        this.cloneSourceIdentifier = this.route.snapshot.queryParamMap.get('clone');

        // Build the data source for the user group list entries.
        this.dataSourceView = this.dataSourceBuilderService.getBuilder<HistoryEntryWrapper>()
            .source([])
            .paginate(this.pager.page)
            .build();

        // Populate interface with requested data
        forkJoin([
            this.loadRequestedConnection(),
            this.schemaService.getConnectionAttributes(this.selectedDataSource),
            this.permissionService.getEffectivePermissions(this.selectedDataSource, this.authenticationService.getCurrentUsername()!),
            this.schemaService.getProtocols(this.selectedDataSource),
            this.connectionGroupService.getConnectionGroupTree(this.selectedDataSource, ConnectionGroup.ROOT_IDENTIFIER, [PermissionSet.ObjectPermissionType.ADMINISTER])
        ])
            .subscribe({
                next    : ([connectionData, attributes, permissions, protocols, rootGroup]) => {

                    this.dataSourceView?.updateSource(this.historyEntryWrappers!);

                    this.attributes = attributes;
                    this.connectionAttributesFormGroup = this.formService.getFormGroup(attributes);
                    this.connectionAttributesFormGroup.patchValue(this.connection?.attributes || {});
                    this.connectionAttributesFormGroup.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((value) => {
                        this.connection!.attributes = value;
                    });

                    this.protocols = protocols;
                    this.parametersFormGroup = this.formService.getFormGroup(this.protocols[this.connection!.protocol].connectionForms);
                    this.parametersFormGroup.patchValue(this.parameters || {});
                    this.parametersFormGroup.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((value) => {
                        this.parameters = value;
                    });

                    this.rootGroup = rootGroup;

                    this.managementPermissions = ManagementPermissions.fromPermissionSet(
                        permissions,
                        PermissionSet.SystemPermissionType.CREATE_CONNECTION,
                        PermissionSet.hasConnectionPermission,
                        this.identifier);

                }, error: this.requestService.DIE
            });

        // Get history date format
        this.translocoService.selectTranslate('MANAGE_CONNECTION.FORMAT_HISTORY_START').subscribe(historyDateFormat => {
            this.historyDateFormat = historyDateFormat;
        });
    }

    /**
     * Returns whether critical data has completed being loaded.
     *
     * @returns
     *     true if enough data has been loaded for the user interface to be
     *     useful, false otherwise.
     */
    isLoaded(): this is Required<ManageConnectionComponent> {

        return this.protocols !== undefined
            && this.rootGroup !== undefined
            && this.connection !== undefined
            && this.parameters !== undefined
            && this.historyDateFormat !== undefined
            && this.historyEntryWrappers !== undefined
            && this.managementPermissions !== undefined
            && this.attributes !== undefined
            && this.dataSourceView !== null;

    }

    /**
     * Loads the data required for performing the management task requested
     * through the route parameters given at load time, automatically preparing
     * the interface for editing an existing connection, cloning an existing
     * connection, or creating an entirely new connection.
     *
     * @returns
     *     An observable which completes when the interface has been prepared
     *     for performing the requested management task.
     */
    private loadRequestedConnection(): Observable<void> {

        // If we are editing an existing connection, pull its data
        if (this.identifier)
            return this.loadExistingConnection(this.selectedDataSource, this.identifier);

        // If we are cloning an existing connection, pull its data instead
        if (this.cloneSourceIdentifier)
            return this.loadClonedConnection(this.selectedDataSource, this.cloneSourceIdentifier);

        // If we are creating a new connection, populate skeleton connection data
        return this.loadSkeletonConnection();

    }

    /**
     * Loads the data associated with the connection having the given
     * identifier, preparing the interface for making modifications to that
     * existing connection.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the connection to
     *     load.
     *
     * @param identifier
     *     The identifier of the connection to load.
     *
     * @returns
     *     An observable which completes when the interface has been prepared for
     *     editing the given connection.
     */
    private loadExistingConnection(dataSource: string, identifier: string): Observable<void> {
        return forkJoin([
            this.connectionService.getConnection(dataSource, identifier),
            this.connectionService.getConnectionHistory(dataSource, identifier),
            this.connectionService.getConnectionParameters(dataSource, identifier)
        ]).pipe(
            map(([connection, historyEntries, parameters]) => {

                this.connection = connection;
                this.parameters = parameters;

                // Wrap all history entries for sake of display
                this.historyEntryWrappers = [];
                historyEntries.forEach(historyEntry => {
                    this.historyEntryWrappers!.push(new HistoryEntryWrapper(historyEntry));
                });

            })
        );
    }

    /**
     * Loads the data associated with the connection having the given
     * identifier, preparing the interface for cloning that existing
     * connection.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the connection
     *     to be cloned.
     *
     * @param identifier
     *     The identifier of the connection being cloned.
     *
     * @returns
     *     An observable which completes when the interface has been prepared for
     *     cloning the given connection.
     */
    private loadClonedConnection(dataSource: string, identifier: string): Observable<void> {
        return forkJoin([
            this.connectionService.getConnection(dataSource, identifier),
            this.connectionService.getConnectionParameters(dataSource, identifier)
        ]).pipe(
            map(([connection, parameters]) => {

                this.connection = connection;
                this.parameters = parameters;

                // Clear the identifier field because this connection is new
                delete this.connection.identifier;

                // Cloned connections have no history
                this.historyEntryWrappers = [];

            })
        );
    }

    /**
     * Loads skeleton connection data, preparing the interface for creating a
     * new connection.
     *
     * @returns
     *     An observable which completes when the interface has been prepared for
     *     creating a new connection.
     */
    private loadSkeletonConnection(): Observable<void> {

        // Use skeleton connection object with no associated permissions,
        // history, or parameters
        this.connection = new Connection({
            protocol        : 'vnc',
            parentIdentifier: this.route.snapshot.queryParamMap.get('parent') || undefined
        });

        this.historyEntryWrappers = [];
        this.parameters = {};

        return of(void (0));

    }

    /**
     * @borrows Protocol.getNamespace
     */
    getNamespace = Protocol.getNamespace;

    /**
     * @borrows Protocol.getName
     */
    getProtocolName = Protocol.getName;

    /**
     * Cancels all pending edits, returning to the main list of connections
     * within the selected data source.
     */
    returnToConnectionList(): void {
        this.router.navigate(['settings', encodeURIComponent(this.selectedDataSource), 'connections']);
    }

    /**
     * Cancels all pending edits, opening an edit page for a new connection
     * which is prepopulated with the data from the connection currently being edited.
     */
    cloneConnection(): void {
        this.router.navigate(
            ['manage', encodeURIComponent(this.selectedDataSource), 'connections'],
            { queryParams: { clone: this.identifier } }
        );
    }

    /**
     * Saves the current connection, creating a new connection or updating the
     * existing connection, returning an observable which completes if the save
     * operation succeeds and rejected if the save operation fails.
     *
     * @returns
     *     An observable which completes if the save operation succeeds and is
     *     rejected with an {@link Error} if the save operation fails.
     */
    saveConnection(): Observable<void> {

        this.connection!.parameters = this.parameters || undefined;

        // Save the connection
        return this.connectionService.saveConnection(this.selectedDataSource, this.connection!);

    }

    /**
     * Deletes the current connection, returning an observable which completes if
     * the delete operation succeeds and rejected if the delete operation fails.
     *
     * @returns
     *     An observable which completes if the delete operation succeeds and is
     *     rejected with an {@link Error} if the delete operation fails.
     */
    deleteConnection(): Observable<void> {
        return this.connectionService.deleteConnection(this.selectedDataSource, this.connection!);
    }

}
