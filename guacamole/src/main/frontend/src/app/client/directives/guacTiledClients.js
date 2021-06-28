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
 * A directive which displays one or more Guacamole clients in an evenly-tiled
 * view. The number of rows and columns used for the arrangement of tiles is
 * automatically determined by the number of clients present.
 */
angular.module('client').directive('guacTiledClients', [function guacTiledClients() {

    var directive = {
        restrict: 'E',
        templateUrl: 'app/client/templates/guacTiledClients.html',
    };

    directive.scope = {

        /**
         * The group of Guacamole clients that should be displayed in an
         * evenly-tiled grid arrangement.
         *
         * @type ManagedClientGroup
         */
        clientGroup : '=',

        /**
         * Whether translation of touch to mouse events should emulate an
         * absolute pointer device, or a relative pointer device.
         *
         * @type boolean
         */
        emulateAbsoluteMouse : '='

    };

    directive.controller = ['$scope', '$injector', '$element',
            function guacTiledClientsController($scope, $injector, $element) {

        // Required types
        var ManagedClient      = $injector.get('ManagedClient');
        var ManagedClientGroup = $injector.get('ManagedClientGroup');

        /**
         * Returns the currently-focused ManagedClient. If there is no such
         * client, or multiple clients are focused, null is returned.
         *
         * @returns {ManagedClient}
         *     The currently-focused client, or null if there are no focused
         *     clients or if multiple clients are focused.
         */
        var getFocusedClient = function getFocusedClient() {

            var managedClientGroup = $scope.clientGroup;
            if (managedClientGroup) {
                var focusedClients = _.filter(managedClientGroup.clients, client => client.clientProperties.focused);
                if (focusedClients.length === 1)
                    return focusedClients[0];
            }

            return null;

        };

        // Re-initialize the reference to the currently-focused client when a
        // new client group is set
        $scope.$watch('clientGroup', function clientGroupChanged() {
            $scope.$emit('guacClientFocused', getFocusedClient());
        });

        /**
         * Returns a callback for guacClick that assigns or updates keyboard
         * focus to the given client, allowing that client to receive and
         * handle keyboard events. Multiple clients may have keyboard focus
         * simultaneously.
         *
         * @param {ManagedClient} client
         *     The client that should receive keyboard focus.
         *
         * @return {guacClick~callback}
         *     The callback that guacClient should invoke when the given client
         *     has been clicked.
         */
        $scope.getFocusAssignmentCallback = function getFocusAssignmentCallback(client) {
            return (shift, ctrl) => {

                // Clear focus of all other clients if not selecting multiple
                if (!shift && !ctrl) {
                    $scope.clientGroup.clients.forEach(client => {
                        client.clientProperties.focused = false;
                    });
                }

                client.clientProperties.focused = true;

                // Fill in any gaps if performing rectangular multi-selection
                // via shift-click
                if (shift) {

                    var minRow = $scope.clientGroup.rows - 1;
                    var minColumn = $scope.clientGroup.columns - 1;
                    var maxRow = 0;
                    var maxColumn = 0;

                    // Determine extents of selected area
                    ManagedClientGroup.forEach($scope.clientGroup, (client, row, column) => {
                        if (client.clientProperties.focused) {
                            minRow = Math.min(minRow, row);
                            minColumn = Math.min(minColumn, column);
                            maxRow = Math.max(maxRow, row);
                            maxColumn = Math.max(maxColumn, column);
                        }
                    });

                    ManagedClientGroup.forEach($scope.clientGroup, (client, row, column) => {
                        client.clientProperties.focused =
                                row >= minRow
                             && row <= maxRow
                             && column >= minColumn
                             && column <= maxColumn;
                    });

                }

                // Update reference to single focused client after focus has
                // changed
                $scope.$emit('guacClientFocused', getFocusedClient());

            };
        };

        /**
         * @borrows ManagedClientGroup.hasMultipleClients
         */
        $scope.hasMultipleClients = ManagedClientGroup.hasMultipleClients;

        /**
         * Returns the CSS width that should be applied to each tile to
         * achieve an even arrangement.
         *
         * @returns {String}
         *     The CSS width that should be applied to each tile.
         */
        $scope.getTileWidth = function getTileWidth() {
            return Math.floor(100 / $scope.clientGroup.columns) + '%';
        };

        /**
         * Returns the CSS height that should be applied to each tile to
         * achieve an even arrangement.
         *
         * @returns {String}
         *     The CSS height that should be applied to each tile.
         */
        $scope.getTileHeight = function getTileHeight() {
            return Math.floor(100 / $scope.clientGroup.rows) + '%';
        };

        /**
         * Returns whether the given ManagedClient has any associated share
         * links.
         *
         * @param {ManagedClient} client
         *     The ManagedClient to test.
         *
         * @returns {Boolean}
         *     true if the given ManagedClient has at least one associated
         *     share link, false otherwise.
         */
        $scope.isShared = function isShared(client) {
            return ManagedClient.isShared(client);
        };

    }];

    return directive;

}]);
