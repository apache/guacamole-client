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
 * Controller for the WebAuthn status panel within the connection menu.
 * Surfaces in-flight ceremony count and last-ceremony status from the
 * client's ManagedWebAuthn instance.
 */
angular.module('webauthn').controller('webAuthnController', ['$scope', '$injector',
    function webAuthnController($scope, $injector) {

        const webAuthnService = $injector.get('webAuthnService');

        /**
         * Whether WebAuthn is available in the current browser at all.
         * @type {Boolean}
         */
        $scope.webAuthnSupported = webAuthnService.isSupported;

        /**
         * The current page origin, exposed for the policy-blocked hint so
         * the user can see the exact value that must be added to the
         * WebAuthenticationRemoteDesktopAllowedOrigins entry.
         *
         * @type {!String}
         */
        $scope.webAuthnOrigin = window.location.origin;

    }
]);
