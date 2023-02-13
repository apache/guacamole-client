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

/**
 * The controller for the connection import page.
 */
angular.module('import').controller('importConnectionsController', ['$scope', '$injector',
        function importConnectionsController($scope, $injector) {

    // Required services
    const $routeParams           = $injector.get('$routeParams');
    const connectionParseService = $injector.get('connectionParseService');
    const connectionService      = $injector.get('connectionService');
    
    // Required types
    const DirectoryPatch      = $injector.get('DirectoryPatch');
    const ParseError          = $injector.get('ParseError');
    const TranslatableMessage = $injector.get('TranslatableMessage');

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
            .catch(handleParseError);

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
        connectionService.patchConnections(
                $routeParams.dataSource, parseResult.patches)
        
                .then(response => {
            console.log("Creation Response", response);

            // TODON'T: Delete connections so we can test over and over
            cleanUpConnections(response);
        });
    }
    
    // Set any caught error message to the scope for display
    const handleParseError = error => {
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
            .catch(handleParseError);
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
