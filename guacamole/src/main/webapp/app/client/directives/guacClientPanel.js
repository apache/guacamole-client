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
 * A toolbar/panel which displays a list of active Guacamole connections. The
 * panel is fixed to the bottom-right corner of its container and can be
 * manually hidden/exposed by the user.
 */
angular.module('client').directive('guacClientPanel', [function guacClientPanel() {

    return {
        // Element only
        restrict: 'E',
        replace: true,
        scope: {

            /**
             * The ManagedClient instances associated with the active
             * connections to be displayed within this panel.
             * 
             * @type ManagedClient[]
             */
            clients : '='
            
        },
        templateUrl: 'app/client/templates/guacClientPanel.html',
        controller: ['$scope', '$element', function guacClientPanelController($scope, $element) {

            /**
             * The DOM element containing the scrollable portion of the client
             * panel.
             *
             * @type Element
             */
            var scrollableArea = $element.find('.client-panel-connection-list')[0];

            /**
             * Whether the client panel is currently hidden. When hidden, the
             * panel will be collapsed against the right side of the
             * containiner.
             *
             * @type Boolean
             */
            $scope.panelHidden = false;

            /**
             * Toggles whether the client panel is currently hidden.
             */
            $scope.togglePanel = function togglePanel() {
                $scope.panelHidden = !$scope.panelHidden;
            };

            // Override vertical scrolling, scrolling horizontally instead
            scrollableArea.addEventListener('wheel', function reorientVerticalScroll(e) {

                var deltaMultiplier = {
                    /* DOM_DELTA_PIXEL */ 0x00: 1,
                    /* DOM_DELTA_LINE  */ 0x01: 15,
                    /* DOM_DELTA_PAGE  */ 0x02: scrollableArea.offsetWidth
                };

                if (e.deltaY) {
                    this.scrollLeft += e.deltaY * (deltaMultiplier[e.deltaMode] || deltaMultiplier(0x01));
                    e.preventDefault();
                }

            });

        }]
    };
}]);