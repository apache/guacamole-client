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

    // Get required types
    var ConnectionGroup  = $injector.get('ConnectionGroup');
    var ClientIdentifier = $injector.get('ClientIdentifier');
            
    // Get required services
    var authenticationService  = $injector.get('authenticationService');
    var connectionGroupService = $injector.get('connectionGroupService');
    var dataSourceService      = $injector.get('dataSourceService');

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

        return $scope.rootConnectionGroup !== null;

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
            if (item.isConnection)
                return ClientIdentifier.toString({
                    dataSource : item.dataSource,
                    type       : ClientIdentifier.Types.CONNECTION,
                    id         : item.identifier
                });

            // If the item is a connection, generate a connection group identifier
            if (item.isConnectionGroup)
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
    });

}]);
