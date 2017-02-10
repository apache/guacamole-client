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
 * Config block which registers CAS-specific field types.
 */
angular.module('guacCAS').config(['formServiceProvider',
        function guacCASConfig(formServiceProvider) {

    // Define field for ticket from CAS service
    formServiceProvider.registerFieldType("GUAC_CAS_TICKET", {
        templateUrl   : '',
        controller    : 'guacCASController',
        module        : 'guacCAS'
    });

}]);

/**
 * Config block which augments the existing routing, providing special handling
 * for the "ticket=" fragments provided by OpenID Connect.
 */
angular.module('index').config(['$routeProvider','$windowProvider',
        function indexRouteConfig($routeProvider,$windowProvider) {

    var $window = $windowProvider.$get();
    var curPath = $window.location.href;
    var ticketPos = curPath.indexOf("?ticket=") + 8;
    var hashPos = curPath.indexOf("#/");
    if (ticketPos > 0 && ticketPos < hashPos) {
        var ticket = curPath.substring(ticketPos, hashPos);
        var newPath = curPath.substring(0,ticketPos - 8) + '#/?ticket=' + ticket;
        $window.location.href = newPath;
    }

}]);
