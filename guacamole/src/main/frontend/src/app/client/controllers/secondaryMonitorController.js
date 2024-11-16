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
 * The controller for the page used to display secondary monitors.
 */
angular.module('client').controller('secondaryMonitorController', ['$scope', '$injector', '$routeParams',
    function clientController($scope, $injector, $routeParams) {

    // Required types
    const ClipboardData          = $injector.get('ClipboardData');

    // Required services
    const $window                = $injector.get('$window');
    const clipboardService       = $injector.get('clipboardService');
    const guacFullscreen         = $injector.get('guacFullscreen');

    // ID of this monitor
    const monitorId = $routeParams.id;

    // Broadcast channel
    const broadcast = new BroadcastChannel('guac_monitors');

    // Latest mouse state
    const mouseState = {};

    // Display size in pixels and position of the monitor
    let displayWidth = 0;
    let displayHeight = 0;
    let monitorPosition = 0;
    let monitorsCount = 0;
    let currentScaling = 1;

    /**
     * In order to open the guacamole menu, we need to hit ctrl-alt-shift. There are
     * several possible keysysms for each key.
     */
    const SHIFT_KEYS  = {0xFFE1 : true, 0xFFE2 : true},
          ALT_KEYS    = {0xFFE9 : true, 0xFFEA : true, 0xFE03 : true,
                         0xFFE7 : true, 0xFFE8 : true},
          CTRL_KEYS   = {0xFFE3 : true, 0xFFE4 : true},
          MENU_KEYS   = angular.extend({}, SHIFT_KEYS, ALT_KEYS, CTRL_KEYS);

    // Instantiate client, using an HTTP tunnel for communications.
    const client = new Guacamole.Client(
        new Guacamole.HTTPTunnel("tunnel")
    );

    const display = client.getDisplay();
    let displayContainer;

    setTimeout(function() {
        displayContainer = document.querySelector('.display')

        // Remove any existing display
        displayContainer.innerHTML = "";
    
        // Add display element
        displayContainer.appendChild(display.getElement());

        // Ready for resize
        pushBroadcastMessage('resize', true);
    }, 1000);

    /**
     * Adjust the display scaling according to the window size.
     */
    $scope.scaleDisplay = function scaleDisplay() {

        // Calculate required scaling factor
        const scaleX = $window.innerWidth / displayWidth;
        const scaleY = $window.innerHeight / displayHeight;

        // Use the lowest scaling to avoid acreen overflow
        if (scaleX <= scaleY)
            currentScaling = scaleX;
        else
            currentScaling = scaleY;
        
        display.scale(currentScaling);
    };

    // Send monitor-close event to broadcast channel on window unload
    $window.addEventListener('unload', function unloadWindow() {
        pushBroadcastMessage('monitorClose', monitorId);
    });

    // Mouse and keyboard
    const mouse = new Guacamole.Mouse(client.getDisplay().getElement());
    const keyboard = new Guacamole.Keyboard(document);

     // Move mouse on screen and send mouse events to main window
     mouse.onEach(['mousedown', 'mouseup', 'mousemove'], function sendMouseEvent(e) {

        // Ensure software cursor is shown
        display.showCursor(true);

        // Update client-side cursor
        display.moveCursor(
            Math.floor(e.state.x / currentScaling),
            Math.floor(e.state.y / currentScaling)
        );

        // Limit mouse move events to reduce latency
        if (mouseState.lastPush && Date.now() - mouseState.lastPush < 100
                && mouseState.down === e.state.down
                && mouseState.up === e.state.up
                && mouseState.left === e.state.left
                && mouseState.middle === e.state.middle
                && mouseState.right === e.state.right)
            return;

        // Click on actual display instead of the first
        const displayOffset = displayWidth * monitorPosition;

        // Convert mouse state to serializable object
        mouseState.down = e.state.down;
        mouseState.up = e.state.up;
        mouseState.left = e.state.left;
        mouseState.middle = e.state.middle;
        mouseState.right = e.state.right;
        mouseState.x = e.state.x / currentScaling + displayOffset;
        mouseState.y = e.state.y / currentScaling;
        mouseState.lastPush = Date.now();

        // Send mouse state to main window
        pushBroadcastMessage('mouseState', mouseState);
    });

    // Hide software cursor when mouse leaves display
    mouse.on('mouseout', function() {
        if (!display) return;
        display.showCursor(false);
    });

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

    // Send keydown events to main window
    keyboard.onkeydown = function (keysym) {
        pushBroadcastMessage('keydown', keysym);
    };

    // Send keyup events to main window
    keyboard.onkeyup = function (keysym) {
        pushBroadcastMessage('keyup', keysym);
    };

    /**
     * Push broadcast message containing instructions that allows additional
     * monitor windows to draw display, resize window and more.
     * 
     * @param {!string} type
     *     The type of message (ex: handler, fullscreen, resize)
     *
     * @param {*} content
     *     The content of the message, can contain any type of serializable
     *      content.
     */
    function pushBroadcastMessage(type, content) {
        const message = {
            [type]: content
        };

        broadcast.postMessage(message);
    };

    /**
     * Handle messages sent by main window in guac_monitors channel. These
     * messages contain instructions to draw the screen, resize window, or
     * request full screen mode.
     * 
     * @param {Event} e
     *     Received message event from guac_monitors channel.
     */
    broadcast.onmessage = function broadcastMessage(message) {

        // Run the client handler to draw display
        if (message.data.handler)
            client.runHandler(message.data.handler.opcode,
                              message.data.handler.parameters);

        if (message.data.monitorsInfos) {

            const monitorsInfos = message.data.monitorsInfos;

            // Store new monitor count and position
            monitorsCount = monitorsInfos.count;
            monitorPosition = monitorsInfos.map[monitorId];

            // Set the monitor count in display
            display.updateMonitors(monitorsCount);

        }

        // Resize display and window with parameters sent by guacd in the size handler
        if (message.data.handler && message.data.handler.opcode === 'size') {

            const parameters = message.data.handler.parameters;
            const default_layer = 0;
            const layer = parseInt(parameters[0]);

            // Ignore other layers (ex: mouse) that can have other size
            if (layer !== default_layer)
                return;

            // Set the new display size
            displayWidth  = parseInt(parameters[1]) / monitorsCount;
            displayHeight = parseInt(parameters[2]);

            // Translate all draw actions on X to draw the current display
            // instead of the first
            client.offsetX = displayWidth * monitorPosition;

            // Get unusable window height and width (ex: titlebar)
            const windowUnusableHeight = $window.outerHeight - $window.innerHeight;
            const windowUnusableWidth = $window.outerWidth - $window.innerWidth;

            // Remove scrollbars
            document.querySelector('.client-main').style.overflow = 'hidden';

            // Resize window to the display size
            $window.resizeTo(
                displayWidth + windowUnusableWidth,
                displayHeight + windowUnusableHeight
            );

            // Adjust scaling to new size
            $scope.scaleDisplay();

        }

        // Full screen mode instructions
        if (message.data.fullscreen) {

            // setFullscreenMode require explicit user action
            if (message.data.fullscreen !== false)
                openConsentButton();

            // Close fullscreen mode instantly
            else
                guacFullscreen.setFullscreenMode(message.data.fullscreen);

        }

    };

    /**
     * Add button to request user consent before enabling fullscreen mode to
     * comply with the setFullscreenMode requirements that require explicit
     * user action. The button is removed after a few seconds if the user does
     * not click on it.
     */
    function openConsentButton() {

        // Show button
        $scope.showFullscreenConsent = true;

        // Auto hide button after delay
        setTimeout(function() {
            $scope.showFullscreenConsent = false;
        }, 10000);

    };

    /**
     * User clicked on the consent button : switch to fullscreen mode and hide
     * the button.
     */
    $scope.enableFullscreenMode = function enableFullscreenMode() {
        guacFullscreen.setFullscreenMode(true);
        $scope.showFullscreenConsent = false;
    };

    /**
     * Returns whether the shortcut for showing/hiding the Guacamole menu
     * (Ctrl+Alt+Shift) has been pressed.
     *
     * @param {Guacamole.Keyboard} keyboard
     *     The Guacamole.Keyboard object tracking the local keyboard state.
     *
     * @returns {boolean}
     *     true if Ctrl+Alt+Shift has been pressed, false otherwise.
     */  
    const isMenuShortcutPressed = function isMenuShortcutPressed(keyboard) {

        // Ctrl+Alt+Shift has NOT been pressed if any key is currently held
        // down that isn't Ctrl, Alt, or Shift
        if (_.findKey(keyboard.pressed, (val, keysym) => !MENU_KEYS[keysym]))
            return false;

        // Verify that one of each required key is held, regardless of
        // left/right location on the keyboard
        return !!(
                _.findKey(SHIFT_KEYS, (val, keysym) => keyboard.pressed[keysym])
                && _.findKey(ALT_KEYS,   (val, keysym) => keyboard.pressed[keysym])
                && _.findKey(CTRL_KEYS,  (val, keysym) => keyboard.pressed[keysym])
        );

    };

    // Opening the Guacamole menu after Ctrl+Alt+Shift, preventing those
    // keypresses from reaching any Guacamole client
    $scope.$on('guacBeforeKeydown', function incomingKeydown(event, keysym, keyboard) {

        // Toggle menu if menu shortcut (Ctrl+Alt+Shift) is pressed
        if (isMenuShortcutPressed(keyboard)) {
        
            // Don't send this key event through to the client, and release
            // all other keys involved in performing this shortcut
            event.preventDefault();
            keyboard.reset();
            
            // Toggle the menu
            $scope.$apply(function() {
                pushBroadcastMessage('guacMenu', true);
            });

        }

    });

}]);
