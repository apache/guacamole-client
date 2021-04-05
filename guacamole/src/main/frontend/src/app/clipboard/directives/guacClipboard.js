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
 * A directive provides an editor whose contents are exposed via a
 * ClipboardData object via the "data" attribute. If this data should also be
 * synced to the local clipboard, or sent via a connected Guacamole client
 * using a "guacClipboard" event, it is up to external code to do so.
 */
angular.module('clipboard').directive('guacClipboard', ['$injector',
    function guacClipboard($injector) {

    // Required types
    var ClipboardData = $injector.get('ClipboardData');

    /**
     * Configuration object for the guacClipboard directive.
     *
     * @type Object.<String, Object>
     */
    var config = {
        restrict    : 'E',
        replace     : true,
        templateUrl : 'app/clipboard/templates/guacClipboard.html'
    };

    // Scope properties exposed by the guacClipboard directive
    config.scope = {

        /**
         * The data to display within the field provided by this directive. This
         * data will modified or replaced when the user manually alters the
         * contents of the field.
         *
         * @type ClipboardData
         */
        data : '='

    };

    // guacClipboard directive controller
    config.controller = ['$scope', '$injector', '$element',
            function guacClipboardController($scope, $injector, $element) {

        /**
         * The DOM element which will contain the clipboard contents within the
         * user interface provided by this directive.
         *
         * @type Element
         */
        var element = $element[0];

        /**
         * Rereads the contents of the clipboard field, updating the
         * ClipboardData object on the scope as necessary. The type of data
         * stored within the ClipboardData object will be heuristically
         * determined from the HTML contents of the clipboard field.
         */
        var updateClipboardData = function updateClipboardData() {

            // Read contents of clipboard textarea
            $scope.$evalAsync(function assignClipboardText() {
                $scope.data = new ClipboardData({
                    type : 'text/plain',
                    data : element.value
                });
            });

        };

        // Update the internally-stored clipboard data when events are fired
        // that indicate the clipboard field may have been changed
        element.addEventListener('input', updateClipboardData);
        element.addEventListener('change', updateClipboardData);

        // Watch clipboard for new data, updating the clipboard textarea as
        // necessary
        $scope.$watch('data', function clipboardDataChanged(data) {

            // If the clipboard data is a string, render it as text
            if (typeof data.data === 'string')
                element.value = data.data;

            // Ignore other data types for now

        }); // end $scope.data watch

    }];

    return config;

}]);
