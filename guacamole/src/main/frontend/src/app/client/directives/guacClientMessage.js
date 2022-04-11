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
 * Directive which displays a message for the client.
 */
angular.module('client').directive('guacClientMessage', [function guacClientMessage() {

    return {
        restrict: 'E',
        replace: true,
        scope: {

            /**
             * The message to display to the client.
             * 
             * @type {!ManagedClientMessage}
             */
            message : '='

        },

        templateUrl: 'app/client/templates/guacClientMessage.html',
        
        controller: ['$scope', '$injector', '$element',
                function guacClientMessageController($scope, $injector, $element) {
            
            const ManagedClientMessage = $injector.get('ManagedClientMessage');
            
            /**
             * Uses the msgcode to retrieve the correct translation key for
             * the client message.
             * 
             * @returns {string}
             */
            $scope.getMessageKey = function getMessageKey() {
                
                let msgString = "DEFAULT";
                if (Object.values(Guacamole.Client.Message).includes($scope.message.msgcode))
                    msgString = Object.keys(Guacamole.Client.Message).find(key => Guacamole.Client.Message[key] === $scope.message.msgcode);
                
                return "CLIENT.CLIENT_MESSAGE_" + msgString.toUpperCase();
            };
            
            /**
             * Returns a set of key/value object pairs that represent the
             * arguments provided as part of the message in the form
             * ARGS[0] = value.
             * 
             * @returns {Object}
             */
            $scope.getMessageArgs = function getMessageArgs() {
                return $scope.message.args.reduce(
                    function(acc, value, index) {
                        acc[`ARGS[${index}]`] = value;
                        return acc;
                    },
                    {}
                );
            };
                
        }]

    };
}]);
