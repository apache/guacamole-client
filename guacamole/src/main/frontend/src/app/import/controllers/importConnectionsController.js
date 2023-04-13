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

/* global _ */

/**
 * The allowed MIME type for CSV files.
 * 
 * @type String
 */
const CSV_MIME_TYPE = 'text/csv';

/**
 * The allowed MIME type for JSON files.
 *
 * @type String
 */
const JSON_MIME_TYPE = 'application/json';

/**
 * The allowed MIME types for YAML files.
 * NOTE: There is no registered MIME type for YAML files. This may result in a
 * wide variety of possible browser-supplied MIME types.
 *
 * @type String[]
 */
const YAML_MIME_TYPES = [
    'text/x-yaml',
    'text/yaml',
    'text/yml',
    'application/x-yaml',
    'application/x-yml',
    'application/yaml',
    'application/yml'
];

/*
 * All file types supported for connection import.
 * 
 * @type {String[]}
 */
const LEGAL_MIME_TYPES = [CSV_MIME_TYPE, JSON_MIME_TYPE, ...YAML_MIME_TYPES];

/**
 * The controller for the connection import page.
 */
angular.module('import').controller('importConnectionsController', ['$scope', '$injector',
        function importConnectionsController($scope, $injector) {
            
    // Required services
    const $location              = $injector.get('$location');
    const $q                     = $injector.get('$q');
    const $routeParams           = $injector.get('$routeParams');
    const connectionParseService = $injector.get('connectionParseService');
    const connectionService      = $injector.get('connectionService');
    const guacNotification       = $injector.get('guacNotification');
    const permissionService      = $injector.get('permissionService');
    const userService            = $injector.get('userService');
    const userGroupService       = $injector.get('userGroupService');

    // Required types
    const DirectoryPatch      = $injector.get('DirectoryPatch');
    const Error               = $injector.get('Error');
    const ParseError          = $injector.get('ParseError');
    const PermissionSet       = $injector.get('PermissionSet');
    const User                = $injector.get('User');
    const UserGroup           = $injector.get('UserGroup');

    /**
     * The result of parsing the current upload, if successful.
     *
     * @type {ParseResult}
     */
    $scope.parseResult = null;

    /**
     * The failure associated with the current attempt to create connections
     * through the API, if any.
     *
     * @type {Error}
     */
    $scope.patchFailure = null;;

    /**
     * True if the file is fully uploaded and ready to be processed, or false
     * otherwise.
     *
     * @type {Boolean}
     */
    $scope.dataReady = false;

    /**
     * True if the file upload has been aborted mid-upload, or false otherwise.
     */
    $scope.aborted = false;

    /**
     * True if fully-uploaded data is being processed, or false otherwise.
     */
    $scope.processing = false;

    /**
     * The MIME type of the uploaded file, if any.
     *
     * @type {String}
     */
    $scope.mimeType = null;

    /**
     * The raw string contents of the uploaded file, if any.
     *
     * @type {String}
     */
    $scope.fileData = null;

    /**
     * The file reader currently being used to upload the file, if any. If
     * null, no file upload is currently in progress.
     *
     * @type {FileReader}
     */
    $scope.fileReader = null;

    /**
     * Clear all file upload state.
     */
    function resetUploadState() {

        $scope.aborted = false;
        $scope.dataReady = false;
        $scope.processing = false;
        $scope.fileData = null;
        $scope.mimeType = null;
        $scope.fileReader = null;
        $scope.parseResult = null;
        $scope.patchFailure = null;
        $scope.fileName = null;

    }

    // Indicate that data is currently being loaded / processed if the the file
    // has been provided but not yet fully uploaded, or if the the file is
    // fully loaded and is currently being processed.
    $scope.isLoading = () => (
            ($scope.fileName && !$scope.dataReady && !$scope.patchFailure)
            || $scope.processing);

    /**
     * Create all users and user groups mentioned in the import file that don't
     * already exist in the current data source. If either creation fails, any
     * already-created entities will be cleaned up, and the returned promise
     * will be rejected.
     *
     * @param {ParseResult} parseResult
     *     The result of parsing the user-supplied import file.
     *
     * @return {Promise.<Object>}
     *     A promise resolving to an object containing the results of the calls
     *     to create the users and groups.
     */
    function createUsersAndGroups(parseResult) {

        const dataSource = $routeParams.dataSource;

        return $q.all({
            existingUsers : userService.getUsers(dataSource),
            existingGroups : userGroupService.getUserGroups(dataSource)
        }).then(({existingUsers, existingGroups}) => {

            const userPatches = Object.keys(parseResult.users)

                // Filter out any existing users
                .filter(identifier => !existingUsers[identifier])

                // A patch to create each new user
                .map(username => new DirectoryPatch({
                    op: 'add',
                    path: '/',
                    value: new User({ username })
                }));

            const groupPatches = Object.keys(parseResult.groups)

                // Filter out any existing groups
                .filter(identifier => !existingGroups[identifier])

                // A patch to create each new user group
                .map(identifier => new DirectoryPatch({
                    op: 'add',
                    path: '/',
                    value: new UserGroup({ identifier })
                }));

            // First, create any required users and groups, automatically cleaning
            // up any created already-created entities if a call fails.
            // NOTE: Generally we'd want to do these calls in parallel, using
            // `$q.all()`. However, `$q.all()` rejects immediately if any of the
            // wrapped promises reject, so the users may not be ready for cleanup
            // at the time that the group promise rejects, or vice versa. While
            // it would be possible to juggle promises and still do these calls
            // in parallel, the code gets pretty complex, so for readability and
            // simplicity, they are executed serially. The performance cost of
            // doing so should be low.
            return userService.patchUsers(dataSource, userPatches).then(userResponse => {

                // Then, if that succeeds, create any required groups
                return userGroupService.patchUserGroups(dataSource, groupPatches).then(

                    // If user group creation succeeds, resolve the returned promise
                    userGroupResponse => ({ userResponse, userGroupResponse}))

                // If the group creation request fails, clean up any created users
                .catch(groupFailure => {
                    cleanUpUsers(userResponse);
                    return groupFailure;
                });

            });

        });

    }

    /**
     * Grant read permissions for each user and group in the supplied parse
     * result to each connection in their connection list. Note that there will
     * be a seperate request for each user and group.
     *
     * @param {ParseResult} parseResult
     *     The result of successfully parsing a user-supplied import file.
     *
     * @param {Object} response
     *     The response from the PATCH API request.
     *
     * @returns {Promise.<Object>}
     *     A promise that will resolve with the result of every permission
     *     granting request.
     */
    function grantConnectionPermissions(parseResult, response) {

        const dataSource = $routeParams.dataSource;

        // All connection grant requests, one per user/group
        const userRequests = {};
        const groupRequests = {};

        // Create a PermissionSet granting access to all connections at
        // the provided indices within the provided parse result
        const createPermissionSet = indices =>
            new PermissionSet({ connectionPermissions: indices.reduce(
                    (permissions, index) => {
                const connectionId = response.patches[index].identifier;
                permissions[connectionId] = [
                        PermissionSet.ObjectPermissionType.READ];
                return permissions;
            }, {}) });

        // Now that we've created all the users, grant access to each
        _.forEach(parseResult.users, (connectionIndices, identifier) =>

            // Grant the permissions - note the group flag is `false`
            userRequests[identifier] = permissionService.patchPermissions(
                dataSource, identifier,

                // Create the permissions to these connections for this user
                createPermissionSet(connectionIndices),

                // Do not remove any permissions
                new PermissionSet(),

                // This call is not for a group
                false));

        // Now that we've created all the groups, grant access to each
        _.forEach(parseResult.groups, (connectionIndices, identifier) =>

            // Grant the permissions - note the group flag is `true`
            groupRequests[identifier] = permissionService.patchPermissions(
                dataSource, identifier,

                // Create the permissions to these connections for this user
                createPermissionSet(connectionIndices),

                // Do not remove any permissions
                new PermissionSet(),

                // This call is for a group
                true));

        // Return the result from all the permission granting calls
        return $q.all({ ...userRequests, ...groupRequests });
    }

    // Given a PATCH API response, create an array of patches to delete every
    // entity created in the original request that generated this response
    const createDeletionPatches = creationResponse =>
        creationResponse.patches.map(patch =>

            // Add one deletion patch per original creation patch
            new DirectoryPatch({
                op: 'remove',
                path: '/' + patch.identifier
            }));

    /**
     * Given a successful response to a connection PATCH request, make another
     * request to delete every created connection in the provided request, i.e.
     * clean up every connection that was created.
     *
     * @param {DirectoryPatchResponse} creationResponse
     *     The response to the connection PATCH request.
     *
     * @returns {DirectoryPatchResponse}
     *     The response to the PATCH deletion request.
     */
    function cleanUpConnections(creationResponse) {

        return connectionService.patchConnections(
            $routeParams.dataSource, createDeletionPatches(creationResponse));

    }

    /**
     * Given a successful response to a user PATCH request, make another
     * request to delete every created user in the provided request.
     *
     * @param {DirectoryPatchResponse} creationResponse
     *     The response to the user PATCH request.
     *
     * @returns {DirectoryPatchResponse}
     *     The response to the PATCH deletion request.
     */
    function cleanUpUsers(creationResponse) {

        return userService.patchUsers(
            $routeParams.dataSource, createDeletionPatches(creationResponse));

    }

    /**
     * Process a successfully parsed import file, creating any specified
     * connections, creating and granting permissions to any specified users
     * and user groups. If successful, the user will be shown a success message.
     * If not, any errors will be displayed and any already-created entities
     * will be rolled back.
     *
     * @param {ParseResult} parseResult
     *     The result of parsing the user-supplied import file.
     */
    function handleParseSuccess(parseResult) {

        $scope.processing = false;
        $scope.parseResult = parseResult;

        // If errors were encounted during file parsing, abort further
        // processing - the user will have a chance to fix the errors and try
        // again
        if (parseResult.hasErrors)
            return;

        const dataSource = $routeParams.dataSource;

        // First, attempt to create the connections
        connectionService.patchConnections(dataSource, parseResult.patches)
                .then(connectionResponse =>

            // If connection creation is successful, create users and groups
            createUsersAndGroups(parseResult).then(() =>

                grantConnectionPermissions(parseResult, connectionResponse)
                        .then(() => {

                    $scope.processing = false;

                    // Display a success message if everything worked
                    guacNotification.showStatus({
                        className  : 'success',
                        title      : 'IMPORT.DIALOG_HEADER_SUCCESS',
                        text       : {
                            key: 'IMPORT.INFO_CONNECTIONS_IMPORTED_SUCCESS',
                            variables: { NUMBER: parseResult.patches.length }
                        },

                        // Add a button to acknowledge and redirect to
                        // the connection listing page
                        actions    : [{
                            name      : 'IMPORT.ACTION_ACKNOWLEDGE',
                            callback  : () => {

                                // Close the notification
                                guacNotification.showStatus(false);

                                // Redirect to connection list page
                                $location.url('/settings/' + dataSource + '/connections');
                            }
                        }]
                    });
                }))

            // If an error occurs while trying to users or groups, or while trying
            // to assign permissions to users or groups, clean up the already-created
            // connections, displaying an error to the user along with a blank slate
            // so they can fix their problems and try again.
            .catch(error => {
                cleanUpConnections(connectionResponse);
                handleError(error);
            }))

        // If an error occurred when the call to create the connections was made,
        // skip any further processing - the user will have a chance to fix the
        // problems and try again
        .catch(patchFailure => {
            $scope.processing = false;
            $scope.patchFailure = patchFailure;
        });
    }

    /**
     * Display the provided error to the user in a dismissable dialog.
     *
     * @argument {ParseError|Error} error
     *     The error to display.
     */
    const handleError = error => {

        // Any error indicates that processing of the file has failed, so clear
        // all upload state to allow for a fresh retry
        resetUploadState();

        let text;

        // If it's a import file parsing error
        if (error instanceof ParseError)
            text = {

                // Use the translation key if available
                key: error.key || error.message,
                variables: error.variables
            };

        // If it's a generic REST error
        else if (error instanceof Error)
            text = error.translatableMessage;

        // If it's an unknown type, just use the message directly
        else
            text = { key: error };

        guacNotification.showStatus({
            className  : 'error',
            title      : 'IMPORT.DIALOG_HEADER_ERROR',
            text,

            // Add a button to hide the error
            actions    : [{
                name      : 'IMPORT.ACTION_ACKNOWLEDGE',
                callback  : () => guacNotification.showStatus(false)
            }]
        });

    };

    /**
     * Process the uploaded import file, importing the connections, granting
     * connection permissions, or displaying errors to the user if there are
     * problems with the provided file.
     *
     * @param {String} mimeType
     *     The MIME type of the uploaded data file.
     *
     * @param {String} data
     *     The raw string contents of the import file.
     */
    function processData(mimeType, data) {

        // Data processing has begun
        $scope.processing = true;

        // The function that will process all the raw data and return a list of
        // patches to be submitted to the API
        let processDataCallback;

        // Choose the appropriate parse function based on the mimetype
        if (mimeType === JSON_MIME_TYPE)
            processDataCallback = connectionParseService.parseJSON;

        else if (mimeType === CSV_MIME_TYPE)
            processDataCallback = connectionParseService.parseCSV;

        else if (YAML_MIME_TYPES.indexOf(mimeType) >= 0)
            processDataCallback = connectionParseService.parseYAML;

        // The file type was validated before being uploaded - this should
        // never happen
        else
            processDataCallback = () => {
                throw new ParseError({
                    message: "Unexpected invalid file type: " + mimeType
                });
            };

        // Make the call to process the data into a series of patches
        processDataCallback(data)

            // Send the data off to be imported if parsing is successful
            .then(handleParseSuccess)

            // Display any error found while parsing the file
            .catch(handleError);
    }

    /**
     * Process the uploaded import data. Only usuable if the upload is fully
     * complete.
     */
    $scope.import = () => processData($scope.mimeType, $scope.fileData);

    /**
     * Returns true if import should be disabled, or false if import should be
     * allowed.
     *
     * @return {Boolean}
     *     True if import should be disabled, otherwise false.
     */
    $scope.importDisabled = () =>

        // Disable import if no data is ready
        !$scope.dataReady ||

        // Disable import if the file is currently being processed
        $scope.processing;

    /**
     * Cancel any in-progress upload, or clear any uploaded-but-errored-out
     * batch.
     */
    $scope.cancel = function() {

        // If the upload is in progress, stop it now; the FileReader will
        // reset the upload state when it stops
        if ($scope.fileReader) {
            $scope.aborted = true;
            $scope.fileReader.abort();
        }

        // Clear any upload state - there's no FileReader handler to do it
        else
            resetUploadState();

    };

    /**
     * Returns true if cancellation should be disabled, or false if
     * cancellation should be allowed.
     *
     * @return {Boolean}
     *     True if cancellation should be disabled, or false if cancellation
     *     should be allowed.
     */
    $scope.cancelDisabled = () =>

        // Disable cancellation if the import has already been cancelled
        $scope.aborted ||

        // Disable cancellation if the file is currently being processed
        $scope.processing ||

        // Disable cancellation if no data is ready or being uploaded
        !($scope.fileReader || $scope.dataReady);

    /**
     * Handle a provided File upload, reading all data onto the scope for
     * import processing, should the user request an import. Note that this
     * function is used as a callback for directives that invoke it with a file
     * list, but directive-level checking should ensure that there is only ever
     * one file provided at a time.
     *
     * @argument {File[]} files
     *     The files to upload onto the scope for further processing. There
     *     should only ever be a single file in the array.
     */
    $scope.handleFiles = files => {

        // There should only ever be a single file in the array
        const file = files[0];

        // The MIME type of the provided file
        const mimeType = file.type;

        // Check if the mimetype is one of the supported types,
        // e.g. "application/json" or "text/csv"
        if (LEGAL_MIME_TYPES.indexOf(mimeType) < 0) {

            // If the provided file is not one of the supported types,
            // display an error and abort processing
            handleError(new ParseError({
                message: "Invalid file type: " + mimeType,
                key: 'IMPORT.ERROR_INVALID_MIME_TYPE',
                variables: { TYPE: mimeType }
            }));
            return;
            
        }

        $scope.fileName = file.name;

        // Initialize upload state
        $scope.aborted = false;
        $scope.dataReady = false;
        $scope.processing = false;
        $scope.uploadStarted = true;

        // Save the MIME type to the scope
        $scope.mimeType = file.type;

        // Save the file to the scope when ready
        $scope.fileReader = new FileReader();
        $scope.fileReader.onloadend = (e => {

            // If the upload was explicitly aborted, clear any upload state and
            // do not process the data
            if ($scope.aborted)
                resetUploadState();

            else {

                // Save the uploaded data
                $scope.fileData = e.target.result;

                // Mark the data as ready
                $scope.dataReady = true;

                // Clear the file reader from the scope now that this file is
                // fully uploaded
                $scope.fileReader = null;

            }
        });

        // Read all the data into memory
        $scope.fileReader.readAsBinaryString(file);
    };

    /**
     * The name of the file that's currently being uploaded, or has yet to
     * be imported, if any.
     */
    $scope.fileName = null;

}]);
