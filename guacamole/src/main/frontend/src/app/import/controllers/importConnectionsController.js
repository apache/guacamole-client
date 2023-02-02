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
    const connectionParseService = $injector.get('connectionParseService');
    const connectionService      = $injector.get('connectionService');
    
    // Required types
    const ParseError          = $injector.get('ParseError');
    const TranslatableMessage = $injector.get('TranslatableMessage');
    
    function handleSuccess(data) {
        console.log("OMG SUCCESS: ", data)
    }
    
    // Set any caught error message to the scope for display
    const handleError = error => {
        console.error(error);
        $scope.error = error;
    }
    
    // Clear the current error
    const clearError = () => delete $scope.error;

    function processData(type, data) {
        
        // The function that will process all the raw data and return a list of
        // patches to be submitted to the API
        let processDataCallback;

        // Parse the data based on the provided mimetype
        switch(type) {

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
            .then(handleSuccess)

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
