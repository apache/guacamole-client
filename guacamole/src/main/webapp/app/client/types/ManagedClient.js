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
 * Provides the ManagedClient class used by the guacClientManager service.
 */
angular.module('client').factory('ManagedClient', ['$rootScope', '$injector',
    function defineManagedClient($rootScope, $injector) {

    // Required types
    var ClientProperties     = $injector.get('ClientProperties');
    var ManagedClientState   = $injector.get('ManagedClientState');
    var ManagedDisplay       = $injector.get('ManagedDisplay');
    var ManagedFileDownload  = $injector.get('ManagedFileDownload');
    var ManagedFileUpload    = $injector.get('ManagedFileUpload');

    // Required services
    var $window                = $injector.get('$window');
    var $document              = $injector.get('$document');
    var authenticationService  = $injector.get('authenticationService');
    var connectionGroupService = $injector.get('connectionGroupService');
    var connectionService      = $injector.get('connectionService');
    var guacAudio              = $injector.get('guacAudio');
    var guacHistory            = $injector.get('guacHistory');
    var guacVideo              = $injector.get('guacVideo');
        
    /**
     * Object which serves as a surrogate interface, encapsulating a Guacamole
     * client while it is active, allowing it to be detached and reattached
     * from different client views.
     * 
     * @constructor
     * @param {ManagedClient|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     ManagedClient.
     */
    var ManagedClient = function ManagedClient(template) {

        // Use empty object by default
        template = template || {};

        /**
         * The ID of the connection associated with this client.
         *
         * @type String
         */
        this.id = template.id;

        /**
         * The actual underlying Guacamole client.
         *
         * @type Guacamole.Client
         */
        this.client = template.client;

        /**
         * The tunnel being used by the underlying Guacamole client.
         *
         * @type Guacamole.Tunnel
         */
        this.tunnel = template.tunnel;

        /**
         * The display associated with the underlying Guacamole client.
         * 
         * @type ManagedDisplay
         */
        this.managedDisplay = template.managedDisplay;

        /**
         * The name returned associated with the connection or connection
         * group in use.
         *
         * @type String
         */
        this.name = template.name;

        /**
         * The current clipboard contents.
         *
         * @type String
         */
        this.clipboardData = template.clipboardData;

        /**
         * All downloaded files. As files are downloaded, their progress can be
         * observed through the elements of this array. It is intended that
         * this array be manipulated externally as needed.
         *
         * @type ManagedFileDownload[]
         */
        this.downloads = template.downloads || [];

        /**
         * All uploaded files. As files are uploaded, their progress can be
         * observed through the elements of this array. It is intended that
         * this array be manipulated externally as needed.
         *
         * @type ManagedFileUpload[]
         */
        this.uploads = template.uploads || [];

        /**
         * The current state of the Guacamole client (idle, connecting,
         * connected, terminated with error, etc.).
         * 
         * @type ManagedClientState
         */
        this.clientState = template.clientState || new ManagedClientState();

        /**
         * Properties associated with the display and behavior of the Guacamole
         * client.
         *
         * @type ClientProperties
         */
        this.clientProperties = template.clientProperties || new ClientProperties();

    };

    /**
     * Returns the string of connection parameters to be passed to the
     * Guacamole client during connection. This string generally contains the
     * desired connection ID, display resolution, and supported audio/video
     * codecs.
     *
     * @param {String} id
     *     The ID of the connection or group to connect to.
     *
     * @param {String} [connectionParameters]
     *     Any additional HTTP parameters to pass while connecting.
     * 
     * @returns {String}
     *     The string of connection parameters to be passed to the Guacamole
     *     client.
     */
    var getConnectString = function getConnectString(id, connectionParameters) {

        // Calculate optimal width/height for display
        var pixel_density = $window.devicePixelRatio || 1;
        var optimal_dpi = pixel_density * 96;
        var optimal_width = $window.innerWidth * pixel_density;
        var optimal_height = $window.innerHeight * pixel_density;

        // Build base connect string
        var connectString =
              "id="         + encodeURIComponent(id)
            + "&authToken=" + encodeURIComponent(authenticationService.getCurrentToken())
            + "&width="     + Math.floor(optimal_width)
            + "&height="    + Math.floor(optimal_height)
            + "&dpi="       + Math.floor(optimal_dpi)
            + (connectionParameters ? '&' + connectionParameters : '');

        // Add audio mimetypes to connect_string
        guacAudio.supported.forEach(function(mimetype) {
            connectString += "&audio=" + encodeURIComponent(mimetype);
        });

        // Add video mimetypes to connect_string
        guacVideo.supported.forEach(function(mimetype) {
            connectString += "&video=" + encodeURIComponent(mimetype);
        });

        return connectString;

    };

    /**
     * Store the thumbnail of the given managed client within the connection
     * history under its associated ID. If the client is not connected, this
     * function has no effect.
     *
     * @param {String} managedClient
     *     The client whose history entry should be updated.
     */
    var updateHistoryEntry = function updateHistoryEntry(managedClient) {

        var display = managedClient.client.getDisplay();

        // Update stored thumbnail of previous connection 
        if (display && display.getWidth() > 0 && display.getHeight() > 0) {

            // Get screenshot
            var canvas = display.flatten();
            
            // Calculate scale of thumbnail (max 320x240, max zoom 100%)
            var scale = Math.min(320 / canvas.width, 240 / canvas.height, 1);
            
            // Create thumbnail canvas
            var thumbnail = $document[0].createElement("canvas");
            thumbnail.width  = canvas.width*scale;
            thumbnail.height = canvas.height*scale;
            
            // Scale screenshot to thumbnail
            var context = thumbnail.getContext("2d");
            context.drawImage(canvas,
                0, 0, canvas.width, canvas.height,
                0, 0, thumbnail.width, thumbnail.height
            );

            guacHistory.updateThumbnail(managedClient.id, thumbnail.toDataURL("image/png"));

        }

    };

    /**
     * Creates a new ManagedClient, connecting it to the specified connection
     * or group.
     *
     * @param {String} id
     *     The ID of the connection or group to connect to.
     *
     * @param {String} [connectionParameters]
     *     Any additional HTTP parameters to pass while connecting.
     * 
     * @returns {ManagedClient}
     *     A new ManagedClient instance which is connected to the connection or
     *     connection group having the given ID.
     */
    ManagedClient.getInstance = function getInstance(id, connectionParameters) {

        var tunnel;

        // If WebSocket available, try to use it.
        if ($window.WebSocket)
            tunnel = new Guacamole.ChainedTunnel(
                new Guacamole.WebSocketTunnel('websocket-tunnel'),
                new Guacamole.HTTPTunnel('tunnel')
            );
        
        // If no WebSocket, then use HTTP.
        else
            tunnel = new Guacamole.HTTPTunnel('tunnel');

        // Get new client instance
        var client = new Guacamole.Client(tunnel);

        // Associate new managed client with new client and tunnel
        var managedClient = new ManagedClient({
            id     : id,
            client : client,
            tunnel : tunnel
        });

        // Fire events for tunnel errors
        tunnel.onerror = function tunnelError(status) {
            $rootScope.$apply(function handleTunnelError() {
                ManagedClientState.setConnectionState(managedClient.clientState,
                    ManagedClientState.ConnectionState.TUNNEL_ERROR,
                    status.code);
            });
        };
        
        // Update connection state as tunnel state changes
        tunnel.onstatechange = function tunnelStateChanged(state) {
            $rootScope.$evalAsync(function updateTunnelState() {
                
                switch (state) {

                    // Connection is being established
                    case Guacamole.Tunnel.State.CONNECTING:
                        ManagedClientState.setConnectionState(managedClient.clientState,
                            ManagedClientState.ConnectionState.CONNECTING);
                        break;

                    // Connection has closed
                    case Guacamole.Tunnel.State.CLOSED:

                        updateHistoryEntry(managedClient);

                        ManagedClientState.setConnectionState(managedClient.clientState,
                            ManagedClientState.ConnectionState.DISCONNECTED);
                        break;
                    
                }
            
            });
        };

        // Update connection state as client state changes
        client.onstatechange = function clientStateChanged(clientState) {
            $rootScope.$evalAsync(function updateClientState() {

                switch (clientState) {

                    // Idle
                    case 0:
                        ManagedClientState.setConnectionState(managedClient.clientState,
                            ManagedClientState.ConnectionState.IDLE);
                        break;

                    // Connected + waiting
                    case 2:
                        ManagedClientState.setConnectionState(managedClient.clientState,
                            ManagedClientState.ConnectionState.WAITING);
                        break;

                    // Connected
                    case 3:
                        ManagedClientState.setConnectionState(managedClient.clientState,
                            ManagedClientState.ConnectionState.CONNECTED);
                        break;

                    // Connecting, disconnecting, and disconnected are all
                    // either ignored or handled by tunnel state

                    case 1: // Connecting
                    case 4: // Disconnecting
                    case 5: // Disconnected
                        break;

                }

            });
        };

        // Disconnect and update status when the client receives an error
        client.onerror = function clientError(status) {
            $rootScope.$apply(function handleClientError() {

                // Disconnect, if connected
                client.disconnect();

                // Update state
                ManagedClientState.setConnectionState(managedClient.clientState,
                    ManagedClientState.ConnectionState.CLIENT_ERROR,
                    status.code);

            });
        };

        // Handle any received clipboard data
        client.onclipboard = function clientClipboardReceived(stream, mimetype) {

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

            // Update state when done
            reader.onend = function clipboard_text_end() {
                $rootScope.$apply(function updateClipboard() {
                    managedClient.clipboardData = data;
                });
            };

        };

        // Handle any received files
        client.onfile = function clientFileReceived(stream, mimetype, filename) {
            $rootScope.$apply(function startDownload() {
                managedClient.downloads.push(ManagedFileDownload.getInstance(stream, mimetype, filename));
            });
        };

        // Manage the client display
        managedClient.managedDisplay = ManagedDisplay.getInstance(client.getDisplay());

        // Connect the Guacamole client
        client.connect(getConnectString(id, connectionParameters));

        // Determine type of connection
        var typePrefix = id.substring(0, 2);

        // If using a connection, pull connection name
        if (typePrefix === 'c/') {
            connectionService.getConnection(id.substring(2))
            .success(function connectionRetrieved(connection) {
                managedClient.name = connection.name;
            });
        }
        
        // If using a connection group, pull connection name
        else if (typePrefix === 'g/') {
            connectionGroupService.getConnectionGroup(id.substring(2))
            .success(function connectionGroupRetrieved(group) {
                managedClient.name = group.name;
            });
        }

        return managedClient;

    };

    /**
     * Uploads the given file to the server through the given Guacamole client.
     * The file transfer can be monitored through the corresponding entry in
     * the uploads array of the given managedClient.
     * 
     * @param {ManagedClient} managedClient
     *     The ManagedClient through which the file is to be uploaded.
     * 
     * @param {File} file
     *     The file to upload.
     */
    ManagedClient.uploadFile = function uploadFile(managedClient, file) {
        managedClient.uploads.push(ManagedFileUpload.getInstance(managedClient.client, file));
    };

    return ManagedClient;

}]);