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
 * A directive which displays the contents of a connection group within an
 * automatically-paginated view.
 */
angular.module('groupList').directive('guacGroupList', [function guacGroupList() {

    return {
        restrict: 'E',
        replace: true,
        scope: {

            /**
             * The connection groups to display as a map of data source
             * identifier to corresponding root group.
             *
             * @type Object.<String, ConnectionGroup>
             */
            connectionGroups : '=',

            /**
             * Arbitrary object which shall be made available to the connection
             * and connection group templates within the scope as
             * <code>context</code>.
             * 
             * @type Object
             */
            context : '=',

            /**
             * The URL or ID of the Angular template to use when rendering a
             * connection. The @link{GroupListItem} associated with that
             * connection will be exposed within the scope of the template
             * as <code>item</code>, and the arbitrary context object, if any,
             * will be exposed as <code>context</code>.
             *
             * @type String
             */
            connectionTemplate : '=',

            /**
             * The URL or ID of the Angular template to use when rendering a
             * connection group. The @link{GroupListItem} associated with that
             * connection group will be exposed within the scope of the
             * template as <code>item</code>, and the arbitrary context object,
             * if any, will be exposed as <code>context</code>.
             *
             * @type String
             */
            connectionGroupTemplate : '=',

            /**
             * Whether the root of the connection group hierarchy given should
             * be shown. If false (the default), only the descendants of the
             * given connection group will be listed.
             * 
             * @type Boolean
             */
            showRootGroup : '=',

            /**
             * The maximum number of connections or groups to show per page.
             *
             * @type Number
             */
            pageSize : '='

        },

        templateUrl: 'app/groupList/templates/guacGroupList.html',
        controller: ['$scope', '$injector', function guacGroupListController($scope, $injector) {

            // Required services
            var activeConnectionService = $injector.get('activeConnectionService');
            var dataSourceService       = $injector.get('dataSourceService');

            // Required types
            var GroupListItem = $injector.get('GroupListItem');

            /**
             * Map of data source identifier to the number of active
             * connections associated with a given connection identifier.
             * If this information is unknown, or there are no active
             * connections for a given identifier, no number will be stored.
             *
             * @type Object.<String, Object.<String, Number>>
             */
            var connectionCount = {};

            /**
             * A list of all items which should appear at the root level. As
             * connections and connection groups from multiple data sources may
             * be included in a guacGroupList, there may be multiple root
             * items, even if the root connection group is shown.
             *
             * @type GroupListItem[]
             */
            $scope.rootItems = [];

            /**
             * Returns the number of active usages of a given connection.
             *
             * @param {String} dataSource
             *     The identifier of the data source containing the given
             *     connection.
             *
             * @param {Connection} connection
             *     The connection whose active connections should be counted.
             *
             * @returns {Number}
             *     The number of currently-active usages of the given
             *     connection.
             */
            var countActiveConnections = function countActiveConnections(dataSource, connection) {
                return connectionCount[dataSource][connection.identifier];
            };

            /**
             * Returns whether the given item represents a connection that can
             * be displayed. If there is no connection template, then no
             * connection is visible.
             * 
             * @param {GroupListItem} item
             *     The item to check.
             *
             * @returns {Boolean}
             *     true if the given item is a connection that can be
             *     displayed, false otherwise.
             */
            $scope.isVisibleConnection = function isVisibleConnection(item) {
                return item.isConnection && !!$scope.connectionTemplate;
            };

            /**
             * Returns whether the given item represents a connection group
             * that can be displayed. If there is no connection group template,
             * then no connection group is visible.
             * 
             * @param {GroupListItem} item
             *     The item to check.
             *
             * @returns {Boolean}
             *     true if the given item is a connection group that can be
             *     displayed, false otherwise.
             */
            $scope.isVisibleConnectionGroup = function isVisibleConnectionGroup(item) {
                return item.isConnectionGroup && !!$scope.connectionGroupTemplate;
            };

            // Set contents whenever the connection group is assigned or changed
            $scope.$watch('connectionGroups', function setContents(connectionGroups) {

                // Reset stored data
                var dataSources = [];
                $scope.rootItems = [];
                connectionCount = {};

                // If connection groups are given, add them to the interface
                if (connectionGroups) {

                    // Add each provided connection group
                    angular.forEach(connectionGroups, function addConnectionGroup(connectionGroup, dataSource) {

                        // Prepare data source for active connection counting
                        dataSources.push(dataSource);
                        connectionCount[dataSource] = {};

                        // Create root item for current connection group
                        var rootItem = GroupListItem.fromConnectionGroup(dataSource, connectionGroup,
                            !!$scope.connectionTemplate, countActiveConnections);

                        // If root group is to be shown, add it as a root item
                        if ($scope.showRootGroup)
                            $scope.rootItems.push(rootItem);

                        // Otherwise, add its children as root items
                        else {
                            angular.forEach(rootItem.children, function addRootItem(child) {
                                $scope.rootItems.push(child);
                            });
                        }

                    });

                    // Count active connections by connection identifier
                    dataSourceService.apply(
                        activeConnectionService.getActiveConnections,
                        dataSources
                    )
                    .then(function activeConnectionsRetrieved(activeConnectionMap) {

                        // Within each data source, count each active connection by identifier
                        angular.forEach(activeConnectionMap, function addActiveConnections(activeConnections, dataSource) {
                            angular.forEach(activeConnections, function addActiveConnection(activeConnection) {

                                // If counter already exists, increment
                                var identifier = activeConnection.connectionIdentifier;
                                if (connectionCount[dataSource][identifier])
                                    connectionCount[dataSource][identifier]++;

                                // Otherwise, initialize counter to 1
                                else
                                    connectionCount[dataSource][identifier] = 1;

                            });
                        });

                    });

                }

            });

            /**
             * Toggle the open/closed status of a group list item.
             * 
             * @param {GroupListItem} groupListItem
             *     The list item to expand, which should represent a
             *     connection group.
             */
            $scope.toggleExpanded = function toggleExpanded(groupListItem) {
                groupListItem.isExpanded = !groupListItem.isExpanded;
            };

        }]

    };
}]);
