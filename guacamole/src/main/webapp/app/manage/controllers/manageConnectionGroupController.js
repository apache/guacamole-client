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
 * The controller for the connection group edit modal.
 */
angular.module('manage').controller('manageConnectionGroupController', ['$scope', '$injector', 
        function manageConnectionGroupController($scope, $injector) {
            
    // Required types
    var ConnectionGroup = $injector.get('ConnectionGroup');
    var PermissionSet   = $injector.get('PermissionSet');

    // Required services
    var connectionGroupService = $injector.get('connectionGroupService');
    var $routeParams           = $injector.get('$routeParams');
    
    // Copy data into a new conection group object in case the user doesn't want to save
    var identifier = $routeParams.id;

    // Pull connection group data
    if (identifier) {
        connectionGroupService.getConnectionGroup(identifier).success(function connectionGroupReceived(connectionGroup) {
            $scope.connectionGroup = connectionGroup;
        });
    }

    else
        $scope.connectionGroup = new ConnectionGroup();

    connectionGroupService.getConnectionGroupTree(ConnectionGroup.ROOT_IDENTIFIER, PermissionSet.ObjectPermissionType.UPDATE)
    .success(function connectionGroupReceived(rootGroup) {
        $scope.rootGroup = rootGroup;
        $scope.loadingConnections = false; 
    });
   
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
     * Close the modal.
     */
    $scope.close = function close() {
        connectionGroupEditModal.deactivate();
    };
    
    /**
     * Save the connection and close the modal.
     */
    $scope.save = function save() {
        connectionGroupService.saveConnectionGroup($scope.connectionGroup).success(function successfullyUpdatedConnectionGroup() {
            
            var oldParentID = oldConnectionGroup.parentIdentifier;
            var newParentID = $scope.connectionGroup.parentIdentifier;
            
            // Copy the data back to the original model
            angular.extend(oldConnectionGroup, $scope.connectionGroup);
            
            // New groups are created by default in root - don't try to move it if it's already there.
            if(newConnectionGroup && newParentID === $scope.rootGroup.identifier) {
                $scope.moveItem($scope.connectionGroup, oldParentID, newParentID);
            } else {
                connectionGroupService.moveConnectionGroup($scope.connectionGroup).then(function moveConnectionGroup() {
                    $scope.moveItem($scope.connectionGroup, oldParentID, newParentID);
                });
            }
            
            // Close the modal
            connectionGroupEditModal.deactivate();
        });
    };
    
    /**
     * Delete the connection and close the modal.
     */
    $scope['delete'] = function deleteConnectionGroup() {
        
        // Nothing to delete if the connection is new
        if(newConnectionGroup)
            // Close the modal
            connectionGroupEditModal.deactivate();
        
        connectionGroupService.deleteConnectionGroup($scope.connectionGroup).success(function successfullyDeletedConnectionGroup() {
            var oldParentID = oldConnectionGroup.parentIdentifier;
            
            // We have to remove this connection group from the heirarchy
            $scope.moveItem($scope.connectionGroup, oldParentID);
            
            // Close the modal
            connectionGroupEditModal.deactivate();
        });
    }
}]);



