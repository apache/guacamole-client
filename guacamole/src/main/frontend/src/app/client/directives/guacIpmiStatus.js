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
 * A lightweight, always-visible chassis power/health indicator for IPMI
 * connections. It is a thin read-only view over ipmiControlService, allowing
 * operators to see the host power state at a glance without opening the menu,
 * where the full chassis controls live.
 */
angular.module('client').directive('guacIpmiStatus', [function guacIpmiStatus() {

    const directive = {
        restrict: 'E',
        replace: true,
        templateUrl: 'app/client/templates/guacIpmiStatus.html'
    };

    directive.scope = {

        /**
         * The ManagedClient of the IPMI connection whose state is shown.
         *
         * @type ManagedClient
         */
        client : '='

    };

    directive.controller = ['$scope', '$injector',
            function guacIpmiStatusController($scope, $injector) {

        // Required services
        const ipmiControlService = $injector.get('ipmiControlService');

        /**
         * The shared chassis state for the current client, or null if no
         * client is available yet.
         *
         * @type Object
         */
        $scope.state = null;

        // Bind to the shared chassis state as soon as a client is available.
        // Accessing the state also initializes the control channel if the menu
        // panel has not already done so.
        $scope.$watch('client', function clientChanged(client) {
            if (client && client.client)
                $scope.state = ipmiControlService.getState(client);
        });

    }];

    return directive;

}]);
