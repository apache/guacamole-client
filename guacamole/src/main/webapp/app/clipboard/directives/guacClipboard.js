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
 * A directive which exposes the current clipboard contents, if possible,
 * allowing the user to edit those contents. If the current clipboard contents
 * cannot be directly accessed, the user can at least directly copy/paste data
 * within the field provided by this directive. The contents of this clipboard
 * directive, whether retrieved from the local or manipulated manually by the
 * user, are exposed via the "data" attribute. In addition to updating the
 * "data" attribute, changes to clipboard data will be broadcast on the scope
 * via "guacClipboard" events.
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
         * The data to display within the field provided by this directive. If
         * the local clipboard can be accessed by JavaScript, this will be set
         * automatically as the local clipboard changes. Failing that, this
         * will be set when the user manually modifies the contents of the
         * field. Changes to this value will be rendered within the field and,
         * if possible, will be pushed to the local clipboard.
         *
         * @type ClipboardData
         */
        data : '='

    };

    // guacClipboard directive controller
    config.controller = ['$scope', '$injector', '$element',
            function guacClipboardController($scope, $injector, $element) {

        // Required services
        var $rootScope       = $injector.get('$rootScope');
        var $window          = $injector.get('$window');
        var clipboardService = $injector.get('clipboardService');

        /**
         * Map of all currently pressed keys by keysym. If a particular key is
         * currently pressed, the value stored under that key's keysym within
         * this map will be true. All keys not currently pressed will not have entries
         * within this map.
         *
         * @type Object.<Number, Boolean>
         */
        var keysCurrentlyPressed = {};

        /**
         * Map of all currently pressed keys (by keysym) to the clipboard
         * contents received while those keys were pressed. All keys not
         * currently pressed will not have entries within this map.
         *
         * @type Object.<Number, Blob>
         */
        var clipboardDataFromKey = {};

        /**
         * The FileReader to use to read File or Blob data received from the
         * clipboard.
         *
         * @type FileReader
         */
        var reader = new FileReader();

        /**
         * Properties which contain the current clipboard contents. Each
         * property is mutually exclusive, and will only contain data if the
         * clipboard contents are of a particular type.
         */
        $scope.content = {

            /**
             * The text contents of the clipboard. If the clipboard contents
             * is not text, this will be null.
             *
             * @type String
             */
            text : null,

            /**
             * The URL of the image is currently stored within the clipboard. If
             * the clipboard currently contains text, this will be null.
             *
             * @type String
             */
            imageURL : null

        };

        // Intercept paste events, handling image data specifically
        $element[0].addEventListener('paste', function dataPasted(e) {

            // For each item within the clipboard
            var items = e.clipboardData.items;
            for (var i = 0; i < items.length; i++) {

                // If the item is an image, attempt to read that image
                if (items[i].kind === 'file' && /^image\//.exec(items[i].type)) {

                    // Retrieven contents as a File
                    var file = items[i].getAsFile();

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
         * Returns whether the clipboard currently contains only an image, the
         * URL of which is exposed via the imageURL property.
         *
         * @returns {Boolean}
         *     true if the current clipboard contains only an image, false
         *     otherwise.
         */
        $scope.isImage = function isImage() {
            return !!$scope.content.imageURL;
        };

        /**
         * Returns whether the clipboard currently contains only text.
         *
         * @returns {Boolean}
         *     true if the clipboard currently contains only text, false
         *     otherwise.
         */
        $scope.isText = function isText() {
            return !$scope.isImage();
        };

        /**
         * Clears the current clipboard contents. If the clipboard currently
         * displays an image, this will also return to a text-based clipboard
         * display.
         */
        $scope.resetClipboard = function resetClipboard() {

            // Reset to blank
            $scope.data = new ClipboardData({
                type : 'text/plain',
                data : ''
            });

        };

        // Keep data in sync with changes to text
        $scope.$watch('content.text', function textChanged(text) {

            if (text) {
                $scope.data = new ClipboardData({
                    type : $scope.data.type,
                    data : text
                });
            }

        });

        // Watch clipboard for new data, associating it with any pressed keys
        $scope.$watch('data', function clipboardChanged(data) {

            // Associate new clipboard data with any currently-pressed key
            for (var keysym in keysCurrentlyPressed)
                clipboardDataFromKey[keysym] = data;

            // Stop any current read process
            reader.abort();

            // If the clipboard data is a string, render it as text
            if (typeof data.data === 'string') {
                $scope.content.text     = data.data;
                $scope.content.imageURL = null;
            }

            // Render Blob/File contents based on mimetype
            else if (data.data instanceof Blob) {

                // If the copied data was an image, display it as such
                if (/^image\//.exec(data.type)) {
                    reader.onload = function updateImageURL() {
                        $scope.$apply(function imageURLLoaded() {
                            $scope.content.text     = null;
                            $scope.content.imageURL = reader.result;
                        });
                    };
                    reader.readAsDataURL(data.data);
                }

                // Ignore other data types

            }

            // Notify of change
            $rootScope.$broadcast('guacClipboard', data);

        });

        // Track pressed keys
        $scope.$on('guacKeydown', function keydownListener(event, keysym, keyboard) {

            // Record key as pressed
            keysCurrentlyPressed[keysym] = true;

        });

        // Update pressed keys as they are released, synchronizing the clipboard
        // with any data that appears to have come from those key presses
        $scope.$on('guacKeyup', function keyupListener(event, keysym, keyboard) {

            // Sync local clipboard with any clipboard data received while this
            // key was pressed (if any)
            var clipboardData = clipboardDataFromKey[keysym];
            if (clipboardData) {
                clipboardService.setLocalClipboard(clipboardData);
                delete clipboardDataFromKey[keysym];
            }

            // Mark key as released
            delete keysCurrentlyPressed[keysym];

        });

        /**
         * Checks whether the clipboard data has changed, updating the stored
         * clipboard data if it has. If this function is being called due to a
         * DOM event, that event should be passed to this function such that the
         * context of the call can be taken into account. Focus events, in
         * particular, need to be considered only in the context of the window.
         *
         * @param {Event} [e]
         *     The event currently being handled, if any.
         */
        var checkClipboard = function checkClipboard(e) {

            // Ignore focus events for anything except the window
            if (e && e.type === 'focus' && e.target !== $window)
                return;

            clipboardService.getLocalClipboard().then(function clipboardRead(data) {
                $scope.data = data;
            });

        };

        // Attempt to read the clipboard if it may have changed
        $window.addEventListener('copy',  checkClipboard, true);
        $window.addEventListener('cut',   checkClipboard, true);
        $window.addEventListener('focus', checkClipboard, true);

        // Clean up on destruction
        $scope.$on('$destroy', function destroyClipboard() {
            $window.removeEventListener('copy',  checkClipboard);
            $window.removeEventListener('cut',   checkClipboard);
            $window.removeEventListener('focus', checkClipboard);
        });

        // Perform initial clipboard check
        checkClipboard();

    }];

    return config;

}]);
