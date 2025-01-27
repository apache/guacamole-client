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

import { Component, Input, ViewEncapsulation } from '@angular/core';
import { Router } from '@angular/router';
import _ from 'lodash';
import { firstValueFrom } from 'rxjs';
import { GuacNotificationService } from '../../../notification/services/guac-notification.service';
import { ConnectionService } from '../../../rest/service/connection.service';
import { PermissionService } from '../../../rest/service/permission.service';
import { UserGroupService } from '../../../rest/service/user-group.service';
import { UserService } from '../../../rest/service/user.service';
import { DirectoryPatch } from '../../../rest/types/DirectoryPatch';
import { DirectoryPatchResponse } from '../../../rest/types/DirectoryPatchResponse';
import { Error } from '../../../rest/types/Error';
import { PermissionSet } from '../../../rest/types/PermissionSet';
import { User } from '../../../rest/types/User';
import { UserGroup } from '../../../rest/types/UserGroup';
import { ConnectionCSVService } from '../../services/connection-csv.service';
import { ConnectionParseService } from '../../services/connection-parse.service';
import { ConnectionImportConfig } from '../../types/ConnectionImportConfig';
import { ParseError } from '../../types/ParseError';
import { ParseResult } from '../../types/ParseResult';
import Operation = DirectoryPatch.Operation;

/**
 * The allowed MIME type for CSV files.
 */
const CSV_MIME_TYPE: string = 'text/csv';

/**
 * A fallback regular expression for CSV filenames, if no MIME type is provided
 * by the browser. Any file that matches this regex will be considered to be a
 * CSV file.
 */
const CSV_FILENAME_REGEX: RegExp = /\.csv$/i;

/**
 * The allowed MIME type for JSON files.
 */
const JSON_MIME_TYPE: string = 'application/json';

/**
 * A fallback regular expression for JSON filenames, if no MIME type is provided
 * by the browser. Any file that matches this regex will be considered to be a
 * JSON file.
 */
const JSON_FILENAME_REGEX: RegExp = /\.json$/i;

/**
 * The allowed MIME types for YAML files.
 * NOTE: There is no registered MIME type for YAML files. This may result in a
 * wide variety of possible browser-supplied MIME types.
 */
const YAML_MIME_TYPES: string[] = [
    'text/x-yaml',
    'text/yaml',
    'text/yml',
    'application/x-yaml',
    'application/x-yml',
    'application/yaml',
    'application/yml'
];

/**
 * A fallback regular expression for YAML filenames, if no MIME type is provided
 * by the browser. Any file that matches this regex will be considered to be a
 * YAML file.
 */
const YAML_FILENAME_REGEX: RegExp = /\.ya?ml$/i;

/**
 * Possible signatures for zip files (which include most modern Microsoft office
 * documents - most notable excel). If any file, regardless of extension, has
 * these starting bytes, it's invalid and must be rejected.
 * For more, see https://en.wikipedia.org/wiki/List_of_file_signatures and
 * https://en.wikipedia.org/wiki/Magic_number_(programming)#Magic_numbers_in_files.
 */
const ZIP_SIGNATURES: string[] = [
    'PK\u0003\u0004',
    'PK\u0005\u0006',
    'PK\u0007\u0008'
];

/*
 * All file types supported for connection import.
 */
const LEGAL_MIME_TYPES: string[] = [CSV_MIME_TYPE, JSON_MIME_TYPE, ...YAML_MIME_TYPES];

/**
 * The component for the connection import page.
 */
@Component({
    selector: 'guac-import-connections',
    templateUrl: './import-connections.component.html',
    encapsulation: ViewEncapsulation.None,
    providers: [ConnectionParseService, ConnectionCSVService],
    standalone: false
})
export class ImportConnectionsComponent {

    /**
     * The unique identifier of the data source to which connections are being
     * imported.
     */
    @Input('dataSource') dataSource!: string;

    /**
     * The result of parsing the current upload, if successful.
     */
    parseResult: ParseResult | null = null;

    /**
     * The failure associated with the current attempt to create connections
     * through the API, if any.
     */
    patchFailure: Error | null = null;

    /**
     * True if the file is fully uploaded and ready to be processed, or false
     * otherwise.
     */
    dataReady: boolean = false;

    /**
     * True if the file upload has been aborted mid-upload, or false otherwise.
     */
    aborted: boolean = false;

    /**
     * True if fully-uploaded data is being processed, or false otherwise.
     */
    processing: boolean = false;

    /**
     * The MIME type of the uploaded file, if any.
     */
    mimeType: string | null = null;

    /**
     * The name of the file that's currently being uploaded, or has yet to
     * be imported, if any.
     */
    fileName: string | null = null;

    /**
     * The raw string contents of the uploaded file, if any.
     */
    fileData: string | null = null;

    /**
     * The file reader currently being used to upload the file, if any. If
     * null, no file upload is currently in progress.
     */
    fileReader: FileReader | null = null;

    /**
     * The configuration options for this import, to be chosen by the user.
     */
    importConfig: ConnectionImportConfig = new ConnectionImportConfig();

    /**
     * Inject required services.
     */
    constructor(private userService: UserService,
                private userGroupService: UserGroupService,
                private permissionService: PermissionService,
                private connectionService: ConnectionService,
                private guacNotification: GuacNotificationService,
                private connectionParseService: ConnectionParseService,
                private router: Router) {
    }

    /**
     * Clear all file upload state.
     */
    private resetUploadState(): void {

        this.aborted = false;
        this.dataReady = false;
        this.processing = false;
        this.fileData = null;
        this.mimeType = null;
        this.fileReader = null;
        this.parseResult = null;
        this.patchFailure = null;
        this.fileName = null;

    }

    // Indicate that data is currently being loaded / processed if the file
    // has been provided but not yet fully uploaded, or if the file is
    // fully loaded and is currently being processed.
    isLoading = (): boolean => (
        (this.fileName && !this.dataReady && !this.patchFailure)
        || this.processing);

    // There are errors to display if the parse result generated errors, or
    // if the patch request failed
    hasErrors = () =>
        !!_.get(this, 'parseResult.hasErrors') || !!this.patchFailure;

    /**
     * Create all users and user groups mentioned in the import file that don't
     * already exist in the current data source. Return an object describing the
     * result of the creation requests.
     *
     * @param parseResult
     *     The result of parsing the user-supplied import file.
     *
     * @return
     *     A promise resolving to an object containing the results of the calls
     *     to create the users and groups.
     */
    private createUsersAndGroups(parseResult: ParseResult): Promise<object> {

        return Promise.all([
            firstValueFrom(this.userService.getUsers(this.dataSource)),
            firstValueFrom(this.userGroupService.getUserGroups(this.dataSource))
        ]).then(([existingUsers, existingGroups]) => {

            const userPatches = Object.keys(parseResult.users)

                // Filter out any existing users
                .filter(identifier => !existingUsers[identifier])

                // A patch to create each new user
                .map(username => new DirectoryPatch({
                    op   : Operation.ADD,
                    path : '/',
                    value: new User({ username })
                }));

            const groupPatches = Object.keys(parseResult.groups)

                // Filter out any existing groups
                .filter(identifier => !existingGroups[identifier])

                // A patch to create each new user group
                .map(identifier => new DirectoryPatch({
                    op   : Operation.ADD,
                    path : '/',
                    value: new UserGroup({ identifier })
                }));

            // Create all the users and groups
            return Promise.all([
                firstValueFrom(this.userService.patchUsers(this.dataSource, userPatches)),
                firstValueFrom(this.userGroupService.patchUserGroups(
                    this.dataSource, groupPatches))
            ]);

        });

    }

    /**
     * Grant read permissions for each user and group in the supplied parse
     * result to each connection in their connection list. Note that there will
     * be a separate request for each user and group.
     *
     * @param parseResult
     *     The result of successfully parsing a user-supplied import file.
     *
     * @param response
     *     The response from the PATCH API request.
     *
     * @returns
     *     A promise that will resolve with the result of every permission
     *     granting request.
     */
    private grantConnectionPermissions(parseResult: ParseResult, response: DirectoryPatchResponse): Promise<object> {

        // All connection grant requests, one per user/group
        const userRequests: Promise<void>[] = [];
        const groupRequests: Promise<void>[] = [];

        // Create a PermissionSet granting access to all connections at
        // the provided indices within the provided parse result
        const createPermissionSet = (indices: number[]) =>
            new PermissionSet({
                connectionPermissions: indices.reduce(
                    (permissions: Record<string, PermissionSet.ObjectPermissionType[]>, index) => {
                        const connectionId = response!.patches![index].identifier!;
                        permissions[connectionId] = [
                            PermissionSet.ObjectPermissionType.READ];
                        return permissions;
                    }, {})
            });

        // Now that we've created all the users, grant access to each
        _.forEach(parseResult.users, (connectionIndices, identifier) =>

            // Grant the permissions - note the group flag is `false`
            userRequests.push(firstValueFrom(this.permissionService.patchPermissions(
                this.dataSource, identifier,

                // Create the permissions to these connections for this user
                createPermissionSet(connectionIndices),

                // Do not remove any permissions
                new PermissionSet(),

                // This call is not for a group
                false))));

        // Now that we've created all the groups, grant access to each
        _.forEach(parseResult.groups, (connectionIndices, identifier) =>

            // Grant the permissions - note the group flag is `true`
            groupRequests.push(firstValueFrom(this.permissionService.patchPermissions(
                this.dataSource, identifier,

                // Create the permissions to these connections for this user
                createPermissionSet(connectionIndices),

                // Do not remove any permissions
                new PermissionSet(),

                // This call is for a group
                true))));

        // Return the result from all the permission granting calls
        return Promise.all([...userRequests, ...groupRequests]);

    }

    /**
     * Process a successfully parsed import file, creating any specified
     * connections, creating and granting permissions to any specified users
     * and user groups. If successful, the user will be shown a success message.
     * If not, any errors will be displayed and any already-created entities
     * will be rolled back.
     *
     * @param parseResult
     *     The result of parsing the user-supplied import file.
     */
    private handleParseSuccess = (parseResult: ParseResult): void => {

        this.processing = false;
        this.parseResult = parseResult;

        // If errors were encountered during file parsing, abort further
        // processing - the user will have a chance to fix the errors and try
        // again
        if (parseResult.hasErrors)
            return;

        // First, attempt to create the connections
        firstValueFrom(this.connectionService.patchConnections(this.dataSource, parseResult.patches))
            .then(connectionResponse =>

                // If connection creation is successful, create users and groups
                this.createUsersAndGroups(parseResult).then(() =>

                    // Grant any new permissions to users and groups. NOTE: Any
                    // existing permissions for updated connections will NOT be
                    // removed - only new permissions will be added.
                    this.grantConnectionPermissions(parseResult, connectionResponse)
                        .then(() => {

                            this.processing = false;

                            // Display a success message if everything worked
                            this.guacNotification.showStatus({
                                className: 'success',
                                title    : 'IMPORT.DIALOG_HEADER_SUCCESS',
                                text     : {
                                    key      : 'IMPORT.INFO_CONNECTIONS_IMPORTED_SUCCESS',
                                    variables: { NUMBER: parseResult.connectionCount }
                                },

                                // Add a button to acknowledge and redirect to
                                // the connection listing page
                                actions: [{
                                    name    : 'IMPORT.ACTION_ACKNOWLEDGE',
                                    callback: () => {

                                        // Close the notification
                                        this.guacNotification.showStatus(false);

                                        // Redirect to connection list page
                                        this.router.navigate(['/settings', this.dataSource, 'connections']);
                                    }
                                }]
                            });
                        }))

                    // If an error occurs while trying to create users or groups,
                    // display the error to the user.
                    .catch(this.handleError)
            )

            // If an error occurred when the call to create the connections was made,
            // skip any further processing - the user will have a chance to fix the
            // problems and try again
            .catch(patchFailure => {
                this.processing = false;
                this.patchFailure = patchFailure;
            });
    };

    /**
     * Display the provided error to the user in a dismissible dialog.
     *
     * @param error
     *     The error to display.
     */
    private handleError = (error: ParseError | Error): void => {

        // Any error indicates that processing of the file has failed, so clear
        // all upload state to allow for a fresh retry
        this.resetUploadState();

        let text;

        // If it's an import file parsing error
        if (error instanceof ParseError)
            text = {

                // Use the translation key if available
                key      : error.key || error.message,
                variables: error.variables
            };

        // If it's a generic REST error
        else if (error instanceof Error)
            text = error.translatableMessage;

        // If it's an unknown type, just use the message directly
        else
            text = { key: error };

        this.guacNotification.showStatus({
            className: 'error',
            title    : 'IMPORT.DIALOG_HEADER_ERROR',
            text,

            // Add a button to hide the error
            actions: [{
                name    : 'IMPORT.ACTION_ACKNOWLEDGE',
                callback: () => this.guacNotification.showStatus(false)
            }]
        });

    };

    /**
     * Display the provided error which occurred during the file drop operation
     * to the user in a dismissible dialog.
     *
     * @param errorTextKey
     *    The translation key of the error message to display.
     */
    handleDropError = (errorTextKey: string): void => {

        this.guacNotification.showStatus({
            className: 'error',
            title    : 'APP.DIALOG_HEADER_ERROR',
            text     : { key: errorTextKey },

            // Add a button to hide the error
            actions: [{
                name    : 'APP.ACTION_ACKNOWLEDGE',
                callback: () => this.guacNotification.showStatus(false)
            }]
        });

    };

    /**
     * Process the uploaded import file, importing the connections, granting
     * connection permissions, or displaying errors to the user if there are
     * problems with the provided file.
     *
     * @param mimeType
     *     The MIME type of the uploaded data file.
     *
     * @param data
     *     The raw string contents of the import file.
     */
    private processData(mimeType: string, data: string): void {

        // Data processing has begun
        this.processing = true;

        // The function that will process all the raw data and return a list of
        // patches to be submitted to the API
        let processDataCallback;

        // Choose the appropriate parse function based on the mimetype
        if (mimeType === JSON_MIME_TYPE)
            processDataCallback = (importConfig: ConnectionImportConfig, jsonData: string) =>
                this.connectionParseService.parseJSON(importConfig, jsonData);

        else if (mimeType === CSV_MIME_TYPE)
            processDataCallback = (importConfig: ConnectionImportConfig, csvData: string) =>
                this.connectionParseService.parseCSV(importConfig, csvData);

        else if (YAML_MIME_TYPES.indexOf(mimeType) >= 0)
            processDataCallback = (importConfig: ConnectionImportConfig, yamlData: string) =>
                this.connectionParseService.parseYAML(importConfig, yamlData);

            // The file type was validated before being uploaded - this should
        // never happen
        else
            processDataCallback = () => {
                throw new ParseError({
                    message  : 'Unexpected invalid file type: ' + mimeType,
                    key      : '',
                    variables: undefined
                });
            };

        // Make the call to process the data into a series of patches
        processDataCallback(this.importConfig, data)

            // Send the data off to be imported if parsing is successful
            .then(this.handleParseSuccess)

            // Display any error found while parsing the file
            .catch(this.handleError);
    }

    /**
     * Process the uploaded import data. Only usable if the upload is fully
     * complete.
     */
    import = (): void => this.processData(this.mimeType!, this.fileData!);

    /**
     * Returns true if import should be disabled, or false if import should be
     * allowed.
     *
     * @return
     *     True if import should be disabled, otherwise false.
     */
    importDisabled = (): boolean =>

        // Disable import if no data is ready
        !this.dataReady ||

        // Disable import if the file is currently being processed
        this.processing;

    /**
     * Cancel any in-progress upload, or clear any uploaded-but-errored-out
     * batch.
     */
    cancel(): void {

        // If the upload is in progress, stop it now; the FileReader will
        // reset the upload state when it stops
        if (this.fileReader) {
            this.aborted = true;
            this.fileReader.abort();
        }

        // Clear any upload state - there's no FileReader handler to do it
        else
            this.resetUploadState();

    }

    /**
     * Returns true if cancellation should be disabled, or false if
     * cancellation should be allowed.
     *
     * @return
     *     True if cancellation should be disabled, or false if cancellation
     *     should be allowed.
     */
    cancelDisabled = (): boolean =>

        // Disable cancellation if the import has already been cancelled
        this.aborted ||

        // Disable cancellation if the file is currently being processed
        this.processing ||

        // Disable cancellation if no data is ready or being uploaded
        !(this.fileReader || this.dataReady);

    /**
     * Handle a provided File upload, reading all data onto the scope for
     * import processing, should the user request an import. Note that this
     * function is used as a callback for directives that invoke it with a file
     * list, but directive-level checking should ensure that there is only ever
     * one file provided at a time.
     *
     * @param files
     *     The files to upload onto the scope for further processing. There
     *     should only ever be a single file in the array.
     */
    handleFiles = (files: FileList): void => {

        // There should only ever be a single file in the array
        const file = files[0];

        // The name and MIME type of the file as provided by the browser
        let fileName = file.name;
        let mimeType = file.type;

        // If no MIME type was provided by the browser at all, use REGEXes as a
        // fallback to try to determine the file type. NOTE: Windows 10/11 are
        // known to do this with YAML files.
        if (!_.trim(mimeType).length) {

            // If the file name matches what we'd expect for a CSV file, set the
            // CSV MIME type and move on
            if (CSV_FILENAME_REGEX.test(fileName))
                mimeType = CSV_MIME_TYPE;

                // If the file name matches what we'd expect for a JSON file, set
            // the JSON MIME type and move on
            else if (JSON_FILENAME_REGEX.test(fileName))
                mimeType = JSON_MIME_TYPE;

                // If the file name matches what we'd expect for a JSON file, set
            // one of the allowed YAML MIME types and move on
            else if (YAML_FILENAME_REGEX.test(fileName))
                mimeType = YAML_MIME_TYPES[0];

            else {

                // If none of the REGEXes pass, there's nothing more to be tried
                this.handleError(new ParseError({
                    message  : 'Unknown type for file: ' + fileName,
                    key      : 'IMPORT.ERROR_DETECTED_INVALID_TYPE',
                    variables: undefined
                }));
                return;

            }

        }

            // Check if the mimetype is one of the supported types,
        // e.g. "application/json" or "text/csv"
        else if (LEGAL_MIME_TYPES.indexOf(mimeType) < 0) {

            // If the provided file is not one of the supported types,
            // display an error and abort processing
            this.handleError(new ParseError({
                message  : 'Invalid file type: ' + mimeType,
                key      : 'IMPORT.ERROR_INVALID_MIME_TYPE',
                variables: { TYPE: mimeType }
            }));
            return;

        }

        // Save the name and type to the scope
        this.fileName = fileName;
        this.mimeType = mimeType;

        // Initialize upload state
        this.aborted = false;
        this.dataReady = false;
        this.processing = false;

        // Save the file to the scope when ready
        this.fileReader = new FileReader();
        this.fileReader.onloadend = (e => {

            // If the upload was explicitly aborted, clear any upload state and
            // do not process the data
            if (this.aborted)
                this.resetUploadState();

            else {

                const fileData = e.target!.result as string;

                // Check if the file has a header of a known-bad type
                if (_.some(ZIP_SIGNATURES,
                    signature => fileData.startsWith(signature))) {

                    // Throw an error and abort processing
                    this.handleError(new ParseError({
                        message  : 'Invalid file type detected',
                        key      : 'IMPORT.ERROR_DETECTED_INVALID_TYPE',
                        variables: undefined
                    }));
                    return;

                }

                // Save the uploaded data
                this.fileData = fileData;

                // Mark the data as ready
                this.dataReady = true;

                // Clear the file reader from the scope now that this file is
                // fully uploaded
                this.fileReader = null;

            }
        });

        // Read all the data into memory
        this.fileReader.readAsText(file);
    };

    /**
     * Make the ConnectionImportConfig.ExistingConnectionMode enum available to
     * the template.
     */
    protected readonly ExistingConnectionMode = ConnectionImportConfig.ExistingConnectionMode;

    /**
     * Make the ConnectionImportConfig.ExistingPermissionMode enum available to
     * the template.
     */
    protected readonly ExistingPermissionMode = ConnectionImportConfig.ExistingPermissionMode;
}
