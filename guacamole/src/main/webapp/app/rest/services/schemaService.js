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
 * Service for operating on metadata via the REST API.
 */
angular.module('rest').factory('schemaService', ['$injector',
        function schemaService($injector) {

    // Required services
    var requestService        = $injector.get('requestService');
    var authenticationService = $injector.get('authenticationService');
    var cacheService          = $injector.get('cacheService');

    var service = {};

    /**
     * Makes a request to the REST API to get the list of available attributes
     * for user objects, returning a promise that provides an array of
     * @link{Form} objects if successful. Each element of the array describes
     * a logical grouping of possible attributes.
     *
     * @param {String} dataSource
     *     The unique identifier of the data source containing the users whose
     *     available attributes are to be retrieved. This identifier
     *     corresponds to an AuthenticationProvider within the Guacamole web
     *     application.
     *
     * @returns {Promise.<Form[]>}
     *     A promise which will resolve with an array of @link{Form}
     *     objects, where each @link{Form} describes a logical grouping of
     *     possible attributes.
     */
    service.getUserAttributes = function getUserAttributes(dataSource) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Retrieve available user attributes
        return requestService({
            cache   : cacheService.schema,
            method  : 'GET',
            url     : 'api/session/data/' + encodeURIComponent(dataSource) + '/schema/userAttributes',
            params  : httpParameters
        });

    };

    /**
     * Makes a request to the REST API to get the list of available attributes
     * for user group objects, returning a promise that provides an array of
     * @link{Form} objects if successful. Each element of the array describes
     * a logical grouping of possible attributes.
     *
     * @param {String} dataSource
     *     The unique identifier of the data source containing the user groups
     *     whose available attributes are to be retrieved. This identifier
     *     corresponds to an AuthenticationProvider within the Guacamole web
     *     application.
     *
     * @returns {Promise.<Form[]>}
     *     A promise which will resolve with an array of @link{Form}
     *     objects, where each @link{Form} describes a logical grouping of
     *     possible attributes.
     */
    service.getUserGroupAttributes = function getUserGroupAttributes(dataSource) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Retrieve available user group attributes
        return requestService({
            cache   : cacheService.schema,
            method  : 'GET',
            url     : 'api/session/data/' + encodeURIComponent(dataSource) + '/schema/userGroupAttributes',
            params  : httpParameters
        });

    };

    /**
     * Makes a request to the REST API to get the list of available attributes
     * for connection objects, returning a promise that provides an array of
     * @link{Form} objects if successful. Each element of the array describes
     * a logical grouping of possible attributes.
     *
     * @param {String} dataSource
     *     The unique identifier of the data source containing the connections
     *     whose available attributes are to be retrieved. This identifier
     *     corresponds to an AuthenticationProvider within the Guacamole web
     *     application.
     *
     * @returns {Promise.<Form[]>}
     *     A promise which will resolve with an array of @link{Form}
     *     objects, where each @link{Form} describes a logical grouping of
     *     possible attributes.
     */
    service.getConnectionAttributes = function getConnectionAttributes(dataSource) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Retrieve available connection attributes
        return requestService({
            cache   : cacheService.schema,
            method  : 'GET',
            url     : 'api/session/data/' + encodeURIComponent(dataSource) + '/schema/connectionAttributes',
            params  : httpParameters
        });

    };

    /**
     * Makes a request to the REST API to get the list of available attributes
     * for sharing profile objects, returning a promise that provides an array
     * of @link{Form} objects if successful. Each element of the array describes
     * a logical grouping of possible attributes.
     *
     * @param {String} dataSource
     *     The unique identifier of the data source containing the sharing
     *     profiles whose available attributes are to be retrieved. This
     *     identifier corresponds to an AuthenticationProvider within the
     *     Guacamole web application.
     *
     * @returns {Promise.<Form[]>}
     *     A promise which will resolve with an array of @link{Form}
     *     objects, where each @link{Form} describes a logical grouping of
     *     possible attributes.
     */
    service.getSharingProfileAttributes = function getSharingProfileAttributes(dataSource) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Retrieve available sharing profile attributes
        return requestService({
            cache   : cacheService.schema,
            method  : 'GET',
            url     : 'api/session/data/' + encodeURIComponent(dataSource) + '/schema/sharingProfileAttributes',
            params  : httpParameters
        });

    };

    /**
     * Makes a request to the REST API to get the list of available attributes
     * for connection group objects, returning a promise that provides an array
     * of @link{Form} objects if successful. Each element of the array
     * a logical grouping of possible attributes.
     *
     * @param {String} dataSource
     *     The unique identifier of the data source containing the connection
     *     groups whose available attributes are to be retrieved. This
     *     identifier corresponds to an AuthenticationProvider within the
     *     Guacamole web application.
     *
     * @returns {Promise.<Form[]>}
     *     A promise which will resolve with an array of @link{Form}
     *     objects, where each @link{Form} describes a logical grouping of
     *     possible attributes.
     */
    service.getConnectionGroupAttributes = function getConnectionGroupAttributes(dataSource) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Retrieve available connection group attributes
        return requestService({
            cache   : cacheService.schema,
            method  : 'GET',
            url     : 'api/session/data/' + encodeURIComponent(dataSource) + '/schema/connectionGroupAttributes',
            params  : httpParameters
        });

    };

    /**
     * Makes a request to the REST API to get the list of protocols, returning
     * a promise that provides a map of @link{Protocol} objects by protocol
     * name if successful.
     *
     * @param {String} dataSource
     *     The unique identifier of the data source defining available
     *     protocols. This identifier corresponds to an AuthenticationProvider
     *     within the Guacamole web application.
     *
     * @returns {Promise.<Object.<String, Protocol>>}
     *     A promise which will resolve with a map of @link{Protocol}
     *     objects by protocol name upon success.
     */
    service.getProtocols = function getProtocols(dataSource) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Retrieve available protocols
        return requestService({
            cache   : cacheService.schema,
            method  : 'GET',
            url     : 'api/session/data/' + encodeURIComponent(dataSource) + '/schema/protocols',
            params  : httpParameters
        });

    };

    return service;

}]);
