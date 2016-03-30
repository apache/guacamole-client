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
 * Provides the ManagedClient class used by the guacClientManager service.
 */
angular.module('client').factory('ManagedClient', ['$rootScope', '$injector',
    function defineManagedClient($rootScope, $injector) {

    // Required types
    var ClientProperties     = $injector.get('ClientProperties');
    var ClientIdentifier     = $injector.get('ClientIdentifier');
    var ManagedClientState   = $injector.get('ManagedClientState');
    var ManagedDisplay       = $injector.get('ManagedDisplay');
    var ManagedFileDownload  = $injector.get('ManagedFileDownload');
    var ManagedFilesystem    = $injector.get('ManagedFilesystem');
    var ManagedFileUpload    = $injector.get('ManagedFileUpload');

    // Required services
    var $document              = $injector.get('$document');
    var $q                     = $injector.get('$q');
    var $rootScope             = $injector.get('$rootScope');
    var $window                = $injector.get('$window');
    var authenticationService  = $injector.get('authenticationService');
    var clipboardService       = $injector.get('clipboardService');
    var connectionGroupService = $injector.get('connectionGroupService');
    var connectionService      = $injector.get('connectionService');
    var guacAudio              = $injector.get('guacAudio');
    var guacHistory            = $injector.get('guacHistory');
    var guacImage              = $injector.get('guacImage');
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
        this.clipboardData = template.clipboardData || '';

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
         * All currently-exposed filesystems. When the Guacamole server exposes
         * a filesystem object, that object will be made available as a
         * ManagedFilesystem within this array.
         *
         * @type ManagedFilesystem[]
         */
        this.filesystems = template.filesystems || [];

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
     * The mimetype of audio data to be sent along the Guacamole connection if
     * audio input is supported.
     *
     * @constant
     * @type String
     */
    ManagedClient.AUDIO_INPUT_MIMETYPE = 'audio/L16;rate=44100,channels=2';

    /**
     * Returns a promise which resolves with the string of connection
     * parameters to be passed to the Guacamole client during connection. This
     * string generally contains the desired connection ID, display resolution,
     * and supported audio/video/image formats. The returned promise is
     * guaranteed to resolve successfully.
     *
     * @param {ClientIdentifier} identifier
     *     The identifier representing the connection or group to connect to.
     *
     * @param {String} [connectionParameters]
     *     Any additional HTTP parameters to pass while connecting.
     * 
     * @returns {Promise.<String>}
     *     A promise which resolves with the string of connection parameters to
     *     be passed to the Guacamole client, once the string is ready.
     */
    var getConnectString = function getConnectString(identifier, connectionParameters) {

        var deferred = $q.defer();

        // Calculate optimal width/height for display
        var pixel_density = $window.devicePixelRatio || 1;
        var optimal_dpi = pixel_density * 96;
        var optimal_width = $window.innerWidth * pixel_density;
        var optimal_height = $window.innerHeight * pixel_density;

        // Build base connect string
        var connectString =
              "token="             + encodeURIComponent(authenticationService.getCurrentToken())
            + "&GUAC_DATA_SOURCE=" + encodeURIComponent(identifier.dataSource)
            + "&GUAC_ID="          + encodeURIComponent(identifier.id)
            + "&GUAC_TYPE="        + encodeURIComponent(identifier.type)
            + "&GUAC_WIDTH="       + Math.floor(optimal_width)
            + "&GUAC_HEIGHT="      + Math.floor(optimal_height)
            + "&GUAC_DPI="         + Math.floor(optimal_dpi)
            + (connectionParameters ? '&' + connectionParameters : '');

        // Add audio mimetypes to connect string
        guacAudio.supported.forEach(function(mimetype) {
            connectString += "&GUAC_AUDIO=" + encodeURIComponent(mimetype);
        });

        // Add video mimetypes to connect string
        guacVideo.supported.forEach(function(mimetype) {
            connectString += "&GUAC_VIDEO=" + encodeURIComponent(mimetype);
        });

        // Add image mimetypes to connect string
        guacImage.getSupportedMimetypes().then(function supportedMimetypesKnown(mimetypes) {

            // Add each image mimetype
            angular.forEach(mimetypes, function addImageMimetype(mimetype) {
                connectString += "&GUAC_IMAGE=" + encodeURIComponent(mimetype);
            });

            // Connect string is now ready - nothing else is deferred
            deferred.resolve(connectString);

        });

        return deferred.promise;

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
     *     The ID of the connection or group to connect to. This String must be
     *     a valid ClientIdentifier string, as would be generated by
     *     ClientIdentifier.toString().
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

                    // Ignore "connecting" state
                    case 1: // Connecting
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

                        // Begin streaming audio input if possible
                        var stream = client.createAudioStream(ManagedClient.AUDIO_INPUT_MIMETYPE);
                        if (!Guacamole.AudioRecorder.getInstance(stream, ManagedClient.AUDIO_INPUT_MIMETYPE))
                            stream.sendEnd();

                        break;

                    // Update history when disconnecting
                    case 4: // Disconnecting
                    case 5: // Disconnected
                        updateHistoryEntry(managedClient);
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
                    clipboardService.setLocalClipboard(data);
                });
            };

        };

        // Handle any received files
        client.onfile = function clientFileReceived(stream, mimetype, filename) {
            $rootScope.$apply(function startDownload() {
                managedClient.downloads.push(ManagedFileDownload.getInstance(stream, mimetype, filename));
            });
        };

        // Handle any received filesystem objects
        client.onfilesystem = function fileSystemReceived(object, name) {
            $rootScope.$apply(function exposeFilesystem() {
                managedClient.filesystems.push(ManagedFilesystem.getInstance(object, name));
            });
        };

        // Manage the client display
        managedClient.managedDisplay = ManagedDisplay.getInstance(client.getDisplay());

        // Parse connection details from ID
        var clientIdentifier = ClientIdentifier.fromString(id);

        // Connect the Guacamole client
        getConnectString(clientIdentifier, connectionParameters)
        .then(function connectClient(connectString) {
            client.connect(connectString);
        });

        // If using a connection, pull connection name
        if (clientIdentifier.type === ClientIdentifier.Types.CONNECTION) {
            connectionService.getConnection(clientIdentifier.dataSource, clientIdentifier.id)
            .success(function connectionRetrieved(connection) {
                managedClient.name = connection.name;
            });
        }
        
        // If using a connection group, pull connection name
        else if (clientIdentifier.type === ClientIdentifier.Types.CONNECTION_GROUP) {
            connectionGroupService.getConnectionGroup(clientIdentifier.dataSource, clientIdentifier.id)
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
     *
     * @param {ManagedFilesystem} [filesystem]
     *     The filesystem to upload the file to, if any. If not specified, the
     *     file will be sent as a generic Guacamole file stream.
     *
     * @param {ManagedFilesystem.File} [directory=filesystem.currentDirectory]
     *     The directory within the given filesystem to upload the file to. If
     *     not specified, but a filesystem is given, the current directory of
     *     that filesystem will be used.
     */
    ManagedClient.uploadFile = function uploadFile(managedClient, file, filesystem, directory) {

        // Use generic Guacamole file streams by default
        var object = null;
        var streamName = null;

        // If a filesystem is given, determine the destination object and stream
        if (filesystem) {
            object = filesystem.object;
            streamName = (directory || filesystem.currentDirectory).streamName + '/' + file.name;
        }

        // Start and manage file upload
        managedClient.uploads.push(ManagedFileUpload.getInstance(managedClient.client, file, object, streamName));

    };

    return ManagedClient;

}]);