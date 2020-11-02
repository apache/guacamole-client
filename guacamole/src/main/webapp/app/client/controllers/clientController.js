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
    var ConnectionGroup    = $injector.get('ConnectionGroup');
    var ManagedClient      = $injector.get('ManagedClient');
    var ManagedClientState = $injector.get('ManagedClientState');
    var ManagedFilesystem  = $injector.get('ManagedFilesystem');
    var Protocol           = $injector.get('Protocol');
    var ScrollState        = $injector.get('ScrollState');

    // Required services
    var $location              = $injector.get('$location');
    var authenticationService  = $injector.get('authenticationService');
    var connectionGroupService = $injector.get('connectionGroupService');
    var clipboardService       = $injector.get('clipboardService');
    var dataSourceService      = $injector.get('dataSourceService');
    var guacClientManager      = $injector.get('guacClientManager');
    var guacNotification       = $injector.get('guacNotification');
    var iconService            = $injector.get('iconService');
    var preferenceService      = $injector.get('preferenceService');
    var requestService         = $injector.get('requestService');
    var tunnelService          = $injector.get('tunnelService');
    var userPageService        = $injector.get('userPageService');

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

    /**
     * In order to open the guacamole menu, we need to hit ctrl-alt-shift. There are
     * several possible keysysms for each key.
     */
    var SHIFT_KEYS  = {0xFFE1 : true, 0xFFE2 : true},
        ALT_KEYS    = {0xFFE9 : true, 0xFFEA : true, 0xFE03 : true,
                       0xFFE7 : true, 0xFFE8 : true},
        CTRL_KEYS   = {0xFFE3 : true, 0xFFE4 : true},
        MENU_KEYS   = angular.extend({}, SHIFT_KEYS, ALT_KEYS, CTRL_KEYS);

    /**
     * Keysym for detecting any END key presses, for the purpose of passing through
     * the Ctrl-Alt-Del sequence to a remote system.
     */
    var END_KEYS = {0xFF57 : true, 0xFFB1 : true};

    /**
     * Keysym for sending the DELETE key when the Ctrl-Alt-End hotkey
     * combo is pressed.
     *
     * @type Number
     */
    var DEL_KEY = 0xFFFF;

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
            authenticationService.logout()
            ['catch'](requestService.IGNORE)
            ['finally'](function logoutComplete() {
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

    }, requestService.WARN);

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
        scrollState : new ScrollState(),

        /**
         * The current desired values of all editable connection parameters as
         * a set of name/value pairs, including any changes made by the user.
         *
         * @type {Object.<String, String>}
         */
        connectionParameters : {}

    };

    // Convenience method for closing the menu
    $scope.closeMenu = function closeMenu() {
        $scope.menu.shown = false;
    };

    /**
     * Applies any changes to connection parameters made by the user within the
     * Guacamole menu.
     */
    $scope.applyParameterChanges = function applyParameterChanges() {
        angular.forEach($scope.menu.connectionParameters, function sendArgv(value, name) {
            ManagedClient.setArgument($scope.client, name, value);
        });
    };

    /**
     * The client which should be attached to the client UI.
     *
     * @type ManagedClient
     */
    $scope.client = guacClientManager.getManagedClient($routeParams.id, $routeParams.params);

    /**
     * All active clients which are not the current client ($scope.client).
     * Each key is the ID of the connection used by that client.
     *
     * @type Object.<String, ManagedClient>
     */
    $scope.otherClients = (function getOtherClients(clients) {
        var otherClients = angular.extend({}, clients);
        delete otherClients[$scope.client.id];
        return otherClients;
    })(guacClientManager.getManagedClients());

    /**
     * The root connection groups of the connection hierarchy that should be
     * presented to the user for selecting a different connection, as a map of
     * data source identifier to the root connection group of that data
     * source. This will be null if the connection group hierarchy has not yet
     * been loaded or if the hierarchy is inapplicable due to only one
     * connection or balancing group being available.
     *
     * @type Object.<String, ConnectionGroup>
     */
    $scope.rootConnectionGroups = null;

    /**
     * Array of all connection properties that are filterable.
     *
     * @type String[]
     */
    $scope.filteredConnectionProperties = [
        'name'
    ];

    /**
     * Array of all connection group properties that are filterable.
     *
     * @type String[]
     */
    $scope.filteredConnectionGroupProperties = [
        'name'
    ];

    // Retrieve root groups and all descendants
    dataSourceService.apply(
        connectionGroupService.getConnectionGroupTree,
        authenticationService.getAvailableDataSources(),
        ConnectionGroup.ROOT_IDENTIFIER
    )
    .then(function rootGroupsRetrieved(rootConnectionGroups) {

        // Store retrieved groups only if there are multiple connections or
        // balancing groups available
        var clientPages = userPageService.getClientPages(rootConnectionGroups);
        if (clientPages.length > 1)
            $scope.rootConnectionGroups = rootConnectionGroups;

    }, requestService.WARN);

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
     * Map of all substituted key presses.  If one key is pressed in place of another
     * the value of the substituted key is stored in an object with the keysym of
     * the original key.
     *
     * @type Object.<Number, Number>
     */
    var substituteKeysPressed = {};

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

    // Update client state/behavior as visibility of the Guacamole menu changes
    $scope.$watch('menu.shown', function menuVisibilityChanged(menuShown, menuShownPreviousState) {
        
        // Send clipboard and argument value data once menu is hidden
        if (!menuShown && menuShownPreviousState) {
            $scope.$broadcast('guacClipboard', $scope.client.clipboardData);
            $scope.applyParameterChanges();
        }

        // Obtain snapshot of current editable connection parameters when menu
        // is opened
        else if (menuShown)
            $scope.menu.connectionParameters = ManagedClient.getArgumentModel($scope.client);

        // Disable client keyboard if the menu is shown
        $scope.client.clientProperties.keyboardEnabled = !menuShown;

    });

    // Update last used timestamp when the active client changes
    $scope.$watch('client', function clientChanged(client) {
        if (client)
            client.lastUsed = new Date().getTime();
    });

    // Update page icon when thumbnail changes
    $scope.$watch('client.thumbnail.canvas', function thumbnailChanged(canvas) {
        iconService.setIcons(canvas);
    });

    // Watch clipboard for new data, associating it with any pressed keys
    $scope.$watch('client.clipboardData', function clipboardChanged(data) {

        // Sync local clipboard as long as the menu is not open
        if (!$scope.menu.shown)
            clipboardService.setLocalClipboard(data)['catch'](angular.noop);

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
        .then(function sharingProfilesRetrieved(sharingProfiles) {
            $scope.sharingProfiles = sharingProfiles;
        }, requestService.WARN);

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

    // Track pressed keys, opening the Guacamole menu after Ctrl+Alt+Shift, or
    // send Ctrl-Alt-Delete when Ctrl-Alt-End is pressed.
    $scope.$on('guacKeydown', function keydownListener(event, keysym, keyboard) {

        // Record key as pressed
        keysCurrentlyPressed[keysym] = true;   
        
        var currentKeysPressedKeys = Object.keys(keysCurrentlyPressed);

        // If only menu keys are pressed, and we have one keysym from each group,
        // and one of the keys is being released, show the menu. 
        if (checkMenuModeActive()) {
            
            // Check that there is a key pressed for each of the required key classes
            if (!_.isEmpty(_.pick(SHIFT_KEYS, currentKeysPressedKeys)) &&
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

        // If one of the End keys is pressed, and we have a one keysym from each
        // of Ctrl and Alt groups, send Ctrl-Alt-Delete.
        if (END_KEYS[keysym] &&
            !_.isEmpty(_.pick(ALT_KEYS, currentKeysPressedKeys)) &&
            !_.isEmpty(_.pick(CTRL_KEYS, currentKeysPressedKeys))
        ) {

            // Don't send this event through to the client.
            event.preventDefault();

            // Remove the original key press
            delete keysCurrentlyPressed[keysym];

            // Record the substituted key press so that it can be
            // properly dealt with later.
            substituteKeysPressed[keysym] = DEL_KEY;

            // Send through the delete key.
            $scope.$broadcast('guacSyntheticKeydown', DEL_KEY);
        }

    });

    // Update pressed keys as they are released, synchronizing the clipboard
    // with any data that appears to have come from those key presses
    $scope.$on('guacKeyup', function keyupListener(event, keysym, keyboard) {

        // Sync local clipboard with any clipboard data received while this
        // key was pressed (if any) as long as the menu is not open
        var clipboardData = clipboardDataFromKey[keysym];
        if (clipboardData && !$scope.menu.shown)
            clipboardService.setLocalClipboard(clipboardData)['catch'](angular.noop);

        // Deal with substitute key presses
        if (substituteKeysPressed[keysym]) {
            event.preventDefault();
            $scope.$broadcast('guacSyntheticKeyup', substituteKeysPressed[keysym]);
            delete substituteKeysPressed[keysym];
        }

        // Mark key as released
        else {
            delete clipboardDataFromKey[keysym];
            delete keysCurrentlyPressed[keysym];
        }

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
        ['catch'](requestService.IGNORE)

        // Show the requested status once the authentication check has finished
        ['finally'](function authenticationCheckComplete() {
            guacNotification.showStatus(status);
        });

    };

    /**
     * Returns whether the current connection has been flagged as unstable due
     * to an apparent network disruption.
     *
     * @returns {Boolean}
     *     true if the current connection has been flagged as unstable, false
     *     otherwise.
     */
    $scope.isConnectionUnstable = function isConnectionUnstable() {
        return $scope.client && $scope.client.clientState.tunnelUnstable;
    };

    /**
     * Notifies the user that the connection state has changed.
     *
     * @param {String} connectionState
     *     The current connection state, as defined by
     *     ManagedClientState.ConnectionState.
     */
    var notifyConnectionState = function notifyConnectionState(connectionState) {

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
            }, angular.noop);

            // Hide status notification
            guacNotification.showStatus(false);

        }

        // Hide status for all other states
        else
            guacNotification.showStatus(false);

    };

    /**
     * Prompts the user to enter additional connection parameters. If the
     * protocol and associated parameters of the underlying connection are not
     * yet known, this function has no effect and should be re-invoked once
     * the parameters are known.
     *
     * @param {Object.<String, String>} requiredParameters
     *     The set of all parameters requested by the server via "required"
     *     instructions, where each object key is the name of a requested
     *     parameter and each value is the current value entered by the user.
     */
    var notifyParametersRequired = function notifyParametersRequired(requiredParameters) {

        /**
         * Action which submits the current set of parameter values, requesting
         * that the connection continue.
         */
        var SUBMIT_PARAMETERS = {
            name      : "CLIENT.ACTION_CONTINUE",
            className : "button",
            callback  : function submitParameters() {
                if ($scope.client) {
                    var params = $scope.client.requiredParameters;
                    $scope.client.requiredParameters = null;
                    ManagedClient.sendArguments($scope.client, params);
                }
            }
        };

        /**
         * Action which cancels submission of additional parameters and
         * disconnects from the current connection.
         */
        var CANCEL_PARAMETER_SUBMISSION = {
            name      : "CLIENT.ACTION_CANCEL",
            className : "button",
            callback  : function cancelSubmission() {
                $scope.client.requiredParameters = null;
                $scope.disconnect();
            }
        };

        // Attempt to prompt for parameters only if the parameters that apply
        // to the underlying connection are known
        if (!$scope.client.protocol || !$scope.client.forms)
            return;

        // Hide any existing status
        guacNotification.showStatus(false);

        // Prompt for parameters
        guacNotification.showStatus({
            formNamespace : Protocol.getNamespace($scope.client.protocol),
            forms : $scope.client.forms,
            formModel : requiredParameters,
            formSubmitCallback : SUBMIT_PARAMETERS.callback,
            actions : [ SUBMIT_PARAMETERS, CANCEL_PARAMETER_SUBMISSION ]
        });

    };

    /**
     * Returns whether the given connection state allows for submission of
     * connection parameters via "argv" instructions.
     *
     * @param {String} connectionState
     *     The connection state to test, as defined by
     *     ManagedClientState.ConnectionState.
     *
     * @returns {boolean}
     *     true if the given connection state allows submission of connection
     *     parameters via "argv" instructions, false otherwise.
     */
    var canSubmitParameters = function canSubmitParameters(connectionState) {
        return (connectionState === ManagedClientState.ConnectionState.WAITING ||
                connectionState === ManagedClientState.ConnectionState.CONNECTED);
    };

    // Show status dialog when connection status changes
    $scope.$watchGroup([
        'client.clientState.connectionState',
        'client.requiredParameters',
        'client.protocol',
        'client.forms'
    ], function clientStateChanged(newValues) {

        var connectionState = newValues[0];
        var requiredParameters = newValues[1];

        // Prompt for parameters only if parameters can actually be submitted
        if (requiredParameters && canSubmitParameters(connectionState))
            notifyParametersRequired(requiredParameters);

        // Otherwise, just show general connection state
        else
            notifyConnectionState(connectionState);

    });

    $scope.zoomIn = function zoomIn() {
        $scope.menu.autoFit = false;
        $scope.client.clientProperties.autoFit = false;
        $scope.client.clientProperties.scale += 0.1;
    };
    
    $scope.zoomOut = function zoomOut() {
        $scope.client.clientProperties.autoFit = false;
        $scope.client.clientProperties.scale -= 0.1;
    };

    /**
     * When zoom is manually set by entering a value
     * into the controller, this method turns off autoFit,
     * both in the menu and the clientProperties.
     */
    $scope.zoomSet = function zoomSet() {
        $scope.menu.autoFit = false;
        $scope.client.clientProperties.autoFit = false;
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
     * @borrows Protocol.getNamespace
     */
    $scope.getProtocolNamespace = Protocol.getNamespace;

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
