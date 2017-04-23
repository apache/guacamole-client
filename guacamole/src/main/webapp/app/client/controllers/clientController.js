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
 * The controller for the page used to connect to a connection or balancing group.
 */
angular.module('client').controller('clientController', ['$scope', '$routeParams', '$injector',
        function clientController($scope, $routeParams, $injector) {

    // Required types
    var ManagedClient      = $injector.get('ManagedClient');
    var ManagedClientState = $injector.get('ManagedClientState');
    var ManagedFilesystem  = $injector.get('ManagedFilesystem');
    var ScrollState        = $injector.get('ScrollState');

    // Required services
    var $location             = $injector.get('$location');
    var authenticationService = $injector.get('authenticationService');
    var clipboardService      = $injector.get('clipboardService');
    var guacClientManager     = $injector.get('guacClientManager');
    var guacNotification      = $injector.get('guacNotification');
    var iconService           = $injector.get('iconService');
    var preferenceService     = $injector.get('preferenceService');
    var tunnelService         = $injector.get('tunnelService');
    var userPageService       = $injector.get('userPageService');

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
    var SHIFT_KEYS  = {0xFFE1 : true, 0xFFE2 : true},
        ALT_KEYS    = {0xFFE9 : true, 0xFFEA : true, 0xFE03 : true,
                       0xFFE7 : true, 0xFFE8 : true},
        CTRL_KEYS   = {0xFFE3 : true, 0xFFE4 : true},
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
        0x0207: true,
        0x0208: true,
        0x0209: true,
        0x020A: true,
        0x020B: true,
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
        0x0207: true,
        0x0208: true,
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
        0x0207: true,
        0x0208: true,
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
        0x0207: true,
        0x0208: true,
        0x0308: true
    };

    /**
     * Action which logs out from Guacamole entirely.
     */
    var LOGOUT_ACTION = {
        name      : "CLIENT.ACTION_LOGOUT",
        className : "logout button",
        callback  : function logoutCallback() {
            authenticationService.logout()['finally'](function logoutComplete() {
                $location.url('/');
            });
        }
    };

    /**
     * Action which returns the user to the home screen. If the home page has
     * not yet been determined, this will be null.
     */
    var NAVIGATE_HOME_ACTION = null;

    // Assign home page action once user's home page has been determined
    userPageService.getHomePage()
    .then(function homePageRetrieved(homePage) {

        // Define home action only if different from current location
        if ($location.path() !== homePage.url) {
            NAVIGATE_HOME_ACTION = {
                name      : "CLIENT.ACTION_NAVIGATE_HOME",
                className : "home button",
                callback  : function navigateHomeCallback() {
                    $location.url(homePage.url);
                }
            };
        }

    });

    /**
     * Action which replaces the current client with a newly-connected client.
     */
    var RECONNECT_ACTION = {
        name      : "CLIENT.ACTION_RECONNECT",
        className : "reconnect button",
        callback  : function reconnectCallback() {
            $scope.client = guacClientManager.replaceManagedClient($routeParams.id, $routeParams.params);
            guacNotification.showStatus(false);
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

    /**
     * Menu-specific properties.
     */
    $scope.menu = {

        /**
         * Whether the menu is currently shown.
         *
         * @type Boolean
         */
        shown : false,

        /**
         * Whether the Guacamole display should be scaled to fit the browser
         * window.
         *
         * @type Boolean
         */
        autoFit : true,

        /**
         * The currently selected input method. This may be any of the values
         * defined within preferenceService.inputMethods.
         *
         * @type String
         */
        inputMethod : preferenceService.preferences.inputMethod,

        /**
         * The current scroll state of the menu.
         *
         * @type ScrollState
         */
        scrollState : new ScrollState()

    };

    // Convenience method for closing the menu
    $scope.closeMenu = function closeMenu() {
        $scope.menu.shown = false;
    };

    /**
     * The client which should be attached to the client UI.
     *
     * @type ManagedClient
     */
    $scope.client = guacClientManager.getManagedClient($routeParams.id, $routeParams.params);

    /**
     * Map of all available sharing profiles for the current connection by
     * their identifiers. If this information is not yet available, or no such
     * sharing profiles exist, this will be an empty object.
     *
     * @type Object.<String, SharingProfile>
     */
    $scope.sharingProfiles = {};

    /**
     * Map of all currently pressed keys by keysym. If a particular key is
     * currently pressed, the value stored under that key's keysym within this
     * map will be true. All keys not currently pressed will not have entries
     * within this map.
     *
     * @type Object.<Number, Boolean>
     */
    var keysCurrentlyPressed = {};

    /**
     * Map of all currently pressed keys (by keysym) to the clipboard contents
     * received from the remote desktop while those keys were pressed. All keys
     * not currently pressed will not have entries within this map.
     *
     * @type Object.<Number, ClipboardData>
     */
    var clipboardDataFromKey = {};

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
            $scope.menu.shown = false;

        // Scroll menu by default
        else {
            $scope.menu.scrollState.left -= deltaX;
            $scope.menu.scrollState.top -= deltaY;
        }

        return false;

    };

    // Update menu or client based on dragging gestures
    $scope.clientDrag = function clientDrag(inProgress, startX, startY, currentX, currentY, deltaX, deltaY) {

        // Show menu if the user swipes from the left
        if (startX <= MENU_DRAG_MARGIN) {

            if (Math.abs(currentY - startY) <  MENU_DRAG_VERTICAL_TOLERANCE
                      && currentX - startX  >= MENU_DRAG_DELTA)
                $scope.menu.shown = true;

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
        $scope.menu.autoFit = false;
        $scope.client.clientProperties.autoFit = false;
        $scope.client.clientProperties.scale = currentScale;

        // Scroll display to keep original pinch location centered within current pinch
        $scope.client.clientProperties.scrollLeft = initialCenterX * currentScale - centerX;
        $scope.client.clientProperties.scrollTop  = initialCenterY * currentScale - centerY;

        return false;

    };

    // Show/hide UI elements depending on input method
    $scope.$watch('menu.inputMethod', function setInputMethod(inputMethod) {

        // Show input methods only if selected
        $scope.showOSK       = (inputMethod === 'osk');
        $scope.showTextInput = (inputMethod === 'text');

    });

    $scope.$watch('menu.shown', function menuVisibilityChanged(menuShown, menuShownPreviousState) {
        
        // Send clipboard data if menu is hidden
        if (!menuShown && menuShownPreviousState)
            $scope.$broadcast('guacClipboard', $scope.client.clipboardData);
        
        // Disable client keyboard if the menu is shown
        $scope.client.clientProperties.keyboardEnabled = !menuShown;

    });

    // Update page icon when thumbnail changes
    $scope.$watch('client.thumbnail.canvas', function thumbnailChanged(canvas) {
        iconService.setIcons(canvas);
    });

    // Watch clipboard for new data, associating it with any pressed keys
    $scope.$watch('client.clipboardData', function clipboardChanged(data) {

        // Sync local clipboard as long as the menu is not open
        if (!$scope.menu.shown)
            clipboardService.setLocalClipboard(data);

        // Associate new clipboard data with any currently-pressed key
        for (var keysym in keysCurrentlyPressed)
            clipboardDataFromKey[keysym] = data;

    });

    // Pull sharing profiles once the tunnel UUID is known
    $scope.$watch('client.tunnel.uuid', function retrieveSharingProfiles(uuid) {

        // Only pull sharing profiles if tunnel UUID is actually available
        if (!uuid)
            return;

        // Pull sharing profiles for the current connection
        tunnelService.getSharingProfiles(uuid)
        .success(function sharingProfilesRetrieved(sharingProfiles) {
            $scope.sharingProfiles = sharingProfiles;
        });

    });

    /**
     * Produces a sharing link for the current connection using the given
     * sharing profile. The resulting sharing link, and any required login
     * information, will be displayed to the user within the Guacamole menu.
     *
     * @param {SharingProfile} sharingProfile
     *     The sharing profile to use to generate the sharing link.
     */
    $scope.share = function share(sharingProfile) {
        ManagedClient.createShareLink($scope.client, sharingProfile);
    };

    /**
     * Returns whether the current connection has any associated share links.
     *
     * @returns {Boolean}
     *     true if the current connection has at least one associated share
     *     link, false otherwise.
     */
    $scope.isShared = function isShared() {
        return ManagedClient.isShared($scope.client);
    };

    /**
     * Returns the total number of share links associated with the current
     * connection.
     *
     * @returns {Number}
     *     The total number of share links associated with the current
     *     connection.
     */
    $scope.getShareLinkCount = function getShareLinkCount() {

        // Count total number of links within the ManagedClient's share link map
        var linkCount = 0;
        for (var dummy in $scope.client.shareLinks)
            linkCount++;

        return linkCount;

    };

    // Track pressed keys, opening the Guacamole menu after Ctrl+Alt+Shift
    $scope.$on('guacKeydown', function keydownListener(event, keysym, keyboard) {

        // Record key as pressed
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
                    $scope.menu.shown = !$scope.menu.shown;
                });
            }
        }

    });

    // Update pressed keys as they are released, synchronizing the clipboard
    // with any data that appears to have come from those key presses
    $scope.$on('guacKeyup', function keyupListener(event, keysym, keyboard) {

        // Sync local clipboard with any clipboard data received while this
        // key was pressed (if any) as long as the menu is not open
        var clipboardData = clipboardDataFromKey[keysym];
        if (clipboardData && !$scope.menu.shown)
            clipboardService.setLocalClipboard(clipboardData);

        // Mark key as released
        delete clipboardDataFromKey[keysym];
        delete keysCurrentlyPressed[keysym];

    });

    // Update page title when client title changes
    $scope.$watch('client.title', function clientTitleChanged(title) {
        $scope.page.title = title;
    });

    /**
     * Displays a notification at the end of a Guacamole connection, whether
     * that connection is ending normally or due to an error. As the end of
     * a Guacamole connection may be due to changes in authentication status,
     * this will also implicitly peform a re-authentication attempt to check
     * for such changes, possibly resulting in auth-related events like
     * guacInvalidCredentials.
     *
     * @param {Notification|Boolean|Object} status
     *     The status notification to show, as would be accepted by
     *     guacNotification.showStatus().
     */
    var notifyConnectionClosed = function notifyConnectionClosed(status) {

        // Re-authenticate to verify auth status at end of connection
        authenticationService.updateCurrentToken($location.search())

        // Show the requested status once the authentication check has finished
        ['finally'](function authenticationCheckComplete() {
            guacNotification.showStatus(status);
        });

    };

    // Show status dialog when connection status changes
    $scope.$watch('client.clientState.connectionState', function clientStateChanged(connectionState) {

        // Hide any existing status
        guacNotification.showStatus(false);

        // Do not display status if status not known
        if (!connectionState)
            return;

        // Build array of available actions
        var actions;
        if (NAVIGATE_HOME_ACTION)
            actions = [ NAVIGATE_HOME_ACTION, RECONNECT_ACTION, LOGOUT_ACTION ];
        else
            actions = [ RECONNECT_ACTION, LOGOUT_ACTION ];

        // Get any associated status code
        var status = $scope.client.clientState.statusCode;

        // Connecting 
        if (connectionState === ManagedClientState.ConnectionState.CONNECTING
         || connectionState === ManagedClientState.ConnectionState.WAITING) {
            guacNotification.showStatus({
                title: "CLIENT.DIALOG_HEADER_CONNECTING",
                text: {
                    key : "CLIENT.TEXT_CLIENT_STATUS_" + connectionState.toUpperCase()
                }
            });
        }

        // Client error
        else if (connectionState === ManagedClientState.ConnectionState.CLIENT_ERROR) {

            // Determine translation name of error
            var errorName = (status in CLIENT_ERRORS) ? status.toString(16).toUpperCase() : "DEFAULT";

            // Determine whether the reconnect countdown applies
            var countdown = (status in CLIENT_AUTO_RECONNECT) ? RECONNECT_COUNTDOWN : null;

            // Show error status
            notifyConnectionClosed({
                className : "error",
                title     : "CLIENT.DIALOG_HEADER_CONNECTION_ERROR",
                text      : {
                    key : "CLIENT.ERROR_CLIENT_" + errorName
                },
                countdown : countdown,
                actions   : actions
            });

        }

        // Tunnel error
        else if (connectionState === ManagedClientState.ConnectionState.TUNNEL_ERROR) {

            // Determine translation name of error
            var errorName = (status in TUNNEL_ERRORS) ? status.toString(16).toUpperCase() : "DEFAULT";

            // Determine whether the reconnect countdown applies
            var countdown = (status in TUNNEL_AUTO_RECONNECT) ? RECONNECT_COUNTDOWN : null;

            // Show error status
            notifyConnectionClosed({
                className : "error",
                title     : "CLIENT.DIALOG_HEADER_CONNECTION_ERROR",
                text      : {
                    key : "CLIENT.ERROR_TUNNEL_" + errorName
                },
                countdown : countdown,
                actions   : actions
            });

        }

        // Disconnected
        else if (connectionState === ManagedClientState.ConnectionState.DISCONNECTED) {
            notifyConnectionClosed({
                title   : "CLIENT.DIALOG_HEADER_DISCONNECTED",
                text    : {
                    key : "CLIENT.TEXT_CLIENT_STATUS_" + connectionState.toUpperCase()
                },
                actions : actions
            });
        }

        // Hide status and sync local clipboard once connected
        else if (connectionState === ManagedClientState.ConnectionState.CONNECTED) {

            // Sync with local clipboard
            clipboardService.getLocalClipboard().then(function clipboardRead(data) {
                $scope.$broadcast('guacClipboard', data);
            });

            // Hide status notification
            guacNotification.showStatus(false);

        }

        // Hide status for all other states
        else
            guacNotification.showStatus(false);

    });

    $scope.formattedScale = function formattedScale() {
        return Math.round($scope.client.clientProperties.scale * 100);
    };
    
    $scope.zoomIn = function zoomIn() {
        $scope.menu.autoFit = false;
        $scope.client.clientProperties.autoFit = false;
        $scope.client.clientProperties.scale += 0.1;
    };
    
    $scope.zoomOut = function zoomOut() {
        $scope.client.clientProperties.autoFit = false;
        $scope.client.clientProperties.scale -= 0.1;
    };
    
    $scope.changeAutoFit = function changeAutoFit() {
        if ($scope.menu.autoFit && $scope.client.clientProperties.minScale) {
            $scope.client.clientProperties.autoFit = true;
        }
        else {
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
        $scope.menu.shown = false;

    };

    /**
     * Action which immediately disconnects the currently-connected client, if
     * any.
     */
    var DISCONNECT_MENU_ACTION = {
        name      : 'CLIENT.ACTION_DISCONNECT',
        className : 'danger disconnect',
        callback  : $scope.disconnect
    };

    // Set client-specific menu actions
    $scope.clientMenuActions = [ DISCONNECT_MENU_ACTION ];

    /**
     * The currently-visible filesystem within the filesystem menu, if the
     * filesystem menu is open. If no filesystem is currently visible, this
     * will be null.
     *
     * @type ManagedFilesystem
     */
    $scope.filesystemMenuContents = null;

    /**
     * Hides the filesystem menu.
     */
    $scope.hideFilesystemMenu = function hideFilesystemMenu() {
        $scope.filesystemMenuContents = null;
    };

    /**
     * Shows the filesystem menu, displaying the contents of the given
     * filesystem within it.
     *
     * @param {ManagedFilesystem} filesystem
     *     The filesystem to show within the filesystem menu.
     */
    $scope.showFilesystemMenu = function showFilesystemMenu(filesystem) {
        $scope.filesystemMenuContents = filesystem;
    };

    /**
     * Returns whether the filesystem menu should be visible.
     *
     * @returns {Boolean}
     *     true if the filesystem menu is shown, false otherwise.
     */
    $scope.isFilesystemMenuShown = function isFilesystemMenuShown() {
        return !!$scope.filesystemMenuContents && $scope.menu.shown;
    };

    // Automatically refresh display when filesystem menu is shown
    $scope.$watch('isFilesystemMenuShown()', function refreshFilesystem() {

        // Refresh filesystem, if defined
        var filesystem = $scope.filesystemMenuContents;
        if (filesystem)
            ManagedFilesystem.refresh(filesystem, filesystem.currentDirectory);

    });

    /**
     * Returns the full path to the given file as an ordered array of parent
     * directories.
     *
     * @param {ManagedFilesystem.File} file
     *     The file whose full path should be retrieved.
     *
     * @returns {ManagedFilesystem.File[]}
     *     An array of directories which make up the hierarchy containing the
     *     given file, in order of increasing depth.
     */
    $scope.getPath = function getPath(file) {

        var path = [];

        // Add all files to path in ascending order of depth
        while (file && file.parent) {
            path.unshift(file);
            file = file.parent;
        }

        return path;

    };

    /**
     * Changes the current directory of the given filesystem to the given
     * directory.
     *
     * @param {ManagedFilesystem} filesystem
     *     The filesystem whose current directory should be changed.
     *
     * @param {ManagedFilesystem.File} file
     *     The directory to change to.
     */
    $scope.changeDirectory = function changeDirectory(filesystem, file) {
        ManagedFilesystem.changeDirectory(filesystem, file);
    };

    /**
     * Begins a file upload through the attached Guacamole client for
     * each file in the given FileList.
     *
     * @param {FileList} files
     *     The files to upload.
     */
    $scope.uploadFiles = function uploadFiles(files) {

        // Ignore file uploads if no attached client
        if (!$scope.client)
            return;

        // Upload each file
        for (var i = 0; i < files.length; i++)
            ManagedClient.uploadFile($scope.client, files[i], $scope.filesystemMenuContents);

    };

    /**
     * Determines whether the attached client has associated file transfers,
     * regardless of those file transfers' state.
     *
     * @returns {Boolean}
     *     true if there are any file transfers associated with the
     *     attached client, false otherise.
     */
    $scope.hasTransfers = function hasTransfers() {

        // There are no file transfers if there is no client
        if (!$scope.client)
            return false;

        return !!$scope.client.uploads.length;

    };

    /**
     * Returns whether the current user can share the current connection with
     * other users. A connection can be shared if and only if there is at least
     * one associated sharing profile.
     *
     * @returns {Boolean}
     *     true if the current user can share the current connection with other
     *     users, false otherwise.
     */
    $scope.canShareConnection = function canShareConnection() {

        // If there is at least one sharing profile, the connection can be shared
        for (var dummy in $scope.sharingProfiles)
            return true;

        // Otherwise, sharing is not possible
        return false;

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

    });

}]);
