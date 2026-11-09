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
 * Directive which displays the Azure AD device code sign-in prompt for a
 * connection. When the server requests sign-in, a QR code of the Microsoft
 * verification URI is shown alongside the user code; the user scans it and
 * completes sign-in on a separate device. The prompt is dismissed automatically
 * once the server reports the flow has completed.
 */
angular.module('client').directive('guacAadSignin', [function guacAadSignin() {

    return {
        restrict: 'E',
        replace: true,
        scope: {

            /**
             * The ManagedClient whose Azure AD sign-in should be displayed.
             *
             * @type ManagedClient
             */
            client: '='

        },
        templateUrl: 'app/client/templates/guacAadSignin.html',
        controller: ['$scope', function controller($scope) {

            /**
             * Cancels sign-in by disconnecting the connection awaiting it.
             */
            $scope.cancel = function cancel() {
                if ($scope.client && $scope.client.client)
                    $scope.client.client.disconnect();
            };

        }]
    };

}]);
