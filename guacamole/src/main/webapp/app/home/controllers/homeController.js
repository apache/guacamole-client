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
 * The controller for the home page.
 */
angular.module('home').controller('homeController', ['$scope', '$injector', 
        function homeController($scope, $injector) {

    // Get required types
    var ConnectionGroup  = $injector.get('ConnectionGroup');
    var ClientIdentifier = $injector.get('ClientIdentifier');
    var GroupListItem    = $injector.get('GroupListItem');
            
    // Get required services
    var authenticationService  = $injector.get('authenticationService');
    var connectionGroupService = $injector.get('connectionGroupService');
    var dataSourceService      = $injector.get('dataSourceService');
    var requestService         = $injector.get('requestService');

    /**
     * Map of data source identifier to the root connection group of that data
     * source, or null if the connection group hierarchy has not yet been
     * loaded.
     *
     * @type Object.<String, ConnectionGroup>
     */
    $scope.rootConnectionGroups = null;

    /**
     * Array of all connection properties that are filterable.
     *
     * @type String[]
     */
    $scope.filteredConnectionProperties = [
        'name'
    ];

    /**
     * Array of all connection group properties that are filterable.
     *
     * @type String[]
     */
    $scope.filteredConnectionGroupProperties = [
        'name'
    ];

    /**
     * Returns whether critical data has completed being loaded.
     *
     * @returns {Boolean}
     *     true if enough data has been loaded for the user interface to be
     *     useful, false otherwise.
     */
    $scope.isLoaded = function isLoaded() {

        return $scope.rootConnectionGroups !== null;

    };

    /**
     * Object passed to the guacGroupList directive, providing context-specific
     * functions or data.
     */
    $scope.context = {

        /**
         * Returns the unique string identifier which must be used when
         * connecting to a connection or connection group represented by the
         * given GroupListItem.
         *
         * @param {GroupListItem} item
         *     The GroupListItem to determine the client identifier of.
         *
         * @returns {String}
         *     The client identifier associated with the connection or
         *     connection group represented by the given GroupListItem, or null
         *     if the GroupListItem cannot have an associated client
         *     identifier.
         */
        getClientIdentifier : function getClientIdentifier(item) {

            // If the item is a connection, generate a connection identifier
            if (item.type === GroupListItem.Type.CONNECTION)
                return ClientIdentifier.toString({
                    dataSource : item.dataSource,
                    type       : ClientIdentifier.Types.CONNECTION,
                    id         : item.identifier
                });

            // If the item is a connection group, generate a connection group identifier
            if (item.type === GroupListItem.Type.CONNECTION_GROUP)
                return ClientIdentifier.toString({
                    dataSource : item.dataSource,
                    type       : ClientIdentifier.Types.CONNECTION_GROUP,
                    id         : item.identifier
                });

            // Otherwise, no such identifier can exist
            return null;

        }

    };

    // Retrieve root groups and all descendants
    dataSourceService.apply(
        connectionGroupService.getConnectionGroupTree,
        authenticationService.getAvailableDataSources(),
        ConnectionGroup.ROOT_IDENTIFIER
    )
    .then(function rootGroupsRetrieved(rootConnectionGroups) {
        $scope.rootConnectionGroups = rootConnectionGroups;
    }, requestService.DIE);

}]);
