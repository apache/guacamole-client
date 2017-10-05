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
 * A directive for the prompt display for the client.
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
             * The client calling this prompt.
             *
             * @type ManagedClient
             */
            client : '='

        },

        templateUrl: 'app/prompt/templates/guacPrompt.html',
        controller: ['$scope', '$injector', function guacPromptController($scope,$injector) {

            var translationStringService = $injector.get('translationStringService');

            /**
             * The data the user enters.
             *
             * @type Object
             */
            $scope.responses = {};

            /**
             * Returns the translation string namespace for the protocol having the
             * given name. The namespace will be of the form:
             *
             * <code>PROTOCOL_NAME</code>
             *
             * where <code>NAME</code> is the protocol name transformed via
             * translationStringService.canonicalize().
             *
             * @param {String} protocolName
             *     The name of the protocol.
             *
             * @returns {String}
             *     The translation namespace for the protocol specified, or null if no
             *     namespace could be generated.
             */
            $scope.getNamespace = function getNamespace(protocolName) {

                // Do not generate a namespace if no protocol is selected
                if (!protocolName)
                    return null;

                return 'PROTOCOL_' + translationStringService.canonicalize(protocolName);

            };

            // Update string value and re-assign to model when field is changed
            $scope.$watch('prompt.responses', function setModel(model) {

                // Assign new model only if provided
                if (model)
                    $scope.responses = model;

                // Otherwise, use blank model
                else
                    $scope.responses = {};

            });


        }]

    };
}]);
