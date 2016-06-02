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
 * Service for operating on the tunnels of in-progress connections (and their
 * underlying objects) via the REST API.
 */
angular.module('rest').factory('tunnelService', ['$injector',
        function tunnelService($injector) {

    // Required services
    var $http                 = $injector.get('$http');
    var $window               = $injector.get('$window');
    var authenticationService = $injector.get('authenticationService');

    var service = {};

    /**
     * Reference to the window.document object.
     *
     * @private
     * @type HTMLDocument
     */
    var document = $window.document;

    /**
     * Makes a request to the REST API to get the list of all tunnels
     * associated with in-progress connections, returning a promise that
     * provides an array of their UUIDs (strings) if successful.
     *
     * @returns {Promise.<String[]>>}
     *     A promise which will resolve with an array of UUID strings, uniquely
     *     identifying each active tunnel.
     */
    service.getTunnels = function getTunnels() {

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
     * Makes a request to the REST API to retrieve the contents of a stream
     * which has been created within the active Guacamole connection associated
     * with the given tunnel. The contents of the stream will automatically be
     * downloaded by the browser.
     *
     * WARNING: Like Guacamole's various reader implementations, this function
     * relies on assigning an "onend" handler to the stream object for the sake
     * of cleaning up resources after the stream closes. If the "onend" handler
     * is overwritten after this function returns, resources may not be
     * properly cleaned up.
     *
     * @param {String} tunnel
     *     The UUID of the tunnel associated with the Guacamole connection
     *     whose stream should be downloaded as a file.
     *
     * @param {Guacamole.InputStream} stream
     *     The stream whose contents should be downloaded.
     *
     * @param {String} mimetype
     *     The mimetype of the stream being downloaded. This is currently
     *     ignored, with the download forced by using
     *     "application/octet-stream".
     *
     * @param {String} filename
     *     The filename that should be given to the downloaded file.
     */
    service.downloadStream = function downloadStream(tunnel, stream, mimetype, filename) {

        // Build download URL
        var url = $window.location.origin
                + $window.location.pathname
                + 'api/tunnels/' + encodeURIComponent(tunnel)
                + '/streams/' + encodeURIComponent(stream.index)
                + '/' + encodeURIComponent(filename)
                + '?token=' + encodeURIComponent(authenticationService.getCurrentToken());

        // Create temporary hidden iframe to facilitate download
        var iframe = document.createElement('iframe');
        iframe.style.position = 'fixed';
        iframe.style.width = '1px';
        iframe.style.height = '1px';
        iframe.style.left = '-1px';
        iframe.style.top = '-1px';

        // The iframe MUST be part of the DOM for the download to occur
        document.body.appendChild(iframe);

        // Automatically remove iframe from DOM when download completes
        stream.onend = function downloadComplete() {
            document.body.removeChild(iframe);
        };

        // Begin download
        iframe.src = url;

    };

    return service;

}]);
