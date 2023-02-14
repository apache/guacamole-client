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
     * Given a successful response to an import PATCH request, make another
     * request to delete every created connection in the provided request, i.e.
     * clean up every connection that was created.
     *
     * @param {DirectoryPatchResponse} creationResponse
     */
    function cleanUpConnections(creationResponse) {

        // The patches to delete - one delete per initial creation
        const deletionPatches = creationResponse.patches.map(patch =>
            new DirectoryPatch({
                op: 'remove',
                path: '/' + patch.identifier
            }));

        console.log("Deletion Patches", deletionPatches);

        connectionService.patchConnections(
            $routeParams.dataSource, deletionPatches)
    
            .then(deletionResponse =>
                console.log("Deletion response", deletionResponse))
            .catch(handleError);

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
                createdUsers: userService.patchUsers(dataSource, userPatches),
                createdGroups: userGroupService.patchUserGroups(dataSource, groupPatches)
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
     * Process a successfully parsed import file, creating any specified
     * connections, creating and granting permissions to any specified users
     * and user groups.
     * 
     * TODO:
     * - Do batch import of connections
     * - Create all users/groups not already present
     * - Grant permissions to all users/groups as defined in the import file
     * - On failure: Roll back everything (maybe ask the user first):
     *   - Attempt to delete all created connections
     *   - Attempt to delete any created users / groups
     *
     * @param {ParseResult} parseResult
     *     The result of parsing the user-supplied import file.
     *
     */
    function handleParseSuccess(parseResult) {

        const dataSource = $routeParams.dataSource;

        console.log("parseResult", parseResult);

        // First, attempt to create the connections
        connectionService.patchConnections(dataSource, parseResult.patches)
                .then(response => {

            // If connection creation is successful, create users and groups
            createUsersAndGroups(parseResult).then(() => {

                grantConnectionPermissions(parseResult, response).then(results => {
                    console.log("permission requests", results);
                   
                    // TODON'T: Delete connections so we can test over and over
                    cleanUpConnections(response);
                })


            });
        });
    }
    
    // Set any caught error message to the scope for display
    const handleError = error => {
        console.error(error);
        $scope.error = error;
    }
    
    // Clear the current error
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
        
        // The function that will process all the raw data and return a list of
        // patches to be submitted to the API
        let processDataCallback;

        // Parse the data based on the provided mimetype
        switch(mimeType) {

            case "application/json":
            case "text/json":
                processDataCallback = connectionParseService.parseJSON;
                break;

            case "text/csv":
                processDataCallback = connectionParseService.parseCSV;
                break;

            case "application/yaml":
            case "application/x-yaml":
            case "text/yaml":
            case "text/x-yaml":
                processDataCallback = connectionParseService.parseYAML;
                break;
                
            default:
                handleError(new ParseError({
                    message: 'Invalid file type: ' + type,
                    key: 'CONNECTION_IMPORT.INVALID_FILE_TYPE',
                    variables: { TYPE: type } 
                }));
                return;
        }
        
        // Make the call to process the data into a series of patches
        processDataCallback(data)

            // Send the data off to be imported if parsing is successful
            .then(handleParseSuccess)

            // Display any error found while parsing the file
            .catch(handleError);
    }

    $scope.upload = function() {
        
        // Clear any error message from the previous upload attempt
        clearError();

        const files = angular.element('#file')[0].files;

        if (files.length <= 0) {
            handleError(new ParseError({
                message: 'No file supplied',
                key: 'CONNECTION_IMPORT.ERROR_NO_FILE_SUPPLIED'
            }));
            return;
        }

        // The file that the user uploaded
        const file = files[0];

        // Call processData when the data is ready
        const reader = new FileReader();
        reader.onloadend = (e => processData(file.type, e.target.result));

        // Read all the data into memory and call processData when done
        reader.readAsBinaryString(file);
    }

}]);
