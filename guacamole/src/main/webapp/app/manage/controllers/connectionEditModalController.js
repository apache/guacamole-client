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
 * The controller for the connection edit modal.
 */
angular.module('manage').controller('connectionEditModalController', ['$scope', '$injector', 
        function connectionEditModalController($scope, $injector) {
            
    var connectionEditModal             = $injector.get('connectionEditModal');
    var connectionService               = $injector.get('connectionService');
    var Connection                      = $injector.get('Connection');
    var HistoryEntryWrapper             = $injector.get('HistoryEntryWrapper');
    
    // Make a copy of the old connection so that we can copy over the changes when done
    var oldConnection = $scope.connection;
    
    // Copy data into a new conection object in case the user doesn't want to save
    $scope.connection = new Connection($scope.connection);
    $scope.connection.parameters = {};
    
    var newConnection = !$scope.connection.identifier;
    
    // Set it to VNC by default
    if(!$scope.connection.protocol)
        $scope.connection.protocol = "vnc";
    
    $scope.historyEntryWrappers = [];
    
    // Wrap all the history entries
    if (!newConnection) {

        connectionService.getConnectionHistory($scope.connection.identifier).success(function wrapHistoryEntries(historyEntries) {
            historyEntries.forEach(function wrapHistoryEntry(historyEntry) {
               $scope.historyEntryWrappers.push(new HistoryEntryWrapper(historyEntry)); 
            });
        });

        connectionService.getConnectionParameters($scope.connection.identifier).success(function setParameters(parameters) {
            $scope.connection.parameters = parameters;
        });

    }
    
    /**
     * Close the modal.
     */
    $scope.close = function close() {
        connectionEditModal.deactivate();
    };
    
    /**
     * Save the connection and close the modal.
     */
    $scope.save = function save() {
        connectionService.saveConnection($scope.connection).success(function successfullyUpdatedConnection() {
            
            var oldParentID = oldConnection.parentIdentifier;
            var newParentID = $scope.connection.parentIdentifier;
            
            // Copy the data back to the original model
            angular.extend(oldConnection, $scope.connection);
            
            // We have to move this connection
            if(oldParentID !== newParentID)
                
                // New connections are created by default in root - don't try to move it if it's already there.
                if(newConnection && newParentID === $scope.rootGroup.identifier) {
                    $scope.moveItem($scope.connection, oldParentID, newParentID);
                } else {
                    connectionService.moveConnection($scope.connection).then(function moveConnection() {
                        $scope.moveItem($scope.connection, oldParentID, newParentID);
                    });
                }
            
            // Close the modal
            connectionEditModal.deactivate();
        });
    };
    
    /**
     * Delete the connection and close the modal.
     */
    $scope['delete'] = function deleteConnection() {
        
        // Nothing to delete if the connection is new
        var newConnection = !$scope.connection.identifier;
        if(newConnection) {
            // Close the modal
            connectionEditModal.deactivate();
            return;
        }
        
        connectionService.deleteConnection($scope.connection).success(function successfullyDeletedConnection() {
            var oldParentID = oldConnection.parentIdentifier;
            
            // We have to remove this connection from the heirarchy
            $scope.moveItem($scope.connection, oldParentID);
            
            // Close the modal
            connectionEditModal.deactivate();
        });
    };

}]);



