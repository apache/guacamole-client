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
 * A controller for choosing a connection from a list of available connections.
 */
angular.module('templateExt').controller('connectionChooserController',
        ['$scope', '$injector', function connectionChooserController($scope, $injector) {

    // Required types
    var ConnectionGroup          = $injector.get('ConnectionGroup');

    // Required services
    var $log                     = $injector.get('$log');
    var $routeParams             = $injector.get('$routeParams');
    var connectionGroupService   = $injector.get('connectionGroupService');

    /**
     * Map of unique identifiers to their corresponding connection
     * groups.
     *
     * @type Object.<String, GroupListItem>
     */
    var connectionGroups = {};
    
    /**
     * Map of unique identifiers to their correspnoding connections.
     * 
     * @type Object.<String, GroupListItem>
     */
    var connections = {};
    
    /**
     * The identifier of the connection being edited. If a new
     * connection is being created, this will not be defined.
     *
     * @type String
     */
    var identifier = $routeParams.id;
    
    /**
     * The current data source that is being used to edit the connection,
     * from which other available connections will be retrieved to use
     * as possible template values.
     * 
     * @type String
     */
    var dataSource = $routeParams.dataSource;

    /**
     * Recursively traverses the given connection group and all
     * children, storing each encountered connection group within the
     * connectionGroups map by its identifier.
     *
     * @param {GroupListItem} group
     *     The connection group to traverse.
     */
    var mapConnectionsAndGroups = function mapConnectionsAndGroups(group) {
        
        // Map given group
        connectionGroups[group.identifier] = group;

        if (group.childConnections)
            group.childConnections.forEach(connection => {
                // Skip this connection
                if (connection.identifier === identifier)
                    return;
                connections[connection.identifier] = connection;
            });

        // Map all child groups
        if (group.childConnectionGroups)
            group.childConnectionGroups.forEach(mapConnectionsAndGroups);
        

    };

    /**
     * Whether the connection list menu is currently open.
     * 
     * @type Boolean
     */
    $scope.menuOpen = false;

    /**
     * The human-readable name of the currently-chosen connection.
     * 
     * @type String
     */
    $scope.chosenConnectionName = null;
    
    /**
     * The rootGroup for the currently-selected data source.
     * 
     * @type ConnectionGroup
     */
    $scope.rootGroups = {};
    
    /**
     * Retrive the root connection group for this data source and save it.
     */
    connectionGroupService.getConnectionGroupTree($routeParams.dataSource, ConnectionGroup.ROOT_IDENTIFIER)
            .then(function connectionGroupDataRetrieved(rootGroup) {
        
        connections = {}
        connectionGroups = {};
        
        // Map all known groups
        mapConnectionsAndGroups(rootGroup);
        
        // Wrap root group in map
        $scope.rootGroups = {};
        $scope.rootGroups[dataSource] = rootGroup;
        
        // Find the currently-selected connection.
        if ($scope.model && $scope.model in connections)
            $scope.chosenConnectionName = connections[$scope.model].name;
        else
            $scope.chosenConnectionName = '--No Template Chosen--';

    });

    /**
     * Toggle the current state of the menu listing connections.
     * If the menu is currently open, it will be closed. If currently
     * closed, it will be opened.
     */
    $scope.toggleMenu = function toggleMenu() {
        $scope.menuOpen = !$scope.menuOpen;
    };

    // Expose selection function to connection list template
    $scope.connectionListContext = {

        /**
         * Selects the given connection item.
         *
         * @param {ConnectionListItem} item
         *     The chosen item.
         */
        chooseConnection : function chooseConnection(item) {

            // Connection cannot set itself as the template
            if (identifier && identifier === item.identifier)
                return false;

            // Record new parent
            $scope.model = item.identifier;
            $scope.chosenConnectionName = item.name;

            // Close menu
            $scope.menuOpen = false;

        }

    };
    
}]);