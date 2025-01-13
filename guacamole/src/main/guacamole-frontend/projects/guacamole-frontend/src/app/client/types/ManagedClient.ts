

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

import { Form } from '../../rest/types/Form';
import { ClientProperties } from './ClientProperties';
import { ManagedClientState } from './ManagedClientState';
import { ManagedClientThumbnail } from './ManagedClientThumbnail';
import { ManagedDisplay } from './ManagedDisplay';
import { ManagedFileUpload } from './ManagedFileUpload';
import { ManagedShareLink } from './ManagedShareLink';
import { ManagedArgument } from './ManagedArgument';
import { ManagedFilesystem } from './ManagedFilesystem';
import { Optional } from '../../util/utility-types';

/**
 * Type definition for the onpipe handler accepted by the Guacamole.Client
 */
export type PipeStreamHandler = NonNullable<typeof Guacamole.Client.prototype.onpipe>;

/**
 * A deferred pipe stream, that has yet to be consumed, as well as all
 * axuilary information needed to pull data from the stream.
 */
export class DeferredPipeStream {

    /**
     * The stream that will receive data from the server.
     */
    stream: Guacamole.InputStream;

    /**
     * The mimetype of the data which will be received.
     */
    mimetype: string;

    /**
     * The name of the pipe.
     */
    name: string;

    /**
     * Creates a new DeferredPipeStream.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     DeferredPipeStream.
     */
    constructor(template: DeferredPipeStream) {

        this.stream = template.stream;
        this.mimetype = template.mimetype;
        this.name = template.name;

    }

}

/**
 * Type definition for the template parameter accepted by the ManagedClient constructor.
 * Properties that have a default value are optional.
 */
export type ManagedClientTemplate = Optional<ManagedClient,
    'managedDisplay'
    | 'name'
    | 'title'
    | 'thumbnail'
    | 'protocol'
    | 'forms'
    | 'requiredParameters'
    | 'uploads'
    | 'filesystems'
    | 'userCount'
    | 'users'
    | 'shareLinks'
    | 'multiTouchSupport'
    | 'clientState'
    | 'clientProperties'
    | 'arguments'
    | 'deferredPipeStreams'
    | 'deferredPipeStreamHandlers'
>;

/**
 * Object which serves as a surrogate interface, encapsulating a Guacamole
 * client while it is active, allowing it to be maintained in the
 * background. One or more ManagedClients are grouped within
 * ManagedClientGroups before being attached to the client view.
 */
export class ManagedClient {

    /**
     * The ID of the connection associated with this client.
     */
    id: string;

    /**
     * The actual underlying Guacamole client.
     */
    client: Guacamole.Client;

    /**
     * The tunnel being used by the underlying Guacamole client.
     */
    tunnel: Guacamole.Tunnel;

    /**
     * The display associated with the underlying Guacamole client.
     */
    managedDisplay?: ManagedDisplay;

    /**
     * The name returned associated with the connection or connection
     * group in use.
     */
    name?: string;

    /**
     * The title which should be displayed as the page title for this
     * client.
     */
    title?: string;

    /**
     * The name which uniquely identifies the protocol of the connection in
     * use. If the protocol cannot be determined, such as when a connection
     * group is in use, this will be null.
     */
    protocol: string | null;

    /**
     * An array of forms describing all known parameters for the connection
     * in use, including those which may not be editable.
     */
    forms: Form[];

    /**
     * The most recently-generated thumbnail for this connection, as
     * stored within the local connection history. If no thumbnail is
     * stored, this will be null.
     */
    thumbnail: ManagedClientThumbnail | null;

    /**
     * The current state of all parameters requested by the server via
     * "required" instructions, where each object key is the name of a
     * requested parameter and each value is the current value entered by
     * the user or null if no parameters are currently being requested.
     */
    requiredParameters: Record<string, string> | null;

    /**
     * All uploaded files. As files are uploaded, their progress can be
     * observed through the elements of this array. It is intended that
     * this array be manipulated externally as needed.
     */
    uploads: ManagedFileUpload[];

    /**
     * All currently-exposed filesystems. When the Guacamole server exposes
     * a filesystem object, that object will be made available as a
     * ManagedFilesystem within this array.
     */
    filesystems: ManagedFilesystem[];

    /**
     * The current number of users sharing this connection, excluding the
     * user that originally started the connection. Duplicate connections
     * from the same user are included in this total.
     */
    userCount: number;

    /**
     * All users currently sharing this connection, excluding the user that
     * originally started the connection. If the connection is not shared,
     * this object will be empty. This map consists of key/value pairs
     * where each key is the user's username and each value is an object
     * tracking the unique connections currently used by that user (a map
     * of Guacamole protocol user IDs to boolean values).
     */
    users: Record<string, Record<string, boolean>>;

    /**
     * All available share links generated for the this ManagedClient via
     * ManagedClient.createShareLink(). Each resulting share link is stored
     * under the identifier of its corresponding SharingProfile.
     */
    shareLinks: Record<string, ManagedShareLink>;

    /**
     * The number of simultaneous touch contacts supported by the remote
     * desktop. Unless explicitly declared otherwise by the remote desktop
     * after connecting, this will be 0 (multi-touch unsupported).
     */
    multiTouchSupport: number;

    /**
     * The current state of the Guacamole client (idle, connecting,
     * connected, terminated with error, etc.).
     */
    clientState: ManagedClientState;

    /**
     * Properties associated with the display and behavior of the Guacamole
     * client.
     */
    clientProperties: ClientProperties;

    /**
     * All editable arguments (connection parameters), stored by their
     * names. Arguments will only be present within this set if their
     * current values have been exposed by the server via an inbound "argv"
     * stream and the server has confirmed that the value may be changed
     * through a successful "ack" to an outbound "argv" stream.
     */
    arguments: Record<string, ManagedArgument>;

    /**
     * Any received pipe streams that have not been consumed by an onpipe
     * handler or registered pipe handler, indexed by pipe stream name.
     */
    deferredPipeStreams: Record<string, DeferredPipeStream> = {};

    /**
     * Handlers for deferred pipe streams, indexed by the name of the pipe
     * stream that the handler should handle.
     */
    deferredPipeStreamHandlers: Record<string, PipeStreamHandler> = {};

    /**
     * The mimetype of audio data to be sent along the Guacamole connection if
     * audio input is supported.
     */
    static readonly AUDIO_INPUT_MIMETYPE: string = 'audio/L16;rate=44100,channels=2';

    /**
     * Creates a new ManagedClient. This constructor initializes the properties of the
     * new ManagedClient with the corresponding properties of the given template.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     ManagedClient.
     */
    constructor(template: ManagedClientTemplate) {
        this.id = template.id;
        this.client = template.client;
        this.tunnel = template.tunnel;
        this.managedDisplay = template.managedDisplay;
        this.name = template.name;
        this.title = template.title;
        this.protocol = template.protocol || null;
        this.forms = template.forms || [];
        this.thumbnail = template.thumbnail = null;
        this.requiredParameters = null;
        this.uploads = template.uploads || [];
        this.filesystems = template.filesystems || [];
        this.userCount = template.userCount || 0;
        this.users = template.users || {};
        this.shareLinks = template.shareLinks || {};
        this.multiTouchSupport = template.multiTouchSupport || 0;
        this.clientState = template.clientState || new ManagedClientState();
        this.clientProperties = template.clientProperties || new ClientProperties();
        this.arguments = template.arguments || {};
        this.deferredPipeStreams = template.deferredPipeStreams || {};
        this.deferredPipeStreamHandlers = template.deferredPipeStreamHandlers || {};
    }


}
