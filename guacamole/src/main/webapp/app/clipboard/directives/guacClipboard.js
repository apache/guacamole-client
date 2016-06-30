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

        // Required services
        var $window          = $injector.get('$window');
        var clipboardService = $injector.get('clipboardService');

        /**
         * The FileReader to use to read File or Blob data received from the
         * clipboard.
         *
         * @type FileReader
         */
        var reader = new FileReader();

        /**
         * The content-editable DOM element which will contain the clipboard
         * contents within the user interface provided by this directive.
         *
         * @type Element
         */
        var element = $element[0];

        /**
         * Returns all files currently contained within the local clipboard,
         * given a ClipboardEvent which should contain the current clipboard
         * data. If no files are contained within the local clipboard, null
         * is returned.
         *
         * @param {ClipboardEvent} e
         *     The ClipboardEvent which should contain the current clipboard
         *     data.
         *
         * @returns {File[]}
         *     An array of all files currently contained with the clipboard, as
         *     provided by the given ClipboardEvent, or null if no files are
         *     present.
         */
        var getClipboardFiles = function getClipboardFiles(e) {

            // Pull the clipboard data object
            var clipboardData = e.clipboardData || $window.clipboardData;

            // Read from the standard clipboard API items collection first
            var items = clipboardData.items;
            if (items) {

                var files = [];

                // Produce array of all files from clipboard data
                for (var i = 0; i < items.length; i++) {
                    if (items[i].kind === 'file')
                        files.push(items[i].getAsFile());
                }

                return files;

            }

            // Failing that, try the files collection
            if (clipboardData.files)
                return clipboardData.files;

            // No files accessible within given data
            return null;

        };

        // Intercept paste events, handling image data specifically
        element.addEventListener('paste', function dataPasted(e) {

            // Read all files from the clipboard data within the event
            var files = getClipboardFiles(e);
            if (!files)
                return;

            // For each item within the clipboard
            for (var i = 0; i < files.length; i++) {

                var file = files[i];

                // If the file is an image, attempt to read that image
                if (/^image\//.exec(file.type)) {

                    // Set clipboard data to contents
                    $scope.$apply(function setClipboardData() {
                        $scope.data = new ClipboardData({
                            type : file.type,
                            data : file
                        });
                    });

                    // Do not paste
                    e.preventDefault();
                    return;

                }

            } // end for each item

        });

        /**
         * Rereads the contents of the clipboard field, updating the
         * ClipboardData object on the scope as necessary. The type of data
         * stored within the ClipboardData object will be heuristically
         * determined from the HTML contents of the clipboard field.
         */
        var updateClipboardData = function updateClipboardData() {

            // If the clipboard contains a single image, parse and assign the
            // image data to the internal clipboard
            var currentImage = clipboardService.getImageContent(element);
            if (currentImage) {

                // Convert the image's data URL into a blob
                var blob = clipboardService.parseDataURL(currentImage);
                if (blob) {

                    // Complete the assignment if conversion was successful
                    $scope.$evalAsync(function assignClipboardData() {
                        $scope.data = new ClipboardData({
                            type : blob.type,
                            data : blob
                        });
                    });

                    return;

                }

            } // end if clipboard is an image

            // If data does not appear to be an image, or image decoding fails,
            // assume clipboard contents are text
            $scope.$evalAsync(function assignClipboardText() {
                $scope.data = new ClipboardData({
                    type : 'text/plain',
                    data : clipboardService.getTextContent(element)
                });
            });

        };

        // Update the internally-stored clipboard data when events are fired
        // that indicate the clipboard field may have been changed
        element.addEventListener('input',                    updateClipboardData);
        element.addEventListener('DOMCharacterDataModified', updateClipboardData);
        element.addEventListener('DOMNodeInserted',          updateClipboardData);
        element.addEventListener('DOMNodeRemoved',           updateClipboardData);

        // Watch clipboard for new data, associating it with any pressed keys
        $scope.$watch('data', function clipboardDataChanged(data) {

            // Stop any current read process
            if (reader.readyState === 1)
                reader.abort();

            // If the clipboard data is a string, render it as text
            if (typeof data.data === 'string')
                clipboardService.setTextContent(element, data.data);

            // Render Blob/File contents based on mimetype
            else if (data.data instanceof Blob) {

                // If the copied data was an image, display it as such
                if (/^image\//.exec(data.type)) {
                    reader.onload = function updateImageURL() {
                        clipboardService.setImageContent(element, reader.result);
                    };
                    reader.readAsDataURL(data.data);
                }

                // Ignore other data types

            }

        }); // end $scope.data watch

    }];

    return config;

}]);
