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

    // Required types
    var Error = $injector.get('Error');

    // Required services
    var $q                    = $injector.get('$q');
    var $window               = $injector.get('$window');
    var authenticationService = $injector.get('authenticationService');
    var requestService        = $injector.get('requestService');

    var service = {};

    /**
     * Reference to the window.document object.
     *
     * @private
     * @type HTMLDocument
     */
    var document = $window.document;

    /**
     * The number of milliseconds to wait after a stream download has completed
     * before cleaning up related DOM resources, if the browser does not
     * otherwise notify us that cleanup is safe.
     *
     * @private
     * @constant
     * @type Number
     */
    var DOWNLOAD_CLEANUP_WAIT = 5000;

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
        return requestService({
            method  : 'GET',
            url     : 'api/session/tunnels',
            params  : httpParameters
        });

    };

    /**
     * Makes a request to the REST API to retrieve the underlying protocol of
     * the connection associated with a particular tunnel, returning a promise
     * that provides a @link{Protocol} object if successful.
     *
     * @param {String} tunnel
     *     The UUID of the tunnel associated with the Guacamole connection
     *     whose underlying protocol is being retrieved.
     *
     * @returns {Promise.<Protocol>}
     *     A promise which will resolve with a @link{Protocol} object upon
     *     success.
     */
    service.getProtocol = function getProtocol(tunnel) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Retrieve the protocol details of the specified tunnel
        return requestService({
            method  : 'GET',
            url     : 'api/session/tunnels/' + encodeURIComponent(tunnel)
                        + '/protocol',
            params  : httpParameters
        });

    };

    /**
     * Retrieves the set of sharing profiles that the current user can use to
     * share the active connection of the given tunnel.
     *
     * @param {String} tunnel
     *     The UUID of the tunnel associated with the Guacamole connection
     *     whose sharing profiles are being retrieved.
     *
     * @returns {Promise.<Object.<String, SharingProfile>>}
     *     A promise which will resolve with a map of @link{SharingProfile}
     *     objects where each key is the identifier of the corresponding
     *     sharing profile.
     */
    service.getSharingProfiles = function getSharingProfiles(tunnel) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Retrieve all associated sharing profiles
        return requestService({
            method  : 'GET',
            url     : 'api/session/tunnels/' + encodeURIComponent(tunnel)
                        + '/activeConnection/connection/sharingProfiles',
            params  : httpParameters
        });

    };

    /**
     * Makes a request to the REST API to generate credentials which have
     * access strictly to the active connection associated with the given
     * tunnel, using the restrictions defined by the given sharing profile,
     * returning a promise that provides the resulting @link{UserCredentials}
     * object if successful.
     *
     * @param {String} tunnel
     *     The UUID of the tunnel associated with the Guacamole connection
     *     being shared.
     *
     * @param {String} sharingProfile
     *     The identifier of the connection object dictating the
     *     semantics/restrictions which apply to the shared session.
     *
     * @returns {Promise.<UserCredentials>}
     *     A promise which will resolve with a @link{UserCredentials} object
     *     upon success.
     */
    service.getSharingCredentials = function getSharingCredentials(tunnel, sharingProfile) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Generate sharing credentials
        return requestService({
            method  : 'GET',
            url     : 'api/session/tunnels/' + encodeURIComponent(tunnel)
                        + '/activeConnection/sharingCredentials/'
                        + encodeURIComponent(sharingProfile),
            params  : httpParameters
        });

    };

    /**
     * Sanitize a filename, replacing all URL path seperators with safe
     * characters.
     *
     * @param {String} filename
     *     An unsanitized filename that may need cleanup.
     *
     * @returns {String}
     *     The sanitized filename.
     */
    var sanitizeFilename = function sanitizeFilename(filename) {
        return filename.replace(/[\\\/]+/g, '_');
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

        // Work-around for IE missing window.location.origin
        if (!$window.location.origin)
            var streamOrigin = $window.location.protocol + '//' + $window.location.hostname + ($window.location.port ? (':' + $window.location.port) : '');
        else
            var streamOrigin = $window.location.origin;

        // Build download URL
        var url = streamOrigin
                + $window.location.pathname
                + 'api/session/tunnels/' + encodeURIComponent(tunnel)
                + '/streams/' + encodeURIComponent(stream.index)
                + '/' + encodeURIComponent(sanitizeFilename(filename))
                + '?token=' + encodeURIComponent(authenticationService.getCurrentToken());

        // Create temporary hidden iframe to facilitate download
        var iframe = document.createElement('iframe');
        iframe.style.position = 'fixed';
        iframe.style.border = 'none';
        iframe.style.width = '1px';
        iframe.style.height = '1px';
        iframe.style.left = '-1px';
        iframe.style.top = '-1px';

        // The iframe MUST be part of the DOM for the download to occur
        document.body.appendChild(iframe);

        // Automatically remove iframe from DOM when download completes, if
        // browser supports tracking of iframe downloads via the "load" event
        iframe.onload = function downloadComplete() {
            document.body.removeChild(iframe);
        };

        // Acknowledge (and ignore) any received blobs
        stream.onblob = function acknowledgeData() {
            stream.sendAck('OK', Guacamole.Status.Code.SUCCESS);
        };

        // Automatically remove iframe from DOM a few seconds after the stream
        // ends, in the browser does NOT fire the "load" event for downloads
        stream.onend = function downloadComplete() {
            $window.setTimeout(function cleanupIframe() {
                if (iframe.parentElement) {
                    document.body.removeChild(iframe);
                }
            }, DOWNLOAD_CLEANUP_WAIT);
        };

        // Begin download
        iframe.src = url;

    };

    /**
     * Makes a request to the REST API to send the contents of the given file
     * along a stream which has been created within the active Guacamole
     * connection associated with the given tunnel. The contents of the file
     * will automatically be split into individual "blob" instructions, as if
     * sent by the connected Guacamole client.
     *
     * @param {String} tunnel
     *     The UUID of the tunnel associated with the Guacamole connection
     *     whose stream should receive the given file.
     *
     * @param {Guacamole.OutputStream} stream
     *     The stream that should receive the given file.
     *
     * @param {File} file
     *     The file that should be sent along the given stream.
     *
     * @param {Function} [progressCallback]
     *     An optional callback which, if provided, will be invoked as the
     *     file upload progresses. The current position within the file, in
     *     bytes, will be provided to the callback as the sole argument.
     *
     * @return {Promise}
     *     A promise which resolves when the upload has completed, and is
     *     rejected with an Error if the upload fails. The Guacamole protocol
     *     status code describing the failure will be included in the Error if
     *     available. If the status code is available, the type of the Error
     *     will be STREAM_ERROR.
     */
    service.uploadToStream = function uploadToStream(tunnel, stream, file,
        progressCallback) {

        var deferred = $q.defer();

        // Work-around for IE missing window.location.origin
        if (!$window.location.origin)
            var streamOrigin = $window.location.protocol + '//' + $window.location.hostname + ($window.location.port ? (':' + $window.location.port) : '');
        else
            var streamOrigin = $window.location.origin;

        // Build upload URL
        var url = streamOrigin
                + $window.location.pathname
                + 'api/session/tunnels/' + encodeURIComponent(tunnel)
                + '/streams/' + encodeURIComponent(stream.index)
                + '/' + encodeURIComponent(sanitizeFilename(file.name))
                + '?token=' + encodeURIComponent(authenticationService.getCurrentToken());

        var xhr = new XMLHttpRequest();

        // Invoke provided callback if upload tracking is supported
        if (progressCallback && xhr.upload) {
            xhr.upload.addEventListener('progress', function updateProgress(e) {
                progressCallback(e.loaded);
            });
        }

        // Resolve/reject promise once upload has stopped
        xhr.onreadystatechange = function uploadStatusChanged() {

            // Ignore state changes prior to completion
            if (xhr.readyState !== 4)
                return;

            // Resolve if HTTP status code indicates success
            if (xhr.status >= 200 && xhr.status < 300)
                deferred.resolve();

            // Parse and reject with resulting JSON error
            else if (xhr.getResponseHeader('Content-Type') === 'application/json')
                deferred.reject(angular.fromJson(xhr.responseText));

            // Warn of lack of permission of a proxy rejects the upload
            else if (xhr.status >= 400 && xhr.status < 500)
                deferred.reject(new Error({
                    'type'       : Error.Type.STREAM_ERROR,
                    'statusCode' : Guacamole.Status.Code.CLIENT_FORBIDDEN,
                    'message'    : 'HTTP ' + xhr.status
                }));

            // Assume internal error for all other cases
            else
                deferred.reject(new Error({
                    'type'       : Error.Type.STREAM_ERROR,
                    'statusCode' : Guacamole.Status.Code.INTERNAL_ERROR,
                    'message'    : 'HTTP ' + xhr.status
                }));

        };

        // Perform upload
        xhr.open('POST', url, true);
        xhr.send(file);

        return deferred.promise;

    };

    return service;

}]);
