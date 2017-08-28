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
 * Config block which registers openid-specific field types.
 */
angular.module('guacOpenID').config(['formServiceProvider',
        function guacOpenIDConfig(formServiceProvider) {

    // Define field for token from OpenID service
    formServiceProvider.registerFieldType("GUAC_OPENID_TOKEN", {
        templateUrl : 'app/ext/guac-openid/templates/openidTokenField.html',
        controller  : 'guacOpenIDController',
        module      : 'guacOpenID'
    });

}]);

/**
 * Config block which augments the existing routing, providing special handling
 * for the "id_token=" fragments provided by OpenID Connect.
 */
angular.module('index').config(['$routeProvider',
        function indexRouteConfig($routeProvider) {

    // Transform "/#/id_token=..." to "/#/?id_token=..."
    $routeProvider.when('/id_token=:response', {

        template   : '',
        controller : ['$location', function reroute($location) {
            var params = $location.path().substring(1);
            $location.url('/');
            $location.search(params);
        }]

    });

}]);
