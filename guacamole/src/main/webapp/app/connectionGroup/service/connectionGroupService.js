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
 * A service for performing useful connection group related functionaltiy.
 */
angular.module('connectionGroup').factory('connectionGroupService', ['$injector', function connectionGroupService($injector) {
            
    var connectionGroupDAO              = $injector.get('connectionGroupDAO');
    var connectionDAO                   = $injector.get('connectionDAO');
    var permissionCheckService          = $injector.get('permissionCheckService');
    var $q                              = $injector.get('$q');
    var displayObjectPreparationService = $injector.get('displayObjectPreparationService');
            
    var service = {};
        
    // Add all groups from this group to the parent group child list
    function addToParent(connectionGroup, parentGroup, context, includeConnections) {
        
        // Include connections by default
        if(typeof includeConnections === 'undefined')
            includeConnections = true;
        
        parentGroup.children.push(connectionGroup);
        
        // Prepare this group for display
        displayObjectPreparationService.prepareConnectionGroup(connectionGroup);
        
        if(includeConnections) {
            // Get all connections in the group and add them under this connection group
            context.openRequest();
            connectionDAO.getConnections(connectionGroup.identifier).success(function fetchConnections(connections) {
                for(var i = 0; i < connections.length; i++) {
                    connections[i].isConnection = true;
                    connectionGroup.children.push(connections[i]);
                }
                context.closeRequest();
            });
        }

        // Get all connection groups in the group and repeat
        context.openRequest();
        connectionGroupDAO.getConnectionGroups(connectionGroup.identifier).success(function fetchConnectionGroups(connectionGroups) {
            for(var i = 0; i < connectionGroups.length; i++) {
                addToParent(connectionGroups[i], connectionGroup, context, includeConnections);
            }
            context.closeRequest();
        });
    }
    
    /**
     * Queries all connections and connection groups under the connection group 
     * with the provided parent ID, and returns them in a heirarchical structure
     * with convinient display properties set on the objects.
     * 
     * @param {array} items The root list of connections and groups. Should be an
     *                      initally empty array that will get filled in as the
     *                      connections and groups are loaded.
     * 
     * @param {string} parentID The parent ID for the connection group.
     *                          If not passed in, it will begin with 
     *                          the root connection group.
     * 
     * @param {boolean} includeConnections Whether or not to include connections
     *                                     in the structure. Defaults to true.
     * 
     * @param {boolean} includeRoot Whether or not to include the root connection group
     *                              in the structure. Defaults to false.
     *                          
     * @return {promise} A promise that will be fulfilled when all connections
     *                   and groups have been loaded.
     */
    service.getAllGroupsAndConnections = function getAllGroupsAndConnections(items, parentID, includeConnections, includeRoot) {
        
        // Include connections by default
        if(typeof includeConnections === 'undefined')
            includeConnections = true;
        
        var context = {
            // The number of requets to the server currently open
            openRequests        : 0,

            // Create the promise
            finishedFetching    : $q.defer(),
            
            // Notify the caller that the promise has been completed
            complete            : function complete() {
                this.finishedFetching.resolve(items);
            },
            
            /**
             * Indicate that a request has been started.
             */ 
            openRequest         : function openRequest() {
                this.openRequests++;
            },
            
            /**
             * Indicate that a request has been completed. If this was the last
             * open request, fulfill the promise.
             */ 
            closeRequest        : function closeRequest() {
                if(--this.openRequests === 0)
                    this.complete();
            }
        };
        
        // Include the root only if it was asked for
        if(includeRoot) {
            context.openRequest();
            connectionGroupDAO.getConnectionGroup(parentID).success(function setRootGroup (rootGroup) {
                items.push(rootGroup);
                rootGroup.children = [];
                getChildrenOfRootGroup(rootGroup.children);
                context.closeRequest();
            });
        } else {
            getChildrenOfRootGroup(items);
        }
        
        // Get the children of the root group
        function getChildrenOfRootGroup(children) {
            context.openRequest();
            connectionGroupDAO.getConnectionGroups(parentID).success(function fetchRootConnectionGroups(connectionGroups) {
                for(var i = 0; i < connectionGroups.length; i++) {
                    addToParent(connectionGroups[i], {children: children}, context, includeConnections);
                }

                if(includeConnections) {
                    // Get all connections in the root group and add them under this connection group
                    context.openRequest();
                    connectionDAO.getConnections().success(function fetchRootConnections(connections) {
                        for(var i = 0; i < connections.length; i++) {
                            
                            // Prepare this connection for display
                            displayObjectPreparationService.prepareConnection(connections[i]);
                            
                            children.push(connections[i]);
                        }
                        context.closeRequest();
                    });
                }
                
                context.closeRequest();
            });     
        }
        
        // Return the promise
        return context.finishedFetching.promise;
    };
    
    
    /**
     * Filters the list of connections and groups using the provided permissions.
     * 
     * @param {array} items The heirarchical list of groups and connections.
     * 
     * @param {object} permissionList The list of permissions to use 
     *                                when filtering.
     * 
     * @param {object} permissionCriteria A map of object type to permission type(s)
     *                                    required for that object type. 
     *                          
     * @return {array} The filtered list.
     */
    service.filterConnectionsAndGroupByPermission = function filterConnectionsAndGroupByPermission(items, permissionList, permissionCriteria) {
        var requiredConnectionPermission      = permissionCriteria.CONNECTION;
        var requiredConnectionGroupPermission = permissionCriteria.CONNECTION_GROUP;
        
        for(var i = 0; i < items.length; i++) {
            var item = items[i];
            
            if(item.isConnection && requiredConnectionPermission) {
                
                /*
                 * If item is a connection and a permission is required for this
                 * item, check now to see if the permission exists. If not,
                 * remove the item.
                 */
                if(!permissionCheckService.checkPermission(permissionList, 
                        "CONNECTION", item.identifier, requiredConnectionPermission)) {
                    items.splice(i, 1);
                    continue;
                } 
            } 
            else {
                
                /*
                 * If item is a group and a permission is required for this
                 * item, check now to see if the permission exists. If not,
                 * remove the item.
                 */
                if(requiredConnectionGroupPermission) {
                    if(!permissionCheckService.checkPermission(permissionList, 
                            "CONNECTION_GROUP", item.identifier, requiredConnectionGroupPermission)) {
                        items.splice(i, 1);
                        continue;
                    }    
                }
                
                // Filter the children of this connection group as well
                if(item.children && item.children.length)
                    service.filterConnectionsAndGroupByPermission(items.children);
            }
        }
        
        return items;
        
    };
    
    return service;
}]);
