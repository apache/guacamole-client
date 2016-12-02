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
 * Config block which registers Duo-specific field types.
 */
angular.module('guacDuo').config(['formServiceProvider',
    function guacDuoConfig(formServiceProvider) {

    // Define field for the signed response from the Duo service
    formServiceProvider.registerFieldType('GUAC_DUO_SIGNED_RESPONSE', {
        module      : 'guacDuo',
        controller  : 'duoSignedResponseController',
        templateUrl : 'app/ext/duo/templates/duoSignedResponseField.html'
    });

}]);
