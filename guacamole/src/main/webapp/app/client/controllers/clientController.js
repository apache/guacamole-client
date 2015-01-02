/*
 * Copyright (C) 2014 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/**
 * The controller for the page used to connect to a connection or balancing group.
 */
angular.module('client').controller('clientController', ['$scope', '$routeParams', '$injector',
        function clientController($scope, $routeParams, $injector) {

    // Required types
    var ManagedClientState = $injector.get('ManagedClientState');
    var ScrollState        = $injector.get('ScrollState');

    // Required services
    var $location          = $injector.get('$location');
    var guacClientManager  = $injector.get('guacClientManager');

    /**
     * The minimum number of pixels a drag gesture must move to result in the
     * menu being shown or hidden.
     *
     * @type Number
     */
    var MENU_DRAG_DELTA = 64;

    /**
     * The maximum X location of the start of a drag gesture for that gesture
     * to potentially show the menu.
     *
     * @type Number
     */
    var MENU_DRAG_MARGIN = 64;

    /**
     * When showing or hiding the menu via a drag gesture, the maximum number
     * of pixels the touch can move vertically and still affect the menu.
     * 
     * @type Number
     */
    var MENU_DRAG_VERTICAL_TOLERANCE = 10;

    /*
     * In order to open the guacamole menu, we need to hit ctrl-alt-shift. There are
     * several possible keysysms for each key.
     */
    var SHIFT_KEYS  = {0xFFE1 : true, 0xFFE2: true},
        ALT_KEYS    = {0xFFE9 : true, 0xFFEA : true, 0xFE03: true},
        CTRL_KEYS   = {0xFFE3 : true, 0xFFE4: true},
        MENU_KEYS   = angular.extend({}, SHIFT_KEYS, ALT_KEYS, CTRL_KEYS);

    /**
     * All client error codes handled and passed off for translation. Any error
     * code not present in this list will be represented by the "DEFAULT"
     * translation.
     */
    var CLIENT_ERRORS = {
        0x0201: true,
        0x0202: true,
        0x0203: true,
        0x0205: true,
        0x0301: true,
        0x0303: true,
        0x0308: true,
        0x031D: true
    };

    /**
     * All error codes for which automatic reconnection is appropriate when a
     * client error occurs.
     */
    var CLIENT_AUTO_RECONNECT = {
        0x0200: true,
        0x0202: true,
        0x0203: true,
        0x0301: true,
        0x0308: true
    };
 
    /**
     * All tunnel error codes handled and passed off for translation. Any error
     * code not present in this list will be represented by the "DEFAULT"
     * translation.
     */
    var TUNNEL_ERRORS = {
        0x0201: true,
        0x0202: true,
        0x0203: true,
        0x0204: true,
        0x0205: true,
        0x0301: true,
        0x0303: true,
        0x0308: true,
        0x031D: true
    };
 
    /**
     * All upload error codes handled and passed off for translation. Any error
     * code not present in this list will be represented by the "DEFAULT"
     * translation.
     */
    var UPLOAD_ERRORS = {
        0x0100: true,
        0x0201: true,
        0x0202: true,
        0x0203: true,
        0x0204: true,
        0x0205: true,
        0x0301: true,
        0x0303: true,
        0x0308: true,
        0x031D: true
    };

    /**
     * All error codes for which automatic reconnection is appropriate when a
     * tunnel error occurs.
     */
    var TUNNEL_AUTO_RECONNECT = {
        0x0200: true,
        0x0202: true,
        0x0203: true,
        0x0308: true
    };

    /**
     * Action which returns the user to the home screen.
     */
    var NAVIGATE_BACK_ACTION = {
        name      : "CLIENT.ACTION_NAVIGATE_BACK",
        className : "back button",
        callback  : function navigateBackCallback() {
            $location.path('/');
        }
    };

    /**
     * Action which replaces the current client with a newly-connected client.
     */
    var RECONNECT_ACTION = {
        name     : "CLIENT.ACTION_RECONNECT",
        callback : function reconnectCallback() {
            $scope.client = guacClientManager.replaceManagedClient(uniqueId, $routeParams.params);
            $scope.showStatus(false);
        }
    };

    /**
     * The reconnect countdown to display if an error or status warrants an
     * automatic, timed reconnect.
     */
    var RECONNECT_COUNTDOWN = {
        text: "CLIENT.TEXT_RECONNECT_COUNTDOWN",
        callback: RECONNECT_ACTION.callback,
        remaining: 15
    };
    
    // Hide menu by default
    $scope.menuShown = false;

    // Use physical keyboard by default
    $scope.inputMethod = 'none';

    // Convenience method for closing the menu
    $scope.closeMenu = function closeMenu() {
        $scope.menuShown = false;
    };

    /**
     * The current scroll state of the menu.
     *
     * @type ScrollState
     */
    $scope.menuScrollState = new ScrollState();

    // Update the model when clipboard data received from client
    $scope.$on('guacClientClipboard', function clientClipboardListener(event, client, mimetype, clipboardData) {
       $scope.clipboardData = clipboardData; 
    });
            
    /*
     * Parse the type, name, and id out of the url paramteres, 
     * as well as any extra parameters if set.
     */
    var uniqueId = $routeParams.type + '/' + $routeParams.id;
    $scope.client = guacClientManager.getManagedClient(uniqueId, $routeParams.params);

    var keysCurrentlyPressed = {};

    /*
     * Check to see if all currently pressed keys are in the set of menu keys.
     */  
    function checkMenuModeActive() {
        for(var keysym in keysCurrentlyPressed) {
            if(!MENU_KEYS[keysym]) {
                return false;
            }
        }
        
        return true;
    }

    // Hide menu when the user swipes from the right
    $scope.menuDrag = function menuDrag(inProgress, startX, startY, currentX, currentY, deltaX, deltaY) {

        // Hide menu if swipe gesture is detected
        if (Math.abs(currentY - startY)  <  MENU_DRAG_VERTICAL_TOLERANCE
                  && startX   - currentX >= MENU_DRAG_DELTA)
            $scope.menuShown = false;

        // Scroll menu by default
        else {
            $scope.menuScrollState.left -= deltaX;
            $scope.menuScrollState.top -= deltaY;
        }

        return false;

    };

    // Update menu or client based on dragging gestures
    $scope.clientDrag = function clientDrag(inProgress, startX, startY, currentX, currentY, deltaX, deltaY) {

        // Show menu if the user swipes from the left
        if (startX <= MENU_DRAG_MARGIN) {

            if (Math.abs(currentY - startY) <  MENU_DRAG_VERTICAL_TOLERANCE
                      && currentX - startX  >= MENU_DRAG_DELTA)
                $scope.menuShown = true;

        }

        // Scroll display if absolute mouse is in use
        else if ($scope.client.clientProperties.emulateAbsoluteMouse) {
            $scope.client.clientProperties.scrollLeft -= deltaX;
            $scope.client.clientProperties.scrollTop -= deltaY;
        }

        return false;

    };

    /**
     * If a pinch gesture is in progress, the scale of the client display when
     * the pinch gesture began.
     *
     * @type Number
     */
    var initialScale = null;

    /**
     * If a pinch gesture is in progress, the X coordinate of the point on the
     * client display that was centered within the pinch at the time the
     * gesture began.
     * 
     * @type Number
     */
    var initialCenterX = 0;

    /**
     * If a pinch gesture is in progress, the Y coordinate of the point on the
     * client display that was centered within the pinch at the time the
     * gesture began.
     * 
     * @type Number
     */
    var initialCenterY = 0;

    // Zoom and pan client via pinch gestures
    $scope.clientPinch = function clientPinch(inProgress, startLength, currentLength, centerX, centerY) {

        // Do not handle pinch gestures while relative mouse is in use
        if (!$scope.client.clientProperties.emulateAbsoluteMouse)
            return false;

        // Stop gesture if not in progress
        if (!inProgress) {
            initialScale = null;
            return false;
        }

        // Set initial scale if gesture has just started
        if (!initialScale) {
            initialScale   = $scope.client.clientProperties.scale;
            initialCenterX = (centerX + $scope.client.clientProperties.scrollLeft) / initialScale;
            initialCenterY = (centerY + $scope.client.clientProperties.scrollTop)  / initialScale;
        }

        // Determine new scale absolutely
        var currentScale = initialScale * currentLength / startLength;

        // Fix scale within limits - scroll will be miscalculated otherwise
        currentScale = Math.max(currentScale, $scope.client.clientProperties.minScale);
        currentScale = Math.min(currentScale, $scope.client.clientProperties.maxScale);

        // Update scale based on pinch distance
        $scope.autoFit = false;
        $scope.client.clientProperties.autoFit = false;
        $scope.client.clientProperties.scale = currentScale;

        // Scroll display to keep original pinch location centered within current pinch
        $scope.client.clientProperties.scrollLeft = initialCenterX * currentScale - centerX;
        $scope.client.clientProperties.scrollTop  = initialCenterY * currentScale - centerY;

        return false;

    };

    // Show/hide UI elements depending on input method
    $scope.$watch('inputMethod', function setInputMethod(inputMethod) {

        // Show input methods only if selected
        $scope.showOSK       = (inputMethod === 'osk');
        $scope.showTextInput = (inputMethod === 'text');

    });

    $scope.$watch('menuShown', function menuVisibilityChanged(menuShown, menuShownPreviousState) {
        
        // Send clipboard data if menu is hidden
        if (!menuShown && menuShownPreviousState && angular.isString($scope.client.clipboardData))
            $scope.$broadcast('guacClipboard', 'text/plain', $scope.client.clipboardData); 
        
        // Disable client keyboard if the menu is shown
        $scope.client.clientProperties.keyboardEnabled = !menuShown;

    });
    
    $scope.$on('guacKeydown', function keydownListener(event, keysym, keyboard) {
        keysCurrentlyPressed[keysym] = true;   
        
        /* 
         * If only menu keys are pressed, and we have one keysym from each group,
         * and one of the keys is being released, show the menu. 
         */
        if(checkMenuModeActive()) {
            var currentKeysPressedKeys = Object.keys(keysCurrentlyPressed);
            
            // Check that there is a key pressed for each of the required key classes
            if(!_.isEmpty(_.pick(SHIFT_KEYS, currentKeysPressedKeys)) &&
               !_.isEmpty(_.pick(ALT_KEYS, currentKeysPressedKeys)) &&
               !_.isEmpty(_.pick(CTRL_KEYS, currentKeysPressedKeys))
            ) {
        
                // Don't send this key event through to the client
                event.preventDefault();
                
                // Reset the keys pressed
                keysCurrentlyPressed = {};
                keyboard.reset();
                
                // Toggle the menu
                $scope.$apply(function() {
                    $scope.menuShown = !$scope.menuShown;
                });
            }
        }
    });

    // Listen for broadcasted keyup events and fire the appropriate listeners
    $scope.$on('guacKeyup', function keyupListener(event, keysym, keyboard) {
        delete keysCurrentlyPressed[keysym];
    });

    // Update page title when client name is received
    $scope.$watch('client.name', function clientNameChanged(name) {
        $scope.page.title = name;
    });

    // Show status dialog when connection status changes
    $scope.$watch('client.clientState.connectionState', function clientStateChanged(connectionState) {

        // Hide status if no known state
        if (!connectionState) {
            $scope.showStatus(false);
            return;
        }

        // Get any associated status code
        var status = $scope.client.clientState.statusCode;

        // Connecting 
        if (connectionState === ManagedClientState.ConnectionState.CONNECTING
         || connectionState === ManagedClientState.ConnectionState.WAITING) {
            $scope.showStatus({
                title: "CLIENT.DIALOG_HEADER_CONNECTING",
                text: "CLIENT.TEXT_CLIENT_STATUS_" + connectionState.toUpperCase()
            });
        }

        // Client error
        else if (connectionState === ManagedClientState.ConnectionState.CLIENT_ERROR) {

            // Determine translation name of error
            var errorName = (status in CLIENT_ERRORS) ? status.toString(16).toUpperCase() : "DEFAULT";

            // Determine whether the reconnect countdown applies
            var countdown = (status in CLIENT_AUTO_RECONNECT) ? RECONNECT_COUNTDOWN : null;

            // Show error status
            $scope.showStatus({
                className: "error",
                title: "CLIENT.DIALOG_HEADER_CONNECTION_ERROR",
                text: "CLIENT.ERROR_CLIENT_" + errorName,
                countdown: countdown,
                actions: [ NAVIGATE_BACK_ACTION, RECONNECT_ACTION ]
            });

        }

        // Tunnel error
        else if (connectionState === ManagedClientState.ConnectionState.TUNNEL_ERROR) {

            // Determine translation name of error
            var errorName = (status in TUNNEL_ERRORS) ? status.toString(16).toUpperCase() : "DEFAULT";

            // Determine whether the reconnect countdown applies
            var countdown = (status in TUNNEL_AUTO_RECONNECT) ? RECONNECT_COUNTDOWN : null;

            // Show error status
            $scope.showStatus({
                className: "error",
                title: "CLIENT.DIALOG_HEADER_CONNECTION_ERROR",
                text: "CLIENT.ERROR_TUNNEL_" + errorName,
                countdown: countdown,
                actions: [ NAVIGATE_BACK_ACTION, RECONNECT_ACTION ]
            });

        }

        // Disconnected
        else if (connectionState === ManagedClientState.ConnectionState.DISCONNECTED) {
            $scope.showStatus({
                title: "CLIENT.DIALOG_HEADER_DISCONNECTED",
                text: "CLIENT.TEXT_CLIENT_STATUS_" + connectionState.toUpperCase(),
                actions: [ NAVIGATE_BACK_ACTION, RECONNECT_ACTION ]
            });
        }

        // Hide status for all other states
        else
            $scope.showStatus(false);

    });

    $scope.formattedScale = function formattedScale() {
        return Math.round($scope.client.clientProperties.scale * 100);
    };
    
    $scope.zoomIn = function zoomIn() {
        $scope.autoFit = false;
        $scope.client.clientProperties.autoFit = false;
        $scope.client.clientProperties.scale += 0.1;
    };
    
    $scope.zoomOut = function zoomOut() {
        $scope.client.clientProperties.autoFit = false;
        $scope.client.clientProperties.scale -= 0.1;
    };
    
    $scope.autoFit = true;
    
    $scope.changeAutoFit = function changeAutoFit() {
        if ($scope.autoFit && $scope.client.clientProperties.minScale) {
            $scope.client.clientProperties.autoFit = true;
        } else {
            $scope.client.clientProperties.autoFit = false;
            $scope.client.clientProperties.scale = 1; 
        }
    };
    
    $scope.autoFitDisabled = function() {
        return $scope.client.clientProperties.minZoom >= 1;
    };

    /**
     * Immediately disconnects the currently-connected client, if any.
     */
    $scope.disconnect = function disconnect() {

        // Disconnect if client is available
        if ($scope.client)
            $scope.client.client.disconnect();

        // Hide menu
        $scope.menuShown = false;

    };

    // Clean up when view destroyed
    $scope.$on('$destroy', function clientViewDestroyed() {

        // Remove client from client manager if no longer connected
        var managedClient = $scope.client;
        if (managedClient) {

            // Get current connection state
            var connectionState = managedClient.clientState.connectionState;

            // If disconnected, remove from management
            if (connectionState === ManagedClientState.ConnectionState.DISCONNECTED
             || connectionState === ManagedClientState.ConnectionState.TUNNEL_ERROR
             || connectionState === ManagedClientState.ConnectionState.CLIENT_ERROR)
                guacClientManager.removeManagedClient(managedClient.id);

        }

        // Hide any status dialog
        $scope.showStatus(false);

    });

}]);
