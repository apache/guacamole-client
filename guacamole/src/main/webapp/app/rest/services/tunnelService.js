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
     * returning a promise that provides a map of @link{ActiveTunnel}
     * objects if successful.
     *
     * @returns {Promise.<Object.<String, ActiveTunnel>>}
     *     A promise which will resolve with a map of @link{ActiveTunnel}
     *     objects, where each key is the UUID of the corresponding tunnel.
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
     * Makes a request to the REST API to delete the tunnels having the given
     * UUIDs, effectively disconnecting the tunnels, returning a promise that
     * can be used for processing the results of the call.
     *
     * @param {String[]} uuids
     *     The UUIDs of the tunnels to delete.
     *
     * @returns {Promise}
     *     A promise for the HTTP call which will succeed if and only if the
     *     delete operation is successful.
     */
    service.deleteActiveTunnels = function deleteActiveTunnels(uuids) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Convert provided array of UUIDs to a patch
        var tunnelPatch = [];
        uuids.forEach(function addTunnelPatch(uuid) {
            tunnelPatch.push({
                op   : 'remove',
                path : '/' + uuid
            });
        });

        // Perform tunnel deletion via PATCH
        return $http({
            method  : 'PATCH',
            url     : 'api/tunnels',
            params  : httpParameters,
            data    : tunnelPatch
        });
        
    };

    return service;

}]);
