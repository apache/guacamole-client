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
 * The module for code implementing SSO using OpenID Connect.
 */
angular.module('guacSsoOpenid', []);

/**
 * Redirect to the SSO logout endpoint when the user logs out, allowing
 * Single Logout to occur if configured.
 */
angular.module('guacSsoOpenid').run(['$rootScope', '$window',
        function guacSsoOpenidLogout($rootScope, $window) {

    // Redirect to SSO logout endpoint when user logs out
    $rootScope.$on('guacLogout', function handleLogout() {
        $window.location.href = 'api/ext/sso/logout';
    });

}]);

// Ensure the guacSsoOpenid module is loaded along with the rest of the app
angular.module('index').requires.push('guacSsoOpenid');
