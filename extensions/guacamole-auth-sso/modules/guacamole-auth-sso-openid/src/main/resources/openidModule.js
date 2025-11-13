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
angular.module('guacSsoOpenid', [
    'auth',
    'rest',
    'storage'
]);

// Ensure the guacSsoOpenid module is loaded along with the rest of the app
angular.module('index').requires.push('guacSsoOpenid');

/**
 * Service for handling OpenID logout operations, including clearing
 * localStorage and cookies.
 */
angular.module('guacSsoOpenid').run(['$injector', '$rootScope',
    function openidLogoutHandler($injector, $rootScope) {

    // Required services
    var localStorageService = $injector.get('localStorageService');
    var requestService = $injector.get('requestService');

    /**
     * Clears all cookies for the current domain and path.
     */
    function clearCookies() {
        var cookies = document.cookie.split(';');
        for (var i = 0; i < cookies.length; i++) {
            var cookie = cookies[i];
            var eqPos = cookie.indexOf('=');
            var name = eqPos > -1 ? cookie.substr(0, eqPos).trim() : cookie.trim();
            
            // Clear the cookie by setting it to expire in the past
            // Try multiple path variations to ensure it's cleared
            var paths = ['/', window.location.pathname];
            paths.forEach(function(path) {
                document.cookie = name + '=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=' + path;
                document.cookie = name + '=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=' + path + ';domain=' + window.location.hostname;
                document.cookie = name + '=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=' + path + ';domain=.' + window.location.hostname;
            });
        }
    }

    /**
     * Clears all localStorage items related to authentication.
     */
    function clearLocalStorage() {
        // Clear the main Guacamole auth token
        localStorageService.removeItem('GUAC_AUTH_TOKEN');
        
        // Clear any OpenID-related items
        var keysToRemove = [];
        for (var i = 0; i < localStorage.length; i++) {
            var key = localStorage.key(i);
            if (key && (
                key.indexOf('GUAC_') === 0 ||
                key.indexOf('openid') !== -1 ||
                key.indexOf('OIDC_') === 0 ||
                key.indexOf('id_token') !== -1 ||
                key.indexOf('access_token') !== -1 ||
                key.indexOf('refresh_token') !== -1
            )) {
                keysToRemove.push(key);
            }
        }
        
        keysToRemove.forEach(function(key) {
            localStorageService.removeItem(key);
        });
    }

    /**
     * Handles logout by clearing localStorage and cookies, then redirecting
     * to Keycloak's logout endpoint to properly end the SSO session.
     */
    function handleLogout() {
        // Clear localStorage first
        clearLocalStorage();
        
        // Clear cookies
        clearCookies();
        
        // Redirect to Keycloak logout endpoint
        // This will properly end the SSO session in Keycloak
        // The logout endpoint will redirect back to the redirect_uri after logout
        window.location.href = 'api/ext/openid/logout';
    }

    // Listen for logout events
    $rootScope.$on('guacLogout', function() {
        handleLogout();
    });

}]);

