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
 * The module for code implementing SSO using SAML.
 */
angular.module('guacSsoSaml', [
    'auth',
    'rest'
]);

/**
 * Loads configured SAML identity providers for display on the login screen and
 * handles Single Logout redirection to the identity provider that
 * authenticated the user.
 */
angular.module('guacSsoSaml').run(['$injector',
        function guacSsoSamlRun($injector) {

    // Required services
    var $location             = $injector.get('$location');
    var $rootScope            = $injector.get('$rootScope');
    var $window               = $injector.get('$window');
    var authenticationService = $injector.get('authenticationService');
    var samlProviderService   = $injector.get('samlProviderService');

    /**
     * Session storage key tracking the SAML identity provider ID used to
     * authenticate the current browser tab's Guacamole session.
     *
     * @type String
     */
    var SAML_IDP_STORAGE_KEY = 'GUAC_SAML_IDP';

    /**
     * Safely reads the stored SAML identity provider ID from sessionStorage.
     *
     * @returns {String}
     *     The stored SAML identity provider ID, or null if none is stored or
     *     storage is unavailable.
     */
    var getStoredIdp = function getStoredIdp() {
        try {
            return $window.sessionStorage.getItem(SAML_IDP_STORAGE_KEY);
        }
        catch (e) {
            return null;
        }
    };

    /**
     * Safely writes or clears the stored SAML identity provider ID in
     * sessionStorage.
     *
     * @param {String} idpId
     *     The SAML identity provider ID to store, or null/empty to clear.
     */
    var setStoredIdp = function setStoredIdp(idpId) {
        try {
            if (idpId)
                $window.sessionStorage.setItem(SAML_IDP_STORAGE_KEY, idpId);
            else
                $window.sessionStorage.removeItem(SAML_IDP_STORAGE_KEY);
        }
        catch (e) {
            // Ignore storage errors (e.g., restricted browsing contexts)
        }
    };

    /**
     * Refreshes the list of configured SAML identity providers on $rootScope
     * for display within the login screen's SSO provider list.
     */
    var refreshProviders = function refreshProviders() {
        samlProviderService.getProviders().then(function providersLoaded(providers) {
            $rootScope.guacSamlProviders = providers;
        });
    };

    // Initialize provider list on startup and refresh whenever credentials are
    // requested so hot-reloaded saml-idps.yml changes take effect immediately
    $rootScope.guacSamlProviders = [];
    refreshProviders();
    $rootScope.$on('guacInvalidCredentials', refreshProviders);

    // Track which SAML IdP authenticated the user so Single Logout targets
    // the matching IdP
    $rootScope.$on('guacLogin', function handleLogin() {
        if (authenticationService.getDataSource() === 'saml') {
            var idp = $location.search().idp;
            if (idp)
                setStoredIdp(idp);
        }
        else {
            setStoredIdp(null);
        }
    });

    // Redirect to SSO logout endpoint when user logs out
    $rootScope.$on('guacLogout', function handleLogout() {
        var idp = getStoredIdp();
        setStoredIdp(null);

        if (idp)
            $window.location.href = 'api/ext/saml/logout/' + encodeURIComponent(idp);
        else
            $window.location.href = 'api/ext/saml/logout';
    });

}]);

// Ensure the guacSsoSaml module is loaded along with the rest of the app
angular.module('index').requires.push('guacSsoSaml');
