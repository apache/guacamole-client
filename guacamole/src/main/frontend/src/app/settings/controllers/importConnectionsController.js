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
angular.module('settings').controller('importConnectionsController', ['$scope', '$injector',
        function importConnectionsController($scope, $injector) {

    // Required services
    const connectionImportParseService = $injector.get('connectionImportParseService');
    const connectionService = $injector.get('connectionService');

    function processData(type, data) {

        let requestBody;

        // Parse the data based on the provided mimetype
        switch(type) {

            case "application/json":
            case "text/json":
                requestBody = connectionImportParseService.parseJSON(data);
                break;

            case "text/csv":
                requestBody = connectionImportParseService.parseCSV(data);
                break;

            case "application/yaml":
            case "application/x-yaml":
            case "text/yaml":
            case "text/x-yaml":
                requestBody = connectionImportParseService.parseYAML(data);
                break;

        }

        console.log(requestBody);
    }

    $scope.upload = function() {

        const files = angular.element('#file')[0].files;

        if (files.length <= 0) {
            console.error("TODO: This should be a proper error tho");
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
