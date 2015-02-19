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
 * The controller for editing or creating connection groups.
 */
angular.module('manage').controller('manageConnectionGroupController', ['$scope', '$injector', 
        function manageConnectionGroupController($scope, $injector) {
            
    // Required types
    var ConnectionGroup = $injector.get('ConnectionGroup');
    var PermissionSet   = $injector.get('PermissionSet');

    // Required services
    var $location              = $injector.get('$location');
    var $routeParams           = $injector.get('$routeParams');
    var authenticationService  = $injector.get('authenticationService');
    var connectionGroupService = $injector.get('connectionGroupService');
    var permissionService      = $injector.get('permissionService');
    
    /**
     * An action to be provided along with the object sent to showStatus which
     * closes the currently-shown status dialog.
     */
    var ACKNOWLEDGE_ACTION = {
        name        : "MANAGE_CONNECTION_GROUP.ACTION_ACKNOWLEDGE",
        // Handle action
        callback    : function acknowledgeCallback() {
            $scope.showStatus(false);
        }
    };

    /**
     * The identifier of the connection group being edited. If a new connection
     * group is being created, this will not be defined.
     *
     * @type String
     */
    var identifier = $routeParams.id;

    /**
     * The root connection group of the connection group hierarchy.
     *
     * @type ConnectionGroup
     */
    $scope.rootGroup = null;

    /**
     * The connection group being modified.
     * 
     * @type ConnectionGroup
     */
    $scope.connectionGroup = null;
    
    /**
     * Whether the user has UPDATE permission for the current connection group.
     * 
     * @type Boolean
     */
    $scope.hasUpdatePermission = null;
    
    /**
     * Whether the user has DELETE permission for the current connection group.
     * 
     * @type Boolean
     */
    $scope.hasDeletePermission = null;

    /**
     * Returns whether critical data has completed being loaded.
     *
     * @returns {Boolean}
     *     true if enough data has been loaded for the user interface to be
     *     useful, false otherwise.
     */
    $scope.isLoaded = function isLoaded() {

        return $scope.rootGroup                !== null
            && $scope.connectionGroup          !== null
            && $scope.canSaveConnectionGroup   !== null
            && $scope.canDeleteConnectionGroup !== null;

    };
    
    // Query the user's permissions for the current connection group
    permissionService.getPermissions(authenticationService.getCurrentUserID())
            .success(function permissionsReceived(permissions) {
                
        // Check if the connection group is new or if the user has UPDATE permission
        $scope.canSaveConnectionGroup =
              !identifier
           || PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.ADMINISTER)
           || PermissionSet.hasConnectionGroupPermission(permissions, PermissionSet.ObjectPermissionType.UPDATE, identifier);

        // Check if connection group is not new and the user has DELETE permission
        $scope.canDeleteConnectionGroup =
           !!identifier && (
                  PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.ADMINISTER)
              ||  PermissionSet.hasConnectionGroupPermission(permissions, PermissionSet.ObjectPermissionType.DELETE, identifier)
           );
    
    });


    // Pull connection group hierarchy
    connectionGroupService.getConnectionGroupTree(ConnectionGroup.ROOT_IDENTIFIER, [PermissionSet.ObjectPermissionType.ADMINISTER])
    .success(function connectionGroupReceived(rootGroup) {
        $scope.rootGroup = rootGroup;
    });

    // If we are editing an existing connection group, pull its data
    if (identifier) {
        connectionGroupService.getConnectionGroup(identifier).success(function connectionGroupReceived(connectionGroup) {
            $scope.connectionGroup = connectionGroup;
        });
    }

    // If we are creating a new connection group, populate skeleton connection group data
    else
        $scope.connectionGroup = new ConnectionGroup();

    /**
     * Available connection group types, as translation string / internal value
     * pairs.
     * 
     * @type Object[]
     */
    $scope.types = [
        {
            label: "MANAGE_CONNECTION_GROUP.NAME_TYPE_ORGANIZATIONAL",
            value: ConnectionGroup.Type.ORGANIZATIONAL
        },
        {
            label: "MANAGE_CONNECTION_GROUP.NAME_TYPE_BALANCING",
            value : ConnectionGroup.Type.BALANCING
        }
    ];
    
    /**
     * Cancels all pending edits, returning to the management page.
     */
    $scope.cancel = function cancel() {
        $location.path('/manage/');
    };
   
    /**
     * Saves the connection group, creating a new connection group or updating
     * the existing connection group.
     */
    $scope.saveConnectionGroup = function saveConnectionGroup() {

        // Save the connection
        connectionGroupService.saveConnectionGroup($scope.connectionGroup)
        .success(function savedConnectionGroup() {
            $location.path('/manage/');
        })

        // Notify of any errors
        .error(function connectionGroupSaveFailed(error) {
            $scope.showStatus({
                'className'  : 'error',
                'title'      : 'MANAGE_CONNECTION_GROUP.DIALOG_HEADER_ERROR',
                'text'       : error.message,
                'actions'    : [ ACKNOWLEDGE_ACTION ]
            });
        });

    };
    
    /**
     * An action to be provided along with the object sent to showStatus which
     * immediately deletes the current connection group.
     */
    var DELETE_ACTION = {
        name        : "MANAGE_CONNECTION_GROUP.ACTION_DELETE",
        className   : "danger",
        // Handle action
        callback    : function deleteCallback() {
            deleteConnectionGroupImmediately();
            $scope.showStatus(false);
        }
    };

    /**
     * An action to be provided along with the object sent to showStatus which
     * closes the currently-shown status dialog.
     */
    var CANCEL_ACTION = {
        name        : "MANAGE_CONNECTION_GROUP.ACTION_CANCEL",
        // Handle action
        callback    : function cancelCallback() {
            $scope.showStatus(false);
        }
    };

    /**
     * Immediately deletes the current connection group, without prompting the
     * user for confirmation.
     */
    var deleteConnectionGroupImmediately = function deleteConnectionGroupImmediately() {

        // Delete the connection group
        connectionGroupService.deleteConnectionGroup($scope.connectionGroup)
        .success(function deletedConnectionGroup() {
            $location.path('/manage/');
        })

        // Notify of any errors
        .error(function connectionGroupDeletionFailed(error) {
            $scope.showStatus({
                'className'  : 'error',
                'title'      : 'MANAGE_CONNECTION_GROUP.DIALOG_HEADER_ERROR',
                'text'       : error.message,
                'actions'    : [ ACKNOWLEDGE_ACTION ]
            });
        });

    };

    /**
     * Deletes the connection group, prompting the user first to confirm that
     * deletion is desired.
     */
    $scope.deleteConnectionGroup = function deleteConnectionGroup() {

        // Confirm deletion request
        $scope.showStatus({
            'title'      : 'MANAGE_CONNECTION_GROUP.DIALOG_HEADER_CONFIRM_DELETE',
            'text'       : 'MANAGE_CONNECTION_GROUP.TEXT_CONFIRM_DELETE',
            'actions'    : [ DELETE_ACTION, CANCEL_ACTION]
        });

    };

}]);
