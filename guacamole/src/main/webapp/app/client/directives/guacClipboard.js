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
angular.module('client').directive('guacClipboard', [function guacClipboard() {

    /**
     * Configuration object for the guacClipboard directive.
     *
     * @type Object.<String, Object>
     */
    var config = {
        restrict    : 'E',
        replace     : true,
        templateUrl : 'app/client/templates/guacClipboard.html'
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
         * @type String
         */
        data : '='

    };

    // guacClipboard directive controller
    config.controller = ['$scope', '$injector', '$element',
            function guacClipboardController($scope, $injector, $element) {

        // Required services
        var $rootScope       = $injector.get('$rootScope');
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
         * @type Object.<Number, String>
         */
        var clipboardDataFromKey = {};

        // Watch clipboard for new data, associating it with any pressed keys
        $scope.$watch('data', function clipboardChanged(data) {

            // Associate new clipboard data with any currently-pressed key
            for (var keysym in keysCurrentlyPressed)
                clipboardDataFromKey[keysym] = data;

            // Notify of updated clipboard data
            $rootScope.$broadcast('guacClipboard', 'text/plain', data);

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
         * Checks whether the clipboard data has changed, firing a new
         * "guacClipboard" event if it has.
         */
        var checkClipboard = function checkClipboard() {
            clipboardService.getLocalClipboard().then(function clipboardRead(data) {
                $scope.data = data;
            });
        };

        // Attempt to read the clipboard if it may have changed
        window.addEventListener('load',  checkClipboard, true);
        window.addEventListener('copy',  checkClipboard, true);
        window.addEventListener('cut',   checkClipboard, true);
        window.addEventListener('focus', function focusGained(e) {

            // Only recheck clipboard if it's the window itself that gained focus
            if (e.target === window)
                checkClipboard();

        }, true);

        // Perform initial clipboard check
        checkClipboard();

    }];

    return config;

}]);
