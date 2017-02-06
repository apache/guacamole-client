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
 * Controller for the "GUAC_RADIUS_CHALLENGE_RESPONSE" field which uses the DuoWeb
 * API to prompt the user for additional credentials, ultimately receiving a
 * signed response from the Duo service.
 */
angular.module('guacRadius').controller('guacRadiusController', ['$scope', '$element',
        function guacRadiusController($scope, $element) {
    console.log("In guacRadiusController() method.");

    // Find the area to display the challenge message
    var radiusChallenge = $element.find(document.querySelector('#radius-challenge-text'));

    // Find the hidden input to put the state in
    var radiusState = $element.find(document.querySelector('#radius-state'));

    // Populate the reply message field
    console.log("RADIUS Reply Message: " + $scope.field.replyMsg);
    radiusChellenge.html($scope.field.replyMsg);

    // Populate the input area for the connection state
    console.log("RADIUS State: " + scope.field.radiusState);
    radiusState.value = $scope.field.radiusState;

}]);
