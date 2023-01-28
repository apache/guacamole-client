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
 * A directive which automatically attempts to log the current user in using
 * SSL/TLS client authentication when the associated element is clicked.
 */
angular.module('element').directive('guacSslAuth', ['$injector', function guacSslAuth($injector) {

    // Required services
    var requestService        = $injector.get('requestService');
    var authenticationService = $injector.get('authenticationService');

    var directive = {
        restrict: 'A'
    };

    directive.link = function linkGuacSslAuth($scope, $element) {

        /**
         * The element which will register the click.
         *
         * @type Element
         */
        const element = $element[0];

        // Attempt SSL/TLS client authentication upon click
        element.addEventListener('click', function elementClicked() {

            // Transform SSL/TLS identity into an opaque "state" value and
            // attempt authentication using that value
            authenticationService.authenticate(
                requestService({
                    method: 'GET',
                    headers : {
                        'Cache-Control' : undefined, // Avoid sending headers that would result in a pre-flight OPTIONS request for CORS
                        'Pragma'        : undefined
                    },
                    url: 'api/ext/ssl/identity'
                })
                .then(function identityRetrieved(data) {
                    return { 'state' : data.state || '' };
                })
            )['catch'](requestService.IGNORE);

        });

    };

    return directive;

}]);
