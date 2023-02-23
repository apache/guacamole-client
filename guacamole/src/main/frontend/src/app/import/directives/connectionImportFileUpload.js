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
 * All legal import file types. Any file not belonging to one of these types
 * must be rejected.
 */
const LEGAL_FILE_TYPES = ["csv", "json", "yaml"];

/**
 * A directive that allows for file upload, either through drag-and-drop or
 * a file browser.
 */
angular.module('import').directive('connectionImportFileUpload', [
        function connectionImportFileUpload() {

    const directive = {
        restrict: 'E',
        replace: true,
        templateUrl: 'app/import/templates/connectionImportFileUpload.html',
        scope: {

            /**
             * The function to invoke when a file is provided to the file upload
             * UI, either by dragging and dropping, or by navigating using the
             * file browser. The function will be called with 2 arguments - the
             * mime type, and the raw string contents of the file.
             *
             * @type function
             */
            onFile : '&',
        }
    };

    directive.controller = ['$scope', '$injector', '$element',
            function fileUploadController($scope, $injector, $element) {

        // Required services
        const $timeout = $injector.get('$timeout');

        /**
         * Whether a drag/drop operation is currently in progress (the user has
         * dragged a file over the Guacamole connection but has not yet
         * dropped it).
         *
         * @type boolean
         */
        $scope.dropPending = false;

        /**
         * The error associated with the file upload, if any. An object of the
         * form { key, variables }, or null if no error has occured.
         */
        $scope.error = null;

        /**
         * The name of the file that's currently being uploaded, or has yet to
         * be imported, if any.
         */
        $scope.fileName = null;

        // Clear the file if instructed to do so by the parent
        $scope.$on('clearFile', () => delete $scope.fileName);

        /**
         * Clear any displayed error message.
         */
        const clearError = () => $scope.error = null;

        /**
         * Set an error for display using the provided translation key and
         * translation variables.
         *
         * @param {String} key
         *     The translation key.
         *
         * @param {Object.<String, String>} variables
         *     The variables to subsitute into the message, if any.
         */
        const setError = (key, variables) => $scope.error = { key, variables };

        /**
         * The location where files can be dragged-and-dropped to.
         *
         * @type Element
         */
        const dropTarget = $element.find('.drop-target')[0];

        /**
         * Displays a visual indication that dropping the file currently
         * being dragged is possible. Further propagation and default behavior
         * of the given event is automatically prevented.
         *
         * @param {Event} e
         *     The event related to the in-progress drag/drop operation.
         */
        const notifyDragStart = function notifyDragStart(e) {

            e.preventDefault();
            e.stopPropagation();

            $scope.$apply(() => {
                $scope.dropPending = true;
            });

        };

        /**
         * Removes the visual indication that dropping the file currently
         * being dragged is possible. Further propagation and default behavior
         * of the given event is automatically prevented.
         *
         * @param {Event} e
         *     The event related to the end of the former drag/drop operation.
         */
        const notifyDragEnd = function notifyDragEnd(e) {

            e.preventDefault();
            e.stopPropagation();

            $scope.$apply(() => {
                $scope.dropPending = false;
            });

        };

        // Add listeners to the drop target to ensure that the visual state
        // stays up to date
        dropTarget.addEventListener('dragenter', notifyDragStart, false);
        dropTarget.addEventListener('dragover',  notifyDragStart, false);
        dropTarget.addEventListener('dragleave', notifyDragEnd,   false);

        /**
         * Given a user-supplied file, validate that the file type is correct,
         * and invoke the onFile callback provided to this directive if so.
         * 
         * @param {File} file
         *     The user-supplied file.
         */
        function handleFile(file) {

            // Clear any error from a previous attempted file upload
            clearError();

            // The MIME type of the provided file
            const mimeType = file.type;

            // Check if the mimetype ends with one of the supported types,
            // e.g. "application/json" or "text/csv"
            if (_.every(LEGAL_FILE_TYPES.map(
                    type => !mimeType.endsWith(type)))) {

                // If the provided file is not one of the supported types,
                // display an error and abort processing
                setError('IMPORT.ERROR_INVALID_FILE_TYPE',
                        { TYPE: mimeType });
                return;
            }

            $scope.fileName = file.name;

            // Invoke the provided file callback using the file
            $scope.onFile({ file });
        }

        /**
         * Drop target event listener that will be invoked if the user drops
         * anything onto the drop target. If a valid file is provided, the
         * onFile callback provided to this directive will be called; otherwise
         * an error will be displayed, if appropriate.
         *
         * @param {Event} e
         *     The drop event that triggered this handler.
         */
        dropTarget.addEventListener('drop', function(e) {

            notifyDragEnd(e);
            
            const files = e.dataTransfer.files;

            // Ignore any non-files that are dragged into the drop area
            if (files.length < 1)
                return;

            if (files.length > 2) {

                // If more than one file was provided, print an error explaining
                // that only a single file is allowed and abort processing
                setError('IMPORT.ERROR_FILE_SINGLE_ONLY');
                return;
            }

            handleFile(files[0]);
            
        }, false);

        /**
         * The hidden file input used to create a file browser.
         *
         * @type Element
         */
        const fileUploadInput = $element.find('.file-upload-input')[0];

        /**
         * A function that will click on the hidden file input to open a file
         * browser to allow the user to select a file for upload.
         */
        $scope.openFileBrowser = () =>
                $timeout(() => fileUploadInput.click(), 0, false);

        /**
         * A handler that will be invoked when a user selectes a file in the
         * file browser. After some error checking, the file will be passed to
         * the onFile callback provided to this directive.
         *
         * @param {Event} e
         *     The event that was triggered when the user selected a file in
         *     their file browser.
         */
        fileUploadInput.onchange = e => {

            // Process the uploaded file
            handleFile(e.target.files[0]);

            // Clear the value to ensure that the change event will be fired
            // if the user selects the same file again
            fileUploadInput.value = null;
            
        };
        
    }];
    return directive;
    
}]);