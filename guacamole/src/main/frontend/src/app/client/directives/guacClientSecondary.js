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
 * A directive for the guacamole client on secondary monitors.
 */
angular.module('client').directive('guacClientSecondary', [function guacClient() {

    const directive = {
        restrict: 'E',
        replace: true,
        templateUrl: 'app/client/templates/guacClient.html'
    };

    directive.scope = {

        /**
         * The client to display within this guacClient directive.
         * 
         * @type ManagedClient
         */
        client : '=',

    };

    directive.controller = ['$scope', '$injector', '$element',
        function guacClientController($scope, $injector, $element) {

        // Required types
        const ClipboardData     = $injector.get('ClipboardData');

        // Required services
        const $document         = $injector.get('$document');
        const $window           = $injector.get('$window');
        const clipboardService  = $injector.get('clipboardService');
        const guacManageMonitor = $injector.get('guacManageMonitor');

        /**
         * The current Guacamole client instance.
         * 
         * @type Guacamole.Client 
         */
        const client = new Guacamole.Client(new Guacamole.Tunnel());

        /**
         * The display of the current Guacamole client instance.
         * 
         * @type Guacamole.Display
         */
        const display = client.getDisplay();

        /**
         * The element associated with the display of the current
         * Guacamole client instance.
         *
         * @type Element
         */
        const displayElement = display.getElement();

        /**
         * The element which must contain the Guacamole display element.
         *
         * @type Element
         */
        const displayContainer = $element.find('.display')[0];

        /**
         * The main containing element for the entire directive.
         * 
         * @type Element
         */
        const main = $element[0];

        /**
         * The tracked mouse.
         *
         * @type Guacamole.Mouse
         */
        const mouse = new Guacamole.Mouse(displayContainer);

        /**
         * The latest known mouse state.
         * 
         * @type Object
         */
        const mouseState = {};

        /**
         * The keyboard.
         * 
         * @type Guacamole.Keyboard
         */
        const keyboard = new Guacamole.Keyboard($document[0]);

        // Set client instance on guacManageMonitor service
        guacManageMonitor.setClient(client);

        // Remove any existing display
        displayContainer.innerHTML = "";

        // Add display element
        displayContainer.appendChild(displayElement);

        // Do nothing when the display element is clicked on
        displayElement.onclick = function(e) {
            e.preventDefault();
            return false;
        };

        // Adjust the display scaling according to the window size.
        $scope.mainElementResized = function mainElementResized() {

            const pixelDensity = $window.devicePixelRatio ?? 1;
            const width  = main.offsetWidth  * pixelDensity;
            const height = main.offsetHeight * pixelDensity;
            const top    = 0 // TODO: $window.screenY ?? 0;

            const size = {
                width: width,
                height: height,
                top: top > 0 ? top : 0,
                monitorId: guacManageMonitor.monitorId,
            };

            // Send resize event to main window
            guacManageMonitor.pushBroadcastMessage('size', size);
        }

        // Ready for resize
        $scope.mainElementResized();

        // Handle any received clipboard data
        client.onclipboard = function clientClipboardReceived(stream, mimetype) {

            let reader;

            // If the received data is text, read it as a simple string
            if (/^text\//.exec(mimetype)) {

                reader = new Guacamole.StringReader(stream);

                // Assemble received data into a single string
                let data = '';
                reader.ontext = function textReceived(text) {
                    data += text;
                };

                // Set clipboard contents once stream is finished
                reader.onend = function textComplete() {
                    clipboardService.setClipboard(new ClipboardData({
                        source : 'secondaryMonitor',
                        type : mimetype,
                        data : data
                    }))['catch'](angular.noop);
                };

            }

            // Otherwise read the clipboard data as a Blob
            else {
                reader = new Guacamole.BlobReader(stream, mimetype);
                reader.onend = function blobComplete() {
                    clipboardService.setClipboard(new ClipboardData({
                        source : 'secondaryMonitor',
                        type : mimetype,
                        data : reader.getBlob()
                    }))['catch'](angular.noop);
                };
            }

        };        

        // Move mouse on screen and send mouse events to main window
        mouse.onEach(['mousedown', 'mouseup', 'mousemove'], function sendMouseEvent(e) {

            // Ensure software cursor is shown
            display.showCursor(true);

            // Update client-side cursor
            display.moveCursor(e.state.x, e.state.y);

            // Click on actual display instead of the first
            const displayOffsetX = guacManageMonitor.getOffsetX();
            const displayOffsetY = guacManageMonitor.getOffsetY();

            // Convert mouse state to serializable object
            mouseState.down = e.state.down;
            mouseState.up = e.state.up;
            mouseState.left = e.state.left;
            mouseState.middle = e.state.middle;
            mouseState.right = e.state.right;
            mouseState.x = e.state.x + displayOffsetX;
            mouseState.y = e.state.y + displayOffsetY;

            // Send mouse state to main window
            guacManageMonitor.pushBroadcastMessage('mouseState', mouseState);
        });

        // Hide software cursor when mouse leaves display
        mouse.on('mouseout', function() {
            if (!display) return;
            display.showCursor(false);
        });

        // Send keydown events to main window
        keyboard.onkeydown = function (keysym) {
            guacManageMonitor.pushBroadcastMessage('keydown', keysym);
        };

        // Send keyup events to main window
        keyboard.onkeyup = function (keysym) {
            guacManageMonitor.pushBroadcastMessage('keyup', keysym);
        };

    }];

    return directive;

}]);
