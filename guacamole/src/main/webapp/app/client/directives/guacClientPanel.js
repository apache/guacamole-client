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
angular.module('client').directive('guacClientPanel', ['$injector', function guacClientPanel($injector) {

    // Required services
    var guacClientManager     = $injector.get('guacClientManager');
    var sessionStorageFactory = $injector.get('sessionStorageFactory');

    // Required types
    var ManagedClientState = $injector.get('ManagedClientState');

    /**
     * Getter/setter for the boolean flag controlling whether the client panel
     * is currently hidden. This flag is maintained in session-local storage to
     * allow the state of the panel to persist despite navigation within the
     * same tab. When hidden, the panel will be collapsed against the right
     * side of the container. By default, the panel is visible.
     *
     * @type Function
     */
    var panelHidden = sessionStorageFactory.create(false);

    return {
        // Element only
        restrict: 'E',
        replace: true,
        scope: {

            /**
             * The ManagedClient instances associated with the active
             * connections to be displayed within this panel.
             * 
             * @type ManagedClient[]|Object.<String, ManagedClient>
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
             * On-scope reference to session-local storage of the flag
             * controlling whether then panel is hidden.
             */
            $scope.panelHidden = panelHidden;

            /**
             * Returns whether this panel currently has any clients associated
             * with it.
             *
             * @return {Boolean}
             *     true if at least one client is associated with this panel,
             *     false otherwise.
             */
            $scope.hasClients = function hasClients() {
                return !!_.find($scope.clients, $scope.isManaged);
            };

            /**
             * Returns whether the status of the given client has changed in a
             * way that requires the user's attention. This may be due to an
             * error, or due to a server-initiated disconnect.
             *
             * @param {ManagedClient} client
             *     The client to test.
             *
             * @returns {Boolean}
             *     true if the given client requires the user's attention,
             *     false otherwise.
             */
            $scope.hasStatusUpdate = function hasStatusUpdate(client) {

                // Test whether the client has encountered an error
                switch (client.clientState.connectionState) {
                    case ManagedClientState.ConnectionState.CONNECTION_ERROR:
                    case ManagedClientState.ConnectionState.TUNNEL_ERROR:
                    case ManagedClientState.ConnectionState.DISCONNECTED:
                        return true;
                }

                return false;

            };

            /**
             * Returns whether the given client is currently being managed by
             * the guacClientManager service.
             *
             * @param {ManagedClient} client
             *     The client to test.
             *
             * @returns {Boolean}
             *     true if the given client is being managed by the
             *     guacClientManager service, false otherwise.
             */
            $scope.isManaged = function isManaged(client) {
                return !!guacClientManager.getManagedClients()[client.id];
            };

            /**
             * Initiates an orderly disconnect of the given client. The client
             * is removed from management such that attempting to connect to
             * the same connection will result in a new connection being
             * established, rather than displaying a notification that the
             * connection has ended.
             *
             * @param {type} client
             * @returns {undefined}
             */
            $scope.disconnect = function disconnect(client) {
                client.client.disconnect();
                guacClientManager.removeManagedClient(client.id);
            };

            /**
             * Toggles whether the client panel is currently hidden.
             */
            $scope.togglePanel = function togglePanel() {
                panelHidden(!panelHidden());
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