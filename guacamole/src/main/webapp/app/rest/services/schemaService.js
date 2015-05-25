/*
 * Copyright (C) 2015 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/**
 * Service for operating on metadata via the REST API.
 */
angular.module('rest').factory('schemaService', ['$injector',
        function schemaService($injector) {

    // Required services
    var $http                 = $injector.get('$http');
    var authenticationService = $injector.get('authenticationService');
    var cacheService          = $injector.get('cacheService');

    var service = {};

    /**
     * Makes a request to the REST API to get the list of available attributes
     * for user objects, returning a promise that provides an array of
     * @link{Field} objects if successful. Each element of the array describes
     * a possible attribute.
     *
     * @returns {Promise.<Field[]>}
     *     A promise which will resolve with an array of @link{Field}
     *     objects, where each @link{Field} describes a possible attribute.
     */
    service.getUserAttributes = function getUserAttributes() {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Retrieve available user attributes
        return $http({
            cache   : cacheService.schema,
            method  : 'GET',
            url     : 'api/schema/users/attributes',
            params  : httpParameters
        });

    };

    /**
     * Makes a request to the REST API to get the list of available attributes
     * for connection objects, returning a promise that provides an array of
     * @link{Field} objects if successful. Each element of the array describes
     * a possible attribute.
     *
     * @returns {Promise.<Field[]>}
     *     A promise which will resolve with an array of @link{Field}
     *     objects, where each @link{Field} describes a possible attribute.
     */
    service.getConnectionAttributes = function getConnectionAttributes() {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Retrieve available connection attributes
        return $http({
            cache   : cacheService.schema,
            method  : 'GET',
            url     : 'api/schema/connections/attributes',
            params  : httpParameters
        });

    };

    /**
     * Makes a request to the REST API to get the list of available attributes
     * for connection group objects, returning a promise that provides an array
     * of @link{Field} objects if successful. Each element of the array
     * describes a possible attribute.
     *
     * @returns {Promise.<Field[]>}
     *     A promise which will resolve with an array of @link{Field}
     *     objects, where each @link{Field} describes a possible attribute.
     */
    service.getConnectionGroupAttributes = function getConnectionGroupAttributes() {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Retrieve available connection group attributes
        return $http({
            cache   : cacheService.schema,
            method  : 'GET',
            url     : 'api/schema/connectionGroups/attributes',
            params  : httpParameters
        });

    };

    /**
     * Makes a request to the REST API to get the list of protocols, returning
     * a promise that provides a map of @link{Protocol} objects by protocol
     * name if successful.
     *
     * @returns {Promise.<Object.<String, Protocol>>}
     *     A promise which will resolve with a map of @link{Protocol}
     *     objects by protocol name upon success.
     */
    service.getProtocols = function getProtocols() {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Retrieve available protocols
        return $http({
            cache   : cacheService.schema,
            method  : 'GET',
            url     : 'api/schema/protocols',
            params  : httpParameters
        });

    };

    return service;

}]);
