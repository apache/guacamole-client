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

import { Component, Input, OnInit, ViewChild, ViewEncapsulation } from '@angular/core';
import { HistoryService } from '../../../rest/service/history.service';
import { RequestService } from '../../../rest/service/request.service';
import { CsvService } from '../../services/csv.service';
import { TranslocoService } from '@ngneat/transloco';
import { SortOrder } from '../../../list/types/SortOrder';
import { BehaviorSubject, combineLatest, take } from 'rxjs';
import { NonNullableProperties } from '../../../util/utility-types';
import { FilterToken } from '../../../list/types/FilterToken';
import { ConnectionHistoryEntryWrapper } from '../../types/ConnectionHistoryEntryWrapper';
import { SortService } from '../../../list/services/sort.service';
import { formatDate } from '@angular/common';
import { saveAs } from 'file-saver';
import { DataSourceBuilderService } from '../../../list/services/data-source-builder.service';
import { GuacPagerComponent } from '../../../list/components/guac-pager/guac-pager.component';
import { DataSource } from '../../../list/types/DataSource';

/**
 * A component for viewing connection history records.
 */
@Component({
    selector: 'guac-settings-connection-history',
    templateUrl: './guac-settings-connection-history.component.html',
    encapsulation: ViewEncapsulation.None
})
export class GuacSettingsConnectionHistoryComponent implements OnInit {

    /**
     * The identifier of the currently-selected data source.
     */
    @Input({required: true}) dataSource!: string;

    /**
     * Reference to the instance of the pager component.
     */
    @ViewChild(GuacPagerComponent, {static: true}) pager!: GuacPagerComponent;

    /**
     * All wrapped matching connection history entries, or null if these
     * entries have not yet been retrieved.
     */
    historyEntryWrappers: ConnectionHistoryEntryWrapper[] | null = null;

    /**
     * The initial SortOrder instance which stores the sort order of the history
     * records.
     */
    private readonly initialOrder = new SortOrder([
        '-entry.startDate',
        '-duration',
        'entry.username',
        'entry.connectionName',
        'entry.remoteHost'
    ]);

    /**
     * Observable of the current SortOrder instance which stores the sort order of the history
     * records. The value is updated by the GuacSortOrderDirective.
     */
    order: BehaviorSubject<SortOrder> = new BehaviorSubject(this.initialOrder);

    /**
     * The search terms to use when filtering the history records.
     */
    searchString = '';

    /**
     * TODO: Document
     */
    dataSourceView: DataSource<ConnectionHistoryEntryWrapper> | null = null;

    /**
     * The date format for use for start/end dates.
     */
    dateFormat: string | null = null;

    /**
     * The names of sortable properties supported by the REST API that
     * correspond to the properties that may be stored within
     * $scope.order.
     */
    private readonly apiSortProperties: Record<string, string> = {
        'entry.startDate': 'startDate',
        '-entry.startDate': '-startDate'
    };

    /**
     * Inject required services.
     */
    constructor(private csvService: CsvService,
                private historyService: HistoryService,
                private requestService: RequestService,
                private translocoService: TranslocoService,
                private sortService: SortService,
                private ds: DataSourceBuilderService) {
    }

    ngOnInit(): void {

        // Get session date format
        this.translocoService.selectTranslate('SETTINGS_CONNECTION_HISTORY.FORMAT_DATE')
            .pipe(take(1))
            .subscribe(retrievedDateFormat => {

                // Store received date format
                this.dateFormat = retrievedDateFormat;

            });

        // Build a view on the history entries
        this.dataSourceView =
            this.ds.getBuilder<ConnectionHistoryEntryWrapper>()
                // Start with an empty list
                .source([])
                .sort(this.order)
                .paginate(this.pager.page)
                .build();

        // Initialize search results
        this.search();

    }

    /**
     * Converts the given sort predicate to a corresponding array of
     * sortable properties supported by the REST API. Any properties
     * within the predicate that are not supported will be dropped.
     *
     * @param predicate
     *     The sort predicate to convert, as exposed by the predicate
     *     property of SortOrder.
     *
     * @returns
     *     A corresponding array of sortable properties, omitting any
     *     properties not supported by the REST API.
     */
    private toAPISortPredicate(predicate: string[]): string[] {
        return predicate
            .map((name) => this.apiSortProperties[name])
            .filter((name) => !!name);
    }

    /**
     * Returns true if the connection history records have been loaded,
     * indicating that information needed to render the page is fully
     * loaded.
     *
     * @returns
     *     true if the history records have been loaded, false
     *     otherwise.
     *
     */
    isLoaded(): this is NonNullableProperties<GuacSettingsConnectionHistoryComponent, 'historyEntryWrappers' | 'dateFormat' | 'dataSourceView'> {
        return this.historyEntryWrappers !== null
            && this.dateFormat !== null
            && this.dataSourceView !== null;
    }

    /**
     * Returns whether the search has completed but contains no history
     * records. This function will return false if there are history
     * records in the results OR if the search has not yet completed.
     *
     * @returns
     *     true if the search results have been loaded but no history
     *     records are present, false otherwise.
     */
    isHistoryEmpty(): boolean {
        return this.isLoaded() && this.historyEntryWrappers.length === 0;
    }

    /**
     * Query the API for the connection record history, filtered by
     * searchString, and ordered by order.
     */
    search(): void {

        // Clear current results
        this.historyEntryWrappers = null;

        // Tokenize search string
        const tokens = FilterToken.tokenize(this.searchString);

        // Transform tokens into list of required string contents
        const requiredContents: any[] = [];

        tokens.forEach(token => {

            // Transform depending on token type
            switch (token.type) {

                // For string literals, use parsed token value
                case 'LITERAL':
                    requiredContents.push(token.value);
                    break;

                // Ignore whitespace
                case 'WHITESPACE':
                    break;

                // For all other token types, use the relevant portion
                // of the original search string
                default:
                    requiredContents.push(token.consumed);

            }

        });

        this.historyService.getConnectionHistory(
            this.dataSource,
            requiredContents,
            this.toAPISortPredicate(this.order.getValue().predicate)
        )
            .subscribe({
                next: historyEntries => {

                    // Wrap all history entries for sake of display
                    this.historyEntryWrappers = [];
                    historyEntries.forEach(historyEntry => {
                        this.historyEntryWrappers!.push(new ConnectionHistoryEntryWrapper(this.dataSource, historyEntry));
                    });

                    if (this.dataSourceView) {
                        this.dataSourceView.updateSource(this.historyEntryWrappers);
                    }

                }, error: this.requestService.DIE

            });

    }

    /**
     * Initiates a download of a CSV version of the displayed history
     * search results.
     */
    downloadCSV(): void {

        // Translate CSV header
        const tableHeaderSessionUsername = this.translocoService.selectTranslate<string>('SETTINGS_CONNECTION_HISTORY.TABLE_HEADER_SESSION_USERNAME');
        const tableHeaderSessionStartdate = this.translocoService.selectTranslate<string>('SETTINGS_CONNECTION_HISTORY.TABLE_HEADER_SESSION_STARTDATE');
        const tableHeaderSessionDuration = this.translocoService.selectTranslate<string>('SETTINGS_CONNECTION_HISTORY.TABLE_HEADER_SESSION_DURATION');
        const tableHeaderSessionConnectionName = this.translocoService.selectTranslate<string>('SETTINGS_CONNECTION_HISTORY.TABLE_HEADER_SESSION_CONNECTION_NAME');
        const tableHeaderSessionRemotehost = this.translocoService.selectTranslate<string>('SETTINGS_CONNECTION_HISTORY.TABLE_HEADER_SESSION_REMOTEHOST');
        const filenameHistoryCsv = this.translocoService.selectTranslate<string>('SETTINGS_CONNECTION_HISTORY.FILENAME_HISTORY_CSV');

        combineLatest([tableHeaderSessionUsername, tableHeaderSessionStartdate, tableHeaderSessionDuration, tableHeaderSessionConnectionName, tableHeaderSessionRemotehost, filenameHistoryCsv])
            .pipe(take(1))
            .subscribe(([tableHeaderSessionUsername, tableHeaderSessionStartdate, tableHeaderSessionDuration, tableHeaderSessionConnectionName, tableHeaderSessionRemotehost, filenameHistoryCsv]) => {

                // Initialize records with translated header row
                const records: any[][] = [[
                    tableHeaderSessionUsername,
                    tableHeaderSessionStartdate,
                    tableHeaderSessionDuration,
                    tableHeaderSessionConnectionName,
                    tableHeaderSessionRemotehost,
                ]];

                // Add rows for all history entries, using the same sort
                // order as the displayed table
                this.sortService.orderByPredicate(this.historyEntryWrappers, this.order.getValue().predicate).forEach(historyEntryWrapper => {
                        records.push([
                            historyEntryWrapper.entry.username,
                            formatDate(historyEntryWrapper.entry.startDate || 0, this.dateFormat!, 'en-US'),
                            historyEntryWrapper.duration / 1000,
                            historyEntryWrapper.entry.connectionName,
                            historyEntryWrapper.entry.remoteHost
                        ]);
                    }
                );

                // Save the result
                saveAs(this.csvService.toBlob(records), filenameHistoryCsv);

            });

    }

}
