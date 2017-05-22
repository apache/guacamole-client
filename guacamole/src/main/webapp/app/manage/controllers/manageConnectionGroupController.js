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
    var guacNotification       = $injector.get('guacNotification');
    var permissionService      = $injector.get('permissionService');
    var schemaService          = $injector.get('schemaService');
    
    /**
     * An action to be provided along with the object sent to showStatus which
     * closes the currently-shown status dialog.
     */
    var ACKNOWLEDGE_ACTION = {
        name        : "MANAGE_CONNECTION_GROUP.ACTION_ACKNOWLEDGE",
        // Handle action
        callback    : function acknowledgeCallback() {
            guacNotification.showStatus(false);
        }
    };

    /**
     * The unique identifier of the data source containing the connection group
     * being edited.
     *
     * @type String
     */
    $scope.selectedDataSource = $routeParams.dataSource;

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
     * All permissions associated with the current user, or null if the user's
     * permissions have not yet been loaded.
     *
     * @type PermissionSet
     */
    $scope.permissions = null;

    /**
     * All available connection group attributes. This is only the set of
     * attribute definitions, organized as logical groupings of attributes, not
     * attribute values.
     *
     * @type Form[]
     */
    $scope.attributes = null;

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
            && $scope.permissions              !== null
            && $scope.attributes               !== null
            && $scope.canSaveConnectionGroup   !== null
            && $scope.canDeleteConnectionGroup !== null;

    };
    
    // Pull connection group attribute schema
    schemaService.getConnectionGroupAttributes($scope.selectedDataSource)
    .success(function attributesReceived(attributes) {
        $scope.attributes = attributes;
    });

    // Query the user's permissions for the current connection group
    permissionService.getPermissions($scope.selectedDataSource, authenticationService.getCurrentUsername())
    .success(function permissionsReceived(permissions) {
                
        $scope.permissions = permissions;
                        
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
    connectionGroupService.getConnectionGroupTree(
        $scope.selectedDataSource,
        ConnectionGroup.ROOT_IDENTIFIER,
        [PermissionSet.ObjectPermissionType.ADMINISTER]
    )
    .success(function connectionGroupReceived(rootGroup) {
        $scope.rootGroup = rootGroup;
    });

    // If we are editing an existing connection group, pull its data
    if (identifier) {
        connectionGroupService.getConnectionGroup($scope.selectedDataSource, identifier)
        .success(function connectionGroupReceived(connectionGroup) {
            $scope.connectionGroup = connectionGroup;
        });
    }

    // If we are creating a new connection group, populate skeleton connection group data
    else
        $scope.connectionGroup = new ConnectionGroup({
            parentIdentifier : $location.search().parent
        });

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
     * Returns whether the current user can change/set all connection group
     * attributes for the connection group being edited, regardless of whether
     * those attributes are already explicitly associated with that connection
     * group.
     *
     * @returns {Boolean}
     *     true if the current user can change all attributes for the
     *     connection group being edited, regardless of whether those
     *     attributes are already explicitly associated with that connection
     *     group, false otherwise.
     */
    $scope.canChangeAllAttributes = function canChangeAllAttributes() {

        // All attributes can be set if we are creating the connection group
        return !identifier;

    };

    /**
     * Cancels all pending edits, returning to the management page.
     */
    $scope.cancel = function cancel() {
        $location.path('/settings/' + encodeURIComponent($scope.selectedDataSource) + '/connections');
    };
   
    /**
     * Saves the connection group, creating a new connection group or updating
     * the existing connection group.
     */
    $scope.saveConnectionGroup = function saveConnectionGroup() {

        // Save the connection
        connectionGroupService.saveConnectionGroup($scope.selectedDataSource, $scope.connectionGroup)
        .success(function savedConnectionGroup() {
            $location.path('/settings/' + encodeURIComponent($scope.selectedDataSource) + '/connections');
        })

        // Notify of any errors
        .error(function connectionGroupSaveFailed(error) {
            guacNotification.showStatus({
                'className'  : 'error',
                'title'      : 'MANAGE_CONNECTION_GROUP.DIALOG_HEADER_ERROR',
                'text'       : error.translatableMessage,
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
            guacNotification.showStatus(false);
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
            guacNotification.showStatus(false);
        }
    };

    /**
     * Immediately deletes the current connection group, without prompting the
     * user for confirmation.
     */
    var deleteConnectionGroupImmediately = function deleteConnectionGroupImmediately() {

        // Delete the connection group
        connectionGroupService.deleteConnectionGroup($scope.selectedDataSource, $scope.connectionGroup)
        .success(function deletedConnectionGroup() {
            $location.path('/settings/' + encodeURIComponent($scope.selectedDataSource) + '/connections');
        })

        // Notify of any errors
        .error(function connectionGroupDeletionFailed(error) {
            guacNotification.showStatus({
                'className'  : 'error',
                'title'      : 'MANAGE_CONNECTION_GROUP.DIALOG_HEADER_ERROR',
                'text'       : error.translatableMessage,
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
        guacNotification.showStatus({
            'title'      : 'MANAGE_CONNECTION_GROUP.DIALOG_HEADER_CONFIRM_DELETE',
            'text'       : {
                key : 'MANAGE_CONNECTION_GROUP.TEXT_CONFIRM_DELETE'
            },
            'actions'    : [ DELETE_ACTION, CANCEL_ACTION]
        });

    };

}]);
