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

/* global Guacamole, _ */

/**
 * Provides the ManagedClient class used by the guacClientManager service.
 */
angular.module('client').factory('ManagedClient', ['$rootScope', '$injector',
    function defineManagedClient($rootScope, $injector) {

    // Required types
    const ClientProperties       = $injector.get('ClientProperties');
    const ClientIdentifier       = $injector.get('ClientIdentifier');
    const ClipboardData          = $injector.get('ClipboardData');
    const ManagedArgument        = $injector.get('ManagedArgument');
    const ManagedClientState     = $injector.get('ManagedClientState');
    const ManagedClientThumbnail = $injector.get('ManagedClientThumbnail');
    const ManagedDisplay         = $injector.get('ManagedDisplay');
    const ManagedFilesystem      = $injector.get('ManagedFilesystem');
    const ManagedFileUpload      = $injector.get('ManagedFileUpload');
    const ManagedShareLink       = $injector.get('ManagedShareLink');
    const ManagedUSB             = $injector.get('ManagedUSB');

    // Required services
    const $document               = $injector.get('$document');
    const $q                      = $injector.get('$q');
    const $window                 = $injector.get('$window');
    const activeConnectionService = $injector.get('activeConnectionService');
    const authenticationService   = $injector.get('authenticationService');
    const clipboardService        = $injector.get('clipboardService');
    const connectionGroupService  = $injector.get('connectionGroupService');
    const connectionService       = $injector.get('connectionService');
    const preferenceService       = $injector.get('preferenceService');
    const requestService          = $injector.get('requestService');
    const tunnelService           = $injector.get('tunnelService');
    const guacAudio               = $injector.get('guacAudio');
    const guacHistory             = $injector.get('guacHistory');
    const guacImage               = $injector.get('guacImage');
    const guacVideo               = $injector.get('guacVideo');

    /**
     * The minimum amount of time to wait between updates to the client
     * thumbnail, in milliseconds.
     *
     * @type Number
     */
    var THUMBNAIL_UPDATE_FREQUENCY = 5000;

    /**
     * A deferred pipe stream, that has yet to be consumed, as well as all
     * axuilary information needed to pull data from the stream.
     *
     * @constructor
     * @param {DeferredPipeStream|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     DeferredPipeStream.
     */
    var DeferredPipeStream = function DeferredPipeStream(template) {

        // Use empty object by default
        template = template || {};

        /**
         * The stream that will receive data from the server.
         *
         * @type Guacamole.InputStream
         */
        this.stream = template.stream;

        /**
         * The mimetype of the data which will be received.
         *
         * @type String
         */
        this.mimetype = template.mimetype;

        /**
         * The name of the pipe.
         *
         * @type String
         */
        this.name = template.name;

    };

    /**
     * Object which serves as a surrogate interface, encapsulating a Guacamole
     * client while it is active, allowing it to be maintained in the
     * background. One or more ManagedClients are grouped within
     * ManagedClientGroups before being attached to the client view.
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
         * The title which should be displayed as the page title for this
         * client.
         *
         * @type String
         */
        this.title = template.title;

        /**
         * The name which uniquely identifies the protocol of the connection in
         * use. If the protocol cannot be determined, such as when a connection
         * group is in use, this will be null.
         *
         * @type {String}
         */
        this.protocol = template.protocol || null;

        /**
         * An array of forms describing all known parameters for the connection
         * in use, including those which may not be editable.
         *
         * @type {Form[]}
         */
        this.forms = template.forms || [];

        /**
         * The most recently-generated thumbnail for this connection, as
         * stored within the local connection history. If no thumbnail is
         * stored, this will be null.
         *
         * @type ManagedClientThumbnail
         */
        this.thumbnail = template.thumbnail;

        /**
         * The current state of all parameters requested by the server via
         * "required" instructions, where each object key is the name of a
         * requested parameter and each value is the current value entered by
         * the user or null if no parameters are currently being requested.
         *
         * @type Object.<String, String>
         */
        this.requiredParameters = null;

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
         * The current number of users sharing this connection, excluding the
         * user that originally started the connection. Duplicate connections
         * from the same user are included in this total.
         */
        this.userCount = template.userCount || 0;

        /**
         * All users currently sharing this connection, excluding the user that
         * originally started the connection. If the connection is not shared,
         * this object will be empty. This map consists of key/value pairs
         * where each key is the user's username and each value is an object
         * tracking the unique connections currently used by that user (a map
         * of Guacamole protocol user IDs to boolean values).
         *
         * @type Object.<string, Object.<string, boolean>>
         */
        this.users = template.users || {};

        /**
         * All available share links generated for the this ManagedClient via
         * ManagedClient.createShareLink(). Each resulting share link is stored
         * under the identifier of its corresponding SharingProfile.
         *
         * @type Object.<String, ManagedShareLink>
         */
        this.shareLinks = template.shareLinks || {};

        /**
         * The number of simultaneous touch contacts supported by the remote
         * desktop. Unless explicitly declared otherwise by the remote desktop
         * after connecting, this will be 0 (multi-touch unsupported).
         *
         * @type Number
         */
        this.multiTouchSupport = template.multiTouchSupport || 0;

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

        /**
         * All editable arguments (connection parameters), stored by their
         * names. Arguments will only be present within this set if their
         * current values have been exposed by the server via an inbound "argv"
         * stream and the server has confirmed that the value may be changed
         * through a successful "ack" to an outbound "argv" stream.
         *
         * @type {Object.<String, ManagedArgument>}
         */
        this.arguments = template.arguments || {};

        /**
         * Any received pipe streams that have not been consumed by an onpipe
         * handler or registered pipe handler, indexed by pipe stream name.
         *
         * @type {Object.<String, Object>}
         */
        this.deferredPipeStreams = template.deferredPipeStreams || {};

        /**
         * Handlers for deferred pipe streams, indexed by the name of the pipe
         * stream that the handler should handle.
         *
         * @type {Object.<String, Function>}
         */
        this.deferredPipeStreamHandlers = template.deferredPipeStreamHandlers || {};

        /**
         * All currently-connected USB devices for this client.
         *
         * @type ManagedUSB[]
         */
        this.usbDevices = template.usbDevices || [];

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
     * @param {number} [width]
     *     The optimal display width, in local CSS pixels. If omitted, the
     *     browser window width will be used.
     *
     * @param {number} [height]
     *     The optimal display height, in local CSS pixels. If omitted, the
     *     browser window height will be used.
     *
     * @returns {Promise.<String>}
     *     A promise which resolves with the string of connection parameters to
     *     be passed to the Guacamole client, once the string is ready.
     */
    const getConnectString = function getConnectString(identifier, width, height) {

        const deferred = $q.defer();

        // Calculate optimal width/height for display
        const pixel_density = $window.devicePixelRatio || 1;
        const optimal_dpi = pixel_density * 96;
        const optimal_width = width * pixel_density;
        const optimal_height = height * pixel_density;

        // Build base connect string
        let connectString =
              "token="             + encodeURIComponent(authenticationService.getCurrentToken())
            + "&GUAC_DATA_SOURCE=" + encodeURIComponent(identifier.dataSource)
            + "&GUAC_ID="          + encodeURIComponent(identifier.id)
            + "&GUAC_TYPE="        + encodeURIComponent(identifier.type)
            + "&GUAC_WIDTH="       + Math.floor(optimal_width)
            + "&GUAC_HEIGHT="      + Math.floor(optimal_height)
            + "&GUAC_DPI="         + Math.floor(optimal_dpi)
            + "&GUAC_TIMEZONE="    + encodeURIComponent(preferenceService.preferences.timezone);

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
     * Requests the creation of a new audio stream, recorded from the user's
     * local audio input device. If audio input is supported by the connection,
     * an audio stream will be created which will remain open until the remote
     * desktop requests that it be closed. If the audio stream is successfully
     * created but is later closed, a new audio stream will automatically be
     * established to take its place. The mimetype used for all audio streams
     * produced by this function is defined by
     * ManagedClient.AUDIO_INPUT_MIMETYPE.
     *
     * @param {Guacamole.Client} client
     *     The Guacamole.Client for which the audio stream is being requested.
     */
    var requestAudioStream = function requestAudioStream(client) {

        // Create new audio stream, associating it with an AudioRecorder
        var stream = client.createAudioStream(ManagedClient.AUDIO_INPUT_MIMETYPE);
        var recorder = Guacamole.AudioRecorder.getInstance(stream, ManagedClient.AUDIO_INPUT_MIMETYPE);

        // If creation of the AudioRecorder failed, simply end the stream
        if (!recorder)
            stream.sendEnd();

        // Otherwise, ensure that another audio stream is created after this
        // audio stream is closed
        else
            recorder.onclose = requestAudioStream.bind(this, client);

    };

    /**
     * Creates a new ManagedClient representing the specified connection or
     * connection group. The ManagedClient will not initially be connected,
     * and must be explicitly connected by invoking ManagedClient.connect().
     *
     * @param {String} id
     *     The ID of the connection or group to connect to. This String must be
     *     a valid ClientIdentifier string, as would be generated by
     *     ClientIdentifier.toString().
     *
     * @returns {ManagedClient}
     *     A new ManagedClient instance which represents the connection or
     *     connection group having the given ID.
     */
    ManagedClient.getInstance = function getInstance(id) {

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

        // Pull protocol-specific information from tunnel once tunnel UUID is
        // known
        tunnel.onuuid = function tunnelAssignedUUID(uuid) {
            tunnelService.getProtocol(uuid).then(function protocolRetrieved(protocol) {
                managedClient.protocol = protocol.name;
                managedClient.forms = protocol.connectionForms;
            }, requestService.WARN);
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

                    // Connection is established / no longer unstable
                    case Guacamole.Tunnel.State.OPEN:
                        ManagedClientState.setTunnelUnstable(managedClient.clientState, false);
                        break;

                    // Connection is established but misbehaving
                    case Guacamole.Tunnel.State.UNSTABLE:
                        ManagedClientState.setTunnelUnstable(managedClient.clientState, true);
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
                    case Guacamole.Client.State.IDLE:
                        ManagedClientState.setConnectionState(managedClient.clientState,
                            ManagedClientState.ConnectionState.IDLE);
                        break;

                    // Connecting
                    case Guacamole.Client.State.CONNECTING:
                        ManagedClientState.setConnectionState(managedClient.clientState,
                            ManagedClientState.ConnectionState.CONNECTING);
                        break;

                    // Connected + waiting
                    case Guacamole.Client.State.WAITING:
                        ManagedClientState.setConnectionState(managedClient.clientState,
                            ManagedClientState.ConnectionState.WAITING);
                        break;

                    // Connected
                    case Guacamole.Client.State.CONNECTED:
                        ManagedClientState.setConnectionState(managedClient.clientState,
                            ManagedClientState.ConnectionState.CONNECTED);

                        // Sync current clipboard data
                        clipboardService.getClipboard().then((data) => {
                            ManagedClient.setClipboard(managedClient, data);
                        }, angular.noop);

                        // Begin streaming audio input if possible
                        requestAudioStream(client);

                        // Update thumbnail with initial display contents
                        ManagedClient.updateThumbnail(managedClient);
                        break;

                    // Update history during disconnect phases
                    case Guacamole.Client.State.DISCONNECTING:
                    case Guacamole.Client.State.DISCONNECTED:
                        ManagedClient.updateThumbnail(managedClient);
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

        // Update user count when a new user joins
        client.onjoin = function userJoined(id, username) {
            $rootScope.$apply(function usersChanged() {

                var connections = managedClient.users[username] || {};
                managedClient.users[username] = connections;

                managedClient.userCount++;
                connections[id] = true;

            });
        };

        // Update user count when a user leaves
        client.onleave = function userLeft(id, username) {
            $rootScope.$apply(function usersChanged() {

                var connections = managedClient.users[username] || {};
                managedClient.users[username] = connections;

                managedClient.userCount--;
                delete connections[id];

                // Delete user entry after no connections remain
                if (_.isEmpty(connections))
                    delete managedClient.users[username];

            });
        };

        // Automatically update the client thumbnail
        client.onsync = function syncReceived() {

            var thumbnail = managedClient.thumbnail;
            var timestamp = new Date().getTime();

            // Update thumbnail if it doesn't exist or is old
            if (!thumbnail || timestamp - thumbnail.timestamp >= THUMBNAIL_UPDATE_FREQUENCY) {
                $rootScope.$apply(function updateClientThumbnail() {
                    ManagedClient.updateThumbnail(managedClient);
                });
            }

        };

        // A default onpipe implementation that will automatically defer any
        // received pipe streams, automatically invoking any registered handlers
        // that may already be set for the received name
        client.onpipe = (stream, mimetype, name) => {

            // Defer the pipe stream
            managedClient.deferredPipeStreams[name] = new DeferredPipeStream(
                    { stream, mimetype, name });

            // Invoke the handler now, if set
            const handler = managedClient.deferredPipeStreamHandlers[name];
            if (handler) {

                // Handle the stream, and clear from the deferred streams
                handler(stream, mimetype, name);
                delete managedClient.deferredPipeStreams[name];
            }
        };

        // Test for argument mutability whenever an argument value is
        // received
        client.onargv = function clientArgumentValueReceived(stream, mimetype, name) {

            // Ignore arguments which do not use a mimetype currently supported
            // by the web application
            if (mimetype !== 'text/plain')
                return;

            var reader = new Guacamole.StringReader(stream);

            // Assemble received data into a single string
            var value = '';
            reader.ontext = function textReceived(text) {
                value += text;
            };

            // Test mutability once stream is finished, storing the current
            // value for the argument only if it is mutable
            reader.onend = function textComplete() {
                ManagedArgument.getInstance(managedClient, name, value).then(function argumentIsMutable(argument) {
                    managedClient.arguments[name] = argument;
                }, function ignoreImmutableArguments() {});
            };

        };

        // Handle any received clipboard data
        client.onclipboard = function clientClipboardReceived(stream, mimetype) {

            var reader;

            // If the received data is text, read it as a simple string
            if (/^text\//.exec(mimetype)) {

                reader = new Guacamole.StringReader(stream);

                // Assemble received data into a single string
                var data = '';
                reader.ontext = function textReceived(text) {
                    data += text;
                };

                // Set clipboard contents once stream is finished
                reader.onend = function textComplete() {
                    clipboardService.setClipboard(new ClipboardData({
                        source : managedClient.id,
                        type : mimetype,
                        data : data
                    }))['catch'](angular.noop);
                };

            }

            // Otherwise read the clipboard data as a Blob
            else {
                reader = new Guacamole.BlobReader(stream, mimetype);
                reader.onend = function blobComplete() {
                    clipboardService.setClipboard(new ClipboardData({
                        source : managedClient.id,
                        type : mimetype,
                        data : reader.getBlob()
                    }))['catch'](angular.noop);
                };
            }

        };

        // Update level of multi-touch support when known
        client.onmultitouch = function multiTouchSupportDeclared(layer, touches) {
            managedClient.multiTouchSupport = touches;
        };

        // Update title when a "name" instruction is received
        client.onname = function clientNameReceived(name) {
            $rootScope.$apply(function updateClientTitle() {
                managedClient.title = name;
            });
        };

        // Handle any received files
        client.onfile = function clientFileReceived(stream, mimetype, filename) {
            tunnelService.downloadStream(tunnel.uuid, stream, mimetype, filename);
        };

        // Handle any received filesystem objects
        client.onfilesystem = function fileSystemReceived(object, name) {
            $rootScope.$apply(function exposeFilesystem() {
                managedClient.filesystems.push(ManagedFilesystem.getInstance(managedClient, object, name));
            });
        };

        // Handle any received prompts
        client.onrequired = function onrequired(parameters) {
            $rootScope.$apply(function promptUser() {
                managedClient.requiredParameters = {};
                angular.forEach(parameters, function populateParameter(name) {
                    managedClient.requiredParameters[name] = '';
                });
            });
        };
        
        // Handle USB disconnection notifications from server  
        client.onusbdisconnect = function usbDisconnectNotify(deviceId) {
            $rootScope.$apply(function handleUSBDisconnect() {
                
                // Find the corresponding ManagedUSB device
                const managedUSB = managedClient.usbDevices.find(usb => 
                    (usb.id || usb.device.serialNumber) === deviceId);
                    
                if (managedUSB) {
                    console.log("Server requested USB device disconnection:", deviceId);
                    
                    // Disconnect the device locally
                    ManagedClient.disconnectUSBDevice(managedClient, managedUSB)
                        .catch(error => {
                            console.error("Error disconnecting USB device:", error);
                        });
                }
            });
        };
        
        // Handle incoming data to a specific USB device endpoint
        client.onusbdata = function usbDataReceived(deviceId, endpointNumber, data) {
            // Find the corresponding ManagedUSB device
            const managedUSB = managedClient.usbDevices.find(usb => 
                (usb.id || usb.device.serialNumber) === deviceId);

            if (managedUSB)
                // Forward data with endpoint information to the ManagedUSB instance
                managedUSB.handleRemoteData(data, endpointNumber);

            else
                console.warn("Received USB data for unknown device:", deviceId);
        };

        // Manage the client display
        managedClient.managedDisplay = ManagedDisplay.getInstance(client.getDisplay());

        // Parse connection details from ID
        var clientIdentifier = ClientIdentifier.fromString(id);

        // Defer actually connecting the Guacamole client until
        // ManagedClient.connect() is explicitly invoked

        // If using a connection, pull connection name and protocol information
        if (clientIdentifier.type === ClientIdentifier.Types.CONNECTION) {
            connectionService.getConnection(clientIdentifier.dataSource, clientIdentifier.id)
            .then(function connectionRetrieved(connection) {
                managedClient.name = managedClient.title = connection.name;
            }, requestService.WARN);
        }
        
        // If using a connection group, pull connection name
        else if (clientIdentifier.type === ClientIdentifier.Types.CONNECTION_GROUP) {
            connectionGroupService.getConnectionGroup(clientIdentifier.dataSource, clientIdentifier.id)
            .then(function connectionGroupRetrieved(group) {
                managedClient.name = managedClient.title = group.name;
            }, requestService.WARN);
        }

        // If using an active connection, pull corresponding connection, then
        // pull connection name and protocol information from that
        else if (clientIdentifier.type === ClientIdentifier.Types.ACTIVE_CONNECTION) {
            activeConnectionService.getActiveConnection(clientIdentifier.dataSource, clientIdentifier.id)
            .then(function activeConnectionRetrieved(activeConnection) {

                // Attempt to retrieve connection details only if the
                // underlying connection is known
                if (activeConnection.connectionIdentifier) {
                    connectionService.getConnection(clientIdentifier.dataSource, activeConnection.connectionIdentifier)
                    .then(function connectionRetrieved(connection) {
                        managedClient.name = managedClient.title = connection.name;
                    }, requestService.WARN);
                }

            }, requestService.WARN);
        }

        return managedClient;

    };

    /**
     * Connects the given ManagedClient instance to its associated connection
     * or connection group. If the ManagedClient has already been connected,
     * including if connected but subsequently disconnected, this function has
     * no effect.
     *
     * @param {ManagedClient} managedClient
     *     The ManagedClient to connect.
     *
     * @param {number} [width]
     *     The optimal display width, in local CSS pixels. If omitted, the
     *     browser window width will be used.
     *
     * @param {number} [height]
     *     The optimal display height, in local CSS pixels. If omitted, the
     *     browser window height will be used.
     */
    ManagedClient.connect = function connect(managedClient, width, height) {

        // Ignore if already connected
        if (managedClient.clientState.connectionState !== ManagedClientState.ConnectionState.IDLE)
            return;

        // Parse connection details from ID
        const clientIdentifier = ClientIdentifier.fromString(managedClient.id);

        // Connect the Guacamole client
        getConnectString(clientIdentifier, width, height)
        .then(function connectClient(connectString) {
            managedClient.client.connect(connectString);
        });

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
        managedClient.uploads.push(ManagedFileUpload.getInstance(managedClient, file, object, streamName));

    };

    /**
     * Sends the given clipboard data over the given Guacamole client, setting
     * the contents of the remote clipboard to the data provided. If the given
     * clipboard data was originally received from that client, the data is
     * ignored and this function has no effect.
     *
     * @param {ManagedClient} managedClient
     *     The ManagedClient over which the given clipboard data is to be sent.
     *
     * @param {ClipboardData} data
     *     The clipboard data to send.
     */
    ManagedClient.setClipboard = function setClipboard(managedClient, data) {

        // Ignore clipboard data that was received from this connection
        if (data.source === managedClient.id)
            return;

        var writer;

        // Create stream with proper mimetype
        var stream = managedClient.client.createClipboardStream(data.type);

        // Send data as a string if it is stored as a string
        if (typeof data.data === 'string') {
            writer = new Guacamole.StringWriter(stream);
            writer.sendText(data.data);
            writer.sendEnd();
        }

        // Otherwise, assume the data is a File/Blob
        else {

            // Write File/Blob asynchronously
            writer = new Guacamole.BlobWriter(stream);
            writer.oncomplete = function clipboardSent() {
                writer.sendEnd();
            };

            // Begin sending data
            writer.sendBlob(data.data);

        }

    };

    /**
     * Assigns the given value to the connection parameter having the given
     * name, updating the behavior of the connection in real-time. If the
     * connection parameter is not editable, this function has no effect.
     *
     * @param {ManagedClient} managedClient
     *     The ManagedClient instance associated with the active connection
     *     being modified.
     *
     * @param {String} name
     *     The name of the connection parameter to modify.
     *
     * @param {String} value
     *     The value to attempt to assign to the given connection parameter.
     */
    ManagedClient.setArgument = function setArgument(managedClient, name, value) {
        var managedArgument = managedClient.arguments[name];
        managedArgument && ManagedArgument.setValue(managedArgument, value);
    };

    /**
     * Sends the given connection parameter values using "argv" streams,
     * updating the behavior of the connection in real-time if the server is
     * expecting or requiring these parameters.
     *
     * @param {ManagedClient} managedClient
     *     The ManagedClient instance associated with the active connection
     *     being modified.
     *
     * @param {Object.<String, String>} values
     *     The set of values to attempt to assign to corresponding connection
     *     parameters, where each object key is the connection parameter being
     *     set.
     */
    ManagedClient.sendArguments = function sendArguments(managedClient, values) {
        angular.forEach(values, function sendArgument(value, name) {
            var stream = managedClient.client.createArgumentValueStream("text/plain", name);
            var writer = new Guacamole.StringWriter(stream);
            writer.sendText(value);
            writer.sendEnd();
        });
    };

    /**
     * Retrieves the current values of all editable connection parameters as a
     * set of name/value pairs suitable for use as the model of a form which
     * edits those parameters.
     *
     * @param {ManagedClient} client
     *     The ManagedClient instance associated with the active connection
     *     whose parameter values are being retrieved.
     *
     * @returns {Object.<String, String>}
     *     A new set of name/value pairs containing the current values of all
     *     editable parameters.
     */
    ManagedClient.getArgumentModel = function getArgumentModel(client) {

        var model = {};

        angular.forEach(client.arguments, function addModelEntry(managedArgument) {
            model[managedArgument.name] = managedArgument.value;
        });

        return model;

    };

    /**
     * Produces a sharing link for the given ManagedClient using the given
     * sharing profile. The resulting sharing link, and any required login
     * information, can be retrieved from the <code>shareLinks</code> property
     * of the given ManagedClient once the various underlying service calls
     * succeed.
     *
     * @param {ManagedClient} client
     *     The ManagedClient which will be shared via the generated sharing
     *     link.
     *
     * @param {SharingProfile} sharingProfile
     *     The sharing profile to use to generate the sharing link.
     *
     * @returns {Promise}
     *     A Promise which is resolved once the sharing link has been
     *     successfully generated, and rejected if generating the link fails.
     */
    ManagedClient.createShareLink = function createShareLink(client, sharingProfile) {

        // Retrieve sharing credentials for the sake of generating a share link
        var credentialRequest = tunnelService.getSharingCredentials(
                client.tunnel.uuid, sharingProfile.identifier);

        // Add a new share link once the credentials are ready
        credentialRequest.then(function sharingCredentialsReceived(sharingCredentials) {
            client.shareLinks[sharingProfile.identifier] =
                ManagedShareLink.getInstance(sharingProfile, sharingCredentials);
        }, requestService.WARN);

        return credentialRequest;

    };

    /**
     * Returns whether the given ManagedClient is being shared. A ManagedClient
     * is shared if it has any associated share links.
     *
     * @param {ManagedClient} client
     *     The ManagedClient to check.
     *
     * @returns {Boolean}
     *     true if the ManagedClient has at least one associated share link,
     *     false otherwise.
     */
    ManagedClient.isShared = function isShared(client) {

        // The connection is shared if at least one share link exists
        for (var dummy in client.shareLinks)
            return true;

        // No share links currently exist
        return false;

    };
    
    /**
     * Returns whether the given client has any associated file transfers,
     * regardless of those file transfers' state.
     *
     * @param {GuacamoleClient} client
     *     The client for which file transfers should be checked.
     * 
     * @returns {boolean}
     *     true if there are any file transfers associated with the
     *     given client, false otherwise.
     */
    ManagedClient.hasTransfers = function hasTransfers(client) {
        return !!(client && client.uploads && client.uploads.length);
    };

    /**
     * Store the thumbnail of the given managed client within the connection
     * history under its associated ID. If the client is not connected, this
     * function has no effect.
     *
     * @param {ManagedClient} managedClient
     *     The client whose history entry should be updated.
     */
    ManagedClient.updateThumbnail = function updateThumbnail(managedClient) {

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

            // Store updated thumbnail within client
            managedClient.thumbnail = new ManagedClientThumbnail({
                timestamp : new Date().getTime(),
                canvas    : thumbnail
            });

            // Update historical thumbnail
            guacHistory.updateThumbnail(managedClient.id, thumbnail.toDataURL("image/png"));

        }

    };


    /**
     * Register a handler that will be automatically invoked for any deferred
     * pipe stream with the provided name, either when a pipe stream with a
     * name matching a registered handler is received, or immediately when this
     * function is called, if such a pipe stream has already been received.
     *
     * NOTE: Pipe streams are automatically deferred by the default onpipe
     * implementation. To preserve this behavior when using a custom onpipe
     * callback, make sure to defer to the default implementation as needed.
     *
     * @param {ManagedClient} managedClient
     *     The client for which the deferred pipe stream handler should be set.
     *
     * @param {String} name
     *     The name of the pipe stream that should be handeled by the provided
     *     handler. If another handler is already registered for this name, it
     *     will be replaced by the handler provided to this function.
     *
     * @param {Function} handler
     *     The handler that should handle any deferred pipe stream with the
     *     provided name. This function must take the same arguments as the
     *     standard onpipe handler - namely, the stream itself, the mimetype,
     *     and the name.
     */
    ManagedClient.registerDeferredPipeHandler = function registerDeferredPipeHandler(
            managedClient, name, handler) {
        managedClient.deferredPipeStreamHandlers[name] = handler;

        // Invoke the handler now, if the pipestream has already been received
        if (managedClient.deferredPipeStreams[name]) {

            // Invoke the handler with the deferred pipe stream
            var deferredStream = managedClient.deferredPipeStreams[name];
            handler(deferredStream.stream,
                    deferredStream.mimetype,
                    deferredStream.name);

            // Clean up the now-consumed pipe stream
            delete managedClient.deferredPipeStreams[name];
        }
    };

    /**
     * Detach the provided deferred pipe stream handler, if it is currently
     * registered for the provided pipe stream name.
     *
     * @param {String} name
     *     The name of the associated pipe stream for the handler that should
     *     be detached.
     *
     * @param {Function} handler
     *     The handler that should be detached.
     *
     * @param {ManagedClient} managedClient
     *     The client for which the deferred pipe stream handler should be
     *     detached.
     */
    ManagedClient.detachDeferredPipeHandler = function detachDeferredPipeHandler(
        managedClient, name, handler) {

        // Remove the handler if found
        if (managedClient.deferredPipeStreamHandlers[name] === handler)
            delete managedClient.deferredPipeStreamHandlers[name];
    };

    /**
     * Connects a WebUSB device to this client, creating a ManagedUSB
     * instance to handle the device connection.
     *
     * @param {ManagedClient} managedClient
     *     The client that should use the USB device.
     *
     * @param {USBDevice} device
     *     The WebUSB device to connect.
     *
     * @returns {Promise.<ManagedUSB>}
     *     A promise that resolves with the created ManagedUSB instance when
     *     the device is connected, or rejects if the connection fails.
     */
    ManagedClient.connectUSBDevice = function connectUSBDevice(managedClient, device) {
        
        // Create a ManagedUSB instance to handle this device
        var managedUSB = ManagedUSB.getInstance(managedClient, device);
        
        // Add to the client's collection of USB devices
        managedClient.usbDevices.push(managedUSB);
        
        // Connect the device
        return managedUSB.connect()
            .catch(function connectionFailed(error) {
                // Remove from collection if connection fails
                var index = managedClient.usbDevices.indexOf(managedUSB);
                if (index !== -1)
                    managedClient.usbDevices.splice(index, 1);

                throw error;
            });
        
    };

    /**
     * Disconnects a USB device from this client.
     *
     * @param {ManagedClient} managedClient
     *     The client the device is connected to.
     *
     * @param {ManagedUSB} managedUSB
     *     The ManagedUSB device to disconnect.
     *
     * @returns {Promise}
     *     A promise that resolves when the device is disconnected.
     */
    ManagedClient.disconnectUSBDevice = function disconnectUSBDevice(
            managedClient, managedUSB) {
        
        // Don't proceed if the device isn't in this client's collection
        var index = managedClient.usbDevices.indexOf(managedUSB);
        if (index === -1)
            return $q.resolve();
        
        // Remove from collection
        managedClient.usbDevices.splice(index, 1);
        
        // Disconnect the device
        return managedUSB.disconnect();
        
    };

    return ManagedClient;

}]);
