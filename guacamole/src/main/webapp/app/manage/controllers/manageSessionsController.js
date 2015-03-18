/*
 * Copyright (C) 2015 Glyptodon LLC
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
 * The controller for the user session administration page.
 */
angular.module('manage').controller('manageSessionsController', ['$scope', '$injector', 
        function manageSessionsController($scope, $injector) {

    // Required types
    var ActiveTunnelWrapper = $injector.get('ActiveTunnelWrapper');
    var ConnectionGroup     = $injector.get('ConnectionGroup');

    // Required services
    var authenticationService  = $injector.get('authenticationService');
    var connectionGroupService = $injector.get('connectionGroupService');
    var guacNotification       = $injector.get('guacNotification');
    var permissionService      = $injector.get('permissionService');
    var tunnelService          = $injector.get('tunnelService');

    /**
     * The root connection group of the connection group hierarchy.
     *
     * @type ConnectionGroup
     */
    $scope.rootGroup = null;

    /**
     * All permissions associated with the current user, or null if the user's
     * permissions have not yet been loaded.
     *
     * @type PermissionSet
     */
    $scope.permissions = null;

    /**
     * The ActiveTunnelWrappers of all active sessions accessible by the current 
     * user, or null if the tunnels have not yet been loaded.
     *
     * @type ActiveTunnelWrapper[]
     */
    $scope.wrappers = null;

    // Query the user's permissions
    permissionService.getPermissions(authenticationService.getCurrentUserID())
            .success(function permissionsReceived(permissions) {
        $scope.permissions = permissions;
    });

    /**
     * Map of all visible connections by object identifier.
     *
     * @type Object.<String, Connection>
     */
    $scope.connections = {};
    
    /**
     * The count of currently selected tunnel wrappers.
     * 
     * @type Number
     */
    var selectedWrapperCount = 0;

    /**
     * Adds the given connection to the internal set of visible
     * connections.
     * 
     * @param {Connection} connection
     *     The connection to add to the internal set of visible connections.
     */
    var addConnection = function addConnection(connection) {

        // Add given connection to set of visible connections
        $scope.connections[connection.identifier] = connection;

    };

    /**
     * Adds all descendant connections of the given connection group to the
     * internal set of connections.
     * 
     * @param {ConnectionGroup} connectionGroup
     *     The connection group whose descendant connections should be added to
     *     the internal set of connections.
     */
    var addDescendantConnections  = function addDescendantConnections(connectionGroup) {

        // Add all child connections
        if (connectionGroup.childConnections)
            connectionGroup.childConnections.forEach(addConnection);

        // Add all child connection groups
        if (connectionGroup.childConnectionGroups)
            connectionGroup.childConnectionGroups.forEach(addDescendantConnections);

    };
    
    // Retrieve all connections 
    connectionGroupService.getConnectionGroupTree(ConnectionGroup.ROOT_IDENTIFIER)
    .success(function connectionGroupReceived(rootGroup) {
        $scope.rootGroup = rootGroup;
        addDescendantConnections($scope.rootGroup);
    });
    
    // Query active sessions
    tunnelService.getActiveTunnels().success(function sessionsRetrieved(tunnels) {
        
        // Wrap all active tunnels for sake of display
        $scope.wrappers = [];
        tunnels.forEach(function wrapActiveTunnel(tunnel) {
           $scope.wrappers.push(new ActiveTunnelWrapper(tunnel)); 
        });
        
    });

    /**
     * Returns whether critical data has completed being loaded.
     *
     * @returns {Boolean}
     *     true if enough data has been loaded for the user interface to be
     *     useful, false otherwise.
     */
    $scope.isLoaded = function isLoaded() {

        return $scope.wrappers             !== null
            && $scope.permissions          !== null
            && $scope.rootGroup            !== null;

    };

    /**
     * An action to be provided along with the object sent to showStatus which
     * closes the currently-shown status dialog.
     */
    var CANCEL_ACTION = {
        name        : "MANAGE_USER.ACTION_CANCEL",
        // Handle action
        callback    : function cancelCallback() {
            guacNotification.showStatus(false);
        }
    };
    
    /**
     * An action to be provided along with the object sent to showStatus which
     * immediately deletes the currently selected sessions.
     */
    var DELETE_ACTION = {
        name        : "MANAGE_SESSION.ACTION_DELETE",
        className   : "danger",
        // Handle action
        callback    : function deleteCallback() {
            deleteSessionsImmediately();
            guacNotification.showStatus(false);
        }
    };
    
    /**
     * Immediately deletes the selected sessions, without prompting the user for
     * confirmation.
     */
    var deleteSessionsImmediately = function deleteSessionsImmediately() {
        // TODO: Use a batch delete function to delete the sessions.
    }; 
    
    /**
     * Delete all selected sessions, prompting the user first to confirm that 
     * deletion is desired.
     */
    $scope.deleteSessions = function deleteSessions() {
        // Confirm deletion request
        guacNotification.showStatus({
            'title'      : 'MANAGE_SESSION.DIALOG_HEADER_CONFIRM_DELETE',
            'text'       : 'MANAGE_SESSION.TEXT_CONFIRM_DELETE',
            'actions'    : [ DELETE_ACTION, CANCEL_ACTION]
        });
    };
    
    /**
     * Returns whether the selected sessions can be deleted.
     * 
     * @returns {Boolean}
     *     true if selected sessions can be deleted, false otherwise.
     */
    $scope.canDeleteSessions = function canDeleteSessions() {
        return selectedWrapperCount > 0;
    };
    
    /**
     * Called whenever a tunnel wrapper changes selected status.
     * 
     * @param {Boolean} selected
     *     Whether the wrapper is now selected.
     */
    $scope.wrapperSelectionChange = function wrapperSelectionChange(selected) {
        if (selected)
            selectedWrapperCount++;
        else
            selectedWrapperCount--;
    };

}]);
