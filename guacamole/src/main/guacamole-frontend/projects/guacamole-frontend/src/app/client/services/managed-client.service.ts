import { DOCUMENT } from '@angular/common';
import { Inject, Injectable } from '@angular/core';

import { GuacAudioService, GuacImageService, GuacVideoService, } from 'guacamole-frontend-lib';
import isEmpty from 'lodash/isEmpty';
import { catchError } from 'rxjs';
import { AuthenticationService } from '../../auth/service/authentication.service';
import { ClipboardService } from '../../clipboard/services/clipboard.service';
import { ClipboardData } from '../../clipboard/types/ClipboardData';
import { GuacHistoryService } from '../../history/guac-history.service';
import { ClientIdentifierService } from '../../navigation/service/client-identifier.service';
import { ClientIdentifier } from '../../navigation/types/ClientIdentifier';
import { ActiveConnectionService } from '../../rest/service/active-connection.service';
import { ConnectionGroupService } from '../../rest/service/connection-group.service';
import { ConnectionService } from '../../rest/service/connection.service';
import { RequestService } from '../../rest/service/request.service';
import { TunnelService } from '../../rest/service/tunnel.service';
import { SharingProfile } from '../../rest/types/SharingProfile';
import { PreferenceService } from '../../settings/services/preference.service';
import { NOOP } from '../../util/noop';
import { ManagedArgument } from '../types/ManagedArgument';
import { DeferredPipeStream, ManagedClient, PipeStreamHandler } from '../types/ManagedClient';
import { ManagedClientState } from '../types/ManagedClientState';
import { ManagedClientThumbnail } from '../types/ManagedClientThumbnail';
import { ManagedDisplay } from '../types/ManagedDisplay';
import { ManagedFilesystem } from '../types/ManagedFilesystem';
import { ManagedShareLink } from '../types/ManagedShareLink';
import { ManagedFileUploadService } from './managed-file-upload.service';
import { ManagedFilesystemService } from './managed-filesystem.service';


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
 * A service for working with ManagedClient objects.
 */
@Injectable({
    providedIn: 'root'
})
export class ManagedClientService {

    /**
     * The minimum amount of time to wait between updates to the client
     * thumbnail, in milliseconds.
     */
    readonly THUMBNAIL_UPDATE_FREQUENCY: number = 5000;

    constructor(
        @Inject(DOCUMENT) private document: Document,
        private authenticationService: AuthenticationService,
        private preferenceService: PreferenceService,
        private tunnelService: TunnelService,
        private requestService: RequestService,
        private clipboardService: ClipboardService,
        private managedFilesystemService: ManagedFilesystemService,
        private managedFileUploadService: ManagedFileUploadService,
        private connectionService: ConnectionService,
        private connectionGroupService: ConnectionGroupService,
        private activeConnectionService: ActiveConnectionService,
        private guacHistory: GuacHistoryService,
        private guacAudio: GuacAudioService,
        private guacVideo: GuacVideoService,
        private guacImage: GuacImageService,
        private clientIdentifierService: ClientIdentifierService
    ) {
    }

    /**
     * Returns a promise which resolves with the string of connection
     * parameters to be passed to the Guacamole client during connection. This
     * string generally contains the desired connection ID, display resolution,
     * and supported audio/video/image formats. The returned promise is
     * guaranteed to resolve successfully.
     *
     * @param identifier
     *     The identifier representing the connection or group to connect to.
     *
     * @param width
     *     The optimal display width, in local CSS pixels.
     *
     * @param height
     *     The optimal display height, in local CSS pixels.
     *
     * @returns
     *     A promise which resolves with the string of connection parameters to
     *     be passed to the Guacamole client, once the string is ready.
     */
    private getConnectString(identifier: ClientIdentifier, width: number, height: number): Promise<string> {

        return new Promise<string>((resolve) => {

            // Calculate optimal width/height for display
            const pixel_density = window.devicePixelRatio || 1;
            const optimal_dpi = pixel_density * 96;
            const optimal_width = width * pixel_density;
            const optimal_height = height * pixel_density;

            // Build base connect string
            let connectString =
                'token=' + encodeURIComponent(this.authenticationService.getCurrentToken()!)
                + '&GUAC_DATA_SOURCE=' + encodeURIComponent(identifier.dataSource)
                + '&GUAC_ID=' + encodeURIComponent(identifier.id!)
                + '&GUAC_TYPE=' + encodeURIComponent(identifier.type)
                + '&GUAC_WIDTH=' + Math.floor(optimal_width)
                + '&GUAC_HEIGHT=' + Math.floor(optimal_height)
                + '&GUAC_DPI=' + Math.floor(optimal_dpi)
                + '&GUAC_TIMEZONE=' + encodeURIComponent(this.preferenceService.preferences.timezone);

            // Add audio mimetypes to connect string
            this.guacAudio.supported.forEach(function (mimetype) {
                connectString += '&GUAC_AUDIO=' + encodeURIComponent(mimetype);
            });

            // Add video mimetypes to connect string
            this.guacVideo.supported.forEach(function (mimetype) {
                connectString += '&GUAC_VIDEO=' + encodeURIComponent(mimetype);
            });

            // Add image mimetypes to connect string
            this.guacImage.getSupportedMimetypes().then(function supportedMimetypesKnown(mimetypes) {

                // Add each image mimetype
                mimetypes.forEach(mimetype => {
                    connectString += '&GUAC_IMAGE=' + encodeURIComponent(mimetype);
                });

                // Connect string is now ready - nothing else is deferred
                resolve(connectString);

            });
        });

    }

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
     * @param client
     *     The Guacamole.Client for which the audio stream is being requested.
     */
    private requestAudioStream(client: Guacamole.Client): void {

        // Create new audio stream, associating it with an AudioRecorder
        const stream = client.createAudioStream(ManagedClient.AUDIO_INPUT_MIMETYPE);
        const recorder = Guacamole.AudioRecorder.getInstance(stream, ManagedClient.AUDIO_INPUT_MIMETYPE);

        // If creation of the AudioRecorder failed, simply end the stream
        if (!recorder)
            stream.sendEnd();

            // Otherwise, ensure that another audio stream is created after this
        // audio stream is closed
        else
            recorder.onclose = this.requestAudioStream.bind(this, client);

    }

    /**
     * Creates a new ManagedClient representing the specified connection or
     * connection group. The ManagedClient will not initially be connected,
     * and must be explicitly connected by invoking ManagedClient.connect().
     *
     * @param id
     *     The ID of the connection or group to connect to. This String must be
     *     a valid ClientIdentifier string, as would be generated by
     *     ClientIdentifierService.toString().
     *
     * @returns
     *     A new ManagedClient instance which represents the connection or
     *     connection group having the given ID.
     */
    getInstance(id: string): ManagedClient {

        let tunnel: Guacamole.Tunnel;

        // If WebSocket available, try to use it.
        if ('WebSocket' in window)
            tunnel = new Guacamole.ChainedTunnel(
                new Guacamole.WebSocketTunnel('websocket-tunnel'),
                new Guacamole.HTTPTunnel('tunnel')
            );

        // If no WebSocket, then use HTTP.
        else
            tunnel = new Guacamole.HTTPTunnel('tunnel');

        // Get new client instance
        const client: Guacamole.Client = new Guacamole.Client(tunnel);

        // Associate new managed client with new client and tunnel
        const managedClient: ManagedClient = new ManagedClient({
            id: id,
            client: client,
            tunnel: tunnel
        });

        // Fire events for tunnel errors
        tunnel.onerror = function tunnelError(status: Guacamole.Status) {
            ManagedClientState.setConnectionState(managedClient.clientState,
                ManagedClientState.ConnectionState.TUNNEL_ERROR,
                status.code);
        };

        // Pull protocol-specific information from tunnel once tunnel UUID is
        // known
        tunnel.onuuid = (uuid: string) => {
            this.tunnelService.getProtocol(uuid).subscribe({
                next: protocol => {
                    managedClient.protocol = protocol.name !== undefined ? protocol.name : null;
                    managedClient.forms = protocol.connectionForms;
                }, error: this.requestService.WARN
            });
        };

        // Update connection state as tunnel state changes
        tunnel.onstatechange = (state: number) => {

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

        };

        // Update connection state as client state changes
        client.onstatechange = (clientState: number) => {

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
                    this.clipboardService.getClipboard().then((data) => {
                        this.setClipboard(managedClient, data);
                    });

                    // Begin streaming audio input if possible
                    this.requestAudioStream(client);

                    // Update thumbnail with initial display contents
                    this.updateThumbnail(managedClient);
                    break;

                // Update history during disconnect phases
                case Guacamole.Client.State.DISCONNECTING:
                case Guacamole.Client.State.DISCONNECTED:
                    this.updateThumbnail(managedClient);
                    break;

            }

        };

        // Disconnect and update status when the client receives an error
        client.onerror = (status: Guacamole.Status) => {

            // Disconnect, if connected
            client.disconnect();

            // Update state
            ManagedClientState.setConnectionState(managedClient.clientState,
                ManagedClientState.ConnectionState.CLIENT_ERROR,
                status.code);

        };

        // Update user count when a new user joins
        client.onjoin = (id: string, username: string) => {

            const connections = managedClient.users[username] || {};
            managedClient.users[username] = connections;

            managedClient.userCount++;
            connections[id] = true;

        };

        // Update user count when a user leaves
        client.onleave = function userLeft(id: string, username: string) {

            const connections = managedClient.users[username] || {};
            managedClient.users[username] = connections;

            managedClient.userCount--;
            delete connections[id];

            // Delete user entry after no connections remain
            if (isEmpty(connections))
                delete managedClient.users[username];

        };

        // Automatically update the client thumbnail
        client.onsync = () => {

            const thumbnail: ManagedClientThumbnail | null = managedClient.thumbnail;
            const timestamp: number = new Date().getTime();

            // Update thumbnail if it doesn't exist or is old
            if (!thumbnail || timestamp - thumbnail.timestamp >= this.THUMBNAIL_UPDATE_FREQUENCY) {
                this.updateThumbnail(managedClient);
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
        client.onargv = (stream: Guacamole.InputStream, mimetype: string, name: string) => {

            // Ignore arguments which do not use a mimetype currently supported
            // by the web application
            if (mimetype !== 'text/plain')
                return;

            const reader = new Guacamole.StringReader(stream);

            // Assemble received data into a single string
            let value = '';
            reader.ontext = text => {
                value += text;
            };

            // Test mutability once stream is finished, storing the current
            // value for the argument only if it is mutable
            reader.onend = () => {
                ManagedArgument.getInstance(managedClient, name, value).then(argument => {
                    managedClient.arguments[name] = argument;
                }, function ignoreImmutableArguments() {
                });
            };

        };

        // Handle any received clipboard data
        client.onclipboard = (stream: Guacamole.InputStream, mimetype: string) => {

            let reader: Guacamole.StringReader | Guacamole.BlobReader;

            // If the received data is text, read it as a simple string
            if (/^text\//.exec(mimetype)) {

                reader = new Guacamole.StringReader(stream);

                // Assemble received data into a single string
                let data = '';
                reader.ontext = text => {
                    data += text;
                };

                // Set clipboard contents once stream is finished
                reader.onend = () => {
                    this.clipboardService.setClipboard(new ClipboardData({
                        source: managedClient.id,
                        type: mimetype,
                        data: data
                    })).catch(NOOP);
                };

            }

            // Otherwise read the clipboard data as a Blob
            else {
                reader = new Guacamole.BlobReader(stream, mimetype);
                reader.onend = () => {
                    this.clipboardService.setClipboard(new ClipboardData({
                        source: managedClient.id,
                        type: mimetype,
                        data: (reader as Guacamole.BlobReader).getBlob()
                    })).catch(NOOP);
                };
            }

        };

        // Update level of multi-touch support when known
        client.onmultitouch = (layer: Guacamole.Display.VisibleLayer, touches: number) => {
            managedClient.multiTouchSupport = touches;
        };

        // Update title when a "name" instruction is received
        client.onname = name => {
            managedClient.title = name;
        };

        // Handle any received files
        client.onfile = (stream: Guacamole.InputStream, mimetype: string, filename: string) => {
            this.tunnelService.downloadStream(tunnel.uuid!, stream, mimetype, filename);
        };

        // Handle any received filesystem objects
        client.onfilesystem = (object: Guacamole.Object, name: string) => {
            managedClient.filesystems.push(this.managedFilesystemService.getInstance(managedClient, object, name));
        };

        // Handle any received prompts
        client.onrequired = (parameters: string[]) => {
            managedClient.requiredParameters = {};
            parameters.forEach(name => {
                managedClient.requiredParameters![name] = '';
            });
        };

        // Manage the client display
        managedClient.managedDisplay = ManagedDisplay.getInstance(client.getDisplay());

        // Parse connection details from ID
        const clientIdentifier = this.clientIdentifierService.fromString(id);

        // Defer actually connecting the Guacamole client until
        // ManagedClient.connect() is explicitly invoked

        // If using a connection, pull connection name and protocol information
        if (clientIdentifier.type === ClientIdentifier.Types.CONNECTION) {
            this.connectionService.getConnection(clientIdentifier.dataSource, clientIdentifier.id!)
                .subscribe({
                    next: connection => {
                        managedClient.name = managedClient.title = connection.name;
                    }, error: this.requestService.WARN
                });
        }

        // If using a connection group, pull connection name
        else if (clientIdentifier.type === ClientIdentifier.Types.CONNECTION_GROUP) {
            this.connectionGroupService.getConnectionGroup(clientIdentifier.dataSource, clientIdentifier.id)
                .subscribe({
                    next: group => {
                        managedClient.name = managedClient.title = group.name;
                    }, error: this.requestService.WARN
                });
        }

            // If using an active connection, pull corresponding connection, then
        // pull connection name and protocol information from that
        else if (clientIdentifier.type === ClientIdentifier.Types.ACTIVE_CONNECTION) {
            this.activeConnectionService.getActiveConnection(clientIdentifier.dataSource, clientIdentifier.id!)
                .subscribe({
                    next: activeConnection => {

                        // Attempt to retrieve connection details only if the
                        // underlying connection is known
                        if (activeConnection.connectionIdentifier) {
                            this.connectionService.getConnection(clientIdentifier.dataSource, activeConnection.connectionIdentifier)
                                .subscribe({
                                    next: connection => {
                                        managedClient.name = managedClient.title = connection.name;
                                    }, error: this.requestService.WARN
                                });
                        }

                    }, error: this.requestService.WARN
                });
        }

        return managedClient;

    }

    /**
     * Connects the given ManagedClient instance to its associated connection
     * or connection group. If the ManagedClient has already been connected,
     * including if connected but subsequently disconnected, this function has
     * no effect.
     *
     * @param managedClient
     *     The ManagedClient to connect.
     *
     * @param width
     *     The optimal display width, in local CSS pixels. If omitted, the
     *     browser window width will be used.
     *
     * @param height
     *     The optimal display height, in local CSS pixels. If omitted, the
     *     browser window height will be used.
     */
    connect(managedClient: ManagedClient, width: number, height: number): void {

        // Ignore if already connected
        if (managedClient.clientState.connectionState !== ManagedClientState.ConnectionState.IDLE)
            return;

        // Parse connection details from ID
        const clientIdentifier = this.clientIdentifierService.fromString(managedClient.id);

        // Connect the Guacamole client
        this.getConnectString(clientIdentifier, width, height)
            .then(function connectClient(connectString) {
                managedClient.client.connect(connectString);
            });

    }

    /**
     * Uploads the given file to the server through the given Guacamole client.
     * The file transfer can be monitored through the corresponding entry in
     * the uploads array of the given managedClient.
     *
     * @param managedClient
     *     The ManagedClient through which the file is to be uploaded.
     *
     * @param file
     *     The file to upload.
     *
     * @param filesystem
     *     The filesystem to upload the file to, if any. If not specified, the
     *     file will be sent as a generic Guacamole file stream.
     *
     * @param [directory=filesystem.currentDirectory]
     *     The directory within the given filesystem to upload the file to. If
     *     not specified, but a filesystem is given, the current directory of
     *     that filesystem will be used.
     */
    uploadFile(managedClient: ManagedClient, file: File, filesystem?: ManagedFilesystem, directory?: ManagedFilesystem.File): void {

        // Use generic Guacamole file streams by default
        let object = null;
        let streamName = null;

        // If a filesystem is given, determine the destination object and stream
        if (filesystem) {
            object = filesystem.object;
            streamName = (directory || filesystem.currentDirectory()).streamName + '/' + file.name;
        }

        // Start and manage file upload
        managedClient.uploads.push(this.managedFileUploadService.getInstance(managedClient, file, object, streamName));

    }

    /**
     * Sends the given clipboard data over the given Guacamole client, setting
     * the contents of the remote clipboard to the data provided. If the given
     * clipboard data was originally received from that client, the data is
     * ignored and this function has no effect.
     *
     * @param managedClient
     *     The ManagedClient over which the given clipboard data is to be sent.
     *
     * @param data
     *     The clipboard data to send.
     */
    setClipboard(managedClient: ManagedClient, data: ClipboardData): void {

        // Ignore clipboard data that was received from this connection
        if (data.source === managedClient.id)
            return;

        let writer: Guacamole.StringWriter | Guacamole.BlobWriter;

        // Create stream with proper mimetype
        const stream = managedClient.client.createClipboardStream(data.type);

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

    }

    /**
     * Assigns the given value to the connection parameter having the given
     * name, updating the behavior of the connection in real-time. If the
     * connection parameter is not editable, this function has no effect.
     *
     * @param managedClient
     *     The ManagedClient instance associated with the active connection
     *     being modified.
     *
     * @param name
     *     The name of the connection parameter to modify.
     *
     * @param value
     *     The value to attempt to assign to the given connection parameter.
     */
    setArgument(managedClient: ManagedClient, name: string, value: string): void {
        const managedArgument = managedClient.arguments[name];
        managedArgument && ManagedArgument.setValue(managedArgument, value);
    }

    /**
     * Sends the given connection parameter values using "argv" streams,
     * updating the behavior of the connection in real-time if the server is
     * expecting or requiring these parameters.
     *
     * @param managedClient
     *     The ManagedClient instance associated with the active connection
     *     being modified.
     *
     * @param values
     *     The set of values to attempt to assign to corresponding connection
     *     parameters, where each object key is the connection parameter being
     *     set.
     */
    sendArguments(managedClient: ManagedClient, values: Record<string, string> | null): void {
        for (const name in values) {
            const value = values[name];

            const stream = managedClient.client.createArgumentValueStream('text/plain', name);
            const writer = new Guacamole.StringWriter(stream);
            writer.sendText(value);
            writer.sendEnd();
        }
    }

    /**
     * Retrieves the current values of all editable connection parameters as a
     * set of name/value pairs suitable for use as the model of a form which
     * edits those parameters.
     *
     * @param client
     *     The ManagedClient instance associated with the active connection
     *     whose parameter values are being retrieved.
     *
     * @returns
     *     A new set of name/value pairs containing the current values of all
     *     editable parameters.
     */
    getArgumentModel(client: ManagedClient): Record<string, string> {

        const model: Record<string, string> = {};

        for (const argumentName in client.arguments) {
            const managedArgument = client.arguments[argumentName];

            model[managedArgument.name] = managedArgument.value;
        }

        return model;

    }

    /**
     * Produces a sharing link for the given ManagedClient using the given
     * sharing profile. The resulting sharing link, and any required login
     * information, can be retrieved from the <code>shareLinks</code> property
     * of the given ManagedClient once the various underlying service calls
     * succeed.
     *
     * @param client
     *     The ManagedClient which will be shared via the generated sharing
     *     link.
     *
     * @param sharingProfile
     *     The sharing profile to use to generate the sharing link.
     */
    createShareLink(client: ManagedClient, sharingProfile: SharingProfile): void {

        // Retrieve sharing credentials for the sake of generating a share link
        this.tunnelService.getSharingCredentials(client.tunnel.uuid!, sharingProfile.identifier!)
            .pipe(catchError(this.requestService.WARN))
            // Add a new share link once the credentials are ready
            .subscribe(sharingCredentials => {
                client.shareLinks[sharingProfile.identifier!] =
                    ManagedShareLink.getInstance(sharingProfile, sharingCredentials)
            });

    }

    /**
     * Returns whether the given ManagedClient is being shared. A ManagedClient
     * is shared if it has any associated share links.
     *
     * @param client
     *     The ManagedClient to check.
     *
     * @returns
     *     true if the ManagedClient has at least one associated share link,
     *     false otherwise.
     */
    isShared(client: ManagedClient): boolean {

        // The connection is shared if at least one share link exists
        for (const dummy in client.shareLinks)
            return true;

        // No share links currently exist
        return false;

    }

    /**
     * Returns whether the given client has any associated file transfers,
     * regardless of those file transfers' state.
     *
     * @param client
     *     The client for which file transfers should be checked.
     *
     * @returns
     *     true if there are any file transfers associated with the
     *     given client, false otherwise.
     */
    hasTransfers(client: ManagedClient): boolean {
        return !!(client && client.uploads && client.uploads.length);
    }

    /**
     * Store the thumbnail of the given managed client within the connection
     * history under its associated ID. If the client is not connected, this
     * function has no effect.
     *
     * @param managedClient
     *     The client whose history entry should be updated.
     */
    updateThumbnail(managedClient: ManagedClient): void {

        const display = managedClient.client.getDisplay();

        // Update stored thumbnail of previous connection
        if (display && display.getWidth() > 0 && display.getHeight() > 0) {

            // Get screenshot
            const canvas = display.flatten();

            // Calculate scale of thumbnail (max 320x240, max zoom 100%)
            const scale = Math.min(320 / canvas.width, 240 / canvas.height, 1);

            // Create thumbnail canvas
            const thumbnail = this.document.createElement('canvas');
            thumbnail.width = canvas.width * scale;
            thumbnail.height = canvas.height * scale;

            // Scale screenshot to thumbnail
            const context = thumbnail.getContext('2d') as CanvasRenderingContext2D;
            context.drawImage(canvas,
                0, 0, canvas.width, canvas.height,
                0, 0, thumbnail.width, thumbnail.height
            );

            // Store updated thumbnail within client
            managedClient.thumbnail = new ManagedClientThumbnail({
                timestamp: new Date().getTime(),
                canvas: thumbnail
            });

            // Update historical thumbnail
            this.guacHistory.updateThumbnail(managedClient.id, thumbnail.toDataURL('image/png'));

        }

    }

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
     * @param managedClient
     *     The client for which the deferred pipe stream handler should be set.
     *
     * @param name
     *     The name of the pipe stream that should be handeled by the provided
     *     handler. If another handler is already registered for this name, it
     *     will be replaced by the handler provided to this function.
     *
     * @param handler
     *     The handler that should handle any deferred pipe stream with the
     *     provided name. This function must take the same arguments as the
     *     standard onpipe handler - namely, the stream itself, the mimetype,
     *     and the name.
     */
    registerDeferredPipeHandler(managedClient: ManagedClient, name: string, handler: PipeStreamHandler): void {
        managedClient.deferredPipeStreamHandlers[name] = handler;

        // Invoke the handler now, if the pipestream has already been received
        if (managedClient.deferredPipeStreams[name]) {

            // Invoke the handler with the deferred pipe stream
            const deferredStream = managedClient.deferredPipeStreams[name];
            handler(deferredStream.stream,
                deferredStream.mimetype,
                deferredStream.name);

            // Clean up the now-consumed pipe stream
            delete managedClient.deferredPipeStreams[name];
        }
    }

    /**
     * Detach the provided deferred pipe stream handler, if it is currently
     * registered for the provided pipe stream name.
     *
     * @param managedClient
     *     The client for which the deferred pipe stream handler should be
     *     detached.
     *
     * @param name
     *     The name of the associated pipe stream for the handler that should
     *     be detached.
     *
     * @param handler
     *     The handler that should be detached.
     */
    detachDeferredPipeHandler(managedClient: ManagedClient, name: string, handler: PipeStreamHandler): void {

        // Remove the handler if found
        if (managedClient.deferredPipeStreamHandlers[name] === handler)
            delete managedClient.deferredPipeStreamHandlers[name];
    }

}
