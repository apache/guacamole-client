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
 * Config block which registers disclaimer-specific field types.
 */
angular.module('guacDisclaimer').config(['formServiceProvider',
    function guacDisclaimerConfig(formServiceProvider) {

    // Define field for the disclaimer to be displayed to the user
    formServiceProvider.registerFieldType('GUAC_DISCLAIMER', {
        module      : 'guacDisclaimer',
        controller  : 'disclaimerFieldController',
        templateUrl : 'app/ext/disclaimer/templates/disclaimerField.html'
    });

}]);
