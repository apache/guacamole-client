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
 * A fallback regular expression for CSV filenames, if no MIME type is provided
 * by the browser. Any file that matches this regex will be considered to be a
 * CSV file.
 *
 * @type RegExp
 */
const CSV_FILENAME_REGEX = /\.csv$/i;

/**
 * The allowed MIME type for JSON files.
 *
 * @type String
 */
const JSON_MIME_TYPE = 'application/json';

/**
 * A fallback regular expression for JSON filenames, if no MIME type is provided
 * by the browser. Any file that matches this regex will be considered to be a
 * JSON file.
 *
 * @type RegExp
 */
const JSON_FILENAME_REGEX = /\.json$/i;

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

/**
 * A fallback regular expression for YAML filenames, if no MIME type is provided
 * by the browser. Any file that matches this regex will be considered to be a
 * YAML file.
 *
 * @type RegExp
 */
const YAML_FILENAME_REGEX = /\.ya?ml$/i;

/**
 * Possible signatures for zip files (which include most modern Microsoft office
 * documents - most notable excel). If any file, regardless of extension, has
 * these starting bytes, it's invalid and must be rejected.
 * For more, see https://en.wikipedia.org/wiki/List_of_file_signatures and
 * https://en.wikipedia.org/wiki/Magic_number_(programming)#Magic_numbers_in_files.
 *
 * @type String[]
 */
const ZIP_SIGNATURES = [
    'PK\u0003\u0004',
    'PK\u0005\u0006',
    'PK\u0007\u0008'
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
    const connectionGroupService = $injector.get('connectionGroupService');
    const connectionParseService = $injector.get('connectionParseService');
    const connectionService      = $injector.get('connectionService');
    const guacNotification       = $injector.get('guacNotification');
    const permissionService      = $injector.get('permissionService');
    const userService            = $injector.get('userService');
    const userGroupService       = $injector.get('userGroupService');

    // Required types
    const Connection             = $injector.get('Connection');
    const ConnectionGroup        = $injector.get('ConnectionGroup');
    const ConnectionImportConfig = $injector.get('ConnectionImportConfig');
    const DirectoryPatch         = $injector.get('DirectoryPatch');
    const Error                  = $injector.get('Error');
    const ImportConnection       = $injector.get('ImportConnection');
    const ParseError             = $injector.get('ParseError');
    const PermissionSet          = $injector.get('PermissionSet');
    const User                   = $injector.get('User');
    const UserGroup              = $injector.get('UserGroup');

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
    $scope.patchFailure = null;

    /**
     * The DirectoryPatch list last sent to patchConnections, kept so
     * connection-import-errors can map API failures back to file rows.
     *
     * @type {DirectoryPatch[]|null}
     */
    $scope.patches = null;

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
     * The name of the file that's currently being uploaded, or has yet to
     * be imported, if any.
     *
     * @type {String}
     */
    $scope.fileName = null;

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
     * The configuration options for this import, to be chosen by the user.
     *
     * @type {ConnectionImportConfig}
     */
    $scope.importConfig = new ConnectionImportConfig();

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
        $scope.patches = null;
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
     * already exist in the current data source. Return an object describing the
     * result of the creation requests.
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

            // Create all the users and groups
            return $q.all({
                userResponse: userService.patchUsers(dataSource, userPatches),
                userGroupResponse: userGroupService.patchUserGroups(
                        dataSource, groupPatches)
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

    /**
     * When `$scope.importConfig.createMissingGroups` is true, loads the
     * connection group tree for the data source, creates any group path
     * segments from the import file that do not yet exist (sequentially, so
     * parents exist before children), and sets `parentIdentifier` on each
     * connection object that still needs it from the resolved `group` path.
     * When the option is false, resolves immediately without modifying
     * `parseResult`.
     *
     * Nothing is returned from the resolved promise; connection objects on
     * `parseResult` are updated in place for use when building patches.
     *
     * @param {DataSource} dataSource
     *     The data source in which groups should exist.
     *
     * @param {ParseResult} parseResult
     *     Parsed import data; connection objects may be updated in place.
     *
     * @returns {Promise}
     *     A promise that resolves with no value when finished, or rejects if
     *     the group tree cannot be loaded or a group cannot be saved.
     */
    function ensureConnectionGroups(dataSource, parseResult) {

        if (!$scope.importConfig.createMissingGroups)
            return $q.resolve();

        return connectionGroupService.getConnectionGroupTree(dataSource).then(rootGroup => {

            const identifierByPath = {};
            const flatten = (group, prefix = "") => {
                // The first node is always "ROOT"
                const currentPath = prefix ? (prefix + "/" + group.name) : "ROOT";
                
                // Map the path string to the REAL system identifier
                if (group.identifier) {
                    identifierByPath[currentPath] = group.identifier;
                }

                if (group.childConnectionGroups) {
                    group.childConnectionGroups.forEach(c => flatten(c, currentPath));
                }
            };
            
            flatten(rootGroup);

            let sequence = $q.resolve();

            // Iterate through connections
            _.forEach(parseResult.connectionObjects, (connectionObject) => {
                const fullPath = connectionObject.group; // e.g., "ROOT/Folder/Subfolder"
                if (!fullPath) return;

                const segments = fullPath.split('/');
                let currentPath = "ROOT"; 

                segments.forEach(segment => {
                    // Ignore empty segments and the literal "ROOT" segment in the path string
                    if (!segment || segment === 'ROOT') return;

                    const parentPath = currentPath;
                    const nextPath = parentPath + "/" + segment;
                    currentPath = nextPath;

                    sequence = sequence.then(() => {
                        // If this absolute path already exists, skip creation
                        if (identifierByPath[nextPath]) return;

                        // Create the new group under the parent's REAL identifier
                        const newGroup = new ConnectionGroup({
                            name: segment,
                            parentIdentifier: identifierByPath[parentPath] 
                        });

                        return connectionGroupService.saveConnectionGroup(dataSource, newGroup)
                            .then(() => {
                                identifierByPath[nextPath] = newGroup.identifier;
                            });
                    });
                });
            });

            return sequence.then(() => {
                parseResult.connectionObjects.forEach(connectionObject => {
                    if (!connectionObject.parentIdentifier) {
                        connectionObject.parentIdentifier =
                            identifierByPath[connectionObject.group] || identifierByPath['ROOT'];
                    }
                });
            });
        });
    }

    /**
     * Converts the parsed connection data into a set of administrative 
     * patches. Each patch represents an atomic operation to create or 
     * update a connection, including its attributes, parameters, and 
     * its placement within the connection group hierarchy.
     *
     * @param {ImportConfig} importConfig
     *     The configuration object defining how the import should be 
     *     processed and which fields should be prioritized.
     *
     * @param {ParseResult} parseResult
     *     The result of parsing the user-supplied import file, used as 
     *     the source data for the generated patches.
     * 
     * @returns {DirectoryPatch[]}
     *     Patches to apply via the connection directory PATCH API.
     */
    function createConnectionPatches(importConfig, parseResult) {

        const patches = [];

        parseResult.connectionObjects.forEach(connectionObject => {

            // The value for the patch is a full-fledged Connection
            const value = new Connection(connectionObject);

            // If a new connection is being created
            if (connectionObject.importMode 
                    === ImportConnection.ImportMode.CREATE) 

                // Add a patch for creating the connection
                patches.push(new DirectoryPatch({
                    op: DirectoryPatch.Operation.ADD,
                    path: '/',
                    value
                }));

            // The connection is being replaced, and permissions are only being
            // added, not replaced
            else if (importConfig.existingPermissionMode ===
                    ConnectionImportConfig.ExistingPermissionMode.PRESERVE)

                // Add a patch for replacing the connection
                patches.push(new DirectoryPatch({
                    op: DirectoryPatch.Operation.REPLACE,
                    path: '/' + connectionObject.identifier,
                    value
                }));

            // The connection is being replaced, and permissions are also being
            // replaced
            else {

                // Add a patch for removing the existing connection
                patches.push(new DirectoryPatch({
                    op: DirectoryPatch.Operation.REMOVE,
                    path: '/' + connectionObject.identifier
                }));

                // Add a second patch for creating the replacement connection
                patches.push(new DirectoryPatch({
                    op: DirectoryPatch.Operation.ADD,
                    path: '/',
                    value
                }));

            }
        });
        return patches;
    };

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

        $scope.patches = null;
        $scope.patchFailure = null;
        $scope.processing = true;

        // 1. Create missing groups (updates parentIdentifier on connection objects)
        ensureConnectionGroups(dataSource, parseResult)
                .then(() => {

            // 2. Build patches from connection objects (includes resolved parent IDs)
            $scope.patches = createConnectionPatches($scope.importConfig,
                    parseResult);

            // 3. Import the connections
            return connectionService.patchConnections(dataSource, $scope.patches)
                    .then(connectionResponse => {

                // 4. Create Users/Groups and Grant Permissions
                return createUsersAndGroups(parseResult).then(() => {

                        // 5. Grant permissions
                        return grantConnectionPermissions(parseResult, connectionResponse)
                                .then(() => {

                            $scope.processing = false;
                            guacNotification.showStatus({
                                className: 'success',
                                title: 'IMPORT.DIALOG_HEADER_SUCCESS',
                                text: {
                                    key: 'IMPORT.INFO_CONNECTIONS_IMPORTED_SUCCESS',
                                    variables: { NUMBER: parseResult.connectionCount }
                                },
                                actions: [{
                                    name: 'IMPORT.ACTION_ACKNOWLEDGE',
                                    callback: () => {
                                        guacNotification.showStatus(false);
                                        $location.url('/settings/' + dataSource + '/connections');
                                    }
                                }]
                            });
                        });
                    })
                    // If an error occurs while trying to create users or groups, 
                    // display the error to the user.
                    .catch(error => {
                        $scope.processing = false;
                        handleError(error);
                    });
                });
        })
        // Use handleError if errors before patches exist (e.g. group tree load/save),
        // because they cannot be shown in the per-connection table. Otherwise, keep
        // parse state and surface API errors in the table via patchFailure.
        .catch(patchFailure => {
            $scope.processing = false;
            if ($scope.patches == null)
                handleError(patchFailure);
            else
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

        // The parse function for this file type; resolves with a ParseResult
        // (connection objects and related metadata).
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
        processDataCallback($scope.importConfig, data)

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
                handleError(new ParseError({
                    message: "Unknown type for file: " + fileName,
                    key: 'IMPORT.ERROR_DETECTED_INVALID_TYPE'
                }));
                return;
                
            }

        }

        // Check if the mimetype is one of the supported types,
        // e.g. "application/json" or "text/csv"
        else if (LEGAL_MIME_TYPES.indexOf(mimeType) < 0) {

            // If the provided file is not one of the supported types,
            // display an error and abort processing
            handleError(new ParseError({
                message: "Invalid file type: " + mimeType,
                key: 'IMPORT.ERROR_INVALID_MIME_TYPE',
                variables: { TYPE: mimeType }
            }));
            return;
            
        }

        // Save the name and type to the scope
        $scope.fileName = fileName;
        $scope.mimeType = mimeType;

        // Initialize upload state
        $scope.aborted = false;
        $scope.dataReady = false;
        $scope.processing = false;
        $scope.uploadStarted = true;

        // Save the file to the scope when ready
        $scope.fileReader = new FileReader();
        $scope.fileReader.onloadend = (e => {

            // If the upload was explicitly aborted, clear any upload state and
            // do not process the data
            if ($scope.aborted)
                resetUploadState();

            else {

                const fileData = e.target.result;

                // Check if the file has a header of a known-bad type
                if (_.some(ZIP_SIGNATURES,
                        signature => fileData.startsWith(signature))) {

                    // Throw an error and abort processing
                    handleError(new ParseError({
                        message: "Invalid file type detected",
                        key: 'IMPORT.ERROR_DETECTED_INVALID_TYPE'
                    }));
                    return;

                }

                // Save the uploaded data
                $scope.fileData = fileData;

                // Mark the data as ready
                $scope.dataReady = true;

                // Clear the file reader from the scope now that this file is
                // fully uploaded
                $scope.fileReader = null;

            }
        });

        // Read all the data into memory
        $scope.fileReader.readAsText(file);
    };
    
}]);
