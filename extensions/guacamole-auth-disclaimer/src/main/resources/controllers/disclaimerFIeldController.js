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
 * Controller for the "GUAC_RADIUS_STATE" field which is used to pass
 * the RADIUS server state to maintain the session with the RADIUS
 * server.
 */
angular.module('guacDisclaimer').controller('disclaimerFieldController', ['$scope', '$injector',
        function disclaimerFieldController($scope, $injector) {

    // Update typed value when model is changed
    $scope.$watch('model', function modelChanged(model) {
        $scope.typedValue = (model === $scope.field.options[0]);
    });

    // Update string value in model when typed value is changed
    $scope.$watch('typedValue', function typedValueChanged(typedValue) {
        $scope.model = (typedValue ? $scope.field.options[0] : '');
    });
    
    /**
     * Return a string representing the last login date/time in the locale of
     * the browser.
     * 
     * @returns {String}
     *     A String representation of the last login timestamp in the locale
     *     of the browser.
     */
    $scope.showLastLogin = function showLastLogin() {
        return new Date($scope.field.lastLogin).toLocaleString();
    }

}]);
