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
import { ActiveConnectionService } from '../../../rest/service/active-connection.service';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import { ConnectionGroupService } from '../../../rest/service/connection-group.service';
import { DataSourceService } from '../../../rest/service/data-source-service.service';
import { RequestService } from '../../../rest/service/request.service';
import { GuacNotificationService } from '../../../notification/services/guac-notification.service';
import { ActiveConnectionWrapper } from '../../types/ActiveConnectionWrapper';
import { SortOrder } from '../../../list/types/SortOrder';
import { ActiveConnection } from '../../../rest/types/ActiveConnection';
import { Connection } from '../../../rest/types/Connection';
import { formatDate } from '@angular/common';
import { NonNullableProperties } from '../../../util/utility-types';
import { NotificationAction } from '../../../notification/types/NotificationAction';
import { BehaviorSubject, forkJoin, Observable, take } from 'rxjs';
import { ClientIdentifier } from '../../../navigation/types/ClientIdentifier';
import { ConnectionGroup } from '../../../rest/types/ConnectionGroup';
import { TranslocoService } from '@ngneat/transloco';
import { SortService } from '../../../list/services/sort.service';
import { ClientIdentifierService } from '../../../navigation/service/client-identifier.service';
import { GuacPagerComponent } from '../../../list/components/guac-pager/guac-pager.component';
import { GuacFilterComponent } from '../../../list/components/guac-filter/guac-filter.component';
import { DataSource } from '../../../list/types/DataSource';
import { DataSourceBuilderService } from '../../../list/services/data-source-builder.service';

/**
 * A component for managing all active Guacamole sessions.
 */
@Component({
    selector: 'guac-guac-settings-sessions',
    templateUrl: './guac-settings-sessions.component.html',
    encapsulation: ViewEncapsulation.None
})
export class GuacSettingsSessionsComponent implements OnInit {

    /**
     * Reference to the instance of the pager component.
     */
    @ViewChild(GuacPagerComponent, {static: true}) pager!: GuacPagerComponent;

    /**
     * Reference to the instance of the filter component.
     */
    @ViewChild(GuacFilterComponent, {static: true}) filter!: GuacFilterComponent;

    /**
     * The identifiers of all data sources accessible by the current
     * user.
     */
    private dataSources: string[] = this.authenticationService.getAvailableDataSources();

    /**
     * TODO: document
     */
    dataSourceView: DataSource<ActiveConnectionWrapper> | null = null;

    /**
     * The ActiveConnectionWrappers of all active sessions accessible
     * by the current user, or null if the active sessions have not yet
     * been loaded.
     */
    wrappers: ActiveConnectionWrapper[] | null = null;

    /**
     * Initial SortOrder instance which maintains the sort order of the visible
     * connection wrappers.
     */
    readonly initialOrder = new SortOrder([
        'activeConnection.username',
        'startDate',
        'activeConnection.remoteHost',
        'name'
    ]);

    /**
     * Observable of the current SortOrder instance which stores the sort order of the
     * visible connection wrappers. The value is updated by the GuacSortOrderDirective.
     */
    order: BehaviorSubject<SortOrder> = new BehaviorSubject(this.initialOrder);

    /**
     * Array of all wrapper properties that are filterable.
     */
    filteredWrapperProperties: string[] = [
        'activeConnection.username',
        'startDate',
        'activeConnection.remoteHost',
        'name'
    ];

    /**
     * All active connections, if known, grouped by corresponding data
     * source identifier, or null if active connections have not yet
     * been loaded.
     */
    private allActiveConnections: Record<string, Record<string, ActiveConnection>> | null = null;

    /**
     * Map of all visible connections by data source identifier and
     * object identifier, or null if visible connections have not yet
     * been loaded.
     */
    private allConnections: Record<string, Record<string, Connection>> | null = null;

    /**
     * The date format for use for session-related dates.
     */
    private sessionDateFormat: string | null = null;

    /**
     * Map of all currently-selected active connection wrappers by
     * data source and identifier.
     */
    private allSelectedWrappers: Record<string, Record<string, ActiveConnectionWrapper>> = {};

    /**
     * Inject required services.
     */
    constructor(private activeConnectionService: ActiveConnectionService,
                private authenticationService: AuthenticationService,
                private connectionGroupService: ConnectionGroupService,
                private dataSourceService: DataSourceService,
                private guacNotification: GuacNotificationService,
                private requestService: RequestService,
                private translocoService: TranslocoService,
                private sortService: SortService,
                private clientIdentifierService: ClientIdentifierService,
                private dataSourceBuilderService: DataSourceBuilderService) {
    }

    ngOnInit(): void {

        // Build the data source for the connection list entries.
        this.dataSourceView = this.dataSourceBuilderService.getBuilder<ActiveConnectionWrapper>()
            .source([])
            .filter(this.filter.searchStringChange, this.filteredWrapperProperties)
            .sort(this.order)
            .paginate(this.pager.page)
            .build();

        // Retrieve all connections
        this.dataSourceService.apply(
            (dataSource: string, connectionGroupID: string) => this.connectionGroupService.getConnectionGroupTree(dataSource, connectionGroupID),
            this.dataSources,
            ConnectionGroup.ROOT_IDENTIFIER
        )
            .then(rootGroups => {

                this.allConnections = {};

                // Load connections from each received root group
                for (const dataSource in rootGroups) {
                    const rootGroup = rootGroups[dataSource];
                    this.allConnections[dataSource] = {};
                    this.addDescendantConnections(dataSource, rootGroup);
                }

                // Attempt to produce wrapped list of active connections
                this.wrapAllActiveConnections();

            }, this.requestService.PROMISE_DIE);

        // Query active sessions
        this.dataSourceService.apply(
            (dataSource: string) => this.activeConnectionService.getActiveConnections(dataSource),
            this.dataSources
        )
            .then(retrievedActiveConnections => {

                // Store received map of active connections
                this.allActiveConnections = retrievedActiveConnections;

                // Attempt to produce wrapped list of active connections
                this.wrapAllActiveConnections();

            }, this.requestService.PROMISE_DIE);

        // Get session date format
        this.translocoService.selectTranslate('SETTINGS_SESSIONS.FORMAT_STARTDATE')
            .pipe(take(1))
            .subscribe(retrievedSessionDateFormat => {

                // Store received date format
                this.sessionDateFormat = retrievedSessionDateFormat;

                // Attempt to produce wrapped list of active connections
                this.wrapAllActiveConnections();

            });

    }


    /**
     * Adds the given connection to the internal set of visible
     * connections.
     *
     * @param dataSource
     *     The identifier of the data source associated with the given
     *     connection.
     *
     * @param connection
     *     The connection to add to the internal set of visible
     *     connections.
     */
    private addConnection(dataSource: string, connection: Connection): void {

        // Add given connection to set of visible connections
        (this.allConnections!)[dataSource][connection.identifier!] = connection;

    }

    /**
     * Adds all descendant connections of the given connection group to
     * the internal set of connections.
     *
     * @param dataSource
     *     The identifier of the data source associated with the given
     *     connection group.
     *
     * @param connectionGroup
     *     The connection group whose descendant connections should be
     *     added to the internal set of connections.
     */
    private addDescendantConnections(dataSource: string, connectionGroup: ConnectionGroup): void {

        // Add all child connections
        connectionGroup.childConnections?.forEach(connection => {
            this.addConnection(dataSource, connection);
        });

        // Add all child connection groups
        connectionGroup.childConnectionGroups?.forEach(connectionGroup => {
            this.addDescendantConnections(dataSource, connectionGroup);
        });

    }

    /**
     * Wraps all loaded active connections, storing the resulting array
     * within the scope. If required data has not yet finished loading,
     * this function has no effect.
     */
    private wrapAllActiveConnections(): void {

        // Abort if not all required data is available
        if (!this.allActiveConnections || !this.allConnections || !this.sessionDateFormat)
            return;

        // Wrap all active connections for sake of display
        this.wrappers = [];
        for (const dataSource in this.allActiveConnections) {
            const activeConnections = this.allActiveConnections[dataSource];
            for (const identifier in activeConnections) {
                const activeConnection = activeConnections[identifier];

                // Retrieve corresponding connection
                const connection = this.allConnections[dataSource][activeConnection.connectionIdentifier!];

                // Add wrapper
                if (activeConnection.username !== null) {
                    this.wrappers.push(new ActiveConnectionWrapper({
                        dataSource: dataSource,
                        name: connection.name,
                        startDate: formatDate(activeConnection.startDate || 0, this.sessionDateFormat, 'en-US'),
                        activeConnection: activeConnection
                    }));
                }

            }

        }

        this.dataSourceView?.updateSource(this.wrappers);

    }

    /**
     * Returns whether critical data has completed being loaded.
     *
     * @returns
     *     true if enough data has been loaded for the user interface
     *     to be useful, false otherwise.
     */
    isLoaded(): this is NonNullableProperties<GuacSettingsSessionsComponent, 'wrappers' | 'dataSourceView'> {
        return this.wrappers !== null
            && this.dataSourceView !== null;
    }

    /**
     * An action to be provided along with the object sent to
     * showStatus which closes the currently-shown status dialog.
     */
    private readonly CANCEL_ACTION: NotificationAction = {
        name: 'SETTINGS_SESSIONS.ACTION_CANCEL',
        // Handle action
        callback: () => {
            this.guacNotification.showStatus(false);
        }
    };

    /**
     * An action to be provided along with the object sent to
     * showStatus which immediately deletes the currently selected
     * sessions.
     */
    private readonly DELETE_ACTION: NotificationAction = {
        name: 'SETTINGS_SESSIONS.ACTION_DELETE',
        className: 'danger',
        // Handle action
        callback: () => {
            this.deleteAllSessionsImmediately();
            this.guacNotification.showStatus(false);
        }
    };

    /**
     * Immediately deletes the selected sessions, without prompting the
     * user for confirmation.
     */
    private deleteAllSessionsImmediately(): void {

        const deletionRequests: Observable<any>[] = [];

        // Perform deletion for each relevant data source
        for (const dataSource in this.allSelectedWrappers) {
            const selectedWrappers = this.allSelectedWrappers[dataSource];

            // Delete sessions, if any are selected
            const identifiers = Object.keys(selectedWrappers);
            if (identifiers.length)
                deletionRequests.push(this.activeConnectionService.deleteActiveConnections(dataSource, identifiers));

        }

        // Update interface
        forkJoin(deletionRequests)
            .subscribe({
                next: () => {
                    // Remove deleted connections from wrapper array
                    this.wrappers = this.wrappers!.filter(wrapper => {
                        return !(wrapper.activeConnection.identifier! in (this.allSelectedWrappers[wrapper.dataSource] || {}));
                    });

                    // Clear selection
                    this.allSelectedWrappers = {};
                },
                error: this.guacNotification.SHOW_REQUEST_ERROR
            });

    }

    /**
     * Delete all selected sessions, prompting the user first to
     * confirm that deletion is desired.
     */
    deleteSessions(): void {
        // Confirm deletion request
        this.guacNotification.showStatus({
            'title': 'SETTINGS_SESSIONS.DIALOG_HEADER_CONFIRM_DELETE',
            'text': {
                'key': 'SETTINGS_SESSIONS.TEXT_CONFIRM_DELETE'
            },
            'actions': [this.DELETE_ACTION, this.CANCEL_ACTION]
        });
    }

    /**
     * Returns the relative URL of the client page which accesses the
     * given active connection. If the active connection is not
     * connectable, null is returned.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the
     *     active connection.
     *
     * @param activeConnection
     *     The active connection to determine the relative URL of.
     *
     * @returns
     *     The relative URL of the client page which accesses the given
     *     active connection, or null if the active connection is not
     *     connectable.
     */
    getClientURL(dataSource: string, activeConnection: ActiveConnection): string | null {

        if (!activeConnection.connectable)
            return null;

        return '/client/' + encodeURIComponent(this.clientIdentifierService.getString({
            dataSource: dataSource,
            type: ClientIdentifier.Types.ACTIVE_CONNECTION,
            id: activeConnection.identifier
        }));

    }

    /**
     * Returns whether the selected sessions can be deleted.
     *
     * @returns
     *     true if selected sessions can be deleted, false otherwise.
     */
    canDeleteSessions(): boolean {

        // We can delete sessions if at least one is selected
        for (const dataSource in this.allSelectedWrappers) {
            for (const identifier in this.allSelectedWrappers[dataSource])
                return true;
        }

        return false;

    }

    /**
     * Called whenever an active connection wrapper changes selected
     * status.
     *
     * @param wrapper
     *     The wrapper whose selected status has changed.
     */
    wrapperSelectionChange(wrapper: ActiveConnectionWrapper): void {

        // Get selection map for associated data source, creating if necessary
        let selectedWrappers = this.allSelectedWrappers[wrapper.dataSource];
        if (!selectedWrappers)
            selectedWrappers = this.allSelectedWrappers[wrapper.dataSource] = {};

        // Add wrapper to map if selected
        if (wrapper.checked)
            selectedWrappers[wrapper.activeConnection.identifier!] = wrapper;

        // Otherwise, remove wrapper from map
        else
            delete selectedWrappers[wrapper.activeConnection.identifier!];

    }

}
