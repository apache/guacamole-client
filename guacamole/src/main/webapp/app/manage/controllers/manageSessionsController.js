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
    var ActiveConnectionWrapper = $injector.get('ActiveConnectionWrapper');
    var ConnectionGroup         = $injector.get('ConnectionGroup');
    var StableSort              = $injector.get('StableSort');

    // Required services
    var activeConnectionService = $injector.get('activeConnectionService');
    var authenticationService   = $injector.get('authenticationService');
    var connectionGroupService  = $injector.get('connectionGroupService');
    var guacNotification        = $injector.get('guacNotification');
    var permissionService       = $injector.get('permissionService');

    /**
     * All permissions associated with the current user, or null if the user's
     * permissions have not yet been loaded.
     *
     * @type PermissionSet
     */
    $scope.permissions = null;

    /**
     * The ActiveConnectionWrappers of all active sessions accessible by the
     * current user, or null if the active sessions have not yet been loaded.
     *
     * @type ActiveConnectionWrapper[]
     */
    $scope.wrappers = null;

    /**
     * StableSort instance which maintains the sort order of the visible
     * connection wrappers.
     *
     * @type StableSort
     */
    $scope.wrapperOrder = new StableSort([
        'activeConnection.username',
        'activeConnection.startDate',
        'activeConnection.remoteHost',
        'name'
    ]);

    /**
     * The root connection group of the connection group hierarchy.
     *
     * @type ConnectionGroup
     */
    var rootGroup = null;

    /**
     * All active connections, if known, or null if active connections have not
     * yet been loaded.
     *
     * @type ActiveConnection
     */
    var activeConnections = null;

    /**
     * Map of all visible connections by object identifier.
     *
     * @type Object.<String, Connection>
     */
    var connections = {};

    /**
     * Map of all currently-selected active connection wrappers by identifier.
     * 
     * @type Object.<String, ActiveConnectionWrapper>
     */
    var selectedWrappers = {};

    /**
     * Adds the given connection to the internal set of visible
     * connections.
     * 
     * @param {Connection} connection
     *     The connection to add to the internal set of visible connections.
     */
    var addConnection = function addConnection(connection) {

        // Add given connection to set of visible connections
        connections[connection.identifier] = connection;

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

    /**
     * Wraps all loaded active connections, storing the resulting array within
     * the scope. If required data has not yet finished loading, this function
     * has no effect.
     */
    var wrapActiveConnections = function wrapActiveConnections() {

        // Abort if not all required data is available
        if (!activeConnections || !connections)
            return;

        // Wrap all active connections for sake of display
        $scope.wrappers = [];
        for (var identifier in activeConnections) {

            var activeConnection = activeConnections[identifier];
            var connection = connections[activeConnection.connectionIdentifier];

            $scope.wrappers.push(new ActiveConnectionWrapper(
                connection.name,
                activeConnection
            )); 

        }

    };

    // Query the user's permissions
    permissionService.getPermissions(authenticationService.getCurrentUserID())
            .success(function permissionsReceived(retrievedPermissions) {
        $scope.permissions = retrievedPermissions;
    });

    // Retrieve all connections 
    connectionGroupService.getConnectionGroupTree(ConnectionGroup.ROOT_IDENTIFIER)
    .success(function connectionGroupReceived(retrievedRootGroup) {

        // Load connections from retrieved group tree
        rootGroup = retrievedRootGroup;
        addDescendantConnections(rootGroup);

        // Attempt to produce wrapped list of active connections
        wrapActiveConnections();

    });
    
    // Query active sessions
    activeConnectionService.getActiveConnections().success(function sessionsRetrieved(retrievedActiveConnections) {

        // Store received list
        activeConnections = retrievedActiveConnections;

        // Attempt to produce wrapped list of active connections
        wrapActiveConnections();

    });

    /**
     * Returns whether critical data has completed being loaded.
     *
     * @returns {Boolean}
     *     true if enough data has been loaded for the user interface to be
     *     useful, false otherwise.
     */
    $scope.isLoaded = function isLoaded() {

        return $scope.wrappers    !== null
            && $scope.permissions !== null;

    };

    /**
     * Returns whether the wrapped session list is sorted by the given
     * property.
     *
     * @param {String} property
     *     The name of the property to check.
     *
     * @returns {Boolean}
     *     true if the wrapped session list is sorted by the given property,
     *     false otherwise.
     */
    $scope.isSortedBy = function isSortedBy(property) {
        return $scope.wrapperOrder.primary === property;
    };

    /**
     * Sets the primary sorting property to the given property, if not already
     * set. If already set, the ascending/descending sort order is toggled.
     *
     * @param {String} property
     *     The name of the property to assign as the primary sorting property.
     */
    $scope.toggleSort = function toggleSort(property) {

        // Sort in ascending order by new property, if different
        if (!$scope.isSortedBy(property))
            $scope.wrapperOrder.reorder(property, false);

        // Otherwise, toggle sort order
        else
            $scope.wrapperOrder.reorder(property, !$scope.wrapperOrder.descending);

    };

    /**
     * An action to be provided along with the object sent to showStatus which
     * closes the currently-shown status dialog.
     */
    var ACKNOWLEDGE_ACTION = {
        name        : "MANAGE_SESSION.ACTION_ACKNOWLEDGE",
        // Handle action
        callback    : function acknowledgeCallback() {
            guacNotification.showStatus(false);
        }
    };

    /**
     * An action to be provided along with the object sent to showStatus which
     * closes the currently-shown status dialog.
     */
    var CANCEL_ACTION = {
        name        : "MANAGE_SESSION.ACTION_CANCEL",
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

        // Perform deletion
        activeConnectionService.deleteActiveConnections(Object.keys(selectedWrappers))
        .success(function activeConnectionsDeleted() {

            // Remove deleted connections from wrapper array
            $scope.wrappers = $scope.wrappers.filter(function activeConnectionStillExists(wrapper) {
                return !(wrapper.activeConnection.identifier in selectedWrappers);
            });

            // Clear selection
            selectedWrappers = {};

        })

        // Notify of any errors
        .error(function activeConnectionDeletionFailed(error) {
            guacNotification.showStatus({
                'className'  : 'error',
                'title'      : 'MANAGE_SESSION.DIALOG_HEADER_ERROR',
                'text'       : error.message,
                'actions'    : [ ACKNOWLEDGE_ACTION ]
            });
        });

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

        // We can delete sessions if at least one is selected
        for (var identifier in selectedWrappers)
            return true;

        return false;

    };
    
    /**
     * Called whenever an active connection wrapper changes selected status.
     * 
     * @param {ActiveConnectionWrapper} wrapper
     *     The wrapper whose selected status has changed.
     */
    $scope.wrapperSelectionChange = function wrapperSelectionChange(wrapper) {

        // Add wrapper to map if selected
        if (wrapper.checked)
            selectedWrappers[wrapper.activeConnection.identifier] = wrapper;

        // Otherwise, remove wrapper from map
        else
            delete selectedWrappers[wrapper.activeConnection.identifier];

    };

}]);
