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
 * A directive for the guacamole client that handles prompting the user for
 * information required to continue a connection.
 */
angular.module('prompt').directive('guacPrompt', [function guacPrompt() {

    return {
        restrict: 'E',
        replace: true,
        scope: {

            /**
             * The prompt to display.
             *
             * @type Prompt|Object 
             */
            prompt : '=',
            
            /**
             * The client this prompt is linked to.
             * 
             * @type Client|Object
             */
            client : '<'

        },

        templateUrl: 'app/prompt/templates/guacPrompt.html',
        controller: ['$scope', '$injector', function guacPromptController($scope, $injector) {

            // Required services
            var Protocol                 = $injector.get('Protocol');
                
            /**
             * Responses to provided prompts.
             * 
             * @Type Object
             */
            $scope.responses = {};
            
            /**
             * Return the translated namespace for the given protocol.
             * 
             * @param {String} protocolName
             *     The name of the protocol.
             *     
             * @return The canonicalized name of the protocol, or null if
             *     no protocol is provided.
             */
            $scope.getProtocolNamespace = Protocol.getNamespace;
            
            // Update responses as model changes
            $scope.$watch('responses', function setModel(model) {
                
                // If model is defined, use it.
                if (model)
                    $scope.responses = model;
                
                // Otherwise use a blank object
                else
                    $scope.responses = {};
                
            });

        }]

    };
}]);
