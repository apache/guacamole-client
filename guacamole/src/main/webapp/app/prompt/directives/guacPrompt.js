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
 * A directive for the guacamole client.
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
            prompt : '='

        },

        templateUrl: 'app/prompt/templates/guacPrompt.html',
        controller: ['$scope', '$injector', '$log', function guacPromptController($scope,$injector,$log) {

            var translationStringService = $injector.get('translationStringService');

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

                $log.debug('Getting namespace for protocol ' + protocolName);

                // Do not generate a namespace if no protocol is selected
                if (!protocolName)
                    return null;

                return 'PROTOCOL_' + translationStringService.canonicalize(protocolName);

            };
        }]

    };
}]);
