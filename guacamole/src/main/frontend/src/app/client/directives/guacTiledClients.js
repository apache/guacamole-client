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

        /**
         * Assigns keyboard focus to the given client, allowing that client to
         * receive and handle keyboard events. Multiple clients may have
         * keyboard focus simultaneously.
         *
         * @param {ManagedClient} client
         *     The client that should receive keyboard focus.
         */
        $scope.assignFocus = function assignFocus(client) {

            // Clear focus of all other clients
            $scope.clientGroup.clients.forEach(client => {
                client.clientProperties.focused = false;
            });

            client.clientProperties.focused = true;

        };

        /**
         * Returns whether multiple clients are currently shown within the
         * tiled grid.
         *
         * @returns {Boolean}
         *     true if two or more clients are currently present, false
         *     otherwise.
         */
        $scope.hasMultipleClients = function hasMultipleClients() {
            return $scope.clientGroup && $scope.clientGroup.clients.length > 1;
        };

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

    }];

    return directive;

}]);
