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
    var ManagedClientGroup = $injector.get('ManagedClientGroup');
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
         * The currently selected input method. This may be any of the values
         * defined within preferenceService.inputMethods.
         *
         * @type String
         */
        inputMethod : preferenceService.preferences.inputMethod,

        /**
         * Whether translation of touch to mouse events should emulate an
         * absolute pointer device, or a relative pointer device.
         *
         * @type Boolean
         */
        emulateAbsoluteMouse : preferenceService.preferences.emulateAbsoluteMouse,

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
     * Guacamole menu to the given ManagedClient. If no client is supplied,
     * this function has no effect.
     *
     * @param {ManagedClient} client
     *     The client to apply parameter changes to.
     */
    $scope.applyParameterChanges = function applyParameterChanges(client) {
        angular.forEach($scope.menu.connectionParameters, function sendArgv(value, name) {
            if (client)
                ManagedClient.setArgument(client, name, value);
        });
    };

    /**
     * The currently-focused client within the current ManagedClientGroup. If
     * there is no current group, no client is focused, or multiple clients are
     * focused, this will be null.
     *
     * @type ManagedClient
     */
    $scope.focusedClient = null;

    /**
     * The set of clients that should be attached to the client UI. This will
     * be immediately initialized by a call to updateAttachedClients() below.
     *
     * @type ManagedClientGroup[]
     */
    $scope.clientGroup = null;

    /**
     * @borrows ManagedClientGroup.getName
     */
    $scope.getName = ManagedClientGroup.getName;

    /**
     * @borrows ManagedClientGroup.getTitle
     */
    $scope.getTitle = ManagedClientGroup.getTitle;

    /**
     * Arbitrary context that should be exposed to the guacGroupList directive
     * displaying the dropdown list of available connections within the
     * Guacamole menu.
     */
    $scope.connectionListContext = {

        /**
         * The set of clients desired within the current view. For each client
         * that should be present within the current view, that client's ID
         * will map to "true" here.
         *
         * @type {Object.<string, boolean>}
         */
        attachedClients : {},

        /**
         * Notifies that the client with the given ID has been added or
         * removed from the set of clients desired within the current view,
         * and the current view should be updated accordingly.
         *
         * @param {string} id
         *     The ID of the client that was added or removed from the current
         *     view.
         */
        updateAttachedClients : function updateAttachedClients(id) {

            // Deconstruct current path into corresponding client IDs
            var ids = ManagedClientGroup.getClientIdentifiers($routeParams.id);

            // Add/remove ID as requested
            if ($scope.connectionListContext.attachedClients[id])
                ids.push(id);
            else
                _.pull(ids, id);

            // Reconstruct path, updating attached clients via change in route
            $location.path('/client/' + encodeURIComponent(ManagedClientGroup.getIdentifier(ids)));

        }

    };

    /**
     * Reloads the contents of $scope.clientGroup to reflect the client IDs
     * currently listed in the URL.
     */
    var updateAttachedClients = function updateAttachedClients() {

        var previousClients = $scope.clientGroup ? $scope.clientGroup.clients.slice() : [];
        detachCurrentGroup();

        $scope.clientGroup = guacClientManager.getManagedClientGroup($routeParams.id);
        $scope.clientGroup.attached = true;

        // Store current set of attached clients for later use within the
        // Guacamole menu
        $scope.connectionListContext.attachedClients = {};
        $scope.clientGroup.clients.forEach((client) => {
            $scope.connectionListContext.attachedClients[client.id] = true;
        });

        // Ensure menu is closed if updated view is not a modification of the
        // current view (has no clients in common). The menu should remain open
        // only while the current view is being modified, not when navigating
        // to an entirely different view.
        if (_.isEmpty(_.intersection(previousClients, $scope.clientGroup.clients)))
            $scope.menu.shown = false;

        // Update newly-attached clients with current contents of clipboard
        clipboardService.resyncClipboard();

    };

    /**
     * Detaches the ManagedClientGroup currently attached to the client
     * interface via $scope.clientGroup such that the interface can be safely
     * cleaned up or another ManagedClientGroup can take its place.
     */
    var detachCurrentGroup = function detachCurrentGroup() {

        var managedClientGroup = $scope.clientGroup;
        if (managedClientGroup) {

            // Flag group as detached
            managedClientGroup.attached = false;

            // Remove all disconnected clients from management (the user has
            // seen their status)
            _.filter(managedClientGroup.clients, client => {

                var connectionState = client.clientState.connectionState;
                return connectionState === ManagedClientState.ConnectionState.DISCONNECTED
                 || connectionState === ManagedClientState.ConnectionState.TUNNEL_ERROR
                 || connectionState === ManagedClientState.ConnectionState.CLIENT_ERROR;

            }).forEach(client => {
                guacClientManager.removeManagedClient(client.id);
            });

        }

    };

    // Init sets of clients based on current URL ...
    updateAttachedClients();

    // ... and re-initialize those sets if the URL has changed without
    // reloading the route
    $scope.$on('$routeUpdate', updateAttachedClients);

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

    // Show menu if the user swipes from the left, hide menu when the user
    // swipes from the right, scroll menu while visible
    $scope.menuDrag = function menuDrag(inProgress, startX, startY, currentX, currentY, deltaX, deltaY) {

        if ($scope.menu.shown) {

            // Hide menu if swipe-from-right gesture is detected
            if (Math.abs(currentY - startY)  <  MENU_DRAG_VERTICAL_TOLERANCE
                      && startX   - currentX >= MENU_DRAG_DELTA)
                $scope.menu.shown = false;

            // Scroll menu by default
            else {
                $scope.menu.scrollState.left -= deltaX;
                $scope.menu.scrollState.top -= deltaY;
            }

        }

        // Show menu if swipe-from-left gesture is detected
        else if (startX <= MENU_DRAG_MARGIN) {
            if (Math.abs(currentY - startY) <  MENU_DRAG_VERTICAL_TOLERANCE
                      && currentX - startX  >= MENU_DRAG_DELTA)
                $scope.menu.shown = true;
        }

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
        
        // Send any argument value data once menu is hidden
        if (!menuShown && menuShownPreviousState)
            $scope.applyParameterChanges($scope.focusedClient);

        // Disable client keyboard if the menu is shown
        angular.forEach($scope.clientGroup.clients, function updateKeyboardEnabled(client) {
            client.clientProperties.keyboardEnabled = !menuShown;
        });

    });

    // Automatically track and cache the currently-focused client
    $scope.$on('guacClientFocused', function focusedClientChanged(event, newFocusedClient) {

        var oldFocusedClient = $scope.focusedClient;
        $scope.focusedClient = newFocusedClient;

        // Apply any parameter changes when focus is changing
        if (oldFocusedClient)
            $scope.applyParameterChanges(oldFocusedClient);

        // Update available connection parameters, if there is a focused
        // client
        $scope.menu.connectionParameters = newFocusedClient ?
            ManagedClient.getArgumentModel(newFocusedClient) : {};

    });

    // Update page icon when thumbnail changes
    $scope.$watch('focusedClient.thumbnail.canvas', function thumbnailChanged(canvas) {
        iconService.setIcons(canvas);
    });

    // Pull sharing profiles once the tunnel UUID is known
    $scope.$watch('focusedClient.tunnel.uuid', function retrieveSharingProfiles(uuid) {

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
        if ($scope.focusedClient)
            ManagedClient.createShareLink($scope.focusedClient, sharingProfile);
    };

    /**
     * Returns whether the current connection has any associated share links.
     *
     * @returns {Boolean}
     *     true if the current connection has at least one associated share
     *     link, false otherwise.
     */
    $scope.isShared = function isShared() {
        return !!$scope.focusedClient && ManagedClient.isShared($scope.focusedClient);
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

        if (!$scope.focusedClient)
            return 0;

        // Count total number of links within the ManagedClient's share link map
        var linkCount = 0;
        for (var dummy in $scope.focusedClient.shareLinks)
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

    // Update pressed keys as they are released
    $scope.$on('guacKeyup', function keyupListener(event, keysym, keyboard) {

        // Deal with substitute key presses
        if (substituteKeysPressed[keysym]) {
            event.preventDefault();
            $scope.$broadcast('guacSyntheticKeyup', substituteKeysPressed[keysym]);
            delete substituteKeysPressed[keysym];
        }

        // Mark key as released
        else
            delete keysCurrentlyPressed[keysym];

    });

    // Update page title when client title changes
    $scope.$watch('getTitle(clientGroup)', function clientTitleChanged(title) {
        $scope.page.title = title;
    });

    /**
     * Returns whether the current connection has been flagged as unstable due
     * to an apparent network disruption.
     *
     * @returns {Boolean}
     *     true if the current connection has been flagged as unstable, false
     *     otherwise.
     */
    $scope.isConnectionUnstable = function isConnectionUnstable() {
        return _.findIndex($scope.clientGroup.clients, client => client.clientState.tunnelUnstable) !== -1;
    };

    /**
     * Immediately disconnects all currently-focused clients, if any.
     */
    $scope.disconnect = function disconnect() {

        // Disconnect if client is available
        if ($scope.clientGroup) {
            $scope.clientGroup.clients.forEach(client => {
                if (client.clientProperties.focused)
                    client.client.disconnect();
            });
        }

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

        // Upload each file
        for (var i = 0; i < files.length; i++)
            ManagedClient.uploadFile($scope.filesystemMenuContents.client, files[i], $scope.filesystemMenuContents);

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
        detachCurrentGroup();
    });

}]);
