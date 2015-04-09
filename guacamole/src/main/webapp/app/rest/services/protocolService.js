/*
 * Copyright (C) 2014 Glyptodon LLC
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
 * Service for operating on protocol metadata via the REST API.
 */
angular.module('rest').factory('protocolService', ['$injector',
        function protocolService($injector) {

    // Required services
    var $http                 = $injector.get('$http');
    var authenticationService = $injector.get('authenticationService');
    var cacheService          = $injector.get('cacheService');

    var service = {};
    
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
            cache   : cacheService.protocols,
            method  : 'GET',
            url     : 'api/protocols',
            params  : httpParameters
        });

    };
    
    return service;

}]);
