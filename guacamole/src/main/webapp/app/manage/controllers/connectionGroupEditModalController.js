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
angular.module('manage').controller('connectionGroupEditModalController', ['$scope', '$injector', 
        function connectionEditModalController($scope, $injector) {
            
    var connectionGroupEditModal        = $injector.get('connectionGroupEditModal');
    var connectionGroupDAO              = $injector.get('connectionGroupDAO');
    var displayObjectPreparationService = $injector.get('displayObjectPreparationService');
    
    // Make a copy of the old connection group so that we can copy over the changes when done
    var oldConnectionGroup = $scope.connectionGroup;
    
    // Copy data into a new conection group object in case the user doesn't want to save
    $scope.connectionGroup = angular.copy($scope.connectionGroup);
    
    var newConnectionGroup = !$scope.connectionGroup.identifier;
    
    $scope.types = [
        {
            label: "organizational",
            value: "ORGANIZATIONAL"
        },
        {
            label: "balancing",
            value: "BALANCING"
        }
    ];
    
    // Set it to organizational by default
    if(!$scope.connectionGroup.type)
        $scope.connectionGroup.type = $scope.types[0].value;
    
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
        connectionGroupDAO.saveConnectionGroup($scope.connectionGroup).success(function successfullyUpdatedConnectionGroup() {
            
            // Prepare this connection group for display
            displayObjectPreparationService.prepareConnectionGroup($scope.connectionGroup);
            
            var oldParentID = oldConnectionGroup.parentIdentifier;
            var newParentID = $scope.connectionGroup.parentIdentifier;
            
            // Copy the data back to the original model
            angular.extend(oldConnectionGroup, $scope.connectionGroup);
            
            // New groups are created by default in root - don't try to move it if it's already there.
            if(newConnectionGroup && newParentID === $scope.rootGroup.identifier) {
                $scope.moveItem($scope.connectionGroup, oldParentID, newParentID);
            } else {
                connectionGroupDAO.moveConnectionGroup($scope.connectionGroup).then(function moveConnectionGroup() {
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
        
        connectionGroupDAO.deleteConnectionGroup($scope.connectionGroup).success(function successfullyDeletedConnectionGroup() {
            var oldParentID = oldConnectionGroup.parentIdentifier;
            
            // We have to remove this connection group from the heirarchy
            $scope.moveItem($scope.connectionGroup, oldParentID);
            
            // Close the modal
            connectionGroupEditModal.deactivate();
        });
    }
}]);



