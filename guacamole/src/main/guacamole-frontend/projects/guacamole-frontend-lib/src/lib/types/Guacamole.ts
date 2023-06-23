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

// @ts-ignore
declare namespace Guacamole {

    /**
     * Dynamic on-screen keyboard. Given the layout object for an on-screen
     * keyboard, this object will construct a clickable on-screen keyboard with its
     * own key events.
     */
    class OnScreenKeyboard {
        /**
         * The number of mousemove events to require before re-enabling mouse
         * event handling after receiving a touch event.
         *
         * @type {Number}
         */
        touchMouseThreshold: number;
        /**
         * The keyboard layout provided at time of construction.
         *
         * @type {Guacamole.OnScreenKeyboard.Layout}
         */
        layout: OnScreenKeyboard.Layout;

        constructor(layout: OnScreenKeyboard.Layout);

        /**
         * Fired whenever the user presses a key on this Guacamole.OnScreenKeyboard.
         *
         * @event
         * @param {Number} keysym The keysym of the key being pressed.
         */
        onkeydown(keysym: number): void;

        /**
         * Fired whenever the user releases a key on this Guacamole.OnScreenKeyboard.
         *
         * @event
         * @param {Number} keysym The keysym of the key being released.
         */
        onkeyup(keysym: number): void;

        /**
         * Returns the element containing the entire on-screen keyboard.
         * @returns {Element} The element containing the entire on-screen keyboard.
         */
        getElement(): any;

        /**
         * Resizes all elements within this Guacamole.OnScreenKeyboard such that
         * the width is close to but does not exceed the specified width. The
         * height of the keyboard is determined based on the width.
         *
         * @param {Number} width The width to resize this Guacamole.OnScreenKeyboard
         *                       to, in pixels.
         */
        resize(width: number): void;

        /**
         * Resets the state of this keyboard, releasing all keys, and firing keyup
         * events for each released key.
         */
        reset(): void;
    }


    namespace OnScreenKeyboard {


        class Layout {

            /**
             * The language of keyboard layout, such as "en_US". This property is for
             * informational purposes only, but it is recommend to conform to the
             * [language code]_[country code] format.
             *
             * @type {String}
             */
            language: string;
            /**
             * The type of keyboard layout, such as "qwerty". This property is for
             * informational purposes only, and does not conform to any standard.
             *
             * @type {String}
             */
            type: string;
            /**
             * Map of key name to corresponding keysym, title, or key object. If only
             * the keysym or title is provided, the key object will be created
             * implicitly. In all cases, the name property of the key object will be
             * taken from the name given in the mapping.
             *
             * @type {Object.<String, Number|String|Guacamole.OnScreenKeyboard.Key|Guacamole.OnScreenKeyboard.Key[]>}
             */
            keys: any;
            /**
             * Arbitrarily nested, arbitrarily grouped key names. The contents of the
             * layout will be traversed to produce an identically-nested grouping of
             * keys in the DOM tree. All strings will be transformed into their
             * corresponding sets of keys, while all objects and arrays will be
             * transformed into named groups and anonymous groups respectively. Any
             * numbers present will be transformed into gaps of that size, scaled
             * according to the same units as each key.
             *
             * @type {Object}
             */
            layout: any;
            /**
             * The width of the entire keyboard, in arbitrary units. The width of each
             * key is relative to this width, as both width values are assumed to be in
             * the same units. The conversion factor between these units and pixels is
             * derived later via a call to resize() on the Guacamole.OnScreenKeyboard.
             *
             * @type {Number}
             */
            width: number;
            /**
             * The width of each key, in arbitrary units, relative to other keys in
             * this layout. The true pixel size of each key will be determined by the
             * overall size of the keyboard. If not defined here, the width of each
             * key will default to 1.
             *
             * @type {Object.<String, Number>}
             */
            keyWidths: any;

            /**
             * @constructor
             * @param {Guacamole.OnScreenKeyboard.Layout|Object} template
             *     The object whose identically-named properties will be used to initialize
             *     the properties of this layout.
             */
            constructor(template: any);
        }

        /**
         * Represents a single key, or a single possible behavior of a key. Each key
         * on the on-screen keyboard must have at least one associated
         * Guacamole.OnScreenKeyboard.Key, whether that key is explicitly defined or
         * implied, and may have multiple Guacamole.OnScreenKeyboard.Key if behavior
         * depends on modifier states.
         */
        class Key {
            /**
             * The unique name identifying this key within the keyboard layout.
             *
             * @type {String}
             */
            name: string;
            /**
             * The human-readable title that will be displayed to the user within the
             * key. If not provided, this will be derived from the key name.
             *
             * @type {String}
             */
            title: string;
            /**
             * The name of the modifier set when the key is pressed and cleared when
             * this key is released, if any. The names of modifiers are distinct from
             * the names of keys; both the "RightShift" and "LeftShift" keys may set
             * the "shift" modifier, for example. By default, the key will affect no
             * modifiers.
             *
             * @type {String}
             */
            modifier: string;
            /**
             * An array containing the names of each modifier required for this key to
             * have an effect. For example, a lowercase letter may require nothing,
             * while an uppercase letter would require "shift", assuming the Shift key
             * is named "shift" within the layout. By default, the key will require
             * no modifiers.
             *
             * @type {String[]}
             */
            requires: string[];

            constructor(template: any, name: string);
        }


    }


    /**
     * Guacamole protocol client. Given a {@link Guacamole.Tunnel},
     * automatically handles incoming and outgoing Guacamole instructions via the
     * provided tunnel, updating its display using one or more canvas elements.
     */
    class Client {
        /**
         * @param {Guacamole.Tunnel} tunnel The tunnel to use to send and receive
         *                                  Guacamole instructions.
         */
        constructor(tunnel: Tunnel);

        /**
         * Produces an opaque representation of Guacamole.Client state which can be
         * later imported through a call to importState(). This object is
         * effectively an independent, compressed snapshot of protocol and display
         * state. Invoking this function implicitly flushes the display.
         *
         * @param {function} callback
         *     Callback which should be invoked once the state object is ready. The
         *     state object will be passed to the callback as the sole parameter.
         *     This callback may be invoked immediately, or later as the display
         *     finishes rendering and becomes ready.
         */
        exportState(callback: any): void;

        /**
         * Restores Guacamole.Client protocol and display state based on an opaque
         * object from a prior call to exportState(). The Guacamole.Client instance
         * used to that state need not be the same as this instance.
         *
         * @param {Object} state
         *     An opaque representation of Guacamole.Client state from a prior call
         *     to exportState().
         *
         * @param {function} [callback]
         *     The function to invoke when state has finished being imported. This
         *     may happen immediately, or later as images within the provided state
         *     object are loaded.
         */
        importState(state: any, callback: any): void;

        /**
         * Returns the underlying display of this Guacamole.Client. The display
         * contains an Element which can be added to the DOM, causing the
         * display to become visible.
         *
         * @return {Guacamole.Display} The underlying display of this
         *                             Guacamole.Client.
         */
        getDisplay(): Display;

        /**
         * Sends a disconnect instruction to the server and closes the tunnel.
         */
        disconnect(): void;

        /**
         * Connects the underlying tunnel of this Guacamole.Client, passing the
         * given arbitrary data to the tunnel during the connection process.
         *
         * @param data Arbitrary connection data to be sent to the underlying
         *             tunnel during the connection process.
         * @throws {Guacamole.Status} If an error occurs during connection.
         */
        connect(data: any): void;

        /**
         * Sends a mouse event having the properties provided by the given mouse
         * state.
         *
         * @param {!Guacamole.Mouse.State} mouseState
         *     The state of the mouse to send in the mouse event.
         *
         * @param {boolean} [applyDisplayScale=false]
         *     Whether the provided mouse state uses local display units, rather
         *     than remote display units, and should be scaled to match the
         *     {@link Guacamole.Display}.
         */
        sendMouseState(mouseState: Mouse.State, applyDisplayScale?: boolean): void;

        /**
         * Sends a touch event having the properties provided by the given touch
         * state.
         *
         * @param {!Guacamole.Touch.State} touchState
         *     The state of the touch contact to send in the touch event.
         *
         * @param {boolean} [applyDisplayScale=false]
         *     Whether the provided touch state uses local display units, rather
         *     than remote display units, and should be scaled to match the
         *     {@link Guacamole.Display}.
         */
        sendTouchState(touchState : Guacamole.Touch.State, applyDisplayScale?: boolean): void;

        /**
         * Sends a key event having the given properties as if the user
         * pressed or released a key.
         *
         * @param {Boolean} pressed Whether the key is pressed (true) or released
         *                          (false).
         * @param {Number} keysym The keysym of the key being pressed or released.
         */
        sendKeyEvent(pressed: number, keysym: number): void;

        /**
         * Sends the current size of the screen.
         *
         * @param {Number} width The width of the screen.
         * @param {Number} height The height of the screen.
         */
        sendSize(width: number, height: number): void;

        /**
         * Sets the clipboard of the remote client to the given text data.
         *
         * @deprecated Use createClipboardStream() instead.
         * @param {String} data The data to send as the clipboard contents.
         */
        setClipboard(data: string): void;

        /**
         * Allocates an available stream index and creates a new
         * Guacamole.OutputStream using that index, associating the resulting
         * stream with this Guacamole.Client. Note that this stream will not yet
         * exist as far as the other end of the Guacamole connection is concerned.
         * Streams exist within the Guacamole protocol only when referenced by an
         * instruction which creates the stream, such as a "clipboard", "file", or
         * "pipe" instruction.
         *
         * @returns {Guacamole.OutputStream}
         *     A new Guacamole.OutputStream with a newly-allocated index and
         *     associated with this Guacamole.Client.
         */
        createOutputStream(): OutputStream;

        /**
         * Opens a new audio stream for writing, where audio data having the give
         * mimetype will be sent along the returned stream. The instruction
         * necessary to create this stream will automatically be sent.
         *
         * @param {String} mimetype
         *     The mimetype of the audio data that will be sent along the returned
         *     stream.
         *
         * @return {Guacamole.OutputStream}
         *     The created audio stream.
         */
        createAudioStream(mimetype: string): OutputStream;

        /**
         * Opens a new file for writing, having the given index, mimetype and
         * filename. The instruction necessary to create this stream will
         * automatically be sent.
         *
         * @param {String} mimetype The mimetype of the file being sent.
         * @param {String} filename The filename of the file being sent.
         * @return {Guacamole.OutputStream} The created file stream.
         */
        createFileStream(mimetype: string, filename: string): OutputStream;

        /**
         * Opens a new pipe for writing, having the given name and mimetype. The
         * instruction necessary to create this stream will automatically be sent.
         *
         * @param {String} mimetype The mimetype of the data being sent.
         * @param {String} name The name of the pipe.
         * @return {Guacamole.OutputStream} The created file stream.
         */
        createPipeStream(mimetype: string, name: string): OutputStream;

        /**
         * Opens a new clipboard object for writing, having the given mimetype. The
         * instruction necessary to create this stream will automatically be sent.
         *
         * @param {String} mimetype The mimetype of the data being sent.
         * @param {String} name The name of the pipe.
         * @return {Guacamole.OutputStream} The created file stream.
         */
        createClipboardStream(mimetype: string): OutputStream;

        /**
         * Opens a new argument value stream for writing, having the given
         * parameter name and mimetype, requesting that the connection parameter
         * with the given name be updated to the value described by the contents
         * of the following stream. The instruction necessary to create this stream
         * will automatically be sent.
         *
         * @param {!string} mimetype
         *     The mimetype of the data being sent.
         *
         * @param {!string} name
         *     The name of the connection parameter to attempt to update.
         *
         * @return {!Guacamole.OutputStream}
         *     The created argument value stream.
         */
        createArgumentValueStream(mimetype: string, name: string): OutputStream;

        /**
         * Creates a new output stream associated with the given object and having
         * the given mimetype and name. The legality of a mimetype and name is
         * dictated by the object itself. The instruction necessary to create this
         * stream will automatically be sent.
         *
         * @param {Number} index
         *     The index of the object for which the output stream is being
         *     created.
         *
         * @param {String} mimetype
         *     The mimetype of the data which will be sent to the output stream.
         *
         * @param {String} name
         *     The defined name of an output stream within the given object.
         *
         * @returns {Guacamole.OutputStream}
         *     An output stream which will write blobs to the named output stream
         *     of the given object.
         */
        createObjectOutputStream(index: number, mimetype: string, name: string): OutputStream;

        /**
         * Requests read access to the input stream having the given name. If
         * successful, a new input stream will be created.
         *
         * @param {Number} index
         *     The index of the object from which the input stream is being
         *     requested.
         *
         * @param {String} name
         *     The name of the input stream to request.
         */
        requestObjectInputStream(index: number, name: string): void;

        /**
         * Acknowledge receipt of a blob on the stream with the given index.
         *
         * @param {Number} index The index of the stream associated with the
         *                       received blob.
         * @param {String} message A human-readable message describing the error
         *                         or status.
         * @param {Number} code The error code, if any, or 0 for success.
         */
        sendAck(index: number, message: string, code: number): void;

        /**
         * Given the index of a file, writes a blob of data to that file.
         *
         * @param {Number} index The index of the file to write to.
         * @param {String} data Base64-encoded data to write to the file.
         */
        sendBlob(index: number, data: string): void;

        /**
         * Marks a currently-open stream as complete. The other end of the
         * Guacamole connection will be notified via an "end" instruction that the
         * stream is closed, and the index will be made available for reuse in
         * future streams.
         *
         * @param {Number} index
         *     The index of the stream to end.
         */
        endStream(index: number): void;

        /**
         * Fired whenever the state of this Guacamole.Client changes.
         *
         * @event
         * @param {Number} state The new state of the client.
         */
        onstatechange(state: number): void;

        /**
         * Fired when the remote client sends a name update.
         *
         * @event
         * @param {String} name The new name of this client.
         */
        onname(name: string): void;

        /**
         * Fired when an error is reported by the remote client, and the connection
         * is being closed.
         *
         * @event
         * @param {Guacamole.Status} status A status object which describes the
         *                                  error.
         */
        onerror(status: Status): void;

        /**
         * Fired when a audio stream is created. The stream provided to this event
         * handler will contain its own event handlers for received data.
         *
         * @event
         * @param {Guacamole.InputStream} stream
         *     The stream that will receive audio data from the server.
         *
         * @param {String} mimetype
         *     The mimetype of the audio data which will be received.
         *
         * @return {Guacamole.AudioPlayer}
         *     An object which implements the Guacamole.AudioPlayer interface and
         *     has been initialized to play the data in the provided stream, or null
         *     if the built-in audio players of the Guacamole client should be
         *     used.
         */
        onaudio(stream: InputStream, mimetype: string): AudioPlayer;

        /**
         * Fired when a video stream is created. The stream provided to this event
         * handler will contain its own event handlers for received data.
         *
         * @event
         * @param {Guacamole.InputStream} stream
         *     The stream that will receive video data from the server.
         *
         * @param {Guacamole.Display.VisibleLayer} layer
         *     The destination layer on which the received video data should be
         *     played. It is the responsibility of the Guacamole.VideoPlayer
         *     implementation to play the received data within this layer.
         *
         * @param {String} mimetype
         *     The mimetype of the video data which will be received.
         *
         * @return {Guacamole.VideoPlayer}
         *     An object which implements the Guacamole.VideoPlayer interface and
         *     has been initialied to play the data in the provided stream, or null
         *     if the built-in video players of the Guacamole client should be
         *     used.
         */
        onvideo(stream: InputStream, layer: Display.VisibleLayer, mimetype: string): VideoPlayer;

        /**
         * Fired when the clipboard of the remote client is changing.
         *
         * @event
         * @param {Guacamole.InputStream} stream The stream that will receive
         *                                       clipboard data from the server.
         * @param {String} mimetype The mimetype of the data which will be received.
         */
        onclipboard: (stream: InputStream, mimetype: string) => void;

        /**
         * Fired when a file stream is created. The stream provided to this event
         * handler will contain its own event handlers for received data.
         *
         * @event
         * @param {Guacamole.InputStream} stream The stream that will receive data
         *                                       from the server.
         * @param {String} mimetype The mimetype of the file received.
         * @param {String} filename The name of the file received.
         */
        onfile(stream: InputStream, mimetype: string, filename: string): void;

        /**
         * Fired when a filesystem object is created. The object provided to this
         * event handler will contain its own event handlers and functions for
         * requesting and handling data.
         *
         * @event
         * @param {Guacamole.Object} object
         *     The created filesystem object.
         *
         * @param {String} name
         *     The name of the filesystem.
         */
        onfilesystem(object: Object, name: string): void;

        /**
         * Fired when a pipe stream is created. The stream provided to this event
         * handler will contain its own event handlers for received data;
         *
         * @event
         * @param {Guacamole.InputStream} stream The stream that will receive data
         *                                       from the server.
         * @param {String} mimetype The mimetype of the data which will be received.
         * @param {String} name The name of the pipe.
         */
        onpipe(stream: InputStream, mimetype: string, name: string): void;

        /**
         * Fired when a "required" instruction is received. A required instruction
         * indicates that additional parameters are required for the connection to
         * continue, such as user credentials.
         *
         * @event
         * @param {!string[]} parameters
         *      The names of the connection parameters that are required to be
         *      provided for the connection to continue.
         */
        onrequired: (parameters: string[]) => void | null;

        /**
         * Fired whenever a sync instruction is received from the server, indicating
         * that the server is finished processing any input from the client and
         * has sent any results.
         *
         * @event
         * @param {Number} timestamp The timestamp associated with the sync
         *                           instruction.
         */
        onsync(timestamp: number): void;

        /**
         * Fired when a user joins a shared connection.
         *
         * @event
         * @param {!string} userID
         *     A unique value representing this specific user's connection to the
         *     shared connection. This value is generated by the server and is
         *     guaranteed to be unique relative to other users of the connection.
         *
         * @param {!string} name
         *     A human-readable name representing the user that joined, such as
         *     their username. This value is provided by the web application during
         *     the connection handshake and is not necessarily unique relative to
         *     other users of the connection.
         */
        onjoin(userID: string, name: string): void | null;

        /**
         * Fired when a user leaves a shared connection.
         *
         * @event
         * @param {!string} userID
         *     A unique value representing this specific user's connection to the
         *     shared connection. This value is generated by the server and is
         *     guaranteed to be unique relative to other users of the connection.
         *
         * @param {!string} name
         *     A human-readable name representing the user that left, such as their
         *     username. This value is provided by the web application during the
         *     connection handshake and is not necessarily unique relative to other
         *     users of the connection.
         */
        onleave(userID: string, name: string): void | null;

        /**
         * Fired when the remote client is explicitly declaring the level of
         * multi-touch support provided by a particular display layer.
         *
         * @event
         * @param {!Guacamole.Display.VisibleLayer} layer
         *     The layer whose multi-touch support level is being declared.
         *
         * @param {!number} touches
         *     The maximum number of simultaneous touches supported by the given
         *     layer, where 0 indicates that touch events are not supported at all.
         */
        onmultitouch: (layer: Display.VisibleLayer, touches: number) => void | null;

        /**
         * Fired when the current value of a connection parameter is being exposed
         * by the server.
         *
         * @event
         * @param {!Guacamole.InputStream} stream
         *     The stream that will receive connection parameter data from the
         *     server.
         *
         * @param {!string} mimetype
         *     The mimetype of the data which will be received.
         *
         * @param {!string} name
         *     The name of the connection parameter whose value is being exposed.
         */
        onargv: (stream: InputStream, mimetype: string, name: string) => void | null;

        /**
         * All possible Guacamole Client states.
         *
         * @type {!Object.<string, number>}
         */
        static State: any;

    }

    /**
     * Core object providing abstract communication for Guacamole. This object
     * is a null implementation whose functions do nothing. Guacamole applications
     * should use {@link Guacamole.HTTPTunnel} instead, or implement their own tunnel based
     * on this one.
     *
     * @constructor
     * @see Guacamole.HTTPTunnel
     */
    class Tunnel {
        static INTERNAL_DATA_OPCODE: string;
        static State: any;

        /**
         * The UUID uniquely identifying this tunnel. If not yet known, this will
         * be null.
         *
         * @type {string}
         */
        uuid: string;

        /**
         * Fired when the UUID that uniquely identifies this tunnel is known.
         *
         * @event
         * @param {!string}
         *     The UUID uniquely identifying this tunnel.
         */
        onuuid: (uuid: string) => void | null;

        /**
         * The current state of this tunnel.
         *
         * @type {Number}
         */
        state: number;

        /**
         * The maximum amount of time to wait for data to be received, in
         * milliseconds. If data is not received within this amount of time,
         * the tunnel is closed with an error. The default value is 15000.
         *
         * @type {Number}
         */
        receiveTimeout: number;

        /**
         * Connect to the tunnel with the given optional data. This data is
         * typically used for authentication. The format of data accepted is
         * up to the tunnel implementation.
         *
         * @param {String} data The data to send to the tunnel when connecting.
         */
        connect(data: string): void;

        /**
         * Disconnect from the tunnel.
         */
        disconnect(): void;

        /**
         * Fired whenever an error is encountered by the tunnel.
         *
         * @event
         * @param {Guacamole.Status} status A status object which describes the
         *                                  error.
         */
        onerror(error: Status): void;

        /**
         * Fired whenever the state of the tunnel changes.
         *
         * @event
         * @param {Number} state The new state of the client.
         */
        onstatechange(state: number): void;

        /**
         * Fired once for every complete Guacamole instruction received, in order.
         *
         * @event
         * @param {String} opcode The Guacamole instruction opcode.
         * @param {Array} parameters The parameters provided for the instruction,
         *                           if any.
         */
        oninstruction(opcode: string, parameters: any): void;


        /**
         * Send the given message through the tunnel to the service on the other
         * side. All messages are guaranteed to be received in the order sent.
         *
         * @param {...*} elements
         *     The elements of the message to send to the service on the other side
         *     of the tunnel.
         */
        sendMessage(elements: any): void;

        /**
         * Changes the stored numeric state of this tunnel, firing the onstatechange
         * event if the new state is different and a handler has been defined.
         *
         * @private
         * @param {Number} state
         *     The new state of this tunnel.
         */
        setState(state: number): void;
    }

    /**
     * Guacamole Tunnel implemented over WebSocket via XMLHttpRequest.
     */
    class WebSocketTunnel extends Tunnel {
        /**
         * @param {String} tunnelURL The URL of the WebSocket tunneling service.
         */
        constructor(tunnelURL: string);
    }


    class SocketIOTunnel extends Tunnel {
        constructor(url: string, connectionOptions: any, eventChannel: string);

        /**
         * Return the socketio socket
         */
        getSocket(): any;
    }

    /**
     * Guacamole Tunnel which cycles between all specified tunnels until
     * no tunnels are left. Another tunnel is used if an error occurs but
     * no instructions have been received. If an instruction has been
     * received, or no tunnels remain, the error is passed directly out
     * through the onerror handler (if defined).
     */
    class ChainedTunnel extends Tunnel {
        /**
         * @param {...*} tunnelChain
         *     The tunnels to use, in order of priority.
         */
        constructor(...tunnelChain: any[]);
    }

    /**
     * Guacamole Tunnel implemented over HTTP via XMLHttpRequest.
     */
    class HTTPTunnel extends Tunnel {
        /**
         * @param {String} tunnelURL
         *     The URL of the HTTP tunneling service.
         *
         * @param {Boolean} [crossDomain=false]
         *     Whether tunnel requests will be cross-domain, and thus must use CORS
         *     mechanisms and headers. By default, it is assumed that tunnel requests
         *     will be made to the same domain.
         *
         * @param {Object} [extraTunnelHeaders={}]
         *     Key value pairs containing the header names and values of any additional
         *     headers to be sent in tunnel requests. By default, no extra headers will
         *     be added.
         */
        constructor(tunnelURL: string, crossDomain?: boolean, extraTunnelHeaders?: any);
    }

    /**
     * Guacamole Tunnel which replays a Guacamole protocol dump from a static file
     * received via HTTP. Instructions within the file are parsed and handled as
     * quickly as possible, while the file is being downloaded.
     */
    class StaticHTTPTunnel extends Tunnel {
        /**
         * @param {String} url
         *     The URL of a Guacamole protocol dump.
         *
         * @param {Boolean} [crossDomain=false]
         *     Whether tunnel requests will be cross-domain, and thus must use CORS
         *     mechanisms and headers. By default, it is assumed that tunnel requests
         *     will be made to the same domain.
         *
         * @param {Object} [extraTunnelHeaders={}]
         *     Key value pairs containing the header names and values of any additional
         *     headers to be sent in tunnel requests. By default, no extra headers will
         *     be added.
         */
        constructor(url: string, crossDomain: boolean, extraTunnelHeaders: any);
    }

    /**
     * Abstract audio player which accepts, queues and plays back arbitrary audio
     * data. It is up to implementations of this class to provide some means of
     * handling a provided Guacamole.InputStream. Data received along the provided
     * stream is to be played back immediately.
     */
    class AudioPlayer {
        /**
         * Determines whether the given mimetype is supported by any built-in
         * implementation of Guacamole.AudioPlayer, and thus will be properly handled
         * by Guacamole.AudioPlayer.getInstance().
         *
         * @param {String} mimetype
         *     The mimetype to check.
         *
         * @returns {Boolean}
         *     true if the given mimetype is supported by any built-in
         *     Guacamole.AudioPlayer, false otherwise.
         */
        static isSupportedType(mimetype: string): boolean;

        /**
         * Returns a list of all mimetypes supported by any built-in
         * Guacamole.AudioPlayer, in rough order of priority. Beware that only the core
         * mimetypes themselves will be listed. Any mimetype parameters, even required
         * ones, will not be included in the list. For example, "audio/L8" is a
         * supported raw audio mimetype that is supported, but it is invalid without
         * additional parameters. Something like "audio/L8;rate=44100" would be valid,
         * however (see https://tools.ietf.org/html/rfc4856).
         *
         * @returns {String[]}
         *     A list of all mimetypes supported by any built-in Guacamole.AudioPlayer,
         *     excluding any parameters.
         */
        static getSupportedTypes(): string[];

        /**
         * Returns an instance of Guacamole.AudioPlayer providing support for the given
         * audio format. If support for the given audio format is not available, null
         * is returned.
         *
         * @param {Guacamole.InputStream} stream
         *     The Guacamole.InputStream to read audio data from.
         *
         * @param {String} mimetype
         *     The mimetype of the audio data in the provided stream.
         *
         * @return {Guacamole.AudioPlayer}
         *     A Guacamole.AudioPlayer instance supporting the given mimetype and
         *     reading from the given stream, or null if support for the given mimetype
         *     is absent.
         */
        static getInstance(stream: InputStream, mimetype: string): AudioPlayer;

        sync(): void;

    }

    /**
     * Implementation of Guacamole.AudioPlayer providing support for raw PCM format
     * audio. This player relies only on the Web Audio API and does not require any
     * browser-level support for its audio formats.
     */
    class RawAudioPlayer extends AudioPlayer {
        /**
         * @augments Guacamole.AudioPlayer
         * @param {Guacamole.InputStream} stream
         *     The Guacamole.InputStream to read audio data from.
         *
         * @param {String} mimetype
         *     The mimetype of the audio data in the provided stream, which must be a
         *     "audio/L8" or "audio/L16" mimetype with necessary parameters, such as:
         *     "audio/L16;rate=44100,channels=2".
         */
        constructor(stream: InputStream, mimetype: string);

        /**
         * Determines whether the given mimetype is supported by
         * Guacamole.RawAudioPlayer.
         *
         * @param {String} mimetype
         *     The mimetype to check.
         *
         * @returns {Boolean}
         *     true if the given mimetype is supported by Guacamole.RawAudioPlayer,
         *     false otherwise.
         */
        static isSupportedType(mimetype: string): boolean;

        /**
         * Returns a list of all mimetypes supported by Guacamole.RawAudioPlayer. Only
         * the core mimetypes themselves will be listed. Any mimetype parameters, even
         * required ones, will not be included in the list. For example, "audio/L8" is
         * a raw audio mimetype that may be supported, but it is invalid without
         * additional parameters. Something like "audio/L8;rate=44100" would be valid,
         * however (see https://tools.ietf.org/html/rfc4856).
         *
         * @returns {String[]}
         *     A list of all mimetypes supported by Guacamole.RawAudioPlayer, excluding
         *     any parameters. If the necessary JavaScript APIs for playing raw audio
         *     are absent, this list will be empty.
         */
        static getSupportedTypes(): string[];

        /** @override */
        sync(): void;
    }

    /**
     * Abstract audio recorder which streams arbitrary audio data to an underlying
     * Guacamole.OutputStream. It is up to implementations of this class to provide
     * some means of handling this Guacamole.OutputStream. Data produced by the
     * recorder is to be sent along the provided stream immediately.
     */
    class AudioRecorder {
        /**
         * Determines whether the given mimetype is supported by any built-in
         * implementation of Guacamole.AudioRecorder, and thus will be properly handled
         * by Guacamole.AudioRecorder.getInstance().
         *
         * @param {String} mimetype
         *     The mimetype to check.
         *
         * @returns {Boolean}
         *     true if the given mimetype is supported by any built-in
         *     Guacamole.AudioRecorder, false otherwise.
         */
        static isSupportedType(mimetype: string): boolean;

        /**
         * Returns a list of all mimetypes supported by any built-in
         * Guacamole.AudioRecorder, in rough order of priority. Beware that only the
         * core mimetypes themselves will be listed. Any mimetype parameters, even
         * required ones, will not be included in the list. For example, "audio/L8" is
         * a supported raw audio mimetype that is supported, but it is invalid without
         * additional parameters. Something like "audio/L8;rate=44100" would be valid,
         * however (see https://tools.ietf.org/html/rfc4856).
         *
         * @returns {String[]}
         *     A list of all mimetypes supported by any built-in
         *     Guacamole.AudioRecorder, excluding any parameters.
         */
        static getSupportedTypes(): string[];

        /**
         * Returns an instance of Guacamole.AudioRecorder providing support for the
         * given audio format. If support for the given audio format is not available,
         * null is returned.
         *
         * @param {Guacamole.OutputStream} stream
         *     The Guacamole.OutputStream to send audio data through.
         *
         * @param {String} mimetype
         *     The mimetype of the audio data to be sent along the provided stream.
         *
         * @return {Guacamole.AudioRecorder}
         *     A Guacamole.AudioRecorder instance supporting the given mimetype and
         *     writing to the given stream, or null if support for the given mimetype
         *     is absent.
         */
        static getInstance(stream: OutputStream, mimetype: string): AudioRecorder;

        /**
         * Callback which is invoked when the audio recording process has stopped
         * and the underlying Guacamole stream has been closed normally. Audio will
         * only resume recording if a new Guacamole.AudioRecorder is started. This
         * Guacamole.AudioRecorder instance MAY NOT be reused.
         *
         * @event
         */
        onclose(): void;

        /**
         * Callback which is invoked when the audio recording process cannot
         * continue due to an error, if it has started at all. The underlying
         * Guacamole stream is automatically closed. Future attempts to record
         * audio should not be made, and this Guacamole.AudioRecorder instance
         * MAY NOT be reused.
         *
         * @event
         */
        onerror(): void;
    }

    /**
     * Implementation of Guacamole.AudioRecorder providing support for raw PCM
     * format audio. This recorder relies only on the Web Audio API and does not
     * require any browser-level support for its audio formats.
     *
     * @constructor
     * @augments Guacamole.AudioRecorder
     * @param {Guacamole.OutputStream} stream
     *     The Guacamole.OutputStream to write audio data to.
     *
     * @param {String} mimetype
     *     The mimetype of the audio data to send along the provided stream, which
     *     must be a "audio/L8" or "audio/L16" mimetype with necessary parameters,
     *     such as: "audio/L16;rate=44100,channels=2".
     */
    class RawAudioRecorder extends AudioRecorder {
        /**
         * @augments Guacamole.AudioRecorder
         * @param {Guacamole.OutputStream} stream
         *     The Guacamole.OutputStream to write audio data to.
         *
         * @param {String} mimetype
         *     The mimetype of the audio data to send along the provided stream, which
         *     must be a "audio/L8" or "audio/L16" mimetype with necessary parameters,
         *     such as: "audio/L16;rate=44100,channels=2".
         */
        constructor(stream: AudioRecorder, mimetype: string);

        /**
         * Determines whether the given mimetype is supported by
         * Guacamole.RawAudioRecorder.
         *
         * @param {String} mimetype
         *     The mimetype to check.
         *
         * @returns {Boolean}
         *     true if the given mimetype is supported by Guacamole.RawAudioRecorder,
         *     false otherwise.
         */
        static isSupportedType(mimetype: string): boolean;

        /**
         * Returns a list of all mimetypes supported by Guacamole.RawAudioRecorder. Only
         * the core mimetypes themselves will be listed. Any mimetype parameters, even
         * required ones, will not be included in the list. For example, "audio/L8" is
         * a raw audio mimetype that may be supported, but it is invalid without
         * additional parameters. Something like "audio/L8;rate=44100" would be valid,
         * however (see https://tools.ietf.org/html/rfc4856).
         *
         * @returns {String[]}
         *     A list of all mimetypes supported by Guacamole.RawAudioRecorder,
         *     excluding any parameters. If the necessary JavaScript APIs for recording
         *     raw audio are absent, this list will be empty.
         */
        static getSupportedTypes(): string[];
    }

    /**
     * Provides cross-browser and cross-keyboard keyboard for a specific element.
     * Browser and keyboard layout variation is abstracted away, providing events
     * which represent keys as their corresponding X11 keysym.
     */
    class Keyboard {
        /**
         * @param {Element} element The Element to use to provide keyboard events.
         */
        constructor(element: any);

        /**
         * Fired whenever the user presses a key with the element associated
         * with this Guacamole.Keyboard in focus.
         *
         * @event
         * @param {Number} keysym The keysym of the key being pressed.
         * @return {Boolean} true if the key event should be allowed through to the
         *                   browser, false otherwise.
         */
        onkeydown(keysym: number): boolean;

        /**
         * Fired whenever the user releases a key with the element associated
         * with this Guacamole.Keyboard in focus.
         *
         * @event
         * @param {Number} keysym The keysym of the key being released.
         */
        onkeyup(keysym: number): void;

        /**
         * Marks a key as pressed, firing the keydown event if registered. Key
         * repeat for the pressed key will start after a delay if that key is
         * not a modifier. The return value of this function depends on the
         * return value of the keydown event handler, if any.
         *
         * @param {Number} keysym The keysym of the key to press.
         * @return {Boolean} true if event should NOT be canceled, false otherwise.
         */
        press(keysym: number): boolean;

        /**
         * Marks a key as released, firing the keyup event if registered.
         *
         * @param {Number} keysym The keysym of the key to release.
         */
        release(keysym: number): void;

        /**
         * Resets the state of this keyboard, releasing all keys, and firing keyup
         * events for each released key.
         */
        reset(): void;

        /**
         * The state of every key, indexed by keysym. If a particular key is
         * pressed, the value of pressed for that keysym will be true. If a key
         * is not currently pressed, it will not be defined.
         */
        pressed: Record<number, boolean | undefined>

        /**
         * Attaches event listeners to the given Element, automatically translating
         * received key, input, and composition events into simple keydown/keyup
         * events signalled through this Guacamole.Keyboard's onkeydown and
         * onkeyup handlers.
         *
         * @param {!(Element|Document)} element
         *     The Element to attach event listeners to for the sake of handling
         *     key or input events.
         */
        listenTo(element: Element | Document): void;
    }

    /**
     * A hidden input field which attempts to keep itself focused at all times,
     * except when another input field has been intentionally focused, whether
     * programatically or by the user. The actual underlying input field, returned
     * by getElement(), may be used as a reliable source of keyboard-related events,
     * particularly composition and input events which may require a focused input
     * field to be dispatched at all.
     */
    class InputSink {
        /**
         * Returns the underlying input field. This input field MUST be manually
         * added to the DOM for the Guacamole.InputSink to have any effect.
         *
         * @returns {!Element}
         *     The underlying input field.
         */
        getElement(): Element;
    }

    /**
     * The Guacamole display. The display does not deal with the Guacamole
     * protocol, and instead implements a set of graphical operations which
     * embody the set of operations present in the protocol. The order operations
     * are executed is guaranteed to be in the same order as their corresponding
     * functions are called.
     */
    class Display {

        /**
         * The X coordinate of the hotspot of the mouse cursor. The hotspot is
         * the relative location within the image of the mouse cursor at which
         * each click occurs.
         *
         * @type {Number}
         */
        cursorHotspotX: number;

        /**
         * The Y coordinate of the hotspot of the mouse cursor. The hotspot is
         * the relative location within the image of the mouse cursor at which
         * each click occurs.
         *
         * @type {Number}
         */
        cursorHotspotY: number;

        /**
         * The current X coordinate of the local mouse cursor. This is not
         * necessarily the location of the actual mouse - it refers only to
         * the location of the cursor image within the Guacamole display, as
         * last set by moveCursor().
         *
         * @type {Number}
         */
        cursorX: number;

        /**
         * The current X coordinate of the local mouse cursor. This is not
         * necessarily the location of the actual mouse - it refers only to
         * the location of the cursor image within the Guacamole display, as
         * last set by moveCursor().
         *
         * @type {Number}
         */
        cursorY: number;

        /**
         * Fired when the default layer (and thus the entire Guacamole display)
         * is resized.
         *
         * @event
         * @param {Number} width The new width of the Guacamole display.
         * @param {Number} height The new height of the Guacamole display.
         */
        onresize(width: number, height: number): void;

        /**
         * Fired whenever the local cursor image is changed. This can be used to
         * implement special handling of the client-side cursor, or to override
         * the default use of a software cursor layer.
         *
         * @event
         * @param {HTMLCanvasElement} canvas The cursor image.
         * @param {Number} x The X-coordinate of the cursor hotspot.
         * @param {Number} y The Y-coordinate of the cursor hotspot.
         */
        oncursor(canvas: any, x: number, y: number): void;

        /**
         * Returns the element which contains the Guacamole display.
         *
         * @return {Element} The element containing the Guacamole display.
         */
        getElement(): any;

        /**
         * Returns the width of this display.
         *
         * @return {Number} The width of this display;
         */
        getWidth(): number;

        /**
         * Returns the height of this display.
         *
         * @return {Number} The height of this display;
         */
        getHeight(): number;

        /**
         * Returns the default layer of this display. Each Guacamole display always
         * has at least one layer. Other layers can optionally be created within
         * this layer, but the default layer cannot be removed and is the absolute
         * ancestor of all other layers.
         *
         * @return {Guacamole.Display.VisibleLayer} The default layer.
         */
        getDefaultLayer(): Display.VisibleLayer;

        /**
         * Returns the cursor layer of this display. Each Guacamole display contains
         * a layer for the image of the mouse cursor. This layer is a special case
         * and exists above all other layers, similar to the hardware mouse cursor.
         *
         * @return {Guacamole.Display.VisibleLayer} The cursor layer.
         */
        getCursorLayer(): Display.VisibleLayer;

        /**
         * Creates a new layer. The new layer will be a direct child of the default
         * layer, but can be moved to be a child of any other layer. Layers returned
         * by this function are visible.
         *
         * @return {Guacamole.Display.VisibleLayer} The newly-created layer.
         */
        createLayer(): Display.VisibleLayer;

        /**
         * Creates a new buffer. Buffers are invisible, off-screen surfaces. They
         * are implemented in the same manner as layers, but do not provide the
         * same nesting semantics.
         *
         * @return {Guacamole.Layer} The newly-created buffer.
         */
        createBuffer(): Layer;

        /**
         * Flush all pending draw tasks, if possible, as a new frame. If the entire
         * frame is not ready, the flush will wait until all required tasks are
         * unblocked.
         *
         * @param {function} callback The function to call when this frame is
         *                            flushed. This may happen immediately, or
         *                            later when blocked tasks become unblocked.
         */
        flush(callback: any): void;

        /**
         * Sets the hotspot and image of the mouse cursor displayed within the
         * Guacamole display.
         *
         * @param {Number} hotspotX The X coordinate of the cursor hotspot.
         * @param {Number} hotspotY The Y coordinate of the cursor hotspot.
         * @param {Guacamole.Layer} layer The source layer containing the data which
         *                                should be used as the mouse cursor image.
         * @param {Number} srcx The X coordinate of the upper-left corner of the
         *                      rectangle within the source layer's coordinate
         *                      space to copy data from.
         * @param {Number} srcy The Y coordinate of the upper-left corner of the
         *                      rectangle within the source layer's coordinate
         *                      space to copy data from.
         * @param {Number} srcw The width of the rectangle within the source layer's
         *                      coordinate space to copy data from.
         * @param {Number} srch The height of the rectangle within the source
         *                      layer's coordinate space to copy data from.
         */
        setCursor(hotspotX: number, hotspotY: number, layer: Layer, srcx: number, srcy: number, srcw: number, srch: number): void;

        /**
         * Sets whether the software-rendered cursor is shown. This cursor differs
         * from the hardware cursor in that it is built into the Guacamole.Display,
         * and relies on its own Guacamole layer to render.
         *
         * @param {Boolean} [shown=true] Whether to show the software cursor.
         */
        showCursor(shown: boolean): void;

        /**
         * Sets the location of the local cursor to the given coordinates. For the
         * sake of responsiveness, this function performs its action immediately.
         * Cursor motion is not maintained within atomic frames.
         *
         * @param {Number} x The X coordinate to move the cursor to.
         * @param {Number} y The Y coordinate to move the cursor to.
         */
        moveCursor(x: number, y: number): void;

        /**
         * Changes the size of the given Layer to the given width and height.
         * Resizing is only attempted if the new size provided is actually different
         * from the current size.
         *
         * @param {Guacamole.Layer} layer The layer to resize.
         * @param {Number} width The new width.
         * @param {Number} height The new height.
         */
        resize(layer: Layer, width: number, height: number): void;

        /**
         * Draws the specified image at the given coordinates. The image specified
         * must already be loaded.
         *
         * @param {Guacamole.Layer} layer The layer to draw upon.
         * @param {Number} x The destination X coordinate.
         * @param {Number} y The destination Y coordinate.
         * @param {Image} image The image to draw. Note that this is an Image
         *                      object - not a URL.
         */
        drawImage(layer: Layer, x: number, y: number, image: any): void;

        /**
         * Draws the image contained within the specified Blob at the given
         * coordinates. The Blob specified must already be populated with image
         * data.
         *
         * @param {Guacamole.Layer} layer
         *     The layer to draw upon.
         *
         * @param {Number} x
         *     The destination X coordinate.
         *
         * @param {Number} y
         *     The destination Y coordinate.
         *
         * @param {Blob} blob
         *     The Blob containing the image data to draw.
         */
        drawBlob(layer: Layer, x: number, y: number, blob: any): void;

        /**
         * Draws the image at the specified URL at the given coordinates. The image
         * will be loaded automatically, and this and any future operations will
         * wait for the image to finish loading.
         *
         * @param {Guacamole.Layer} layer The layer to draw upon.
         * @param {Number} x The destination X coordinate.
         * @param {Number} y The destination Y coordinate.
         * @param {String} url The URL of the image to draw.
         */
        draw(layer: Layer, x: number, y: number, url: string): void;

        /**
         * Plays the video at the specified URL within this layer. The video
         * will be loaded automatically, and this and any future operations will
         * wait for the video to finish loading. Future operations will not be
         * executed until the video finishes playing.
         *
         * @param {Guacamole.Layer} layer The layer to draw upon.
         * @param {String} mimetype The mimetype of the video to play.
         * @param {Number} duration The duration of the video in milliseconds.
         * @param {String} url The URL of the video to play.
         */
        play(layer: Layer, mimetype: string, duration: number, url: string): void;

        /**
         * Transfer a rectangle of image data from one Layer to this Layer using the
         * specified transfer function.
         *
         * @param {Guacamole.Layer} srcLayer The Layer to copy image data from.
         * @param {Number} srcx The X coordinate of the upper-left corner of the
         *                      rectangle within the source Layer's coordinate
         *                      space to copy data from.
         * @param {Number} srcy The Y coordinate of the upper-left corner of the
         *                      rectangle within the source Layer's coordinate
         *                      space to copy data from.
         * @param {Number} srcw The width of the rectangle within the source Layer's
         *                      coordinate space to copy data from.
         * @param {Number} srch The height of the rectangle within the source
         *                      Layer's coordinate space to copy data from.
         * @param {Guacamole.Layer} dstLayer The layer to draw upon.
         * @param {Number} x The destination X coordinate.
         * @param {Number} y The destination Y coordinate.
         * @param {Function} transferFunction The transfer function to use to
         *                                    transfer data from source to
         *                                    destination.
         */
        transfer(srcLayer: Layer,
                 srcx: number,
                 srcy: number,
                 srcw: number,
                 srch: number,
                 dstLayer: Layer,
                 x: number,
                 y: number,
                 transferFunction: any): void;

        /**
         * Put a rectangle of image data from one Layer to this Layer directly
         * without performing any alpha blending. Simply copy the data.
         *
         * @param {Guacamole.Layer} srcLayer The Layer to copy image data from.
         * @param {Number} srcx The X coordinate of the upper-left corner of the
         *                      rectangle within the source Layer's coordinate
         *                      space to copy data from.
         * @param {Number} srcy The Y coordinate of the upper-left corner of the
         *                      rectangle within the source Layer's coordinate
         *                      space to copy data from.
         * @param {Number} srcw The width of the rectangle within the source Layer's
         *                      coordinate space to copy data from.
         * @param {Number} srch The height of the rectangle within the source
         *                      Layer's coordinate space to copy data from.
         * @param {Guacamole.Layer} dstLayer The layer to draw upon.
         * @param {Number} x The destination X coordinate.
         * @param {Number} y The destination Y coordinate.
         */
        put(srcLayer: Layer, srcx: number, srcy: number, srcw: number, srch: number, dstLayer: Layer, x: number, y: number): void;

        /**
         * Copy a rectangle of image data from one Layer to this Layer. This
         * operation will copy exactly the image data that will be drawn once all
         * operations of the source Layer that were pending at the time this
         * function was called are complete. This operation will not alter the
         * size of the source Layer even if its autosize property is set to true.
         *
         * @param {Guacamole.Layer} srcLayer The Layer to copy image data from.
         * @param {Number} srcx The X coordinate of the upper-left corner of the
         *                      rectangle within the source Layer's coordinate
         *                      space to copy data from.
         * @param {Number} srcy The Y coordinate of the upper-left corner of the
         *                      rectangle within the source Layer's coordinate
         *                      space to copy data from.
         * @param {Number} srcw The width of the rectangle within the source Layer's
         *                      coordinate space to copy data from.
         * @param {Number} srch The height of the rectangle within the source
         *                      Layer's coordinate space to copy data from.
         * @param {Guacamole.Layer} dstLayer The layer to draw upon.
         * @param {Number} x The destination X coordinate.
         * @param {Number} y The destination Y coordinate.
         */
        copy(srcLayer: Layer, srcx: number, srcy: number, srcw: number, srch: number, dstLayer: Layer, x: number, y: number): void;

        /**
         * Starts a new path at the specified point.
         *
         * @param {Guacamole.Layer} layer The layer to draw upon.
         * @param {Number} x The X coordinate of the point to draw.
         * @param {Number} y The Y coordinate of the point to draw.
         */
        moveTo(layer: Layer, x: number, y: number): void;

        /**
         * Add the specified line to the current path.
         *
         * @param {Guacamole.Layer} layer The layer to draw upon.
         * @param {Number} x The X coordinate of the endpoint of the line to draw.
         * @param {Number} y The Y coordinate of the endpoint of the line to draw.
         */
        lineTo(layer: Layer, x: number, y: number): void;

        /**
         * Add the specified arc to the current path.
         *
         * @param {Guacamole.Layer} layer The layer to draw upon.
         * @param {Number} x The X coordinate of the center of the circle which
         *                   will contain the arc.
         * @param {Number} y The Y coordinate of the center of the circle which
         *                   will contain the arc.
         * @param {Number} radius The radius of the circle.
         * @param {Number} startAngle The starting angle of the arc, in radians.
         * @param {Number} endAngle The ending angle of the arc, in radians.
         * @param {Boolean} negative Whether the arc should be drawn in order of
         *                           decreasing angle.
         */
        arc(layer: Layer, x: number, y: number, radius: number, startAngle: number, endAngle: number, negative: boolean): void;

        /**
         * Starts a new path at the specified point.
         *
         * @param {Guacamole.Layer} layer The layer to draw upon.
         * @param {Number} cp1x The X coordinate of the first control point.
         * @param {Number} cp1y The Y coordinate of the first control point.
         * @param {Number} cp2x The X coordinate of the second control point.
         * @param {Number} cp2y The Y coordinate of the second control point.
         * @param {Number} x The X coordinate of the endpoint of the curve.
         * @param {Number} y The Y coordinate of the endpoint of the curve.
         */
        curveTo(layer: Layer, cp1x: number, cp1y: number, cp2x: number, cp2y: number, x: number, y: number): void;

        /**
         * Closes the current path by connecting the end point with the start
         * point (if any) with a straight line.
         *
         * @param {Guacamole.Layer} layer The layer to draw upon.
         */
        close(layer: Layer): void;

        /**
         * Add the specified rectangle to the current path.
         *
         * @param {Guacamole.Layer} layer The layer to draw upon.
         * @param {Number} x The X coordinate of the upper-left corner of the
         *                   rectangle to draw.
         * @param {Number} y The Y coordinate of the upper-left corner of the
         *                   rectangle to draw.
         * @param {Number} w The width of the rectangle to draw.
         * @param {Number} h The height of the rectangle to draw.
         */
        rect(layer: Layer, x: number, y: number, w: number, h: number): void;

        /**
         * Clip all future drawing operations by the current path. The current path
         * is implicitly closed. The current path can continue to be reused
         * for other operations (such as fillColor()) but a new path will be started
         * once a path drawing operation (path() or rect()) is used.
         *
         * @param {Guacamole.Layer} layer The layer to affect.
         */
        clip(layer: Layer): void;

        /**
         * Stroke the current path with the specified color. The current path
         * is implicitly closed. The current path can continue to be reused
         * for other operations (such as clip()) but a new path will be started
         * once a path drawing operation (path() or rect()) is used.
         *
         * @param {Guacamole.Layer} layer The layer to draw upon.
         * @param {String} cap The line cap style. Can be "round", "square",
         *                     or "butt".
         * @param {String} join The line join style. Can be "round", "bevel",
         *                      or "miter".
         * @param {Number} thickness The line thickness in pixels.
         * @param {Number} r The red component of the color to fill.
         * @param {Number} g The green component of the color to fill.
         * @param {Number} b The blue component of the color to fill.
         * @param {Number} a The alpha component of the color to fill.
         */
        strokeColor(layer: Layer, cap: string, join: string, thickness: number, r: number, g: number, b: number, a: number): void;

        /**
         * Fills the current path with the specified color. The current path
         * is implicitly closed. The current path can continue to be reused
         * for other operations (such as clip()) but a new path will be started
         * once a path drawing operation (path() or rect()) is used.
         *
         * @param {Guacamole.Layer} layer The layer to draw upon.
         * @param {Number} r The red component of the color to fill.
         * @param {Number} g The green component of the color to fill.
         * @param {Number} b The blue component of the color to fill.
         * @param {Number} a The alpha component of the color to fill.
         */
        fillColor(layer: Layer, r: number, g: number, b: number, a: number): void;

        /**
         * Stroke the current path with the image within the specified layer. The
         * image data will be tiled infinitely within the stroke. The current path
         * is implicitly closed. The current path can continue to be reused
         * for other operations (such as clip()) but a new path will be started
         * once a path drawing operation (path() or rect()) is used.
         *
         * @param {Guacamole.Layer} layer The layer to draw upon.
         * @param {String} cap The line cap style. Can be "round", "square",
         *                     or "butt".
         * @param {String} join The line join style. Can be "round", "bevel",
         *                      or "miter".
         * @param {Number} thickness The line thickness in pixels.
         * @param {Guacamole.Layer} srcLayer The layer to use as a repeating pattern
         *                                   within the stroke.
         */
        strokeLayer(layer: Layer, cap: string, join: string, thickness: number, srcLayer: Layer): void;

        /**
         * Fills the current path with the image within the specified layer. The
         * image data will be tiled infinitely within the stroke. The current path
         * is implicitly closed. The current path can continue to be reused
         * for other operations (such as clip()) but a new path will be started
         * once a path drawing operation (path() or rect()) is used.
         *
         * @param {Guacamole.Layer} layer The layer to draw upon.
         * @param {Guacamole.Layer} srcLayer The layer to use as a repeating pattern
         *                                   within the fill.
         */
        fillLayer(layer: Layer, srcLayer: Layer): void;

        /**
         * Push current layer state onto stack.
         *
         * @param {Guacamole.Layer} layer The layer to draw upon.
         */
        push(layer: Layer): void;

        /**
         * Pop layer state off stack.
         *
         * @param {Guacamole.Layer} layer The layer to draw upon.
         */
        pop(layer: Layer): void;

        /**
         * Reset the layer, clearing the stack, the current path, and any transform
         * matrix.
         *
         * @param {Guacamole.Layer} layer The layer to draw upon.
         */
        reset(layer: Layer): void;

        /**
         * Sets the given affine transform (defined with six values from the
         * transform's matrix).
         *
         * @param {Guacamole.Layer} layer The layer to modify.
         * @param {Number} a The first value in the affine transform's matrix.
         * @param {Number} b The second value in the affine transform's matrix.
         * @param {Number} c The third value in the affine transform's matrix.
         * @param {Number} d The fourth value in the affine transform's matrix.
         * @param {Number} e The fifth value in the affine transform's matrix.
         * @param {Number} f The sixth value in the affine transform's matrix.
         */
        setTransform(layer: Layer, a: number, b: number, c: number, d: number, e: number, f: number): void;

        /**
         * Applies the given affine transform (defined with six values from the
         * transform's matrix).
         *
         * @param {Guacamole.Layer} layer The layer to modify.
         * @param {Number} a The first value in the affine transform's matrix.
         * @param {Number} b The second value in the affine transform's matrix.
         * @param {Number} c The third value in the affine transform's matrix.
         * @param {Number} d The fourth value in the affine transform's matrix.
         * @param {Number} e The fifth value in the affine transform's matrix.
         * @param {Number} f The sixth value in the affine transform's matrix.
         */
        transform(layer: Layer, a: number, b: number, c: number, d: number, e: number, f: number): void;

        /**
         * Sets the channel mask for future operations on this Layer.
         *
         * The channel mask is a Guacamole-specific compositing operation identifier
         * with a single bit representing each of four channels (in order): source
         * image where destination transparent, source where destination opaque,
         * destination where source transparent, and destination where source
         * opaque.
         *
         * @param {Guacamole.Layer} layer The layer to modify.
         * @param {Number} mask The channel mask for future operations on this
         *                      Layer.
         */
        setChannelMask(layer: Layer, mask: number): void;

        /**
         * Sets the miter limit for stroke operations using the miter join. This
         * limit is the maximum ratio of the size of the miter join to the stroke
         * width. If this ratio is exceeded, the miter will not be drawn for that
         * joint of the path.
         *
         * @param {Guacamole.Layer} layer The layer to modify.
         * @param {Number} limit The miter limit for stroke operations using the
         *                       miter join.
         */
        setMiterLimit(layer: Layer, limit: number): void;

        /**
         * Removes the given layer container entirely, such that it is no longer
         * contained within its parent layer, if any.
         *
         * @param {Guacamole.Display.VisibleLayer} layer
         *     The layer being removed from its parent.
         */
        dispose(layer: Display.VisibleLayer): void;

        /**
         * Applies the given affine transform (defined with six values from the
         * transform's matrix) to the given layer.
         *
         * @param {Guacamole.Display.VisibleLayer} layer
         *     The layer being distorted.
         *
         * @param {Number} a
         *     The first value in the affine transform's matrix.
         *
         * @param {Number} b
         *     The second value in the affine transform's matrix.
         *
         * @param {Number} c
         *     The third value in the affine transform's matrix.
         *
         * @param {Number} d
         *     The fourth value in the affine transform's matrix.
         *
         * @param {Number} e
         *     The fifth value in the affine transform's matrix.
         *
         * @param {Number} f
         *     The sixth value in the affine transform's matrix.
         */
        distort(layer: Display.VisibleLayer, a: number, b: number, c: number, d: number, e: number, f: number): void;

        /**
         * Moves the upper-left corner of the given layer to the given X and Y
         * coordinate, sets the Z stacking order, and reparents the layer
         * to the given parent layer.
         *
         * @param {Guacamole.Display.VisibleLayer} layer
         *     The layer being moved.
         *
         * @param {Guacamole.Display.VisibleLayer} parent
         *     The parent to set.
         *
         * @param {Number} x
         *     The X coordinate to move to.
         *
         * @param {Number} y
         *     The Y coordinate to move to.
         *
         * @param {Number} z
         *     The Z coordinate to move to.
         */
        move(layer: Display.VisibleLayer, parent: Display.VisibleLayer, x: number, y: number, z: number): void;

        /**
         * Sets the opacity of the given layer to the given value, where 255 is
         * fully opaque and 0 is fully transparent.
         *
         * @param {Guacamole.Display.VisibleLayer} layer
         *     The layer whose opacity should be set.
         *
         * @param {Number} alpha
         *     The opacity to set.
         */
        shade(layer: Display.VisibleLayer, alpha: number): void;

        /**
         * Sets the scale of the client display element such that it renders at
         * a relatively smaller or larger size, without affecting the true
         * resolution of the display.
         *
         * @param {Number} scale The scale to resize to, where 1.0 is normal
         *                       size (1:1 scale).
         */
        scale(scale: number): void;

        /**
         * Returns the scale of the display.
         *
         * @return {Number} The scale of the display.
         */
        getScale(): number;

        /**
         * Returns a canvas element containing the entire display, with all child
         * layers composited within.
         *
         * @return {HTMLCanvasElement} A new canvas element containing a copy of
         *                             the display.
         */
        flatten(): any;

    }

    namespace Display {
        /**
         * Simple container for Guacamole.Layer, allowing layers to be easily
         * repositioned and nested. This allows certain operations to be accelerated
         * through DOM manipulation, rather than raster operations.
         */
        class VisibleLayer {

            /**
             * The opacity of the layer container, where 255 is fully opaque and 0 is
             * fully transparent.
             */
            alpha: number;

            /**
             * X coordinate of the upper-left corner of this layer container within
             * its parent, in pixels.
             * @type {Number}
             */
            x: number;

            /**
             * Y coordinate of the upper-left corner of this layer container within
             * its parent, in pixels.
             * @type {Number}
             */
            y: number;

            /**
             * Z stacking order of this layer relative to other sibling layers.
             * @type {Number}
             */
            z: number;

            /**
             * The affine transformation applied to this layer container. Each element
             * corresponds to a value from the transformation matrix, with the first
             * three values being the first row, and the last three values being the
             * second row. There are six values total.
             *
             * @type {Number[]}
             */
            matrix: number[];

            /**
             * The parent layer container of this layer, if any.
             * @type {Guacamole.Display.VisibleLayer}
             */
            parent: VisibleLayer;

            /**
             * Set of all children of this layer, indexed by layer index. This object
             * will have one property per child.
             */
            children: any;

            /**
             * @augments Guacamole.Layer
             * @param {Number} width The width of the Layer, in pixels. The canvas element
             *                       backing this Layer will be given this width.
             * @param {Number} height The height of the Layer, in pixels. The canvas element
             *                        backing this Layer will be given this height.
             */
            constructor(width: number, height: number);

            resize(width: number, height: number): void;

            /**
             * Returns the element containing the canvas and any other elements
             * associated with this layer.
             * @returns {Element} The element containing this layer's canvas.
             */
            getElement(): any;

            /**
             * Moves the upper-left corner of this layer to the given X and Y
             * coordinate.
             *
             * @param {Number} x The X coordinate to move to.
             * @param {Number} y The Y coordinate to move to.
             */
            translate(x: number, y: number): void;

            /**
             * Moves the upper-left corner of this VisibleLayer to the given X and Y
             * coordinate, sets the Z stacking order, and reparents this VisibleLayer
             * to the given VisibleLayer.
             *
             * @param {Guacamole.Display.VisibleLayer} parent The parent to set.
             * @param {Number} x The X coordinate to move to.
             * @param {Number} y The Y coordinate to move to.
             * @param {Number} z The Z coordinate to move to.
             */
            move(parent: VisibleLayer, x: number, y: number, z: number): void;

            /**
             * Sets the opacity of this layer to the given value, where 255 is fully
             * opaque and 0 is fully transparent.
             *
             * @param {Number} a The opacity to set.
             */
            shade(a: number): void;

            /**
             * Removes this layer container entirely, such that it is no longer
             * contained within its parent layer, if any.
             */
            dispose(): void;

            /**
             * Applies the given affine transform (defined with six values from the
             * transform's matrix).
             *
             * @param {Number} a The first value in the affine transform's matrix.
             * @param {Number} b The second value in the affine transform's matrix.
             * @param {Number} c The third value in the affine transform's matrix.
             * @param {Number} d The fourth value in the affine transform's matrix.
             * @param {Number} e The fifth value in the affine transform's matrix.
             * @param {Number} f The sixth value in the affine transform's matrix.
             */
            distort(a: number, b: number, c: number, d: number, e: number, f: number): void;

        }
    }

    /**
     * Provides cross-browser absolute touch event translation for a given element.
     *
     * Touch events are translated into mouse events as if the touches occurred
     * on a touchscreen (tapping anywhere on the screen clicks at that point,
     * long-press to right-click).
     */
    class Mouse extends Guacamole.Event.Target {
        /**
         * @param {Element} element The Element to use to provide touch events.
         */
        constructor(element: any);

        /**
         * Fired whenever a mouse button is effectively pressed. This can happen
         * as part of a "mousedown" gesture initiated by the user by pressing one
         * finger over the touchscreen element, as part of a "scroll" gesture
         * initiated by dragging two fingers up or down, etc.
         *
         * @event
         * @param {Guacamole.Mouse.State} state The current mouse state.
         */
        onmousedown(state: Mouse.State): void;

        /**
         * Fired whenever a mouse button is effectively released. This can happen
         * as part of a "mouseup" gesture initiated by the user by removing the
         * finger pressed against the touchscreen element, or as part of a "scroll"
         * gesture initiated by dragging two fingers up or down, etc.
         *
         * @event
         * @param {Guacamole.Mouse.State} state The current mouse state.
         */
        onmouseup(state: Mouse.State): void;

        /**
         * Fired whenever the user moves the mouse by dragging their finger over
         * the touchscreen element. Note that unlike Guacamole.Mouse.Touchpad,
         * dragging a finger over the touchscreen element will always cause
         * the mouse button to be effectively down, as if clicking-and-dragging.
         *
         * @event
         * @param {Guacamole.Mouse.State} state The current mouse state.
         */
        onmousemove(state: Mouse.State): void;

        /**
         * Changes the local mouse cursor to the given canvas, having the given
         * hotspot coordinates. This affects styling of the element backing this
         * Guacamole.Mouse only, and may fail depending on browser support for
         * setting the mouse cursor.
         *
         * If setting the local cursor is desired, it is up to the implementation
         * to do something else, such as use the software cursor built into
         * Guacamole.Display, if the local cursor cannot be set.
         *
         * @param canvas
         *     The cursor image.
         *
         * @param x
         *     The X-coordinate of the cursor hotspot.
         *
         * @param y
         *     The Y-coordinate of the cursor hotspot.
         *
         * @return
         *     true if the cursor was successfully set, false if the cursor could
         *     not be set for any reason.
         */
        setCursor(canvas: HTMLCanvasElement, x: number, y: number): boolean;
    }

    namespace Mouse {
        /**
         * Simple container for properties describing the state of a mouse.
         */
        class State {
            /**
             * The current X position of the mouse pointer.
             * @type {Number}
             */
            x: number;
            /**
             * The current Y position of the mouse pointer.
             * @type {Number}
             */
            y: number;
            /**
             * Whether the left mouse button is currently pressed.
             * @type {Boolean}
             */
            left: boolean;
            /**
             * Whether the middle mouse button is currently pressed.
             * @type {Boolean}
             */
            middle: boolean;
            /**
             * Whether the right mouse button is currently pressed.
             * @type {Boolean}
             */
            right: boolean;
            /**
             * Whether the up mouse button is currently pressed. This is the fourth
             * mouse button, associated with upward scrolling of the mouse scroll
             * wheel.
             * @type {Boolean}
             */
            up: boolean;
            /**
             * Whether the down mouse button is currently pressed. This is the fifth
             * mouse button, associated with downward scrolling of the mouse scroll
             * wheel.
             * @type {Boolean}
             */
            down: boolean;

            /**
             * @param {Number} x The X position of the mouse pointer in pixels.
             * @param {Number} y The Y position of the mouse pointer in pixels.
             * @param {Boolean} left Whether the left mouse button is pressed.
             * @param {Boolean} middle Whether the middle mouse button is pressed.
             * @param {Boolean} right Whether the right mouse button is pressed.
             * @param {Boolean} up Whether the up mouse button is pressed (the fourth
             *                     button, usually part of a scroll wheel).
             * @param {Boolean} down Whether the down mouse button is pressed (the fifth
             *                       button, usually part of a scroll wheel).
             */
            constructor(x: number, y: number, left: boolean, middle: boolean, right: boolean, up: boolean, down: boolean);
        }

        /**
         * Provides cross-browser absolute touch event translation for a given element.
         *
         * Touch events are translated into mouse events as if the touches occurred
         * on a touchscreen (tapping anywhere on the screen clicks at that point,
         * long-press to right-click).
         *
         * @example
         * var touchscreen = new Guacamole.Mouse.Touchscreen(client.getDisplay().getElement());
         *
         * // Emulate a mouse using touchscreen-style gestures, forwarding all mouse
         * // interaction over Guacamole connection
         * touchscreen.onEach(['mousedown', 'mousemove', 'mouseup'], function sendMouseEvent(e) {
         *
         *     // Re-show software mouse cursor if possibly hidden by a prior call to
         *     // showCursor(), such as a "mouseout" event handler that hides the
         *     // cursor
         *     client.getDisplay().showCursor(true);
         *
         *     client.sendMouseState(e.state, true);
         *
         * });
         *
         * @constructor
         * @augments Guacamole.Mouse.Event.Target
         * @param {!Element} element
         *     The Element to use to provide touch events.
         */
        class Touchscreen {

            /**
             * Provides cross-browser absolute touch event translation for a given element.
             *
             * Touch events are translated into mouse events as if the touches occurred
             * on a touchscreen (tapping anywhere on the screen clicks at that point,
             * long-press to right-click).
             *
             * @example
             * var touchscreen = new Guacamole.Mouse.Touchscreen(client.getDisplay().getElement());
             *
             * // Emulate a mouse using touchscreen-style gestures, forwarding all mouse
             * // interaction over Guacamole connection
             * touchscreen.onEach(['mousedown', 'mousemove', 'mouseup'], function sendMouseEvent(e) {
             *
             *     // Re-show software mouse cursor if possibly hidden by a prior call to
             *     // showCursor(), such as a "mouseout" event handler that hides the
             *     // cursor
             *     client.getDisplay().showCursor(true);
             *
             *     client.sendMouseState(e.state, true);
             *
             * });
             *
             * @constructor
             * @augments Guacamole.Mouse.Event.Target
             * @param {!Element} element
             *     The Element to use to provide touch events.
             */
            constructor(element: Element);
        }

        /**
         * Provides cross-browser relative touch event translation for a given element.
         */
        class Touchpad {

            /**
             * Provides cross-browser relative touch event translation for a given element.
             *
             * Touch events are translated into mouse events as if the touches occurred
             * on a touchpad (drag to push the mouse pointer, tap to click).
             *
             * @example
             * var touchpad = new Guacamole.Mouse.Touchpad(client.getDisplay().getElement());
             *
             * // Emulate a mouse using touchpad-style gestures, forwarding all mouse
             * // interaction over Guacamole connection
             * touchpad.onEach(['mousedown', 'mousemove', 'mouseup'], function sendMouseEvent(e) {
             *
             *     // Re-show software mouse cursor if possibly hidden by a prior call to
             *     // showCursor(), such as a "mouseout" event handler that hides the
             *     // cursor
             *     client.getDisplay().showCursor(true);
             *
             *     client.sendMouseState(e.state, true);
             *
             * });
             *
             * @constructor
             * @augments Guacamole.Mouse.Event.Target
             * @param {!Element} element
             *     The Element to use to provide touch events.
             */
            constructor(element: Element);
        }

        /**
         * Base event type for all mouse events. The mouse producing the event may be
         * the user's local mouse (as with {@link Guacamole.Mouse}) or an emulated
         * mouse (as with {@link Guacamole.Mouse.Touchpad}).
         */
        class MouseEvent extends Guacamole.Event.DOMEvent {

            /**
             * Base event type for all mouse events. The mouse producing the event may be
             * the user's local mouse (as with {@link Guacamole.Mouse}) or an emulated
             * mouse (as with {@link Guacamole.Mouse.Touchpad}).
             *
             * @constructor
             * @augments Guacamole.Event.DOMEvent
             * @param {!string} type
             *     The type name of the event ("mousedown", "mouseup", etc.)
             *
             * @param {!Guacamole.Mouse.State} state
             *     The current mouse state.
             *
             * @param {Event|Event[]} [events=[]]
             *     The DOM events that are related to this event, if any.
             */
            constructor(type: string, state: Guacamole.Mouse.State, events?: Event|Event[]);

            /**
             * The current mouse state at the time this event was fired.
             *
             * @type {!Guacamole.Mouse.State}
             */
            state: Guacamole.Mouse.State;
        }

        namespace Event {

            /**
             * An object which can dispatch {@link Guacamole.Mouse.Event} objects
             * representing mouse events. These mouse events may be produced from an actual
             * mouse device (as with {@link Guacamole.Mouse}), from an emulated mouse
             * device (as with {@link Guacamole.Mouse.Touchpad}, or may be programmatically
             * generated (using functions like [dispatch()]{@link Guacamole.Mouse.Event.Target#dispatch},
             * [press()]{@link Guacamole.Mouse.Event.Target#press}, and
             * [release()]{@link Guacamole.Mouse.Event.Target#release}).
             */
            class Target extends Guacamole.Event.Target {
                /**
                 * An object which can dispatch {@link Guacamole.Mouse.Event} objects
                 * representing mouse events. These mouse events may be produced from an actual
                 * mouse device (as with {@link Guacamole.Mouse}), from an emulated mouse
                 * device (as with {@link Guacamole.Mouse.Touchpad}, or may be programmatically
                 * generated (using functions like [dispatch()]{@link Guacamole.Mouse.Event.Target#dispatch},
                 * [press()]{@link Guacamole.Mouse.Event.Target#press}, and
                 * [release()]{@link Guacamole.Mouse.Event.Target#release}).
                 *
                 * @constructor
                 * @augments Guacamole.Event.Target
                 */
                constructor();
            }
        }
    }

    /**
     * An arbitrary event, emitted by a {@link Guacamole.Event.Target}. This object
     * should normally serve as the base class for a different object that is more
     * specific to the event type.
     */
    class Event {

        /**
         * An arbitrary event, emitted by a {@link Guacamole.Event.Target}. This object
         * should normally serve as the base class for a different object that is more
         * specific to the event type.
         *
         * @constructor
         * @param {!string} type
         *     The unique name of this event type.
         */
        constructor(type: string);

        /**
         * Requests that the default behavior of related DOM events be prevented.
         * Whether this request will be honored by the browser depends on the
         * nature of those events and the timing of the request.
         */
        preventDefault(): void;

        /**
         * Stops further propagation of related events through the DOM. Only events
         * that are directly related to this event will be stopped.
         */
        stopPropagation(): void;

    }

    namespace Event {

        /**
         * A {@link Guacamole.Event} that may relate to one or more DOM events.
         * Continued propagation and default behavior of the related DOM events may be
         * prevented with {@link Guacamole.Event.DOMEvent#stopPropagation stopPropagation()}
         * and {@link Guacamole.Event.DOMEvent#preventDefault preventDefault()}
         * respectively.
         */
        class DOMEvent extends Guacamole.Event {

            /**
             * A {@link Guacamole.Event} that may relate to one or more DOM events.
             * Continued propagation and default behavior of the related DOM events may be
             * prevented with {@link Guacamole.Event.DOMEvent#stopPropagation stopPropagation()}
             * and {@link Guacamole.Event.DOMEvent#preventDefault preventDefault()}
             * respectively.
             *
             * @constructor
             * @augments Guacamole.Event
             *
             * @param {!string} type
             *     The unique name of this event type.
             *
             * @param {Event|Event[]} [events=[]]
             *     The DOM events that are related to this event, if any. Future calls to
             *     {@link Guacamole.Event.DOMEvent#preventDefault preventDefault()} and
             *     {@link Guacamole.Event.DOMEvent#stopPropagation stopPropagation()} will
             *     affect these events.
             */
            constructor(type: string, events?: Event|Event[]);
        }

        /**
         * A callback function which handles an event dispatched by an event
         * target.
         *
         * @param {!Guacamole.Event} event
         *     The event that was dispatched.
         *
         * @param {!Guacamole.Event.Target} target
         *     The object that dispatched the event.
         */
        type TargetListener = (event: Guacamole.Event, target: Guacamole.Event.Target) => void;

        /**
         * An object which can dispatch {@link Guacamole.Event} objects. Listeners
         * registered with {@link Guacamole.Event.Target#on on()} will automatically
         * be invoked based on the type of {@link Guacamole.Event} passed to
         * {@link Guacamole.Event.Target#dispatch dispatch()}. It is normally
         * subclasses of Guacamole.Event.Target that will dispatch events, and usages
         * of those subclasses that will catch dispatched events with on().
         */
        class Target {

            /**
             * An object which can dispatch {@link Guacamole.Event} objects. Listeners
             * registered with {@link Guacamole.Event.Target#on on()} will automatically
             * be invoked based on the type of {@link Guacamole.Event} passed to
             * {@link Guacamole.Event.Target#dispatch dispatch()}. It is normally
             * subclasses of Guacamole.Event.Target that will dispatch events, and usages
             * of those subclasses that will catch dispatched events with on().
             *
             * @constructor
             */
            constructor();



            /**
             * Registers a listener for events having the given type, as dictated by
             * the {@link Guacamole.Event#type type} property of {@link Guacamole.Event}
             * provided to {@link Guacamole.Event.Target#dispatch dispatch()}.
             *
             * @param {!string} type
             *     The unique name of this event type.
             *
             * @param {!Guacamole.Event.Target~listener} listener
             *     The function to invoke when an event having the given type is
             *     dispatched. The {@link Guacamole.Event} object provided to
             *     {@link Guacamole.Event.Target#dispatch dispatch()} will be passed to
             *     this function, along with the dispatching Guacamole.Event.Target.
             */
            on(type: string, listener: Guacamole.Event.TargetListener): void;

            /**
             * Registers a listener for events having the given types, as dictated by
             * the {@link Guacamole.Event#type type} property of {@link Guacamole.Event}
             * provided to {@link Guacamole.Event.Target#dispatch dispatch()}.
             * <p>
             * Invoking this function is equivalent to manually invoking
             * {@link Guacamole.Event.Target#on on()} for each of the provided types.
             *
             * @param {!string[]} types
             *     The unique names of the event types to associate with the given
             *     listener.
             *
             * @param {!Guacamole.Event.Target~listener} listener
             *     The function to invoke when an event having any of the given types
             *     is dispatched. The {@link Guacamole.Event} object provided to
             *     {@link Guacamole.Event.Target#dispatch dispatch()} will be passed to
             *     this function, along with the dispatching Guacamole.Event.Target.
             */
            onEach(types: string[], listener: Guacamole.Event.TargetListener): void;

        }
    }

    /**
     * Abstract ordered drawing surface. Each Layer contains a canvas element and
     * provides simple drawing instructions for drawing to that canvas element,
     * however unlike the canvas element itself, drawing operations on a Layer are
     * guaranteed to run in order, even if such an operation must wait for an image
     * to load before completing.
     */
    class Layer {
        /**
         * @param {Number} width The width of the Layer, in pixels. The canvas element
         *                       backing this Layer will be given this width.
         *
         * @param {Number} height The height of the Layer, in pixels. The canvas element
         *                        backing this Layer will be given this height.
         */
        constructor(width: number, height: number);

        /**
         * Returns the canvas element backing this Layer. Note that the dimensions
         * of the canvas may not exactly match those of the Layer, as resizing a
         * canvas while maintaining its state is an expensive operation.
         *
         * @returns {HTMLCanvasElement}
         *     The canvas element backing this Layer.
         */
        getCanvas(): any;

        /**
         * Returns a new canvas element containing the same image as this Layer.
         * Unlike getCanvas(), the canvas element returned is guaranteed to have
         * the exact same dimensions as the Layer.
         *
         * @returns {HTMLCanvasElement}
         *     A new canvas element containing a copy of the image content this
         *     Layer.
         */
        toCanvas(): any;

        /**
         * Changes the size of this Layer to the given width and height. Resizing
         * is only attempted if the new size provided is actually different from
         * the current size.
         *
         * @param {Number} newWidth The new width to assign to this Layer.
         * @param {Number} newHeight The new height to assign to this Layer.
         */
        resize(newWidth: number, newHeight: number): void;

        /**
         * Draws the specified image at the given coordinates. The image specified
         * must already be loaded.
         *
         * @param {Number} x The destination X coordinate.
         * @param {Number} y The destination Y coordinate.
         * @param {Image} image The image to draw. Note that this is an Image
         *                      object - not a URL.
         */
        drawImage(x: number, y: number, image: any): void;

        /**
         * Transfer a rectangle of image data from one Layer to this Layer using the
         * specified transfer function.
         *
         * @param {Guacamole.Layer} srcLayer The Layer to copy image data from.
         * @param {Number} srcx The X coordinate of the upper-left corner of the
         *                      rectangle within the source Layer's coordinate
         *                      space to copy data from.
         * @param {Number} srcy The Y coordinate of the upper-left corner of the
         *                      rectangle within the source Layer's coordinate
         *                      space to copy data from.
         * @param {Number} srcw The width of the rectangle within the source Layer's
         *                      coordinate space to copy data from.
         * @param {Number} srch The height of the rectangle within the source
         *                      Layer's coordinate space to copy data from.
         * @param {Number} x The destination X coordinate.
         * @param {Number} y The destination Y coordinate.
         * @param {Function} transferFunction The transfer function to use to
         *                                    transfer data from source to
         *                                    destination.
         */
        transfer(srcLayer: Layer, srcx: number, srcy: number, srcw: number, srch: number, x: number, y: number, transferFunction: any): void;

        /**
         * Put a rectangle of image data from one Layer to this Layer directly
         * without performing any alpha blending. Simply copy the data.
         *
         * @param {Guacamole.Layer} srcLayer The Layer to copy image data from.
         * @param {Number} srcx The X coordinate of the upper-left corner of the
         *                      rectangle within the source Layer's coordinate
         *                      space to copy data from.
         * @param {Number} srcy The Y coordinate of the upper-left corner of the
         *                      rectangle within the source Layer's coordinate
         *                      space to copy data from.
         * @param {Number} srcw The width of the rectangle within the source Layer's
         *                      coordinate space to copy data from.
         * @param {Number} srch The height of the rectangle within the source
         *                      Layer's coordinate space to copy data from.
         * @param {Number} x The destination X coordinate.
         * @param {Number} y The destination Y coordinate.
         */
        put(srcLayer: Layer, srcx: number, srcy: number, srcw: number, srch: number, x: number, y: number): void;

        /**
         * Copy a rectangle of image data from one Layer to this Layer. This
         * operation will copy exactly the image data that will be drawn once all
         * operations of the source Layer that were pending at the time this
         * function was called are complete. This operation will not alter the
         * size of the source Layer even if its autosize property is set to true.
         *
         * @param {Guacamole.Layer} srcLayer The Layer to copy image data from.
         * @param {Number} srcx The X coordinate of the upper-left corner of the
         *                      rectangle within the source Layer's coordinate
         *                      space to copy data from.
         * @param {Number} srcy The Y coordinate of the upper-left corner of the
         *                      rectangle within the source Layer's coordinate
         *                      space to copy data from.
         * @param {Number} srcw The width of the rectangle within the source Layer's
         *                      coordinate space to copy data from.
         * @param {Number} srch The height of the rectangle within the source
         *                      Layer's coordinate space to copy data from.
         * @param {Number} x The destination X coordinate.
         * @param {Number} y The destination Y coordinate.
         */
        copy(srcLayer: Layer, srcx: number, srcy: number, srcw: number, srch: number, x: number, y: number): void;

        /**
         * Starts a new path at the specified point.
         *
         * @param {Number} x The X coordinate of the point to draw.
         * @param {Number} y The Y coordinate of the point to draw.
         */
        moveTo(x: number, y: number): void;

        /**
         * Add the specified line to the current path.
         *
         * @param {Number} x The X coordinate of the endpoint of the line to draw.
         * @param {Number} y The Y coordinate of the endpoint of the line to draw.
         */
        lineTo(x: number, y: number): void;

        /**
         * Add the specified arc to the current path.
         *
         * @param {Number} x The X coordinate of the center of the circle which
         *                   will contain the arc.
         * @param {Number} y The Y coordinate of the center of the circle which
         *                   will contain the arc.
         * @param {Number} radius The radius of the circle.
         * @param {Number} startAngle The starting angle of the arc, in radians.
         * @param {Number} endAngle The ending angle of the arc, in radians.
         * @param {Boolean} negative Whether the arc should be drawn in order of
         *                           decreasing angle.
         */
        arc(x: number, y: number, radius: number, startAngle: number, endAngle: number, negative: boolean): void;

        /**
         * Starts a new path at the specified point.
         *
         * @param {Number} cp1x The X coordinate of the first control point.
         * @param {Number} cp1y The Y coordinate of the first control point.
         * @param {Number} cp2x The X coordinate of the second control point.
         * @param {Number} cp2y The Y coordinate of the second control point.
         * @param {Number} x The X coordinate of the endpoint of the curve.
         * @param {Number} y The Y coordinate of the endpoint of the curve.
         */
        curveTo(cp1x: number, cp1y: number, cp2x: number, cp2y: number, x: number, y: number): void;

        /**
         * Closes the current path by connecting the end point with the start
         * point (if any) with a straight line.
         */
        close(): void;

        /**
         * Add the specified rectangle to the current path.
         *
         * @param {Number} x The X coordinate of the upper-left corner of the
         *                   rectangle to draw.
         * @param {Number} y The Y coordinate of the upper-left corner of the
         *                   rectangle to draw.
         * @param {Number} w The width of the rectangle to draw.
         * @param {Number} h The height of the rectangle to draw.
         */
        rect(x: number, y: number, w: number, h: number): void;

        /**
         * Clip all future drawing operations by the current path. The current path
         * is implicitly closed. The current path can continue to be reused
         * for other operations (such as fillColor()) but a new path will be started
         * once a path drawing operation (path() or rect()) is used.
         */
        clip(): void;

        /**
         * Stroke the current path with the specified color. The current path
         * is implicitly closed. The current path can continue to be reused
         * for other operations (such as clip()) but a new path will be started
         * once a path drawing operation (path() or rect()) is used.
         *
         * @param {String} cap The line cap style. Can be "round", "square",
         *                     or "butt".
         * @param {String} join The line join style. Can be "round", "bevel",
         *                      or "miter".
         * @param {Number} thickness The line thickness in pixels.
         * @param {Number} r The red component of the color to fill.
         * @param {Number} g The green component of the color to fill.
         * @param {Number} b The blue component of the color to fill.
         * @param {Number} a The alpha component of the color to fill.
         */
        strokeColor(cap: string, join: string, thickness: number, r: number, g: number, b: number, a: number): void;

        /**
         * Fills the current path with the specified color. The current path
         * is implicitly closed. The current path can continue to be reused
         * for other operations (such as clip()) but a new path will be started
         * once a path drawing operation (path() or rect()) is used.
         *
         * @param {Number} r The red component of the color to fill.
         * @param {Number} g The green component of the color to fill.
         * @param {Number} b The blue component of the color to fill.
         * @param {Number} a The alpha component of the color to fill.
         */
        fillColor(r: number, g: number, b: number, a: number): void;

        /**
         * Stroke the current path with the image within the specified layer. The
         * image data will be tiled infinitely within the stroke. The current path
         * is implicitly closed. The current path can continue to be reused
         * for other operations (such as clip()) but a new path will be started
         * once a path drawing operation (path() or rect()) is used.
         *
         * @param {String} cap The line cap style. Can be "round", "square",
         *                     or "butt".
         * @param {String} join The line join style. Can be "round", "bevel",
         *                      or "miter".
         * @param {Number} thickness The line thickness in pixels.
         * @param {Guacamole.Layer} srcLayer The layer to use as a repeating pattern
         *                                   within the stroke.
         */
        strokeLayer(cap: string, join: string, thickness: number, srcLayer: Layer): void;

        /**
         * Fills the current path with the image within the specified layer. The
         * image data will be tiled infinitely within the stroke. The current path
         * is implicitly closed. The current path can continue to be reused
         * for other operations (such as clip()) but a new path will be started
         * once a path drawing operation (path() or rect()) is used.
         *
         * @param {Guacamole.Layer} srcLayer The layer to use as a repeating pattern
         *                                   within the fill.
         */
        fillLayer(srcLayer: Layer): void;

        /**
         * Push current layer state onto stack.
         */
        push(): void;

        /**
         * Pop layer state off stack.
         */
        pop(): void;

        /**
         * Reset the layer, clearing the stack, the current path, and any transform
         * matrix.
         */
        reset(): void;

        /**
         * Sets the given affine transform (defined with six values from the
         * transform's matrix).
         *
         * @param {Number} a The first value in the affine transform's matrix.
         * @param {Number} b The second value in the affine transform's matrix.
         * @param {Number} c The third value in the affine transform's matrix.
         * @param {Number} d The fourth value in the affine transform's matrix.
         * @param {Number} e The fifth value in the affine transform's matrix.
         * @param {Number} f The sixth value in the affine transform's matrix.
         */
        setTransform(a: number, b: number, c: number, d: number, e: number, f: number): void;

        /**
         * Applies the given affine transform (defined with six values from the
         * transform's matrix).
         *
         * @param {Number} a The first value in the affine transform's matrix.
         * @param {Number} b The second value in the affine transform's matrix.
         * @param {Number} c The third value in the affine transform's matrix.
         * @param {Number} d The fourth value in the affine transform's matrix.
         * @param {Number} e The fifth value in the affine transform's matrix.
         * @param {Number} f The sixth value in the affine transform's matrix.
         */
        transform(a: number, b: number, c: number, d: number, e: number, f: number): void;

        /**
         * Sets the channel mask for future operations on this Layer.
         *
         * The channel mask is a Guacamole-specific compositing operation identifier
         * with a single bit representing each of four channels (in order): source
         * image where destination transparent, source where destination opaque,
         * destination where source transparent, and destination where source
         * opaque.
         *
         * @param {Number} mask The channel mask for future operations on this
         *                      Layer.
         */
        setChannelMask(mask: number): void;

        /**
         * Sets the miter limit for stroke operations using the miter join. This
         * limit is the maximum ratio of the size of the miter join to the stroke
         * width. If this ratio is exceeded, the miter will not be drawn for that
         * joint of the path.
         *
         * @param {Number} limit The miter limit for stroke operations using the
         *                       miter join.
         */
        setMiterLimit(limit: number): void;

    }

    /**
     * A Guacamole status. Each Guacamole status consists of a status code, defined
     * by the protocol, and an optional human-readable message, usually only
     * included for debugging convenience.
     *
     * @constructor
     * @param {Number} code
     *     The Guacamole status code, as defined by Guacamole.Status.Code.
     *
     * @param {String} [message]
     *     An optional human-readable message.
     */
    class Status {
        /**
         * Enumeration of all Guacamole status codes.
         */
        static Code: any;

        /**
         * The Guacamole status code.
         *
         * @see Guacamole.Status.Code
         * @type {!number}
         */
        code: number;

        /**
         * An arbitrary human-readable message associated with this status, if any.
         * The human-readable message is not required, and is generally provided
         * for debugging purposes only. For user feedback, it is better to translate
         * the Guacamole status code into a message.
         *
         * @type {string}
         */
        message: string;

        /**
         * @param {Number} code
         *     The Guacamole status code, as defined by Guacamole.Status.Code.
         *
         * @param {String} [message]
         *     An optional human-readable message.
         */
        constructor(code: number, message: string);

        /**
         * Returns whether this status represents an error.
         * @returns {Boolean} true if this status represents an error, false
         *                    otherwise.
         */
        isError(): boolean;
    }

    /**
     * An input stream abstraction used by the Guacamole client to facilitate
     * transfer of files or other binary data.
     */
    class InputStream {

        /**
         * The index of this stream
         */
        index: number;

        /**
         * @param {Guacamole.Client} client The client owning this stream.
         * @param {Number} index The index of this stream.
         */
        constructor(client: Client, index: number);

        /**
         * Called when a blob of data is received.
         *
         * @event
         * @param {!string} data
         *     The received base64 data.
         */
        onblob: ((data: string) => void) | null;

        /**
         * Called when this stream is closed.
         *
         * @event
         */
        onend: (() => void) | null;

        /**
         * Acknowledges the receipt of a blob.
         *
         * @param {String} message A human-readable message describing the error
         *                         or status.
         * @param {Number} code The error code, if any, or 0 for success.
         */
        sendAck(message: string, code: number): void;

    }

    /**
     * Abstract stream which can receive data.
     */
    class OutputStream {

        /**
         * The index of this stream
         */
        index: number;


        /**
         * @param {Guacamole.Client} client The client owning this stream.
         * @param {Number} index The index of this stream.
         */
        constructor(client: Client, index: number);

        /**
         * Fired whenever an acknowledgement is received from the server, indicating
         * that a stream operation has completed, or an error has occurred.
         *
         * @event
         * @param {!Guacamole.Status} status
         *     The status of the operation.
         */
        onack: ((status: Status) => void) | null;

        /**
         * Writes the given base64-encoded data to this stream as a blob.
         *
         * @param {String} data The base64-encoded data to send.
         */
        sendBlob(data: string): void;

        /**
         * Closes this stream.
         */
        sendEnd(): void;
    }

    /**
     * Integer pool which returns consistently increasing integers while integers
     * are in use, and previously-used integers when possible.
     */
    class IntegerPool {
        /**
         * Returns the next available integer in the pool. If possible, a previously
         * used integer will be returned.
         *
         * @return {Number} The next available integer.
         */
        next(): number;

        /**
         * Frees the given integer, allowing it to be reused.
         *
         * @param {Number} integer The integer to free.
         */
        free(integer: number): void;

    }

    /**
     * A reader which automatically handles the given input stream, returning
     * strictly text data. Note that this object will overwrite any installed event
     * handlers on the given Guacamole.InputStream.
     */
    class StringReader {
        /**
         * @param {Guacamole.InputStream} stream The stream that data will be read
         *                                       from.
         */
        constructor(stream: InputStream);

        /**
         * Fired once for every blob of text data received.
         *
         * @event
         * @param {String} text The data packet received.
         */
        ontext(text: string): void;

        /**
         * Fired once this stream is finished and no further data will be written.
         * @event
         */
        onend(): void;

    }

    /**
     * A writer which automatically writes to the given output stream with the
     * contents of provided Blob objects.
     *
     * @constructor
     * @param {Guacamole.OutputStream} stream
     *     The stream that data will be written to.
     */
    class BlobWriter {
        /**
         * @param {Guacamole.OutputStream} stream
         *     The stream that data will be written to.
         */
        constructor(stream: OutputStream);

        /**
         * Sends the contents of the given blob over the underlying stream.
         *
         * @param {Blob} blob
         *     The blob to send.
         */
        sendBlob(blob: any): void;

        /**
         * Signals that no further text will be sent, effectively closing the
         * stream.
         */
        sendEnd(): void;

        /**
         * Fired for received data, if acknowledged by the server.
         *
         * @event
         * @param {Guacamole.Status} status
         *     The status of the operation.
         */
        onack(status: Status): void;

        /**
         * Fired when an error occurs reading a blob passed to
         * [sendBlob()]{@link Guacamole.BlobWriter#sendBlob}. The transfer for the
         * the given blob will cease, but the stream will remain open.
         *
         * @event
         * @param {Blob} blob
         *     The blob that was being read when the error occurred.
         *
         * @param {Number} offset
         *     The offset of the failed read attempt within the blob, in bytes.
         *
         * @param {DOMError} error
         *     The error that occurred.
         */
        onerror(blob: any, offset: number, error: any): void;

        /**
         * Fired for each successfully-read chunk of data as a blob is being sent
         * via [sendBlob()]{@link Guacamole.BlobWriter#sendBlob}.
         *
         * @event
         * @param {Blob} blob
         *     The blob that is being read.
         *
         * @param {Number} offset
         *     The offset of the read that just succeeded.
         */
        onprogress(blob: any, offset: number): void;

        /**
         * Fired when a blob passed to
         * [sendBlob()]{@link Guacamole.BlobWriter#sendBlob} has finished being
         * sent.
         *
         * @event
         * @param {Blob} blob
         *     The blob that was sent.
         */
        oncomplete(blob: any): void;

    }

    /**
     * A reader which automatically handles the given input stream, assembling all
     * received blobs into a single blob by appending them to each other in order.
     * Note that this object will overwrite any installed event handlers on the
     * given Guacamole.InputStream.
     */
    class BlobReader {
        /**
         * @param {Guacamole.InputStream} stream The stream that data will be read
         *                                       from.
         * @param {String} mimetype The mimetype of the blob being built.
         */
        constructor(stream: InputStream, mimetype: string);

        /**
         * Returns the current length of this Guacamole.InputStream, in bytes.
         * @return {Number} The current length of this Guacamole.InputStream.
         */
        getLength(): number;

        /**
         * Returns the contents of this Guacamole.BlobReader as a Blob.
         * @return {Blob} The contents of this Guacamole.BlobReader.
         */
        getBlob(): any;

        /**
         * Fired once for every blob of data received.
         *
         * @event
         * @param {Number} length The number of bytes received.
         */
        onprogress(length: number): void;

        /**
         * Fired once this stream is finished and no further data will be written.
         * @event
         */
        onend(): void;

    }

    /**
     * A reader which automatically handles the given input stream, returning
     * received blobs as a single data URI built over the course of the stream.
     * Note that this object will overwrite any installed event handlers on the
     * given Guacamole.InputStream.
     */
    class DataURIReader {
        /**
         * @param {Guacamole.InputStream} stream
         * @param mimetype
         */
        constructor(stream: InputStream, mimetype: InputStream);

        /**
         * Returns the data URI of all data received through the underlying stream
         * thus far.
         *
         * @returns {String}
         *     The data URI of all data received through the underlying stream thus
         *     far.
         */
        getURI(): string;

        /**
         * Fired once this stream is finished and no further data will be written.
         *
         * @event
         */
        onend(): void;
    }

    /**
     * An object used by the Guacamole client to house arbitrarily-many named
     * input and output streams.
     */
    class Object {
        /**
         * @param {Guacamole.Client} client
         *     The client owning this object.
         *
         * @param {Number} index
         *     The index of this object.
         */
        constructor(client: Client, index: number);

        /**
         * Called when this object receives the body of a requested input stream.
         * By default, all objects will invoke the callbacks provided to their
         * requestInputStream() functions based on the name of the stream
         * requested. This behavior can be overridden by specifying a different
         * handler here.
         *
         * @event
         * @param {Guacamole.InputStream} inputStream
         *     The input stream of the received body.
         *
         * @param {String} mimetype
         *     The mimetype of the data being received.
         *
         * @param {String} name
         *     The name of the stream whose body has been received.
         */
        onbody(inputStream: InputStream, mimetype: string, name: string): void;

        /**
         * Called when this object is being undefined. Once undefined, no further
         * communication involving this object may occur.
         *
         * @event
         */
        onundefine(): void;

        /**
         * Requests read access to the input stream having the given name. If
         * successful, a new input stream will be created.
         *
         * @param {String} name
         *     The name of the input stream to request.
         *
         * @param {Function} [bodyCallback]
         *     The callback to invoke when the body of the requested input stream
         *     is received. This callback will be provided a Guacamole.InputStream
         *     and its mimetype as its two only arguments. If the onbody handler of
         *     this object is overridden, this callback will not be invoked.
         */
        requestInputStream(name: string, bodyCallback: any): void;

        /**
         * Creates a new output stream associated with this object and having the
         * given mimetype and name. The legality of a mimetype and name is dictated
         * by the object itself.
         *
         * @param {String} mimetype
         *     The mimetype of the data which will be sent to the output stream.
         *
         * @param {String} name
         *     The defined name of an output stream within this object.
         *
         * @returns {Guacamole.OutputStream}
         *     An output stream which will write blobs to the named output stream
         *     of this object.
         */
        createOutputStream(mimetype: string, name: string): OutputStream;

        /**
         * The reserved name denoting the root stream of any object. The contents of
         * the root stream MUST be a JSON map of stream name to mimetype.
         *
         * @constant
         * @type {!string}
         */
        ROOT_STREAM: string;

        /**
         * The mimetype of a stream containing JSON which maps available stream names
         * to their corresponding mimetype. The root stream of a Guacamole.Object MUST
         * have this mimetype.
         *
         * @constant
         * @type {!string}
         */
        STREAM_INDEX_MIMETYPE: string;
    }

    /**
     * A reader which automatically handles the given input stream, assembling all
     * received blobs into a JavaScript object by appending them to each other, in
     * order, and decoding the result as JSON. Note that this object will overwrite
     * any installed event handlers on the given Guacamole.InputStream.
     */
    class JSONReader {
        /**
         * @param {Guacamole.InputStream} stream
         */
        constructor(stream: InputStream);

        /**
         * Returns the current length of this Guacamole.JSONReader, in characters.
         *
         * @return {Number}
         *     The current length of this Guacamole.JSONReader.
         */
        getLength(): number;

        /**
         * Returns the contents of this Guacamole.JSONReader as a JavaScript
         * object.
         *
         * @return {Object}
         *     The contents of this Guacamole.JSONReader, as parsed from the JSON
         *     contents of the input stream.
         */
        getJSON(): any;

        /**
         * Fired once for every blob of data received.
         *
         * @event
         * @param {Number} length
         *     The number of characters received.
         */
        onprogress(length: number): void;

        /**
         * Fired once this stream is finished and no further data will be written.
         *
         * @event
         */
        onend(): void;
    }

    /**
     * A writer which automatically writes to the given output stream with text
     * data.
     */
    class StringWriter {
        /**
         * @param stream @param {Guacamole.OutputStream} stream The stream that data will be written to.
         */
        constructor(stream: OutputStream);

        /**
         * Sends the given text.
         *
         * @param {String} text The text to send.
         */
        sendText(text: string): void;

        /**
         * Signals that no further text will be sent, effectively closing the
         * stream.
         */
        sendEnd(): void;

        /**
         * Fired for received data, if acknowledged by the server.
         * @event
         * @param {Guacamole.Status} status The status of the operation.
         */
        onack: null | ((status: Status) => void);
    }

    /**
     * Provides cross-browser multi-touch events for a given element. The events of
     * the given element are automatically populated with handlers that translate
     * touch events into a non-browser-specific event provided by the
     * Guacamole.Touch instance.
     */
    class Touch {

        /**
         * Provides cross-browser multi-touch events for a given element. The events of
         * the given element are automatically populated with handlers that translate
         * touch events into a non-browser-specific event provided by the
         * Guacamole.Touch instance.
         *
         * @constructor
         * @augments Guacamole.Event.Target
         * @param {!Element} element
         *     The Element to use to provide touch events.
         */
        constructor(element: Element);
    }

    namespace Touch {

        /**
         * The current state of a touch contact.
         */
        class State{

            /**
             * The current state of a touch contact.
             *
             * @constructor
             * @augments Guacamole.Position
             * @param {Guacamole.Touch.State|object} [template={}]
             *     The object whose properties should be copied within the new
             *     Guacamole.Touch.State.
             */
            constructor(template?: State | any);
        }

        /**
         * An event which represents a change in state of a single touch contact,
         * including the creation or removal of that contact. If multiple contacts are
         * involved in a touch interaction, each contact will be associated with its
         * own event.
         */
        class Event extends Guacamole.Event.DOMEvent {
            /**
             * An event which represents a change in state of a single touch contact,
             * including the creation or removal of that contact. If multiple contacts are
             * involved in a touch interaction, each contact will be associated with its
             * own event.
             *
             * @constructor
             * @augments Guacamole.Event.DOMEvent
             * @param {!string} type
             *     The name of the touch event type. Possible values are "touchstart",
             *     "touchmove", and "touchend".
             *
             * @param {!TouchEvent} event
             *     The DOM touch event that produced this Guacamole.Touch.Event.
             *
             * @param {!Guacamole.Touch.State} state
             *     The state of the touch contact associated with this event.
             */
            constructor(type: string, event: TouchEvent, state: Guacamole.Touch.State);

            /**
             * The state of the touch contact associated with this event.
             *
             * @type {!Guacamole.Touch.State}
             */
            state: Guacamole.Touch.State;
        }
    }

    /**
     * Simple Guacamole protocol parser that invokes an oninstruction event when
     * full instructions are available from data received via receive().
     */
    class Parser {
        /**
         * Appends the given instruction data packet to the internal buffer of
         * this Guacamole.Parser, executing all completed instructions at
         * the beginning of this buffer, if any.
         *
         * @param {String} packet The instruction data to receive.
         */
        receive(packet: string): void;

        /**
         * Fired once for every complete Guacamole instruction received, in order.
         *
         * @event
         * @param {String} opcode The Guacamole instruction opcode.
         * @param {Array} parameters The parameters provided for the instruction,
         *                           if any.
         */
        oninstruction(opcode: string, parameters: any): void;

    }

    /**
     * A reader which automatically handles the given input stream, returning
     * strictly received packets as array buffers. Note that this object will
     * overwrite any installed event handlers on the given Guacamole.InputStream.
     */
    class ArrayBufferReader {
        /**
         * @param {Guacamole.InputStream} stream The stream that data will be read
         */
        constructor(stream: InputStream);

        /**
         * Fired once for every blob of data received.
         *
         * @event
         * @param {ArrayBuffer} buffer The data packet received.
         */
        ondata(buffer: ArrayBuffer): void;

        /**
         * Fired once this stream is finished and no further data will be written.
         * @event
         */
        onend(): void;
    }

    /**
     * A writer which automatically writes to the given output stream with arbitrary
     * binary data, supplied as ArrayBuffers.
     */
    class ArrayBufferWriter {
        /**
         * @param {Guacamole.OutputStream} stream The stream that data will be written to.
         */
        constructor(stream: OutputStream);

        /**
         * Sends the given data.
         *
         * @param {ArrayBuffer|TypedArray} data The data to send.
         */
        sendData(data: any): void;

        /**
         * Signals that no further text will be sent, effectively closing the
         * stream.
         */
        sendEnd(): void;

        /**
         * Fired for received data, if acknowledged by the server.
         * @event
         * @param {Guacamole.Status} status The status of the operation.
         */
        onack(status: Status): void;
    }

    /**
     * A description of the format of raw PCM audio, such as that used by
     * Guacamole.RawAudioPlayer and Guacamole.RawAudioRecorder. This object
     * describes the number of bytes per sample, the number of channels, and the
     * overall sample rate.
     */
    class RawAudioFormat {
        /**
         * @param {Guacamole.RawAudioFormat|Object} template
         *     The object whose properties should be copied into the corresponding
         *     properties of the new Guacamole.RawAudioFormat.
         */
        constructor(template: any);

        /**
         * Parses the given mimetype, returning a new Guacamole.RawAudioFormat
         * which describes the type of raw audio data represented by that mimetype. If
         * the mimetype is not a supported raw audio data mimetype, null is returned.
         *
         * @param {String} mimetype
         *     The audio mimetype to parse.
         *
         * @returns {Guacamole.RawAudioFormat}
         *     A new Guacamole.RawAudioFormat which describes the type of raw
         *     audio data represented by the given mimetype, or null if the given
         *     mimetype is not supported.
         */
        static parse(mimetype: string): RawAudioFormat;
    }

    /**
     * Abstract video player which accepts, queues and plays back arbitrary video
     * data. It is up to implementations of this class to provide some means of
     * handling a provided Guacamole.InputStream and rendering the received data to
     * the provided Guacamole.Display.VisibleLayer. Data received along the
     * provided stream is to be played back immediately.
     */
    class VideoPlayer {
        /**
         * Determines whether the given mimetype is supported by any built-in
         * implementation of Guacamole.VideoPlayer, and thus will be properly handled
         * by Guacamole.VideoPlayer.getInstance().
         *
         * @param {String} mimetype
         *     The mimetype to check.
         *
         * @returns {Boolean}
         *     true if the given mimetype is supported by any built-in
         *     Guacamole.VideoPlayer, false otherwise.
         */
        static isSupportedType(mimetype: string): boolean;

        /**
         * Returns a list of all mimetypes supported by any built-in
         * Guacamole.VideoPlayer, in rough order of priority. Beware that only the core
         * mimetypes themselves will be listed. Any mimetype parameters, even required
         * ones, will not be included in the list.
         *
         * @returns {String[]}
         *     A list of all mimetypes supported by any built-in Guacamole.VideoPlayer,
         *     excluding any parameters.
         */
        static getSupportedTypes(): string[];

        /**
         * Returns an instance of Guacamole.VideoPlayer providing support for the given
         * video format. If support for the given video format is not available, null
         * is returned.
         *
         * @param {Guacamole.InputStream} stream
         *     The Guacamole.InputStream to read video data from.
         *
         * @param {Guacamole.Display.VisibleLayer} layer
         *     The destination layer in which this Guacamole.VideoPlayer should play
         *     the received video data.
         *
         * @param {String} mimetype
         *     The mimetype of the video data in the provided stream.
         *
         * @return {Guacamole.VideoPlayer}
         *     A Guacamole.VideoPlayer instance supporting the given mimetype and
         *     reading from the given stream, or null if support for the given mimetype
         *     is absent.
         */
        static getInstance(stream: InputStream, layer: Display.VisibleLayer, mimetype: string): VideoPlayer;

        /**
         * Notifies this Guacamole.VideoPlayer that all video up to the current
         * point in time has been given via the underlying stream, and that any
         * difference in time between queued video data and the current time can be
         * considered latency.
         */
        sync(): void;
    }

    /**
     * A recording of a Guacamole session. Given a {@link Guacamole.Tunnel} or Blob,
     * the Guacamole.SessionRecording automatically parses Guacamole instructions
     * within the recording source as it plays back the recording. Playback of the
     * recording may be controlled through function calls to the
     * Guacamole.SessionRecording, even while the recording has not yet finished
     * being created or downloaded. Parsing of the contents of the recording will
     * begin immediately and automatically after this constructor is invoked.
     */
    class SessionRecording {

        /**
         * @param {!Blob|Guacamole.Tunnel} source
         *     The Blob from which the instructions of the recording should
         *     be read.
         */
        constructor(source: Blob | Tunnel);

        /**
         * Fired when loading of this recording has completed and all frames
         * are available.
         *
         * @event
         */
        onload: null | (() => void);

        /**
         * Fired when an error occurs which prevents the recording from being
         * played back.
         *
         * @event
         * @param {!string} message
         *     A human-readable message describing the error that occurred.
         */
        onerror: null | ((message: string) => void);

        /**
         * Fired when further loading of this recording has been explicitly
         * aborted through a call to abort().
         *
         * @event
         */
        onabort: null | (() => void);

        /**
         * Fired when new frames have become available while the recording is
         * being downloaded.
         *
         * @event
         * @param {!number} duration
         *     The new duration of the recording, in milliseconds.
         *
         * @param {!number} parsedSize
         *     The number of bytes that have been loaded/parsed.
         */
        onprogress: null | ((duration: number, parsedSize: number) => void);

        /**
         * Fired whenever playback of the recording has started.
         *
         * @event
         */
        onplay: null | (() => void);

        /**
         * Fired whenever playback of the recording has been paused. This may
         * happen when playback is explicitly paused with a call to pause(), or
         * when playback is implicitly paused due to reaching the end of the
         * recording.
         *
         * @event
         */
        onpause: null | (() => void);

        /**
         * Fired whenever the playback position within the recording changes.
         *
         * @event
         * @param {!number} position
         *     The new position within the recording, in milliseconds.
         *
         * @param {!number} current
         *     The number of frames that have been seeked through. If not
         *     seeking through multiple frames due to a call to seek(), this
         *     will be 1.
         *
         * @param {!number} total
         *     The number of frames that are being seeked through in the
         *     current seek operation. If not seeking through multiple frames
         *     due to a call to seek(), this will be 1.
         */
        onseek: null | ((position: number, current: number, total: number) => void);

        /**
         * Connects the underlying tunnel, beginning download of the Guacamole
         * session. Playback of the Guacamole session cannot occur until at least
         * one frame worth of instructions has been downloaded. If the underlying
         * recording source is a Blob, this function has no effect.
         *
         * @param {string} [data]
         *     The data to send to the tunnel when connecting.
         */
        connect(data?: string): void;

        /**
         * Aborts the loading process, stopping further processing of the
         * provided data. If the underlying recording source is a Guacamole tunnel,
         * it will be disconnected.
         */
        abort(): void;

        /**
         * Returns the underlying display of the Guacamole.Client used by this
         * Guacamole.SessionRecording for playback. The display contains an Element
         * which can be added to the DOM, causing the display (and thus playback of
         * the recording) to become visible.
         *
         * @return {!Guacamole.Display}
         *     The underlying display of the Guacamole.Client used by this
         *     Guacamole.SessionRecording for playback.
         */
        getDisplay(): Display;

        /**
         * Returns whether playback is currently in progress.
         *
         * @returns {!boolean}
         *     true if playback is currently in progress, false otherwise.
         */
        isPlaying(): boolean;

        /**
         * Returns the duration of this recording, in milliseconds. If the
         * recording is still being downloaded, this value will gradually increase.
         *
         * @returns {!number}
         *     The duration of this recording, in milliseconds.
         */
        getDuration(): number;

        /**
         * Begins continuous playback of the recording downloaded thus far.
         * Playback of the recording will continue until pause() is invoked or
         * until no further frames exist. Playback is initially paused when a
         * Guacamole.SessionRecording is created, and must be explicitly started
         * through a call to this function. If playback is already in progress,
         * this function has no effect. If a seek operation is in progress,
         * playback resumes at the current position, and the seek is aborted as if
         * completed.
         */
        play(): void;

        /**
         * Seeks to the given position within the recording. If the recording is
         * currently being played back, playback will continue after the seek is
         * performed. If the recording is currently paused, playback will be
         * paused after the seek is performed. If a seek operation is already in
         * progress, that seek is first aborted. The seek operation will proceed
         * asynchronously.
         *
         * @param {!number} position
         *     The position within the recording to seek to, in milliseconds.
         *
         * @param {function} [callback]
         *     The callback to invoke once the seek operation has completed.
         */
        seek(position: number, callback?: () => void): void;

        /**
         * Cancels the current seek operation, setting the current frame of the
         * recording to wherever the seek operation was able to reach prior to
         * being cancelled. If a callback was provided to seek(), that callback
         * is invoked. If a seek operation is not currently underway, this
         * function has no effect.
         */
        cancel(): void;

        /**
         * Pauses playback of the recording, if playback is currently in progress.
         * If playback is not in progress, this function has no effect. If a seek
         * operation is in progress, the seek is aborted. Playback is initially
         * paused when a Guacamole.SessionRecording is created, and must be
         * explicitly started through a call to play().
         */
        pause(): void;

        /**
         * A single frame of Guacamole session data. Each frame is made up of the set
         * of instructions used to generate that frame, and the timestamp as dictated
         * by the "sync" instruction terminating the frame. Optionally, a frame may
         * also be associated with a snapshot of Guacamole client state, such that the
         * frame can be rendered without replaying all previous frames.
         *
         * @private
         * @constructor
         * @param {!number} timestamp
         *     The timestamp of this frame, as dictated by the "sync" instruction which
         *     terminates the frame.
         *
         * @param {!number} start
         *     The byte offset within the blob of the first character of the first
         *     instruction of this frame.
         *
         * @param {!number} end
         *     The byte offset within the blob of character which follows the last
         *     character of the last instruction of this frame.
         */
        _Frame(timestamp: number, start: number, end: number): void;

        /**
         * A read-only Guacamole.Tunnel implementation which streams instructions
         * received through explicit calls to its receiveInstruction() function.
         *
         * @private
         * @constructor
         * @augments {Guacamole.Tunnel}
         */
        _PlaybackTunnel(): void;
    }
}
