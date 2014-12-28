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
angular.module('home').controller('clientController', ['$scope', '$routeParams', '$injector',
        function clientController($scope, $routeParams, $injector) {

    // Required types
    var ClientProperties = $injector.get('ClientProperties');
    var ScrollState      = $injector.get('ScrollState');

    // Required services
    var connectionGroupService = $injector.get('connectionGroupService');
    var connectionService      = $injector.get('connectionService');

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
     * The reconnect action to be provided along with the object sent to
     * showStatus.
     */
    var RECONNECT_ACTION = {
        name        : "CLIENT.ACTION_RECONNECT",
        // Handle reconnect action
        callback    : function reconnectCallback() {
            $scope.id = uniqueId;
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
    
    // Client settings and state
    $scope.clientProperties = new ClientProperties();
    
    // Initialize clipboard data to an empty string
    $scope.clipboardData = ""; 
    
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
    $scope.id = uniqueId;
    $scope.connectionParameters = $routeParams.params || '';

    // Pull connection name from server
    switch ($routeParams.type) {

        // Connection
        case 'c':
            connectionService.getConnection($routeParams.id).success(function (connection) {
                $scope.connectionName = $scope.page.title = connection.name;
            });
            break;

        // Connection group
        case 'g':
            connectionGroupService.getConnectionGroup($routeParams.id).success(function (group) {
                $scope.connectionName = $scope.page.title = group.name;
            });
            break;

    }

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
        else if ($scope.clientProperties.emulateAbsoluteMouse) {
            $scope.clientProperties.scrollLeft -= deltaX;
            $scope.clientProperties.scrollTop -= deltaY;
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

        // Stop gesture if not in progress
        if (!inProgress) {
            initialScale = null;
            return false;
        }

        // Set initial scale if gesture has just started
        if (!initialScale) {
            initialScale   = $scope.clientProperties.scale;
            initialCenterX = (centerX + $scope.clientProperties.scrollLeft) / initialScale;
            initialCenterY = (centerY + $scope.clientProperties.scrollTop)  / initialScale;
        }

        // Determine new scale absolutely
        currentScale = initialScale * currentLength / startLength;

        // Fix scale within limits - scroll will be miscalculated otherwise
        currentScale = Math.max(currentScale, $scope.clientProperties.minScale);
        currentScale = Math.min(currentScale, $scope.clientProperties.maxScale);

        // Update scale based on pinch distance
        $scope.autoFit = false;
        $scope.clientProperties.autoFit = false;
        $scope.clientProperties.scale = currentScale;

        // Scroll display to keep original pinch location centered within current pinch
        $scope.clientProperties.scrollLeft = initialCenterX * currentScale - centerX;
        $scope.clientProperties.scrollTop  = initialCenterY * currentScale - centerY;

        return false;

    };

    // Show/hide UI elements depending on input method
    $scope.$watch('inputMethod', function setInputMethod(inputMethod) {

        // Show input methods only if selected
        $scope.showOSK       = (inputMethod === 'osk');
        $scope.showTextInput = (inputMethod === 'text');

    });

    $scope.$watch('menuShown', function setKeyboardEnabled(menuShown, menuShownPreviousState) {
        
        // Send clipboard data if menu is hidden
        if (!menuShown && menuShownPreviousState)
            $scope.$broadcast('guacClipboard', 'text/plain', $scope.clipboardData); 
        
        // Disable client keyboard if the menu is shown
        $scope.clientProperties.keyboardEnabled = !menuShown;

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
                $scope.safeApply(function() {
                    $scope.menuShown = !$scope.menuShown;
                });
            }
        }
    });

    // Listen for broadcasted keyup events and fire the appropriate listeners
    $scope.$on('guacKeyup', function keyupListener(event, keysym, keyboard) {
        delete keysCurrentlyPressed[keysym];
    });

    // Show status dialog when client status changes
    $scope.$on('guacClientStateChange', function clientStateChangeListener(event, client, status) {

        // Show new status if not yet connected
        if (status !== "connected") {
            $scope.showStatus({
                title: "CLIENT.DIALOG_HEADER_CONNECTING",
                text: "CLIENT.TEXT_CLIENT_STATUS_" + status.toUpperCase()
            });
        }

        // Hide status upon connecting
        else
            $scope.showStatus(false);

    });

    // Show status dialog when client errors occur
    $scope.$on('guacClientError', function clientErrorListener(event, client, status) {

        // Determine translation name of error
        var errorName = (status in CLIENT_ERRORS) ? status.toString(16).toUpperCase() : "DEFAULT";

        // Determine whether the reconnect countdown applies
        var countdown = (status in CLIENT_AUTO_RECONNECT) ? RECONNECT_COUNTDOWN : null;

        // Override any existing status
        $scope.showStatus(false);

        // Show error status
        $scope.showStatus({
            className: "error",
            title: "CLIENT.DIALOG_HEADER_CONNECTION_ERROR",
            text: "CLIENT.ERROR_CLIENT_" + errorName,
            countdown: countdown,
            actions: [ RECONNECT_ACTION ]
        });

    });

    // Show status dialog when tunnel status changes
    $scope.$on('guacTunnelStateChange', function tunnelStateChangeListener(event, tunnel, status) {

        // Show new status only if disconnected
        if (status === "closed") {

            // Disconnect
            $scope.id = null;

            $scope.showStatus({
                title: "CLIENT.DIALOG_HEADER_DISCONNECTED",
                text: "CLIENT.TEXT_TUNNEL_STATUS_" + status.toUpperCase()
            });
        }

    });

    // Show status dialog when tunnel errors occur
    $scope.$on('guacTunnelError', function tunnelErrorListener(event, tunnel, status) {

        // Determine translation name of error
        var errorName = (status in TUNNEL_ERRORS) ? status.toString(16).toUpperCase() : "DEFAULT";

        // Determine whether the reconnect countdown applies
        var countdown = (status in TUNNEL_AUTO_RECONNECT) ? RECONNECT_COUNTDOWN : null;

        // Override any existing status
        $scope.showStatus(false);

        // Show error status
        $scope.showStatus({
            className: "error",
            title: "CLIENT.DIALOG_HEADER_CONNECTION_ERROR",
            text: "CLIENT.ERROR_TUNNEL_" + errorName,
            countdown: countdown,
            actions: [ RECONNECT_ACTION ]
        });

    });

    $scope.formattedScale = function formattedScale() {
        return Math.round($scope.clientProperties.scale * 100);
    };
    
    $scope.zoomIn = function zoomIn() {
        $scope.autoFit = false;
        $scope.clientProperties.autoFit = false;
        $scope.clientProperties.scale += 0.1;
    };
    
    $scope.zoomOut = function zoomOut() {
        $scope.clientProperties.autoFit = false;
        $scope.clientProperties.scale -= 0.1;
    };
    
    $scope.autoFit = true;
    
    $scope.changeAutoFit = function changeAutoFit() {
        if ($scope.autoFit && $scope.clientProperties.minScale) {
            $scope.clientProperties.autoFit = true;
        } else {
            $scope.clientProperties.autoFit = false;
            $scope.clientProperties.scale = 1; 
        }
    };
    
    $scope.autoFitDisabled = function() {
        return $scope.clientProperties.minZoom >= 1;
    };

    /**
     * Returns a progress object, as required by $scope.addNotification(), which
     * contains the given number of bytes as an appropriate combination of
     * progress value and associated unit.
     *
     * @param {String} text
     *     The translation string to associate with the progress object
     *     returned.
     *
     * @param {Number} bytes The number of bytes.
     * @param {Number} [length] The file length, in bytes, if known.
     *
     * @returns {Object}
     *     A progress object, as required by $scope.addNotification().
     */
    var getFileProgress = function getFileProgress(text, bytes, length) {

        // Gigabytes
        if (bytes > 1000000000)
            return {
                text  : text,
                value : (bytes / 1000000000).toFixed(1),
                ratio : bytes / length,
                unit  : "gb"
            };

        // Megabytes
        if (bytes > 1000000)
            return {
                text  : text,
                value : (bytes / 1000000).toFixed(1),
                ratio : bytes / length,
                unit  : "mb"
            };

        // Kilobytes
        if (bytes > 1000)
            return {
                text  : text,
                value : (bytes / 1000).toFixed(1),
                ratio : bytes / length,
                unit  : "kb"
            };

        // Bytes
        return {
            text  : text,
            value : bytes,
            ratio : bytes / length,
            unit  : "b"
        };

    };
            
    // Mapping of download stream index to notification object
    var downloadNotifications = {};
    
    // Mapping of download stream index to notification ID
    var downloadNotificationIDs = {};
    
    $scope.$on('guacClientFileDownloadStart', function handleClientFileDownloadStart(event, guacClient, streamIndex, mimetype, filename) {
        $scope.safeApply(function() {
            
            var notification = {
                className  : 'download',
                title      : 'CLIENT.DIALOG_TITLE_FILE_TRANSFER',
                text       : filename
            };
            
            downloadNotifications[streamIndex]   = notification;
            downloadNotificationIDs[streamIndex] = $scope.addNotification(notification);
            
        });
    });

    $scope.$on('guacClientFileDownloadProgress', function handleClientFileDownloadProgress(event, guacClient, streamIndex, mimetype, filename, length) {
        $scope.safeApply(function() {
            
            var notification = downloadNotifications[streamIndex];
            if (notification)
                notification.progress = getFileProgress('CLIENT.TEXT_FILE_TRANSFER_PROGRESS', length);
            
        });
    });
    
    $scope.$on('guacClientFileDownloadEnd', function handleClientFileDownloadEnd(event, guacClient, streamIndex, mimetype, filename, blob) {
        $scope.safeApply(function() {

            var notification = downloadNotifications[streamIndex];
            var notificationID = downloadNotificationIDs[streamIndex];
            
            /**
             * Saves the current file.
             */
            var saveFile = function saveFile() {
                saveAs(blob, filename);
                $scope.removeNotification(notificationID);
                delete downloadNotifications[streamIndex];
                delete downloadNotificationIDs[streamIndex];
            };
            
            // Add download action and remove progress indicator
            if (notificationID && notification) {
                delete notification.progress;
                notification.actions = [
                    {
                        name       : 'CLIENT.ACTION_SAVE_FILE',
                        callback   : saveFile
                    }
                ];
            }

        });
    });

    // Mapping of upload stream index to notification object
    var uploadNotifications = {};
    
    // Mapping of upload stream index to notification ID
    var uploadNotificationIDs = {};
    
    $scope.$on('guacClientFileUploadStart', function handleClientFileUploadStart(event, guacClient, streamIndex, mimetype, filename, length) {
        $scope.safeApply(function() {
            
            var notification = {
                className  : 'upload',
                title      : 'CLIENT.DIALOG_TITLE_FILE_TRANSFER',
                text       : filename
            };
            
            uploadNotifications[streamIndex]   = notification;
            uploadNotificationIDs[streamIndex] = $scope.addNotification(notification);
            
        });
    });

    $scope.$on('guacClientFileUploadProgress', function handleClientFileUploadProgress(event, guacClient, streamIndex, mimetype, filename, length, offset) {
        $scope.safeApply(function() {
            
            var notification = uploadNotifications[streamIndex];
            if (notification)
                notification.progress = getFileProgress('CLIENT.TEXT_FILE_TRANSFER_PROGRESS', offset, length);
            
        });
    });
    
    $scope.$on('guacClientFileUploadEnd', function handleClientFileUploadEnd(event, guacClient, streamIndex, mimetype, filename, length) {
        $scope.safeApply(function() {

            var notification = uploadNotifications[streamIndex];
            var notificationID = uploadNotificationIDs[streamIndex];
            
            /**
             * Close the notification.
             */
            var closeNotification = function closeNotification() {
                $scope.removeNotification(notificationID);
                delete uploadNotifications[streamIndex];
                delete uploadNotificationIDs[streamIndex];
            };
            
            // Show that the file has uploaded successfully
            if (notificationID && notification) {
                delete notification.progress;
                notification.actions = [
                    {
                        name       : 'CLIENT.ACTION_ACKNOWLEDGE',
                        callback   : closeNotification
                    }
                ];
            }

        });
    });
    
    $scope.$on('guacClientFileUploadError', function handleClientFileUploadError(event, guacClient, streamIndex, mimetype, fileName, length, status) {
        $scope.safeApply(function() {

            var notification = uploadNotifications[streamIndex];
            var notificationID = uploadNotificationIDs[streamIndex];

            // Determine translation name of error
            var errorName = (status in UPLOAD_ERRORS) ? status.toString(16).toUpperCase() : "DEFAULT";

            /**
             * Close the notification.
             */
            var closeNotification = function closeNotification() {
                $scope.removeNotification(notificationID);
                delete uploadNotifications[streamIndex];
                delete uploadNotificationIDs[streamIndex];
            };

            // Show that the file upload has failed
            if (notificationID && notification) {
                delete notification.progress;
                notification.actions = [
                    {
                        name       : 'CLIENT.ACTION_ACKNOWLEDGE',
                        callback   : closeNotification
                    }
                ];
                notification.text = "CLIENT.ERROR_UPLOAD_" + errorName;
                notification.className = "upload error";
            }
            
        });
    });

}]);
