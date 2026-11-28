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
 * Service for retrieving the list of configured SAML identity providers from
 * the Guacamole SAML extension REST API.
 */
angular.module('guacSsoSaml').factory('samlProviderService', ['$injector',
        function samlProviderServiceProvider($injector) {

    // Required services
    var requestService = $injector.get('requestService');

    /**
     * Fallback provider list used if the provider discovery endpoint cannot be
     * reached, preserving single-IdP SAML login behavior.
     *
     * @type Object[]
     */
    var DEFAULT_PROVIDERS = [
        {
            id          : 'default',
            displayName : null,
            loginUrl    : 'api/ext/saml/login'
        }
    ];

    var service = {};

    /**
     * Retrieves all configured SAML identity providers for presentation on the
     * Guacamole login page.
     *
     * @returns {Promise.<Object[]>}
     *     A promise resolving with an array of provider descriptor objects,
     *     each having "id", "displayName", and "loginUrl" properties.
     */
    service.getProviders = function getProviders() {
        return requestService({
            method : 'GET',
            url    : 'api/ext/saml/providers'
        })
        .then(function providersRetrieved(providers) {
            if (angular.isArray(providers) && providers.length > 0)
                return providers;
            return DEFAULT_PROVIDERS;
        })
        ['catch'](function providersUnavailable() {
            return DEFAULT_PROVIDERS;
        });
    };

    return service;

}]);
