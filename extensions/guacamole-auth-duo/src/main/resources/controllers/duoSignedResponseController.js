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
 * Controller for the "GUAC_DUO_SIGNED_RESPONSE" field which uses the DuoWeb
 * API to prompt the user for additional credentials, ultimately receiving a
 * signed response from the Duo service.
 */
angular.module('guacDuo').controller('duoSignedResponseController', ['$scope', '$element',
    function duoSignedResponseController($scope, $element) {

    /**
     * The iframe which contains the Duo authentication interface.
     *
     * @type HTMLIFrameElement
     */
    var iframe = $element.find('iframe')[0];

    /**
     * The submit button which should be used to submit the login form once
     * the Duo response has been received.
     *
     * @type HTMLInputElement
     */
    var submit = $element.find('input[type="submit"]')[0];

    /**
     * Whether the Duo interface has finished loading within the iframe.
     *
     * @type Boolean
     */
    $scope.duoInterfaceLoaded = false;

    /**
     * Submits the signed response from Duo once the user has authenticated.
     * This is a callback invoked by the DuoWeb API after the user has been
     * verified and the signed response has been received.
     *
     * @param {HTMLFormElement} form
     *     The form element provided by the DuoWeb API containing the signed
     *     response as the value of an input field named "sig_response".
     */
    var submitSignedResponse = function submitSignedResponse(form) {

        // Update model to match received response
        $scope.$apply(function updateModel() {
            $scope.model = form.elements['sig_response'].value;
        });

        // Submit updated credentials
        submit.click();

    };

    // Update Duo loaded state when iframe finishes loading
    iframe.onload = function duoLoaded() {
        $scope.$apply(function updateLoadedState() {
            $scope.duoInterfaceLoaded = true;
        });
    };

    // Initialize Duo interface within iframe
    Duo.init({
        iframe          : iframe,
        host            : $scope.field.apiHost,
        sig_request     : $scope.field.signedRequest,
        submit_callback : submitSignedResponse
    });

}]);
