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
    var connectionGroupService = $injector.get('connectionGroupService');
    
    /**
     * An action to be provided along with the object sent to showStatus which
     * closes the currently-shown status dialog.
     */
    var ACKNOWLEDGE_ACTION = {
        name        : "manage.error.action.acknowledge",
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

    // Pull connection group hierarchy
    connectionGroupService.getConnectionGroupTree(ConnectionGroup.ROOT_IDENTIFIER, PermissionSet.ObjectPermissionType.UPDATE)
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
            label: "organizational",
            value: ConnectionGroup.Type.ORGANIZATIONAL
        },
        {
            label : "balancing",
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
                'title'      : 'manage.error.title',
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
        name        : "manage.edit.connectionGroup.delete",
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
        name        : "manage.edit.connectionGroup.cancel",
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
                'title'      : 'manage.error.title',
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
            'title'      : 'manage.edit.connectionGroup.confirmDelete.title',
            'text'       : 'manage.edit.connectionGroup.confirmDelete.text',
            'actions'    : [ DELETE_ACTION, CANCEL_ACTION]
        });

    };

}]);
