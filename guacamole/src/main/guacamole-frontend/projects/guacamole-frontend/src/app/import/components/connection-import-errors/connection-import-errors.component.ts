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

import { Component, DoCheck, Input, OnChanges, OnInit, SimpleChanges, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';
import _ from 'lodash';
import { firstValueFrom, of } from 'rxjs';
import { GuacFilterComponent } from '../../../list/components/guac-filter/guac-filter.component';
import { GuacPagerComponent } from '../../../list/components/guac-pager/guac-pager.component';
import { DataSourceBuilderService } from '../../../list/services/data-source-builder.service';
import { DataSource } from '../../../list/types/DataSource';
import { SortOrder } from '../../../list/types/SortOrder';
import { Connection } from '../../../rest/types/Connection';
import { DirectoryPatch } from '../../../rest/types/DirectoryPatch';
import { Error } from '../../../rest/types/Error';
import { TranslatableMessage } from '../../../rest/types/TranslatableMessage';
import { DisplayErrorList } from '../../types/DisplayErrorList';
import { ImportConnectionError } from '../../types/ImportConnectionError';
import { ParseError } from '../../types/ParseError';
import { ParseResult } from '../../types/ParseResult';

/**
 * A component that displays errors that occurred during parsing of a connection
 * import file, or errors that were returned from the API during the connection
 * batch creation attempt.
 */
@Component({
    // eslint-disable-next-line @angular-eslint/component-selector -- element name of the AngularJS directive
    selector: 'connection-import-errors',
    templateUrl: './connection-import-errors.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class ConnectionImportErrorsComponent implements OnInit, OnChanges, DoCheck {

    /**
     * The result of parsing the import file. Any errors in this file
     * will be displayed to the user.
     */
    @Input() parseResult: ParseResult | null = null;

    /**
     * The error associated with an attempt to batch create the
     * connections represented by the ParseResult, if the ParseResult
     * had no errors. If the provided ParseResult has errors, no request
     * should have been made, and any provided patch error will be
     * ignored.
     */
    @Input() patchFailure: Error | null = null;

    /**
     * The DirectoryPatch list sent to patchConnections. It is used to
     * map API errors back to import file rows.
     */
    @Input() patches: DirectoryPatch<Connection>[] | null = null;

    /**
     * The parse result as of the last change detection run, used to detect
     * a new parse result.
     */
    private lastParseResult?: ParseResult | null;

    /**
     * Whether the parse result had errors as of the last change detection
     * run, used to detect errors that are added to the same parse result.
     */
    private lastParseResultHasErrors?: boolean;

    /**
     * Reference to the instance of the filter component.
     */
    @ViewChild(GuacFilterComponent, { static: true }) filter!: GuacFilterComponent;

    /**
     * Reference to the instance of the pager component.
     */
    @ViewChild(GuacPagerComponent, { static: true }) pager!: GuacPagerComponent;

    /**
     * All connections with their associated errors for display. These may
     * be either parsing failures, or errors returned from the API. Both
     * error types will be adapted to a common display format, though the
     * error types will never be mixed, because no REST request should ever
     * be made if there are client-side parse errors.
     */
    connectionErrors: ImportConnectionError[] = [];

    /**
     * The sorted and paginated data source for the connection errors list.
     */
    connectionErrorsDataSource: DataSource<ImportConnectionError> | null = null;

    /**
     * SortOrder instance which maintains the sort order of the visible
     * connection errors.
     */
    errorOrder: SortOrder = new SortOrder([
        'rowNumber',
        'name',
        'group',
        'protocol',
        'errors',
    ]);

    /**
     * Array of all connection error properties that are filterable.
     */
    filteredErrorProperties: string[] = [
        'rowNumber',
        'name',
        'group',
        'protocol',
        'errors',
    ];

    /**
     * Inject required services.
     */
    constructor(private dataSourceBuilderService: DataSourceBuilderService,
                private translocoService: TranslocoService) {
    }

    /**
     * Create the data source of the connection error list, which filters by
     * the search string of the filter component, sorts by errorOrder and
     * paginates with the pager component.
     */
    ngOnInit(): void {

        this.connectionErrorsDataSource = this.dataSourceBuilderService
            .getBuilder<ImportConnectionError>()
            // Start with an empty list
            .source(this.connectionErrors)
            // Filter based on the search string provided by the guac-filter component
            .filter(this.filter.searchStringChange, this.filteredErrorProperties)
            // Sort according to the specified sort order
            .sort(of(this.errorOrder))
            // Paginate using the GuacPagerComponent
            .paginate(this.pager.page)
            .build();

    }


    /**
     * Generate a ImportConnectionError representing any errors associated
     * with the row at the given index within the given patch result.
     *
     * @param parseResult
     *     The result of parsing the connection import file.
     *
     * @param patches
     *     The patches sent to the API for the given parse result.
     *
     * @param index
     *     The current row within the patches array, 0-indexed.
     *
     * @param row
     *     The current row within the original connection, 0-indexed.
     *     If any REMOVE patches are present, this may be greater than
     *     the index.
     *
     * @returns
     *     The connection error object associated with the given row in the
     *     given parse result.
     */
    private generatePatchConnectionError = (parseResult: ParseResult, patches: DirectoryPatch<Connection>[],
                                            index: number, row: number): ImportConnectionError => {

        // Get the patch associated with the current row
        const patch = patches[index];

        // The value of a patch is just the Connection object
        const connectionObject = patch.value!;

        return new ImportConnectionError({

            // Add 1 to the provided row to get the position in the file
            rowNumber: row + 1,

            // Basic connection information - name, group, and protocol.
            name    : connectionObject.name!,
            group   : parseResult.groupPaths[index],
            protocol: connectionObject.protocol,

            // The human-readable error messages
            errors: new DisplayErrorList([])
        });
    };

    /**
     * Update the error list when a new connection patch failure is seen.
     */
    ngOnChanges(changes: SimpleChanges): void {

        if (changes['patchFailure']) {
            const patchFailure: Error | null = changes['patchFailure'].currentValue;
            this.patchFailureChanged(patchFailure);
        }
    }

    /**
     * Custom change detection logic for the component.
     */
    ngDoCheck(): void {

        // If a new parse result with errors is seen, update the display list.
        // Watch hasErrors as well: it may be set after parseResult is assigned
        // (e.g. when ensureConnectionGroups fails to resolve a group path).
        const parseResult = this.parseResult;
        const hasErrors = parseResult?.hasErrors;

        if (parseResult !== this.lastParseResult || hasErrors !== this.lastParseResultHasErrors) {

            this.lastParseResult = parseResult;
            this.lastParseResultHasErrors = hasErrors;

            this.parseResultChanged(parseResult);

        }

    }

    /**
     * If a new connection patch failure is seen, update the display list
     *
     * @param patchFailure
     *   The new patch failure to display.
     */
    private patchFailureChanged(patchFailure: Error | null): void {

        const { parseResult, patches } = this;

        // Do not attempt to process anything before the data has loaded
        if (!patchFailure || !parseResult || !patches)
            return;

        // All promises from all translation requests. The connection errors
        // will not be updated until all translations are ready.
        const translationPromises: Promise<void>[] = [];

        // Any error returned from the API specifically associated with the
        // preceding REMOVE patch
        let removeError: TranslatableMessage | null = null;

        // Fetch the API error, if any, of the patch at the given index
        const getAPIError = (index: number): TranslatableMessage | null =>
            _.get(patchFailure, ['patches', index, 'error']) ?? null;

        // The row number for display. Unlike the index, this number will
        // skip any REMOVE patches. In other words, this is the index of
        // connections within the original import file.
        let row = 0;

        // Set up the list of connection errors based on the existing parse
        // result, with error messages fetched from the patch failure
        const connectionErrors = patches.reduce(
            (errors: ImportConnectionError[], patch, index) => {

                // Do not process display REMOVE patches - they are always
                // followed by ADD patches containing the actual content
                // (and errors, if any)
                if (patch.op === DirectoryPatch.Operation.REMOVE) {

                    // Save the API error, if any, so it can be displayed
                    // alongside the connection information associated with the
                    // following ADD patch
                    removeError = getAPIError(index);

                    // Do not add an entry for this remove patch - it should
                    // always be followed by a corresponding CREATE patch
                    // containing the relevant connection information
                    return errors;

                }

                // Generate a connection error for display
                const connectionError = this.generatePatchConnectionError(
                    parseResult, patches, index, row++);

                // Add the error associated with the previous REMOVE patch, if
                // any, to the error associated with the current patch, if any
                const apiErrors = [removeError, getAPIError(index)];

                // Clear the previous REMOVE patch error after consuming it
                removeError = null;

                // Go through each potential API error
                apiErrors.forEach(error =>

                    // If the API error exists, fetch the translation and
                    // update it when it's ready
                    error && translationPromises.push(firstValueFrom(this.translocoService.selectTranslate<string>(
                        error.key!, error.variables))
                        .then(translatedError => {
                                connectionError.errors.getArray().push(translatedError);
                            }
                        )));

                errors.push(connectionError);
                return errors;

            }, []);

        // Once all the translations have been completed, update the
        // connectionErrors all in one go, to ensure no excessive reloading
        Promise.all(translationPromises).then(() => {
            this.connectionErrors = connectionErrors;
            this.connectionErrorsDataSource?.updateSource(connectionErrors);
        });

    }

    /**
     * Generate a ImportConnectionError representing any errors associated
     * with the row at the given index within the given parse result.
     *
     * @param parseResult
     *     The result of parsing the connection import file.
     *
     * @param index
     *     The current row within the patches array, 0-indexed.
     *
     * @param row
     *     The current row within the original connection, 0-indexed.
     *     If any REMOVE patches are present, this may be greater than
     *     the index.
     *
     * @returns
     *     The connection error object associated with the given row in the
     *     given parse result.
     */
    private generateParseConnectionError = (parseResult: ParseResult, index: number, row: number): ImportConnectionError => {

        // Get the connection details
        const connectionObject = parseResult.connectionObjects[index];

        return new ImportConnectionError({

            // Physical position in the file
            rowNumber: row + 1,

            // Basic connection information - name, group, and protocol.
            name    : connectionObject.name,
            group   : connectionObject.group,
            protocol: connectionObject.protocol,

            // Pull parse-time errors from the ImportConnection object
            errors: new DisplayErrorList([...(connectionObject.errors || [])])
        });
    };

    /**
     * If a new parse result with errors is seen, update the display list.
     *
     * @param parseResult
     *    The new parse result to display.
     */
    private parseResultChanged(parseResult: ParseResult | null): void {

        // Do not process if there are no errors in the provided result
        if (!parseResult || !parseResult.hasErrors)
            return;

        // All promises from all translation requests. The connection errors
        // will not be updated until all translations are ready.
        const translationPromises: Promise<void>[] = [];

        // The parse result should only be updated on a fresh file import;
        // therefore it should be safe to skip checking the patch errors
        // entirely - if set, they will be from the previous file and no
        // longer relevant.

        // Set up the list of connection errors based on the updated parse
        // result using connectionObjects.
        const connectionErrors = parseResult.connectionObjects.map(
            (connectionObject, index) => {

                // Generate a connection error for display.
                // In this new architecture, index and row are identical.
                const connectionError = this.generateParseConnectionError(
                    parseResult, index, index);

                // Go through the errors and check if any are translateable.
                // We access the underlying array from the DisplayErrorList.
                connectionError.errors.getArray().forEach(
                    (error: any, errorIndex) => {

                        // If this error is a ParseError, it can be translated.
                        if (error instanceof ParseError)

                            // Fetch the translation and update it when it's ready
                            translationPromises.push(firstValueFrom(this.translocoService.selectTranslate(
                                error.key, error.variables))
                                .then(translatedError => {
                                    connectionError.errors.getArray()[errorIndex] = translatedError;
                                }));

                            // If the error is not a known translatable type, add the
                        // message directly to the error array
                        else
                            connectionError.errors.getArray()[errorIndex] = (
                                error.message ? error.message : error);

                    });

                return connectionError;
            });

        // Once all the translations have been completed, update the
        // connectionErrors all in one go, to ensure no excessive reloading
        Promise.all(translationPromises).then(() => {
            this.connectionErrors = connectionErrors;
            this.connectionErrorsDataSource?.updateSource(connectionErrors);
        });

    }

}
