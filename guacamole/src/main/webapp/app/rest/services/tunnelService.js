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
 * Service for operating on tunnels via the REST API.
 */
angular.module('rest').factory('tunnelService', ['$http', 'authenticationService',
        function tunnelService($http, authenticationService) {

    var service = {};

    /**
     * Makes a request to the REST API to get the list of active tunnels,
     * returning a promise that provides an array of @link{ActiveTunnel}
     * objects if successful.
     *
     * @returns {Promise.<ActiveTunnel[]>}
     *     A promise which will resolve with an array of @link{ActiveTunnel}
     *     objects upon success.
     */
    service.getActiveTunnels = function getActiveTunnels() {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Retrieve tunnels
        return $http({
            method  : 'GET',
            url     : 'api/tunnels',
            params  : httpParameters
        });

    };

    /**
     * Makes a request to the REST API to delete the tunnel having the given
     * UUID, effectively disconnecting the tunnel, returning a promise that can
     * be used for processing the results of the call.
     *
     * @param {String} uuid
     *     The UUID of the tunnel to delete.
     *
     * @returns {Promise}
     *     A promise for the HTTP call which will succeed if and only if the
     *     delete operation is successful.
     */
    service.deleteActiveTunnel = function deleteActiveTunnel(uuid) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Delete connection
        return $http({
            method  : 'DELETE',
            url     : 'api/tunnels/' + encodeURIComponent(uuid),
            params  : httpParameters
        });

    };

    return service;

}]);
