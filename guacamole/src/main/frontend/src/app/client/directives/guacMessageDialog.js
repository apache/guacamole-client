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
 * Directive which displays all client messages.
 */
angular.module('client').directive('guacMessageDialog', [function guacMessageDialog() {

    return {
        restrict: 'E',
        replace: true,
        scope: {

            /**
             * The client group whose messages should be managed by this
             * directive.
             * 
             * @type ManagedClientGroup
             */
            clientGroup : '='

        },

        templateUrl: 'app/client/templates/guacMessageDialog.html',
        controller: ['$scope', '$injector', function guacMessageDialogController($scope, $injector) {

            // Required types
            const ManagedClient            = $injector.get('ManagedClient');
            const ManagedClientGroup       = $injector.get('ManagedClientGroup');

            /**
             * Removes all messages.
             */
            $scope.clearAllMessages = function clearAllMessages() {
                
                // Nothing to clear if no client group attached
                if (!$scope.clientGroup)
                    return;

                // Remove each client's messages
                $scope.clientGroup.clients.forEach(client =>  {
                    client.messages = [];
                });

            };

            /**
             * @borrows ManagedClientGroup.hasMultipleClients
             */
            $scope.hasMultipleClients = ManagedClientGroup.hasMultipleClients;

            /**
             * @borrows ManagedClient.hasMessages
             */
            $scope.hasMessages = ManagedClient.hasMessages;

        }]

    };
}]);
