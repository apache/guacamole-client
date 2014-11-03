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
 * The controller for the administration page.
 */
angular.module('manage').controller('manageController', ['$scope', '$injector', 
        function manageController($scope, $injector) {
            
    // Get the dependencies commonJS style
    var connectionGroupService      = $injector.get('connectionGroupService');
    var connectionEditModal         = $injector.get('connectionEditModal');
    var connectionGroupEditModal    = $injector.get('connectionGroupEditModal');
    var userEditModal               = $injector.get('userEditModal');
    var protocolDAO                 = $injector.get('protocolDAO');
    var userDAO                     = $injector.get('userDAO');
    var userService                 = $injector.get('userService');
    
    // All the connections and connection groups in root
    $scope.connectionsAndGroups = [];
    
    // All users that the current user has permission to edit
    $scope.users = [];
    
    $scope.basicPermissionsLoaded.then(function basicPermissionsHaveBeenLoaded() {
        connectionGroupService.getAllGroupsAndConnections([], undefined, true, true).then(function filterConnectionsAndGroups(rootGroupList) {
            $scope.rootGroup = rootGroupList[0];
            $scope.connectionsAndGroups = $scope.rootGroup.children;
            
            // Filter the items to only include ones that we have UPDATE for
            if(!$scope.currentUserIsAdmin) {
                connectionGroupService.filterConnectionsAndGroupByPermission(
                    $scope.connectionsAndGroups,
                    $scope.currentUserPermissions,
                    {
                        'CONNECTION':       'UPDATE',
                        'CONNECTION_GROUP': 'UPDATE'
                    }
                );
            }
        });
        
        userDAO.getUsers().success(function filterEditableUsers(users) {
            $scope.users = users;
            
            // Filter the users to only include ones that we have UPDATE for
            if(!$scope.currentUserIsAdmin) {
                userService.filterUsersByPermission(
                    $scope.users,
                    $scope.currentUserPermissions,
                    'UPDATE'
                );
            }
        });
    });
    
    /**
     * Move the connection or connection group within the group heirarchy, 
     * initially place a new item, or remove an item from the heirarchy.
     * @param {object} item The connection or connection group to move.
     * @param {string} fromID The ID of the group to move the item from, if relevant.
     * @param {string} toID The ID of the group to move the item to, if relevant.
     */
    $scope.moveItem = function moveItem(item, fromID, toID) {
        
        // Remove the item from the old group, if there was one
        if(fromID) {
            var oldParent   = findGroup($scope.rootGroup, fromID),
                oldChildren = oldParent.children;
            
            // Find and remove the item from the old group
            for(var i = 0; i < oldChildren.length; i++) {
                var child = oldChildren[i];
                if(child.isConnection === item.isConnection &&
                        child.identifier === item.identifier) {
                    oldChildren.splice(i, 1);
                    break;
                }
            }
        }
        
        // Add the item to the new group, if there is one
        if(toID) {
            var newParent = findGroup($scope.rootGroup, toID);
            newParent.children.push(item);
        }
    };
    
    function findGroup(group, parentID) {
        // Only searching in groups
        if(group.isConnection)
            return;
        
        if(group.identifier === parentID)
            return group;
        
        for(var i = 0; i < group.children.length; i++) {
            var child = group.children[i];
            var foundGroup = findGroup(child, parentID);
            if(foundGroup) return foundGroup;
        }
    }
        
    
    $scope.protocols = {};
    
    // Get the protocol information from the server and copy it into the scope
    protocolDAO.getProtocols().success(function fetchProtocols(protocols) {
        angular.extend($scope.protocols, protocols);
    });
    
    /**
     * Toggle the open/closed status of the connectionGroup.
     * 
     * @param {object} connectionGroup The connection group to toggle.
     */
    $scope.toggleExpanded = function toggleExpanded(connectionGroup) {
        connectionGroup.expanded = !connectionGroup.expanded;
    };
    
    /**
     * Open a modal to edit the connection.
     *  
     * @param {object} connection The connection to edit.
     */
    $scope.editConnection = function editConnection(connection) {
        connectionEditModal.activate(
        {
            connection : connection, 
            protocols  : $scope.protocols,
            moveItem   : $scope.moveItem,
            rootGroup  : $scope.rootGroup
        });
    };
    
    /**
     * Open a modal to edit a new connection.
     */
    $scope.newConnection = function newConnection() {
        connectionEditModal.activate(
        {
            connection : {}, 
            protocols  : $scope.protocols,
            moveItem   : $scope.moveItem,
            rootGroup  : $scope.rootGroup
        });
    };
    
    /**
     * Open a modal to edit a new connection group.
     */
    $scope.newConnectionGroup = function newConnectionGroup() {
        connectionGroupEditModal.activate(
        {
            connectionGroup : {}, 
            moveItem        : $scope.moveItem,
            rootGroup       : $scope.rootGroup
        });
    };
    
    /**
     * Open a modal to edit the connection group.
     *  
     * @param {object} connection The connection group to edit.
     */
    $scope.editConnectionGroup = function editConnectionGroup(connectionGroup) {
        connectionGroupEditModal.activate(
        {
            connectionGroup : connectionGroup, 
            moveItem        : $scope.moveItem,
            rootGroup       : $scope.rootGroup
        });
    };
    
    // Remove the user from the current list of users
    function removeUser(user) {
        for(var i = 0; i < $scope.users.length; i++) {
            if($scope.users[i].username === user.username) {
                $scope.users.splice(i, 1);
                break;
            }
        }
    }
    
    /**
     * Open a modal to edit the user.
     *  
     * @param {object} user The user to edit.
     */
    $scope.editUser = function editUser(user) {
        userEditModal.activate(
        {
            user            : user, 
            rootGroup       : $scope.rootGroup,
            removeUser      : removeUser
        });
    };
    
    $scope.newUsername = "";
    
    /**
     * Open a modal to edit the user.
     *  
     * @param {object} user The user to edit.
     */
    $scope.newUser = function newUser() {
        if($scope.newUsername) {
            var newUser = {
                username: $scope.newUsername
            };
            
            userDAO.createUser(newUser).success(function addUserToList() {
                $scope.users.push(newUser);
            });
            
            $scope.newUsername = "";
        }
    };
    
}]);



