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
 * A directive for displaying a group of Guacamole clients as a non-interactive
 * thumbnail of tiled client displays.
 */
angular.module('client').directive('guacTiledThumbnails', [function guacTiledThumbnails() {

    var directive = {
        restrict: 'E',
        replace: true,
        templateUrl: 'app/client/templates/guacTiledThumbnails.html'
    };

    directive.scope = {

        /**
         * The group of clients to display as a thumbnail of tiled client
         * displays.
         *
         * @type ManagedClientGroup
         */
        clientGroup : '='

    };

    directive.controller = ['$scope', '$injector', '$element',
            function guacTiledThumbnailsController($scope, $injector, $element) {

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