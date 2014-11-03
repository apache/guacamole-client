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
angular.module('manage').controller('userEditModalController', ['$scope', '$injector', 
        function userEditModalController($scope, $injector) {
            
    var userEditModal                   = $injector.get('userEditModal');
    var userDAO                         = $injector.get('userDAO');
    var permissionDAO                   = $injector.get('permissionDAO');
    
    // Make a copy of the old user so that we can copy over the changes when done
    var oldUser = $scope.user;
    
    // Copy data into a new conection object in case the user doesn't want to save
    $scope.user = angular.copy($scope.user);
    
    /**
     * Close the modal.
     */
    $scope.close = function close() {
        userEditModal.deactivate();
    };
    
    /*
     * All the permissions that have been modified since this modal was opened.
     * Maps of type or id to value.
     */ 
    $scope.modifiedSystemPermissions = {};
    $scope.modifiedConnectionPermissions = {};
    $scope.modifiedConnectionGroupPermissions = {};
    
    $scope.markSystemPermissionModified = function markSystemPermissionModified(type) {
        $scope.modifiedSystemPermissions[type] = $scope.systemPermissions[type];
    };
    
    $scope.markConnectionPermissionModified = function markConnectionPermissionModified(id) {
        $scope.modifiedConnectionPermissions[id] = $scope.connectionPermissions[id];
    };
    
    $scope.markConnectionGroupPermissionModified = function markConnectionGroupPermissionModified(id) {
        $scope.modifiedConnectionGroupPermissions[id] = $scope.connectionGroupPermissions[id];
    };
    
    /**
     * Save the user and close the modal.
     */
    $scope.save = function save() {
        
        if($scope.passwordMatch !== $scope.user.password) {
            //TODO: Display an error
            return;
        }
        
        userDAO.saveUser($scope.user).success(function successfullyUpdatedUser() {
            
            //Figure out what permissions have changed
            var connectionPermissionsToCreate = [],
                connectionPermissionsToDelete = [],
                connectionGroupPermissionsToCreate = [],
                connectionGroupPermissionsToDelete = [],
                systemPermissionsToCreate = [],
                systemPermissionsToDelete = [];
            
            for(var type in $scope.modifiedSystemPermissions) {
                // It was added
                if($scope.modifiedSystemPermissions[type] && !originalSystemPermissions[type]) {
                    systemPermissionsToCreate.push(type);
                }
                // It was removed
                else if(!$scope.modifiedSystemPermissions[type] && originalSystemPermissions[type]) {
                    systemPermissionsToDelete.push(type);
                }
            }
            
            for(var id in $scope.modifiedConnectionPermissions) {
                // It was added
                if($scope.modifiedConnectionPermissions[id] && !originalConnectionPermissions[id]) {
                    connectionPermissionsToCreate.push(id);
                }
                // It was removed
                else if(!$scope.modifiedConnectionPermissions[id] && originalConnectionPermissions[id]) {
                    connectionPermissionsToDelete.push(id);
                }
            }
            
            for(var id in $scope.modifiedConnectionGroupPermissions) {
                // It was added
                if($scope.modifiedConnectionGroupPermissions[id] && !originalConnectionGroupPermissions[id]) {
                    connectionGroupPermissionsToCreate.push(id);
                }
                // It was removed
                else if(!$scope.modifiedConnectionGroupPermissions[id] && originalConnectionGroupPermissions[id]) {
                    connectionGroupPermissionsToDelete.push(id);
                }
            }
            
            var permissionsToAdd = [];
            var permissionsToRemove = [];
            
            // Create new connection permissions
            for(var i = 0; i < connectionPermissionsToCreate.length; i++) {
                permissionsToAdd.push({
                    objectType :        "CONNECTION",
                    objectIdentifier :  connectionPermissionsToCreate[i],
                    permissionType :    "READ"
                });
            }
            
            // Delete old connection permissions
            for(var i = 0; i < connectionPermissionsToDelete.length; i++) {
                permissionsToRemove.push({
                    objectType :        "CONNECTION",
                    objectIdentifier :  connectionPermissionsToDelete[i],
                    permissionType :    "READ"
                });
            }
            
            // Create new connection group permissions
            for(var i = 0; i < connectionGroupPermissionsToCreate.length; i++) {
                permissionsToAdd.push({
                    objectType :        "CONNECTION_GROUP",
                    objectIdentifier :  connectionGroupPermissionsToCreate[i],
                    permissionType :    "READ"
                });
            }
            
            // Delete old connection group permissions
            for(var i = 0; i < connectionGroupPermissionsToDelete.length; i++) {
                permissionsToRemove.push({
                    objectType :        "CONNECTION_GROUP",
                    objectIdentifier :  connectionGroupPermissionsToDelete[i],
                    permissionType :    "READ"
                });
            }
            
            // Create new system permissions
            for(var i = 0; i < systemPermissionsToCreate.length; i++) {
                permissionsToAdd.push({
                    objectType :        "SYSTEM",
                    permissionType :    systemPermissionsToCreate[i]
                });
            }
            
            // Delete old system permissions
            for(var i = 0; i < systemPermissionsToDelete.length; i++) {
                permissionsToRemove.push({
                    objectType :        "SYSTEM",
                    permissionType :    systemPermissionsToDelete[i]
                });
            }
        
            function completeSaveProcess() {
                // Close the modal
                userEditModal.deactivate();
            }
            
            function handleFailure() {
                //TODO: Handle the permission API call failure
            }
            
            if(permissionsToAdd.length || permissionsToRemove.length) {
                // Make the call to update the permissions
                permissionDAO.patchPermissions(
                        $scope.user.username, permissionsToAdd, permissionsToRemove)
                        .success(completeSaveProcess).error(handleFailure);
            } else {
                completeSaveProcess();
            }
            
        });
    };
    
    $scope.permissions = [];

    // Maps of connection and connection group IDs to access permission booleans
    $scope.connectionPermissions = {};
    $scope.connectionGroupPermissions = {};
    $scope.systemPermissions = {};
    
    // The original permissions to compare against 
    var originalConnectionPermissions,
        originalConnectionGroupPermissions,
        originalSystemPermissions;
    
    // Get the permissions for the user we are editing
    permissionDAO.getPermissions($scope.user.username).success(function gotPermissions(permissions) {
        $scope.permissions = permissions;
        
        // Figure out if the user has any system level permissions
        for(var i = 0; i < $scope.permissions.length; i++) {
            var permission = $scope.permissions[i];
            if(permission.objectType === "SYSTEM") {
                
                $scope.systemPermissions[permission.permissionType] = true;
                
            // Only READ permission is editable via this UI
            } else if (permission.permissionType === "READ") {
                switch(permission.objectType) {
                    case "CONNECTION":
                        $scope.connectionPermissions[permission.objectIdentifier] = true;
                        break;
                    case "CONNECTION_GROUP":
                        $scope.connectionGroupPermissions[permission.objectIdentifier] = true;
                        break;
                }
            }
        }
        
        // Copy the original permissions so we can compare later
        originalConnectionPermissions = angular.copy($scope.connectionPermissions);
        originalConnectionGroupPermissions = angular.copy($scope.connectionGroupPermissions);
        originalSystemPermissions = angular.copy($scope.systemPermissions);
        
    });
    
    /**
     * Delete the user and close the modal.
     */
    $scope['delete'] = function deleteUser() {
        userDAO.deleteUser($scope.user).success(function successfullyDeletedUser() {
            
            // Remove the user from the list
            $scope.removeUser($scope.user);
            
            // Close the modal
            userEditModal.deactivate();
        });
    }
    
    /**
     * Toggle the open/closed status of the connectionGroup.
     * 
     * @param {object} connectionGroup The connection group to toggle.
     */
    $scope.toggleExpanded = function toggleExpanded(connectionGroup) {
        connectionGroup.expanded = !connectionGroup.expanded;
    };
}]);



