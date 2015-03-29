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
 * The controller for editing or creating connections.
 */
angular.module('manage').controller('manageConnectionController', ['$scope', '$injector',
        function manageConnectionController($scope, $injector) {

    // Required types
    var Connection          = $injector.get('Connection');
    var ConnectionGroup     = $injector.get('ConnectionGroup');
    var HistoryEntryWrapper = $injector.get('HistoryEntryWrapper');
    var PermissionSet       = $injector.get('PermissionSet');

    // Required services
    var $location                = $injector.get('$location');
    var $routeParams             = $injector.get('$routeParams');
    var $translate               = $injector.get('$translate');
    var authenticationService    = $injector.get('authenticationService');
    var guacNotification         = $injector.get('guacNotification');
    var connectionService        = $injector.get('connectionService');
    var connectionGroupService   = $injector.get('connectionGroupService');
    var permissionService        = $injector.get('permissionService');
    var protocolService          = $injector.get('protocolService');
    var translationStringService = $injector.get('translationStringService');

    /**
     * An action to be provided along with the object sent to showStatus which
     * closes the currently-shown status dialog.
     */
    var ACKNOWLEDGE_ACTION = {
        name        : "MANAGE_CONNECTION.ACTION_ACKNOWLEDGE",
        // Handle action
        callback    : function acknowledgeCallback() {
            guacNotification.showStatus(false);
        }
    };
    
    /**
     * The identifier of the original connection from which this connection is
     * being cloned. Only valid if this is a new connection.
     * 
     * @type String
     */
    var cloneSourceIdentifier = $location.search().clone;

    /**
     * The identifier of the connection being edited. If a new connection is
     * being created, this will not be defined.
     *
     * @type String
     */
    var identifier = $routeParams.id;

    /**
     * All known protocols.
     *
     * @type Object.<String, Protocol>
     */
    $scope.protocols = null;

    /**
     * The root connection group of the connection group hierarchy.
     *
     * @type ConnectionGroup
     */
    $scope.rootGroup = null;

    /**
     * The connection being modified.
     * 
     * @type Connection
     */
    $scope.connection = null;

    /**
     * The parameter name/value pairs associated with the connection being
     * modified.
     *
     * @type Object.<String, String>
     */
    $scope.parameters = null;

    /**
     * The date format for use within the connection history.
     *
     * @type String
     */
    $scope.historyDateFormat = null;

    /**
     * The usage history of the connection being modified.
     *
     * @type HistoryEntryWrapper[]
     */
    $scope.historyEntryWrappers = null;
    
    /**
     * Whether the user can save the connection being edited. This could be
     * updating an existing connection, or creating a new connection.
     * 
     * @type Boolean
     */
    $scope.canSaveConnection = null;
    
    /**
     * Whether the user can delete the connection being edited.
     * 
     * @type Boolean
     */
    $scope.canDeleteConnection = null;
    
    /**
     * Whether the user can clone the connection being edited.
     * 
     * @type Boolean
     */
    $scope.canCloneConnection = null;

    /**
     * All permissions associated with the current user, or null if the user's
     * permissions have not yet been loaded.
     *
     * @type PermissionSet
     */
    $scope.permissions = null;

    /**
     * Returns whether critical data has completed being loaded.
     *
     * @returns {Boolean}
     *     true if enough data has been loaded for the user interface to be
     *     useful, false otherwise.
     */
    $scope.isLoaded = function isLoaded() {

        return $scope.protocols            !== null
            && $scope.rootGroup            !== null
            && $scope.connection           !== null
            && $scope.parameters           !== null
            && $scope.historyDateFormat    !== null
            && $scope.historyEntryWrappers !== null
            && $scope.permissions          !== null
            && $scope.canSaveConnection    !== null
            && $scope.canDeleteConnection  !== null
            && $scope.canCloneConnection   !== null;

    };

    // Pull connection group hierarchy
    connectionGroupService.getConnectionGroupTree(ConnectionGroup.ROOT_IDENTIFIER, 
        [PermissionSet.ObjectPermissionType.ADMINISTER])
    .success(function connectionGroupReceived(rootGroup) {
        $scope.rootGroup = rootGroup;
    });
    
    // Query the user's permissions for the current connection
    permissionService.getPermissions(authenticationService.getCurrentUserID())
            .success(function permissionsReceived(permissions) {
                
        $scope.permissions = permissions;
                        
        // Check if the connection is new or if the user has UPDATE permission
        $scope.canSaveConnection =
               !identifier
            || PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.ADMINISTER)
            || PermissionSet.hasConnectionPermission(permissions, PermissionSet.ObjectPermissionType.UPDATE, identifier);
            
        // Check if connection is not new and the user has DELETE permission
        $scope.canDeleteConnection =
            !!identifier && (
                   PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.ADMINISTER)
               ||  PermissionSet.hasConnectionPermission(permissions, PermissionSet.ObjectPermissionType.DELETE, identifier)
            );
                
        // Check if the connection is not new and the user has UPDATE and CREATE_CONNECTION permissions
        $scope.canCloneConnection =
            !!identifier && (
               PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.ADMINISTER) || (
                       PermissionSet.hasConnectionPermission(permissions, PermissionSet.ObjectPermissionType.UPDATE, identifier)
                   &&  PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.CREATE_CONNECTION)
               )
            );
    
    });
   
    // Get protocol metadata
    protocolService.getProtocols().success(function protocolsReceived(protocols) {
        $scope.protocols = protocols;
    });

    // Get history date format
    $translate('MANAGE_CONNECTION.FORMAT_HISTORY_START').then(function historyDateFormatReceived(historyDateFormat) {
        $scope.historyDateFormat = historyDateFormat;
    });

    // If we are editing an existing connection, pull its data
    if (identifier) {

        // Pull data from existing connection
        connectionService.getConnection(identifier).success(function connectionRetrieved(connection) {
            $scope.connection = connection;
        });

        // Pull connection history
        connectionService.getConnectionHistory(identifier).success(function historyReceived(historyEntries) {

            // Wrap all history entries for sake of display
            $scope.historyEntryWrappers = [];
            historyEntries.forEach(function wrapHistoryEntry(historyEntry) {
               $scope.historyEntryWrappers.push(new HistoryEntryWrapper(historyEntry)); 
            });

        });

        // Pull connection parameters
        connectionService.getConnectionParameters(identifier).success(function parametersReceived(parameters) {
            $scope.parameters = parameters;
        });
    }
    
    // If we are cloning an existing connection, pull its data instead
    else if (cloneSourceIdentifier) {

        // Pull data from cloned connection
        connectionService.getConnection(cloneSourceIdentifier).success(function connectionRetrieved(connection) {
            $scope.connection = connection;
            
            // Clear the identifier field because this connection is new
            delete $scope.connection.identifier;
        });

        // Do not pull connection history
        $scope.historyEntryWrappers = [];
        
        // Pull connection parameters from cloned connection
        connectionService.getConnectionParameters(cloneSourceIdentifier).success(function parametersReceived(parameters) {
            $scope.parameters = parameters;
        });
    }

    // If we are creating a new connection, populate skeleton connection data
    else {
        $scope.connection = new Connection({ protocol: 'vnc' });
        $scope.historyEntryWrappers = [];
        $scope.parameters = {};
    }

    /**
     * Given the internal name of a protocol, produces the translation string
     * for the localized version of that protocol's name.
     *
     * @param {String} protocolName
     *     The name of the protocol.
     * 
     * @returns {String}
     *     The translation string which produces the localized name of the
     *     protocol specified.
     */
    $scope.getProtocolName = function getProtocolName(protocolName) {
        return 'PROTOCOL_' + translationStringService.canonicalize(protocolName) + '.NAME';
    };

    /**
     * Given the internal name of a protocol and the internal name of a
     * parameter for that protocol, produces the translation string
     * for the localized, human-readable name of that protocol parameter.
     *
     * @param {String} protocolName
     *     The name of the protocol.
     * 
     * @param {String} parameterName
     *     The name of the protocol parameter.
     * 
     * @returns {String}
     *     The translation string which produces the translated name of the
     *     protocol parameter specified.
     */
    $scope.getProtocolParameterName = function getProtocolParameterName(protocolName, parameterName) {
        return 'PROTOCOL_'      + translationStringService.canonicalize(protocolName)
             + '.FIELD_HEADER_' + translationStringService.canonicalize(parameterName);
    };

    /**
     * Cancels all pending edits, returning to the management page.
     */
    $scope.cancel = function cancel() {
        $location.path('/manage/modules/connections/');
    };
    
    /**
     * Cancels all pending edits, opening an edit page for a new connection
     * which is prepopulated with the data from the connection currently being edited. 
     */
    $scope.cloneConnection = function cloneConnection() {
        $location.path('/manage/connections').search('clone', identifier);
    };
            
    /**
     * Saves the connection, creating a new connection or updating the existing
     * connection.
     */
    $scope.saveConnection = function saveConnection() {

        $scope.connection.parameters = $scope.parameters;

        // Save the connection
        connectionService.saveConnection($scope.connection)
        .success(function savedConnection() {
            $location.path('/manage/modules/connections/');
        })

        // Notify of any errors
        .error(function connectionSaveFailed(error) {
            guacNotification.showStatus({
                'className'  : 'error',
                'title'      : 'MANAGE_CONNECTION.DIALOG_HEADER_ERROR',
                'text'       : error.message,
                'actions'    : [ ACKNOWLEDGE_ACTION ]
            });
        });

    };
    
    /**
     * An action to be provided along with the object sent to showStatus which
     * immediately deletes the current connection.
     */
    var DELETE_ACTION = {
        name        : "MANAGE_CONNECTION.ACTION_DELETE",
        className   : "danger",
        // Handle action
        callback    : function deleteCallback() {
            deleteConnectionImmediately();
            guacNotification.showStatus(false);
        }
    };

    /**
     * An action to be provided along with the object sent to showStatus which
     * closes the currently-shown status dialog.
     */
    var CANCEL_ACTION = {
        name        : "MANAGE_CONNECTION.ACTION_CANCEL",
        // Handle action
        callback    : function cancelCallback() {
            guacNotification.showStatus(false);
        }
    };

    /**
     * Immediately deletes the current connection, without prompting the user
     * for confirmation.
     */
    var deleteConnectionImmediately = function deleteConnectionImmediately() {

        // Delete the connection
        connectionService.deleteConnection($scope.connection)
        .success(function deletedConnection() {
            $location.path('/manage/modules/connections/');
        })

        // Notify of any errors
        .error(function connectionDeletionFailed(error) {
            guacNotification.showStatus({
                'className'  : 'error',
                'title'      : 'MANAGE_CONNECTION.DIALOG_HEADER_ERROR',
                'text'       : error.message,
                'actions'    : [ ACKNOWLEDGE_ACTION ]
            });
        });

    };

    /**
     * Deletes the connection, prompting the user first to confirm that
     * deletion is desired.
     */
    $scope.deleteConnection = function deleteConnection() {

        // Confirm deletion request
        guacNotification.showStatus({
            'title'      : 'MANAGE_CONNECTION.DIALOG_HEADER_CONFIRM_DELETE',
            'text'       : 'MANAGE_CONNECTION.TEXT_CONFIRM_DELETE',
            'actions'    : [ DELETE_ACTION, CANCEL_ACTION]
        });

    };

}]);
