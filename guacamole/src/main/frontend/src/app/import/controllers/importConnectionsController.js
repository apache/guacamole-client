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
 * The controller for the connection import page.
 */
angular.module('import').controller('importConnectionsController', ['$scope', '$injector',
        function importConnectionsController($scope, $injector) {

    // Required services
    const $q                     = $injector.get('$q');
    const $routeParams           = $injector.get('$routeParams');
    const connectionParseService = $injector.get('connectionParseService');
    const connectionService      = $injector.get('connectionService');
    const permissionService      = $injector.get('permissionService');
    const userService            = $injector.get('userService');
    const userGroupService       = $injector.get('userGroupService');
    
    // Required types
    const DirectoryPatch      = $injector.get('DirectoryPatch');
    const ParseError          = $injector.get('ParseError');
    const PermissionSet       = $injector.get('PermissionSet');
    const TranslatableMessage = $injector.get('TranslatableMessage');
    const User                = $injector.get('User');
    const UserGroup           = $injector.get('UserGroup');

    /**
     * Any error that may have occured during import file parsing.
     *
     * @type {ParseError}
     */
    $scope.error = null;

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

        // Broadcast an event to clear the file upload UI
        $scope.$broadcast('clearFile');
        
    }

    /**
     * Create all users and user groups mentioned in the import file that don't
     * already exist in the current data source.
     *
     * @param {ParseResult} parseResult
     *     The result of parsing the user-supplied import file.
     *
     * @return {Object}
     *     An object containing the results of the calls to create the users
     *     and groups.
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

            return $q.all({
                userResponse: userService.patchUsers(dataSource, userPatches),
                groupResponse: userGroupService.patchUserGroups(dataSource, groupPatches)
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
            $routeParams.dataSource, createDeletionPatches(creationResponse))

            // TODO: Better error handling? Make additional cleanup requests?
            .catch(handleError);

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
            $routeParams.dataSource, createDeletionPatches(creationResponse))

            // TODO: Better error handling? Make additional cleanup requests?
            .catch(handleError);

    }

    /**
     * Given a successful response to a user group PATCH request, make another
     * request to delete every created user group in the provided request.
     *
     * @param {DirectoryPatchResponse} creationResponse
     *     The response to the user group PATCH creation request.
     *
     * @returns {DirectoryPatchResponse}
     *     The response to the PATCH deletion request.
     */
    function cleanUpUserGroups(creationResponse) {

        return userGroupService.patchUserGroups(
            $routeParams.dataSource, createDeletionPatches(creationResponse))

            // TODO: Better error handling? Make additional cleanup requests?
            .catch(handleError);

    }

    /**
     * Make requests to delete all connections, users, and/or groups from any
     * provided PATCH API responses. If any responses are not provided, no
     * cleanup will be attempted.
     *
     * @param {DirectoryPatchResponse} connectionResponse
     *     The response to the connection PATCH creation request.
     *
     * @param {DirectoryPatchResponse} userResponse
     *     The response to the user PATCH creation request.
     *
     * @param {DirectoryPatchResponse} userGroupResponse
     *     The response to the user group PATCH creation request.
     * 
     * @returns {Object}
     *     An object containing PATCH deletion responses corresponding to any
     *     provided connection, user, and/or user group creation responses.
     */
    function cleanUpAll(connectionResponse, userResponse, userGroupResponse) {

        // All cleanup requests that need to be made
        const requests = {};

        // If the connection response was provided, clean up connections
        if (connectionResponse)
            requests.connectionCleanup = cleanUpConnections(connectionResponse);

        // If the user response was provided, clean up users
        if (userResponse)
            requests.userCleanup = cleanUpUsers(userResponse);

        // If the user group response was provided, clean up user groups
        if (connectionResponse)
            requests.userGroupCleanup = cleanUpUserGroups(userGroupResponse);

        // Return when all cleanup is complete
        return $q.all(requests);
    }

    /**
     * Process a successfully parsed import file, creating any specified
     * connections, creating and granting permissions to any specified users
     * and user groups. If successful, the user will be shown a success message.
     * If not, any errors will be displayed, and the user will be given ???an
     * option??? to roll back any already-created entities.
     *
     * @param {ParseResult} parseResult
     *     The result of parsing the user-supplied import file.
     */
    function handleParseSuccess(parseResult) {

        const dataSource = $routeParams.dataSource;

        console.log("parseResult", parseResult);

        // First, attempt to create the connections
        connectionService.patchConnections(dataSource, parseResult.patches)
                .then(connectionResponse => {

            // If connection creation is successful, create users and groups
            createUsersAndGroups(parseResult).then(
                    ({userResponse, groupResponse}) => 

                grantConnectionPermissions(parseResult, connectionResponse)
                        .then(() =>
                   
                    // TODON'T: Delete the stuff so we can test over and over
                    cleanUpAll(connectionResponse, userResponse, groupResponse)
                        .then(resetUploadState)

                ));
        });
    }
    
    /**
     * Set any caught error message to the scope for display.
     *
     * @argument {ParseError} error
     *     The error to display.
     */
    const handleError = error => {

        // Any error indicates that processing of the file has failed, so clear
        // all upload state to allow for a fresh retry
        resetUploadState();

        // Set the error for display
        console.error(error);
        $scope.error = error;
        
    };
    
    /**
     * Clear the current displayed error.
     */
    const clearError = () => delete $scope.error;

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
        if (mimeType.endsWith("json"))
            processDataCallback = connectionParseService.parseJSON;

        else if (mimeType.endsWith("csv"))
            processDataCallback = connectionParseService.parseCSV;

        else if (mimeType.endsWith("yaml"))
            processDataCallback = connectionParseService.parseYAML;

        // We don't expect this to happen - the file upload directive should
        // have already have filtered out any invalid file types
        else {
            handleError(new ParseError({
                message: 'Invalid file type: ' + mimeType,
                key: 'CONNECTION_IMPORT.INVALID_FILE_TYPE',
                variables: { TYPE: mimeType }
            }));
            return;
        }

        // Make the call to process the data into a series of patches
        // TODO: Check if there's errors, and if so, display those rather than
        // just YOLOing a create call
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
     * @return {Boolean}
     *     True if import should be disabled, or false if cancellation
     *     should be allowed.
     */
    $scope.importDisabled = () =>
        
        // Disable import if no data is ready
        !$scope.dataReady ||

        // Disable import if the file is currently being processed
        $scope.processing;

    /**
     * Cancel any in-progress upload, or clear any uploaded-but
     */
    $scope.cancel = function() {

        // Clear any error message
        clearError();

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
     * import processing, should the user request an import.
     *
     * @argument {File} file
     *     The file to upload onto the scope for further processing.
     */
    $scope.handleFile = function(file) {
        
        // Clear any error message from the previous upload attempt
        clearError();

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

}]);
