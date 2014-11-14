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
 * A service for creating Guacamole clients.
 */
angular.module('client').factory('guacClientFactory', ['$rootScope',
        function guacClientFactory($rootScope) {

    var service = {};

    /**
     * Returns a new Guacamole client instance which connects using the
     * provided tunnel.
     *
     * @param {Guacamole.Tunnel} tunnel The tunnel to connect through.
     * @returns {Guacamole.Client} A new Guacamole client instance.
     */
    service.getInstance = function getClientInstance(tunnel) {

        // Instantiate client
        var guac = new Guacamole.Client(tunnel);

        /*
         * Fire guacClientStateChange events when client state changes.
         */
        guac.onstatechange = function onClientStateChange(clientState) {

            switch (clientState) {

                // Idle
                case 0:
                    $rootScope.$broadcast('guacClientStateChange', guac, "idle");
                    break;

                // Connecting
                case 1:
                    $rootScope.$broadcast('guacClientStateChange', guac, "connecting");
                    break;

                // Connected + waiting
                case 2:
                    $rootScope.$broadcast('guacClientStateChange', guac, "waiting");
                    break;

                // Connected
                case 3:
                    $rootScope.$broadcast('guacClientStateChange', guac, "connected");

                    // Update server clipboard with current data
                    if ($rootScope.clipboard)
                        guac.setClipboard($rootScope.clipboard);

                    break;

                // Disconnecting / disconnected are handled by tunnel instead
                case 4:
                case 5:
                    break;

            }
        };
        
        /*
         * Fire guacClientName events when a new name is received.
         */
        guac.onname = function onClientName(name) {
            $rootScope.$broadcast('guacClientName', guac, name);
        };

        /*
         * Disconnect and fire guacClientError when the client receives an
         * error.
         */
        guac.onerror = function onClientError(status) {

            // Disconnect, if connected
            guac.disconnect();
            
            $rootScope.$broadcast('guacClientError', guac, status.code);

        };

        /*
         * Fire guacClientClipboard events after new clipboard data is received.
         */
        guac.onclipboard = function onClientClipboard(stream, mimetype) {

            // Only text/plain is supported for now
            if (mimetype !== "text/plain") {
                stream.sendAck("Only text/plain supported", Guacamole.Status.Code.UNSUPPORTED);
                return;
            }

            var reader = new Guacamole.StringReader(stream);
            var data = "";

            // Append any received data to buffer
            reader.ontext = function clipboard_text_received(text) {
                data += text;
                stream.sendAck("Received", Guacamole.Status.Code.SUCCESS);
            };

            // Emit event when done
            reader.onend = function clipboard_text_end() {
                $rootScope.$broadcast('guacClientClipboard', guac, data);
            };

        };

        /*
         * Fire guacFileStart, guacFileProgress, and guacFileEnd events during
         * the receipt of files.
         */
        guac.onfile = function onClientFile(stream, mimetype, filename) {

            // Begin file download
            var guacFileStartEvent = $rootScope.$broadcast('guacFileStart', guac, stream.index, mimetype, filename);
            if (!guacFileStartEvent.defaultPrevented) {

                var blob_reader = new Guacamole.BlobReader(stream, mimetype);

                // Update progress as data is received
                blob_reader.onprogress = function onprogress() {
                    $rootScope.$broadcast('guacFileProgress', guac, stream.index, mimetype, filename);
                    stream.sendAck("Received", Guacamole.Status.Code.SUCCESS);
                };

                // When complete, prompt for download
                blob_reader.onend = function onend() {
                    $rootScope.$broadcast('guacFileEnd', guac, stream.index, mimetype, filename);
                };

                stream.sendAck("Ready", Guacamole.Status.Code.SUCCESS);
                
            }
            
            // Respond with UNSUPPORTED if download (default action) canceled within event handler
            else
                stream.sendAck("Download canceled", Guacamole.Status.Code.UNSUPPORTED);

        };

        return guac;
        
    };

    return service;

}]);
