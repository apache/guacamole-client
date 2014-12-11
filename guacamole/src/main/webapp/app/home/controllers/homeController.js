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
 * The controller for the home page.
 */
angular.module('home').controller('homeController', ['$scope', '$injector', 
        function homeController($scope, $injector) {
            
    // The parameter name for getting the history from local storage
    var GUAC_HISTORY_STORAGE_KEY = "GUAC_HISTORY";
                                    
    // Get the dependencies commonJS style
    var legacyConnectionGroupService  = $injector.get("legacyConnectionGroupService");
    var guacHistory             = $injector.get("guacHistory");
    
    // All the connections and connection groups in root
    $scope.connectionsAndGroups = [];
    
    // All valid recent connections
    $scope.recentConnections = [];
    
    // Set status to loading until we have all the connections and groups loaded
    $scope.loading = true;
    
    /* Fetch all connections and groups, then find which recent connections
     * still refer to valid connections and groups.
     */
    legacyConnectionGroupService.getAllGroupsAndConnections($scope.connectionsAndGroups)
    .then(function findRecentConnections() {
        
        // TODONT: Munch the guacHistory recentConnections list into a legacy-style object
        var recentConnections = {};
        for (var i=0; i < guacHistory.recentConnections.length; i++) {
            var entry = guacHistory.recentConnections[i];
            recentConnections[encodeURIComponent(entry.id)] = {
                id        : entry.id,
                thumbnail : entry.thumbnail
            };
        }
        
        // Figure out which recent connection entries are valid
        $scope.connectionsAndGroups.forEach(function findValidEntries (connectionOrGroup) {
            
            var type = connectionOrGroup.isConnection ? "c" : "g";
            
            // Find the unique ID to index into the recent connections
            var uniqueId = encodeURIComponent(
                type + "/" + connectionOrGroup.identifier
            );
    
            /* 
             * If it's a valid recent connection, add it to the list,
             * along with enough information to make a connection url.
             */
            var recentConnection = recentConnections[uniqueId];
            if(recentConnection) {
                recentConnection.type = type;
                recentConnection.id   = connectionOrGroup.identifier;
                $scope.recentConnections.push(recentConnection);
            }
        });
        
        $scope.loading = false;
    });
    
    /**
     * Toggle the open/closed status of the connectionGroup.
     * 
     * @param {object} connectionGroup The connection group to toggle.
     */
    $scope.toggleExpanded = function toggleExpanded(connectionGroup) {
        connectionGroup.expanded = !connectionGroup.expanded;
    };
    
}]);
