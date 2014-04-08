/*
 * Copyright (C) 2013 Glyptodon LLC
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

var Guacamole = Guacamole || {};

/**
 * Guacamole protocol client. Given a display element and {@link Guacamole.Tunnel},
 * automatically handles incoming and outgoing Guacamole instructions via the
 * provided tunnel, updating the display using one or more canvas elements.
 * 
 * @constructor
 * @param {Guacamole.Tunnel} tunnel The tunnel to use to send and receive
 *                                  Guacamole instructions.
 */
Guacamole.Client = function(tunnel) {

    var guac_client = this;

    var STATE_IDLE          = 0;
    var STATE_CONNECTING    = 1;
    var STATE_WAITING       = 2;
    var STATE_CONNECTED     = 3;
    var STATE_DISCONNECTING = 4;
    var STATE_DISCONNECTED  = 5;

    var currentState = STATE_IDLE;
    
    var currentTimestamp = 0;
    var pingInterval = null;

    var displayWidth = 0;
    var displayHeight = 0;
    var displayScale = 1;

    /**
     * Translation from Guacamole protocol line caps to Layer line caps.
     * @private
     */
    var lineCap = {
        0: "butt",
        1: "round",
        2: "square"
    };

    /**
     * Translation from Guacamole protocol line caps to Layer line caps.
     * @private
     */
    var lineJoin = {
        0: "bevel",
        1: "miter",
        2: "round"
    };

    // Create bounding div 
    var bounds = document.createElement("div");
    bounds.style.position = "relative";
    bounds.style.width = (displayWidth*displayScale) + "px";
    bounds.style.height = (displayHeight*displayScale) + "px";

    // Create display
    var display = document.createElement("div");
    display.style.position = "relative";
    display.style.width = displayWidth + "px";
    display.style.height = displayHeight + "px";

    // Ensure transformations on display originate at 0,0
    display.style.transformOrigin =
    display.style.webkitTransformOrigin =
    display.style.MozTransformOrigin =
    display.style.OTransformOrigin =
    display.style.msTransformOrigin =
        "0 0";

    // Create default layer
    var default_layer_container = new Guacamole.Client.LayerContainer(0, displayWidth, displayHeight);

    // Position default layer
    var default_layer_container_element = default_layer_container.getElement();
    default_layer_container_element.style.position = "absolute";
    default_layer_container_element.style.left = "0px";
    default_layer_container_element.style.top  = "0px";
    default_layer_container_element.style.overflow = "hidden";
    default_layer_container_element.style.zIndex = "0";

    // Create cursor layer
    var cursor = new Guacamole.Client.LayerContainer(null, 0, 0);
    cursor.getLayer().setChannelMask(Guacamole.Layer.SRC);
    cursor.getLayer().autoflush = true;

    // Position cursor layer
    var cursor_element = cursor.getElement();
    cursor_element.style.position = "absolute";
    cursor_element.style.left = "0px";
    cursor_element.style.top  = "0px";
    cursor_element.style.zIndex = "1";

    // Add default layer and cursor to display
    display.appendChild(default_layer_container.getElement());
    display.appendChild(cursor.getElement());

    // Add display to bounds
    bounds.appendChild(display);

    // Initially, only default layer exists
    var layers =  [default_layer_container];

    // No initial buffers
    var buffers = [];

    // No initial parsers
    var parsers = [];

    // No initial audio channels 
    var audio_channels = [];

    // No initial streams 
    var streams = [];

    // Pool of available stream indices
    var stream_indices = new Guacamole.IntegerPool();

    // Array of allocated output streams by index
    var output_streams = [];

    function setState(state) {
        if (state != currentState) {
            currentState = state;
            if (guac_client.onstatechange)
                guac_client.onstatechange(currentState);
        }
    }

    function isConnected() {
        return currentState == STATE_CONNECTED
            || currentState == STATE_WAITING;
    }

    var cursorHotspotX = 0;
    var cursorHotspotY = 0;

    var cursorX = 0;
    var cursorY = 0;

    function moveCursor(x, y) {

        // Move cursor layer
        cursor.translate(x - cursorHotspotX, y - cursorHotspotY);

        // Update stored position
        cursorX = x;
        cursorY = y;

    }

    /**
     * Returns an element containing the display of this Guacamole.Client.
     * Adding the element returned by this function to an element in the body
     * of a document will cause the client's display to be visible.
     * 
     * @return {Element} An element containing ths display of this
     *                   Guacamole.Client.
     */
    this.getDisplay = function() {
        return bounds;
    };

    /**
     * Sends the current size of the screen.
     * 
     * @param {Number} width The width of the screen.
     * @param {Number} height The height of the screen.
     */
    this.sendSize = function(width, height) {

        // Do not send requests if not connected
        if (!isConnected())
            return;

        tunnel.sendMessage("size", width, height);

    };

    /**
     * Sends a key event having the given properties as if the user
     * pressed or released a key.
     * 
     * @param {Boolean} pressed Whether the key is pressed (true) or released
     *                          (false).
     * @param {Number} keysym The keysym of the key being pressed or released.
     */
    this.sendKeyEvent = function(pressed, keysym) {
        // Do not send requests if not connected
        if (!isConnected())
            return;

        tunnel.sendMessage("key", keysym, pressed);
    };

    /**
     * Sends a mouse event having the properties provided by the given mouse
     * state.
     * 
     * @param {Guacamole.Mouse.State} mouseState The state of the mouse to send
     *                                           in the mouse event.
     */
    this.sendMouseState = function(mouseState) {

        // Do not send requests if not connected
        if (!isConnected())
            return;

        // Update client-side cursor
        moveCursor(
            Math.floor(mouseState.x),
            Math.floor(mouseState.y)
        );

        // Build mask
        var buttonMask = 0;
        if (mouseState.left)   buttonMask |= 1;
        if (mouseState.middle) buttonMask |= 2;
        if (mouseState.right)  buttonMask |= 4;
        if (mouseState.up)     buttonMask |= 8;
        if (mouseState.down)   buttonMask |= 16;

        // Send message
        tunnel.sendMessage("mouse", Math.floor(mouseState.x), Math.floor(mouseState.y), buttonMask);
    };

    /**
     * Sets the clipboard of the remote client to the given text data.
     *
     * @deprecated Use createClipboardStream() instead. 
     * @param {String} data The data to send as the clipboard contents.
     */
    this.setClipboard = function(data) {

        // Do not send requests if not connected
        if (!isConnected())
            return;

        // Open stream
        var stream = guac_client.createClipboardStream("text/plain");
        var writer = new Guacamole.StringWriter(stream);

        // Send text chunks
        for (var i=0; i<data.length; i += 4096)
            writer.sendText(data.substring(i, i+4096));

        // Close stream
        writer.sendEnd();

    };

    /**
     * Opens a new file for writing, having the given index, mimetype and
     * filename.
     * 
     * @param {String} mimetype The mimetype of the file being sent.
     * @param {String} filename The filename of the file being sent.
     * @return {Guacamole.OutputStream} The created file stream.
     */
    this.createFileStream = function(mimetype, filename) {

        // Allocate index
        var index = stream_indices.next();

        // Create new stream
        tunnel.sendMessage("file", index, mimetype, filename);
        var stream = output_streams[index] = new Guacamole.OutputStream(guac_client, index);

        // Override sendEnd() of stream to automatically free index
        var old_end = stream.sendEnd;
        stream.sendEnd = function() {
            old_end();
            stream_indices.free(index);
            delete output_streams[index];
        };

        // Return new, overridden stream
        return stream;

    };

    /**
     * Opens a new pipe for writing, having the given name and mimetype. 
     * 
     * @param {String} mimetype The mimetype of the data being sent.
     * @param {String} name The name of the pipe.
     * @return {Guacamole.OutputStream} The created file stream.
     */
    this.createPipeStream = function(mimetype, name) {

        // Allocate index
        var index = stream_indices.next();

        // Create new stream
        tunnel.sendMessage("pipe", index, mimetype, name);
        var stream = output_streams[index] = new Guacamole.OutputStream(guac_client, index);

        // Override sendEnd() of stream to automatically free index
        var old_end = stream.sendEnd;
        stream.sendEnd = function() {
            old_end();
            stream_indices.free(index);
            delete output_streams[index];
        };

        // Return new, overridden stream
        return stream;

    };

    /**
     * Opens a new clipboard object for writing, having the given mimetype.
     * 
     * @param {String} mimetype The mimetype of the data being sent.
     * @param {String} name The name of the pipe.
     * @return {Guacamole.OutputStream} The created file stream.
     */
    this.createClipboardStream = function(mimetype) {

        // Allocate index
        var index = stream_indices.next();

        // Create new stream
        tunnel.sendMessage("clipboard", index, mimetype);
        var stream = output_streams[index] = new Guacamole.OutputStream(guac_client, index);

        // Override sendEnd() of stream to automatically free index
        var old_end = stream.sendEnd;
        stream.sendEnd = function() {
            old_end();
            stream_indices.free(index);
            delete output_streams[index];
        };

        // Return new, overridden stream
        return stream;

    };

    /**
     * Acknowledge receipt of a blob on the stream with the given index.
     * 
     * @param {Number} index The index of the stream associated with the
     *                       received blob.
     * @param {String} message A human-readable message describing the error
     *                         or status.
     * @param {Number} code The error code, if any, or 0 for success.
     */
    this.sendAck = function(index, message, code) {

        // Do not send requests if not connected
        if (!isConnected())
            return;

        tunnel.sendMessage("ack", index, message, code);
    };

    /**
     * Given the index of a file, writes a blob of data to that file.
     * 
     * @param {Number} index The index of the file to write to.
     * @param {String} data Base64-encoded data to write to the file.
     */
    this.sendBlob = function(index, data) {

        // Do not send requests if not connected
        if (!isConnected())
            return;

        tunnel.sendMessage("blob", index, data);
    };

    /**
     * Marks a currently-open stream as complete.
     * 
     * @param {Number} index The index of the stream to end.
     */
    this.endStream = function(index) {

        // Do not send requests if not connected
        if (!isConnected())
            return;

        tunnel.sendMessage("end", index);
    };

    /**
     * Fired whenever the state of this Guacamole.Client changes.
     * 
     * @event
     * @param {Number} state The new state of the client.
     */
    this.onstatechange = null;

    /**
     * Fired when the remote client sends a name update.
     * 
     * @event
     * @param {String} name The new name of this client.
     */
    this.onname = null;

    /**
     * Fired when an error is reported by the remote client, and the connection
     * is being closed.
     * 
     * @event
     * @param {Guacamole.Status} status A status object which describes the
     *                                  error.
     */
    this.onerror = null;

    /**
     * Fired when the clipboard of the remote client is changing.
     * 
     * @event
     * @param {Guacamole.InputStream} stream The stream that will receive
     *                                       clipboard data from the server.
     * @param {String} mimetype The mimetype of the data which will be received.
     */
    this.onclipboard = null;

    /**
     * Fired when the default layer (and thus the entire Guacamole display)
     * is resized.
     * 
     * @event
     * @param {Number} width The new width of the Guacamole display.
     * @param {Number} height The new height of the Guacamole display.
     */
    this.onresize = null;

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
    this.onfile = null;

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
    this.onpipe = null;

    /**
     * Fired whenever a sync instruction is received from the server, indicating
     * that the server is finished processing any input from the client and
     * has sent any results.
     * 
     * @event
     * @param {Number} timestamp The timestamp associated with the sync
     *                           instruction.
     */
    this.onsync = null;

    // Layers
    function getBufferLayer(index) {

        index = -1 - index;
        var buffer = buffers[index];

        // Create buffer if necessary
        if (buffer == null) {
            buffer = new Guacamole.Layer(0, 0);
            buffer.autoflush = 1;
            buffer.autosize = 1;
            buffers[index] = buffer;
        }

        return buffer;

    }

    function getLayerContainer(index) {

        var layer = layers[index];
        if (layer == null) {

            // Add new layer
            layer = new Guacamole.Client.LayerContainer(index, displayWidth, displayHeight);
            layers[index] = layer;

            // Get and position layer
            var layer_element = layer.getElement();
            layer_element.style.position = "absolute";
            layer_element.style.left = "0px";
            layer_element.style.top = "0px";
            layer_element.style.overflow = "hidden";

            // Add to default layer container
            layer.move(default_layer_container, 0, 0, 0);

        }

        return layer;

    }

    function getLayer(index) {
       
        // If buffer, just get layer
        if (index < 0)
            return getBufferLayer(index);

        // Otherwise, retrieve layer from layer container
        return getLayerContainer(index).getLayer();

    }

    function getParser(index) {

        var parser = parsers[index];

        // If parser not yet created, create it, and tie to the
        // oninstruction handler of the tunnel.
        if (parser == null) {
            parser = parsers[index] = new Guacamole.Parser();
            parser.oninstruction = tunnel.oninstruction;
        }

        return parser;

    }

    function getAudioChannel(index) {

        var audio_channel = audio_channels[index];

        // If audio channel not yet created, create it
        if (audio_channel == null)
            audio_channel = audio_channels[index] = new Guacamole.AudioChannel();

        return audio_channel;

    }

    /**
     * Handlers for all defined layer properties.
     * @private
     */
    var layerPropertyHandlers = {

        "miter-limit": function(layer, value) {
            layer.setMiterLimit(parseFloat(value));
        }

    };
    
    /**
     * Handlers for all instruction opcodes receivable by a Guacamole protocol
     * client.
     * @private
     */
    var instructionHandlers = {

        "ack": function(parameters) {

            var stream_index = parseInt(parameters[0]);
            var reason = parameters[1];
            var code = parameters[2];

            // Get stream
            var stream = output_streams[stream_index];
            if (stream) {

                // Signal ack if handler defined
                if (stream.onack)
                    stream.onack(new Guacamole.Status(code, reason));

                // If code is an error, invalidate stream
                if (code >= 0x0100) {
                    stream_indices.free(stream_index);
                    delete output_streams[stream_index];
                }

            }

        },

        "arc": function(parameters) {

            var layer = getLayer(parseInt(parameters[0]));
            var x = parseInt(parameters[1]);
            var y = parseInt(parameters[2]);
            var radius = parseInt(parameters[3]);
            var startAngle = parseFloat(parameters[4]);
            var endAngle = parseFloat(parameters[5]);
            var negative = parseInt(parameters[6]);

            layer.arc(x, y, radius, startAngle, endAngle, negative != 0);

        },

        "audio": function(parameters) {

            var stream_index = parseInt(parameters[0]);
            var channel = getAudioChannel(parseInt(parameters[1]));
            var mimetype = parameters[2];
            var duration = parseFloat(parameters[3]);

            // Create stream 
            var stream = streams[stream_index] =
                    new Guacamole.InputStream(guac_client, stream_index);

            // Assemble entire stream as a blob
            var blob_reader = new Guacamole.BlobReader(stream, mimetype);

            // Play blob as audio
            blob_reader.onend = function() {
                channel.play(mimetype, duration, blob_reader.getBlob());
            };

            // Send success response
            guac_client.sendAck(stream_index, "OK", 0x0000);

        },

        "blob": function(parameters) {

            // Get stream 
            var stream_index = parseInt(parameters[0]);
            var data = parameters[1];
            var stream = streams[stream_index];

            // Write data
            stream.onblob(data);

        },

        "cfill": function(parameters) {

            var channelMask = parseInt(parameters[0]);
            var layer = getLayer(parseInt(parameters[1]));
            var r = parseInt(parameters[2]);
            var g = parseInt(parameters[3]);
            var b = parseInt(parameters[4]);
            var a = parseInt(parameters[5]);

            layer.setChannelMask(channelMask);

            layer.fillColor(r, g, b, a);

        },

        "clip": function(parameters) {

            var layer = getLayer(parseInt(parameters[0]));

            layer.clip();

        },

        "clipboard": function(parameters) {

            var stream_index = parseInt(parameters[0]);
            var mimetype = parameters[1];

            // Create stream 
            if (guac_client.onclipboard) {
                var stream = streams[stream_index] = new Guacamole.InputStream(guac_client, stream_index);
                guac_client.onclipboard(stream, mimetype);
            }

            // Otherwise, unsupported
            else
                guac_client.sendAck(stream_index, "Clipboard unsupported", 0x0100);

        },

        "close": function(parameters) {

            var layer = getLayer(parseInt(parameters[0]));

            layer.close();

        },

        "copy": function(parameters) {

            var srcL = getLayer(parseInt(parameters[0]));
            var srcX = parseInt(parameters[1]);
            var srcY = parseInt(parameters[2]);
            var srcWidth = parseInt(parameters[3]);
            var srcHeight = parseInt(parameters[4]);
            var channelMask = parseInt(parameters[5]);
            var dstL = getLayer(parseInt(parameters[6]));
            var dstX = parseInt(parameters[7]);
            var dstY = parseInt(parameters[8]);

            dstL.setChannelMask(channelMask);

            dstL.copy(
                srcL,
                srcX,
                srcY,
                srcWidth, 
                srcHeight, 
                dstX,
                dstY 
            );

        },

        "cstroke": function(parameters) {

            var channelMask = parseInt(parameters[0]);
            var layer = getLayer(parseInt(parameters[1]));
            var cap = lineCap[parseInt(parameters[2])];
            var join = lineJoin[parseInt(parameters[3])];
            var thickness = parseInt(parameters[4]);
            var r = parseInt(parameters[5]);
            var g = parseInt(parameters[6]);
            var b = parseInt(parameters[7]);
            var a = parseInt(parameters[8]);

            layer.setChannelMask(channelMask);

            layer.strokeColor(cap, join, thickness, r, g, b, a);

        },

        "cursor": function(parameters) {

            cursorHotspotX = parseInt(parameters[0]);
            cursorHotspotY = parseInt(parameters[1]);
            var srcL = getLayer(parseInt(parameters[2]));
            var srcX = parseInt(parameters[3]);
            var srcY = parseInt(parameters[4]);
            var srcWidth = parseInt(parameters[5]);
            var srcHeight = parseInt(parameters[6]);

            // Reset cursor size
            cursor.resize(srcWidth, srcHeight);

            // Draw cursor to cursor layer
            cursor.getLayer().copy(
                srcL,
                srcX,
                srcY,
                srcWidth, 
                srcHeight, 
                0,
                0 
            );

            // Update cursor position (hotspot may have changed)
            moveCursor(cursorX, cursorY);

        },

        "curve": function(parameters) {

            var layer = getLayer(parseInt(parameters[0]));
            var cp1x = parseInt(parameters[1]);
            var cp1y = parseInt(parameters[2]);
            var cp2x = parseInt(parameters[3]);
            var cp2y = parseInt(parameters[4]);
            var x = parseInt(parameters[5]);
            var y = parseInt(parameters[6]);

            layer.curveTo(cp1x, cp1y, cp2x, cp2y, x, y);

        },

        "dispose": function(parameters) {
            
            var layer_index = parseInt(parameters[0]);

            // If visible layer, remove from parent
            if (layer_index > 0) {

                // Remove from parent
                var layer_container = getLayerContainer(layer_index);
                layer_container.dispose();

                // Delete reference
                delete layers[layer_index];

            }

            // If buffer, just delete reference
            else if (layer_index < 0)
                delete buffers[-1 - layer_index];

            // Attempting to dispose the root layer currently has no effect.

        },

        "distort": function(parameters) {

            var layer_index = parseInt(parameters[0]);
            var a = parseFloat(parameters[1]);
            var b = parseFloat(parameters[2]);
            var c = parseFloat(parameters[3]);
            var d = parseFloat(parameters[4]);
            var e = parseFloat(parameters[5]);
            var f = parseFloat(parameters[6]);

            // Only valid for visible layers (not buffers)
            if (layer_index >= 0) {

                // Set layer transform 
                var layer_container = getLayerContainer(layer_index);
                layer_container.transform(a, b, c, d, e, f);

             }

        },
 
        "error": function(parameters) {

            var reason = parameters[0];
            var code = parameters[1];

            // Call handler if defined
            if (guac_client.onerror)
                guac_client.onerror(new Guacamole.Status(code, reason));

            guac_client.disconnect();

        },

        "end": function(parameters) {

            // Get stream
            var stream_index = parseInt(parameters[0]);
            var stream = streams[stream_index];

            // Signal end of stream
            if (stream.onend)
                stream.onend();

        },

        "file": function(parameters) {

            var stream_index = parseInt(parameters[0]);
            var mimetype = parameters[1];
            var filename = parameters[2];

            // Create stream 
            if (guac_client.onfile) {
                var stream = streams[stream_index] = new Guacamole.InputStream(guac_client, stream_index);
                guac_client.onfile(stream, mimetype, filename);
            }

            // Otherwise, unsupported
            else
                guac_client.sendAck(stream_index, "File transfer unsupported", 0x0100);

        },

        "identity": function(parameters) {

            var layer = getLayer(parseInt(parameters[0]));

            layer.setTransform(1, 0, 0, 1, 0, 0);

        },

        "lfill": function(parameters) {

            var channelMask = parseInt(parameters[0]);
            var layer = getLayer(parseInt(parameters[1]));
            var srcLayer = getLayer(parseInt(parameters[2]));

            layer.setChannelMask(channelMask);

            layer.fillLayer(srcLayer);

        },

        "line": function(parameters) {

            var layer = getLayer(parseInt(parameters[0]));
            var x = parseInt(parameters[1]);
            var y = parseInt(parameters[2]);

            layer.lineTo(x, y);

        },

        "lstroke": function(parameters) {

            var channelMask = parseInt(parameters[0]);
            var layer = getLayer(parseInt(parameters[1]));
            var srcLayer = getLayer(parseInt(parameters[2]));

            layer.setChannelMask(channelMask);

            layer.strokeLayer(srcLayer);

        },

        "move": function(parameters) {
            
            var layer_index = parseInt(parameters[0]);
            var parent_index = parseInt(parameters[1]);
            var x = parseInt(parameters[2]);
            var y = parseInt(parameters[3]);
            var z = parseInt(parameters[4]);

            // Only valid for non-default layers
            if (layer_index > 0 && parent_index >= 0) {

                // Get container element
                var layer_container = getLayerContainer(layer_index);
                var parent = getLayerContainer(parent_index);

                // Move layer
                layer_container.move(parent, x, y, z);

            }

        },

        "name": function(parameters) {
            if (guac_client.onname) guac_client.onname(parameters[0]);
        },

        "nest": function(parameters) {
            var parser = getParser(parseInt(parameters[0]));
            parser.receive(parameters[1]);
        },

        "pipe": function(parameters) {

            var stream_index = parseInt(parameters[0]);
            var mimetype = parameters[1];
            var name = parameters[2];

            // Create stream 
            if (guac_client.onpipe) {
                var stream = streams[stream_index] = new Guacamole.InputStream(guac_client, stream_index);
                guac_client.onpipe(stream, mimetype, name);
            }

            // Otherwise, unsupported
            else
                guac_client.sendAck(stream_index, "Named pipes unsupported", 0x0100);

        },

        "png": function(parameters) {

            var channelMask = parseInt(parameters[0]);
            var layer = getLayer(parseInt(parameters[1]));
            var x = parseInt(parameters[2]);
            var y = parseInt(parameters[3]);
            var data = parameters[4];

            layer.setChannelMask(channelMask);

            layer.draw(
                x,
                y,
                "data:image/png;base64," + data
            );

        },

        "pop": function(parameters) {

            var layer = getLayer(parseInt(parameters[0]));

            layer.pop();

        },

        "push": function(parameters) {

            var layer = getLayer(parseInt(parameters[0]));

            layer.push();

        },
 
        "rect": function(parameters) {

            var layer = getLayer(parseInt(parameters[0]));
            var x = parseInt(parameters[1]);
            var y = parseInt(parameters[2]);
            var w = parseInt(parameters[3]);
            var h = parseInt(parameters[4]);

            layer.rect(x, y, w, h);

        },
        
        "reset": function(parameters) {

            var layer = getLayer(parseInt(parameters[0]));

            layer.reset();

        },
        
        "set": function(parameters) {

            var layer = getLayer(parseInt(parameters[0]));
            var name = parameters[1];
            var value = parameters[2];

            // Call property handler if defined
            var handler = layerPropertyHandlers[name];
            if (handler)
                handler(layer, value);

        },

        "shade": function(parameters) {
            
            var layer_index = parseInt(parameters[0]);
            var a = parseInt(parameters[1]);

            // Only valid for visible layers (not buffers)
            if (layer_index >= 0) {
                var layer_container = getLayerContainer(layer_index);
                layer_container.shade(a);
            }

        },

        "size": function(parameters) {

            var layer_index = parseInt(parameters[0]);
            var width = parseInt(parameters[1]);
            var height = parseInt(parameters[2]);

            // If not buffer, resize layer and container
            if (layer_index >= 0) {

                // Resize layer
                var layer_container = getLayerContainer(layer_index);
                layer_container.resize(width, height);

                // If layer is default, resize display
                if (layer_index == 0) {

                    displayWidth = width;
                    displayHeight = height;

                    // Update (set) display size
                    display.style.width = displayWidth + "px";
                    display.style.height = displayHeight + "px";

                    // Update bounds size
                    bounds.style.width = (displayWidth*displayScale) + "px";
                    bounds.style.height = (displayHeight*displayScale) + "px";

                    // Call resize event handler if defined
                    if (guac_client.onresize)
                        guac_client.onresize(width, height);

                }

            }

            // If buffer, resize layer only
            else {
                var layer = getBufferLayer(parseInt(parameters[0]));
                layer.resize(width, height);
            }

        },
        
        "start": function(parameters) {

            var layer = getLayer(parseInt(parameters[0]));
            var x = parseInt(parameters[1]);
            var y = parseInt(parameters[2]);

            layer.moveTo(x, y);

        },

        "sync": function(parameters) {

            var timestamp = parameters[0];

            // When all layers have finished rendering all instructions
            // UP TO THIS POINT IN TIME, send sync response.

            var layersToSync = 0;
            function syncLayer() {

                layersToSync--;

                // Send sync response when layers are finished
                if (layersToSync == 0) {
                    if (timestamp != currentTimestamp) {
                        tunnel.sendMessage("sync", timestamp);
                        currentTimestamp = timestamp;
                    }
                }

            }

            // Count active, not-ready layers and install sync tracking hooks
            for (var i=0; i<layers.length; i++) {

                var layer_container = layers[i];
                if (layer_container) {

                    var layer = layer_container.getLayer();
                    if (layer) {

                        // Flush layer
                        layer.flush();

                        // If still not ready, sync later
                        if (!layer.isReady()) {
                            layersToSync++;
                            layer.sync(syncLayer);
                        }

                    }

                } // end if layer exists

            } // end for each layer

            // If all layers are ready, then we didn't install any hooks.
            // Send sync message now,
            if (layersToSync == 0) {
                if (timestamp != currentTimestamp) {
                    tunnel.sendMessage("sync", timestamp);
                    currentTimestamp = timestamp;
                }
            }

            // If received first update, no longer waiting.
            if (currentState == STATE_WAITING)
                setState(STATE_CONNECTED);

            // Call sync handler if defined
            if (guac_client.onsync)
                guac_client.onsync(timestamp);

        },

        "transfer": function(parameters) {

            var srcL = getLayer(parseInt(parameters[0]));
            var srcX = parseInt(parameters[1]);
            var srcY = parseInt(parameters[2]);
            var srcWidth = parseInt(parameters[3]);
            var srcHeight = parseInt(parameters[4]);
            var function_index = parseInt(parameters[5]);
            var dstL = getLayer(parseInt(parameters[6]));
            var dstX = parseInt(parameters[7]);
            var dstY = parseInt(parameters[8]);

            /* SRC */
            if (function_index === 0x3)
                dstL.put(
                    srcL,
                    srcX,
                    srcY,
                    srcWidth, 
                    srcHeight, 
                    dstX,
                    dstY
                );

            /* Anything else that isn't a NO-OP */
            else if (function_index !== 0x5)
                dstL.transfer(
                    srcL,
                    srcX,
                    srcY,
                    srcWidth, 
                    srcHeight, 
                    dstX,
                    dstY,
                    Guacamole.Client.DefaultTransferFunction[function_index]
                );

        },

        "transform": function(parameters) {

            var layer = getLayer(parseInt(parameters[0]));
            var a = parseFloat(parameters[1]);
            var b = parseFloat(parameters[2]);
            var c = parseFloat(parameters[3]);
            var d = parseFloat(parameters[4]);
            var e = parseFloat(parameters[5]);
            var f = parseFloat(parameters[6]);

            layer.transform(a, b, c, d, e, f);

        },

        "video": function(parameters) {

            var stream_index = parseInt(parameters[0]);
            var layer = getLayer(parseInt(parameters[1]));
            var mimetype = parameters[2];
            var duration = parseFloat(parameters[3]);

            // Create stream 
            var stream = streams[stream_index] =
                    new Guacamole.InputStream(guac_client, stream_index);

            // Assemble entire stream as a blob
            var blob_reader = new Guacamole.BlobReader(stream, mimetype);

            // Play video once finished 
            blob_reader.onend = function() {

                // Read data from blob from stream
                var reader = new FileReader();
                reader.onload = function() {

                    var binary = "";
                    var bytes = new Uint8Array(reader.result);

                    // Produce binary string from bytes in buffer
                    for (var i=0; i<bytes.byteLength; i++)
                        binary += String.fromCharCode(bytes[i]);

                    // Play video
                    layer.play(mimetype, duration, "data:" + mimetype + ";base64," + window.btoa(binary));

                };
                reader.readAsArrayBuffer(blob_reader.getBlob());

            };

            // Send success response
            tunnel.sendMessage("ack", stream_index, "OK", 0x0000);

        }

    };

    tunnel.oninstruction = function(opcode, parameters) {

        var handler = instructionHandlers[opcode];
        if (handler)
            handler(parameters);

    };

    /**
     * Sends a disconnect instruction to the server and closes the tunnel.
     */
    this.disconnect = function() {

        // Only attempt disconnection not disconnected.
        if (currentState != STATE_DISCONNECTED
                && currentState != STATE_DISCONNECTING) {

            setState(STATE_DISCONNECTING);

            // Stop ping
            if (pingInterval)
                window.clearInterval(pingInterval);

            // Send disconnect message and disconnect
            tunnel.sendMessage("disconnect");
            tunnel.disconnect();
            setState(STATE_DISCONNECTED);

        }

    };
    
    /**
     * Connects the underlying tunnel of this Guacamole.Client, passing the
     * given arbitrary data to the tunnel during the connection process.
     *
     * @param data Arbitrary connection data to be sent to the underlying
     *             tunnel during the connection process.
     * @throws {Guacamole.Status} If an error occurs during connection.
     */
    this.connect = function(data) {

        setState(STATE_CONNECTING);

        try {
            tunnel.connect(data);
        }
        catch (status) {
            setState(STATE_IDLE);
            throw status;
        }

        // Ping every 5 seconds (ensure connection alive)
        pingInterval = window.setInterval(function() {
            tunnel.sendMessage("sync", currentTimestamp);
        }, 5000);

        setState(STATE_WAITING);
    };

    /**
     * Sets the scale of the client display element such that it renders at
     * a relatively smaller or larger size, without affecting the true
     * resolution of the display.
     *
     * @param {Number} scale The scale to resize to, where 1.0 is normal
     *                       size (1:1 scale).
     */
    this.scale = function(scale) {

        display.style.transform =
        display.style.WebkitTransform =
        display.style.MozTransform =
        display.style.OTransform =
        display.style.msTransform =

            "scale(" + scale + "," + scale + ")";

        displayScale = scale;

        // Update bounds size
        bounds.style.width = (displayWidth*displayScale) + "px";
        bounds.style.height = (displayHeight*displayScale) + "px";

    };

    /**
     * Returns the width of the display.
     *
     * @return {Number} The width of the display.
     */
    this.getWidth = function() {
        return displayWidth;
    };

    /**
     * Returns the height of the display.
     *
     * @return {Number} The height of the display.
     */
    this.getHeight = function() {
        return displayHeight;
    };

    /**
     * Returns the scale of the display.
     *
     * @return {Number} The scale of the display.
     */
    this.getScale = function() {
        return displayScale;
    };

    /**
     * Returns a canvas element containing the entire display, with all child
     * layers composited within.
     *
     * @return {HTMLCanvasElement} A new canvas element containing a copy of
     *                             the display.
     */
    this.flatten = function() {
       
        // Get default layer
        var default_layer = getLayerContainer(0);

        // Get destination canvas
        var canvas = document.createElement("canvas");
        canvas.width = default_layer.width;
        canvas.height = default_layer.height;

        var context = canvas.getContext("2d");

        // Returns sorted array of children
        function get_children(layer) {

            // Build array of children
            var children = [];
            for (var index in layer.children)
                children.push(layer.children[index]);

            // Sort
            children.sort(function children_comparator(a, b) {

                // Compare based on Z order
                var diff = a.z - b.z;
                if (diff !== 0)
                    return diff;

                // If Z order identical, use document order
                var a_element = a.getElement();
                var b_element = b.getElement();
                var position = b_element.compareDocumentPosition(a_element);

                if (position & Node.DOCUMENT_POSITION_PRECEDING) return -1;
                if (position & Node.DOCUMENT_POSITION_FOLLOWING) return  1;

                // Otherwise, assume same
                return 0;

            });

            // Done
            return children;

        }

        // Draws the contents of the given layer at the given coordinates
        function draw_layer(layer, x, y) {

            // Draw layer
            if (layer.width > 0 && layer.height > 0) {

                // Save and update alpha
                var initial_alpha = context.globalAlpha;
                context.globalAlpha *= layer.alpha / 255.0;

                // Copy data
                context.drawImage(layer.getLayer().getCanvas(), x, y);

                // Draw all children
                var children = get_children(layer);
                for (var i=0; i<children.length; i++) {
                    var child = children[i];
                    draw_layer(child, x + child.x, y + child.y);
                }

                // Restore alpha
                context.globalAlpha = initial_alpha;

            }

        }

        // Draw default layer and all children
        draw_layer(default_layer, 0, 0);

        // Return new canvas copy
        return canvas;
        
    };

};

/**
 * Simple container for Guacamole.Layer, allowing layers to be easily
 * repositioned and nested. This allows certain operations to be accelerated
 * through DOM manipulation, rather than raster operations.
 * 
 * @constructor
 *
 * @param {Number} index The unique integer which identifies this layer.
 *
 * @param {Number} width The width of the Layer, in pixels. The canvas element
 *                       backing this Layer will be given this width.
 *                       
 * @param {Number} height The height of the Layer, in pixels. The canvas element
 *                        backing this Layer will be given this height.
 */
Guacamole.Client.LayerContainer = function(index, width, height) {

    /**
     * Reference to this LayerContainer.
     * @private
     */
    var layer_container = this;

    /**
     * Unique integer identifying this layer container.
     * @type Number
     */
    this.index = index;

    /**
     * The opacity of the layer container, where 255 is fully opaque and 0 is
     * fully transparent.
     */
    this.alpha = 0xFF;

    /**
     * X coordinate of the upper-left corner of this layer container within
     * its parent, in pixels.
     * @type Number
     */
    this.x = 0;

    /**
     * Y coordinate of the upper-left corner of this layer container within
     * its parent, in pixels.
     * @type Number
     */
    this.y = 0;

    /**
     * Z stacking order of this layer relative to other sibling layers.
     * @type Number
     */
    this.z = 0;

    /**
     * The affine transformation applied to this layer container. Each element
     * corresponds to a value from the transformation matrix, with the first
     * three values being the first row, and the last three values being the
     * second row. There are six values total.
     * 
     * @type Number[]
     */
    this.matrix = [1, 0, 0, 1, 0, 0];

    /**
     * The width of this layer in pixels.
     * @type Number
     */
    this.width = width;

    /**
     * The height of this layer in pixels.
     * @type Number
     */
    this.height = height;

    /**
     * The parent layer container of this layer, if any.
     * @type Guacamole.Client.LayerContainer
     */
    this.parent = null;

    /**
     * Set of all children of this layer, indexed by layer index. This object
     * will have one property per child.
     */
    this.children = {};

    // Create layer with given size
    var layer = new Guacamole.Layer(width, height);

    // Set layer position
    var canvas = layer.getCanvas();
    canvas.style.position = "absolute";
    canvas.style.left = "0px";
    canvas.style.top = "0px";

    // Create div with given size
    var div = document.createElement("div");
    div.appendChild(canvas);
    div.style.width = width + "px";
    div.style.height = height + "px";

    /**
     * Changes the size of this LayerContainer and the contained Layer to the
     * given width and height.
     * 
     * @param {Number} width The new width to assign to this Layer.
     * @param {Number} height The new height to assign to this Layer.
     */
    this.resize = function(width, height) {

        layer_container.width = width;
        layer_container.height = height;

        // Resize layer
        layer.resize(width, height);

        // Resize containing div
        div.style.width = width + "px";
        div.style.height = height + "px";

    };
  
    /**
     * Returns the Layer contained within this LayerContainer.
     * @returns {Guacamole.Layer} The Layer contained within this
     *                            LayerContainer.
     */
    this.getLayer = function() {
        return layer;
    };

    /**
     * Returns the element containing the Layer within this LayerContainer.
     * @returns {Element} The element containing the Layer within this
     *                    LayerContainer.
     */
    this.getElement = function() {
        return div;
    };

    /**
     * The translation component of this LayerContainer's transform.
     * @private
     */
    var translate = "translate(0px, 0px)"; // (0, 0)

    /**
     * The arbitrary matrix component of this LayerContainer's transform.
     * @private
     */
    var matrix = "matrix(1, 0, 0, 1, 0, 0)"; // Identity

    /**
     * Moves the upper-left corner of this LayerContainer to the given X and Y
     * coordinate.
     * 
     * @param {Number} x The X coordinate to move to.
     * @param {Number} y The Y coordinate to move to.
     */
    this.translate = function(x, y) {

        layer_container.x = x;
        layer_container.y = y;

        // Generate translation
        translate = "translate("
                        + x + "px,"
                        + y + "px)";

        // Set layer transform 
        div.style.transform =
        div.style.WebkitTransform =
        div.style.MozTransform =
        div.style.OTransform =
        div.style.msTransform =

            translate + " " + matrix;

    };

    /**
     * Moves the upper-left corner of this LayerContainer to the given X and Y
     * coordinate, sets the Z stacking order, and reparents this LayerContainer
     * to the given LayerContainer.
     * 
     * @param {Guacamole.Client.LayerContainer} parent The parent to set.
     * @param {Number} x The X coordinate to move to.
     * @param {Number} y The Y coordinate to move to.
     * @param {Number} z The Z coordinate to move to.
     */
    this.move = function(parent, x, y, z) {

        // Set parent if necessary
        if (layer_container.parent !== parent) {

            // Maintain relationship
            if (layer_container.parent)
                delete layer_container.parent.children[layer_container.index];
            layer_container.parent = parent;
            parent.children[layer_container.index] = layer_container;

            // Reparent element
            var parent_element = parent.getElement();
            parent_element.appendChild(div);

        }

        // Set location
        layer_container.translate(x, y);
        layer_container.z = z;
        div.style.zIndex = z;

    };

    /**
     * Sets the opacity of this layer to the given value, where 255 is fully
     * opaque and 0 is fully transparent.
     * 
     * @param {Number} a The opacity to set.
     */
    this.shade = function(a) {
        layer_container.alpha = a;
        div.style.opacity = a/255.0;
    };

    /**
     * Removes this layer container entirely, such that it is no longer
     * contained within its parent layer, if any.
     */
    this.dispose = function() {

        // Remove from parent container
        if (layer_container.parent) {
            delete layer_container.parent.children[layer_container.index];
            layer_container.parent = null;
        }

        // Remove from parent element
        if (div.parentNode)
            div.parentNode.removeChild(div);
        
    };

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
    this.transform = function(a, b, c, d, e, f) {

        // Store matrix
        layer_container.matrix = [a, b, c, d, e, f];

        // Generate matrix transformation
        matrix =

            /* a c e
             * b d f
             * 0 0 1
             */
    
            "matrix(" + a + "," + b + "," + c + "," + d + "," + e + "," + f + ")";

        // Set layer transform 
        div.style.transform =
        div.style.WebkitTransform =
        div.style.MozTransform =
        div.style.OTransform =
        div.style.msTransform =

            translate + " " + matrix;

    };

};

/**
 * Map of all Guacamole binary raster operations to transfer functions.
 * @private
 */
Guacamole.Client.DefaultTransferFunction = {

    /* BLACK */
    0x0: function (src, dst) {
        dst.red = dst.green = dst.blue = 0x00;
    },

    /* WHITE */
    0xF: function (src, dst) {
        dst.red = dst.green = dst.blue = 0xFF;
    },

    /* SRC */
    0x3: function (src, dst) {
        dst.red   = src.red;
        dst.green = src.green;
        dst.blue  = src.blue;
        dst.alpha = src.alpha;
    },

    /* DEST (no-op) */
    0x5: function (src, dst) {
        // Do nothing
    },

    /* Invert SRC */
    0xC: function (src, dst) {
        dst.red   = 0xFF & ~src.red;
        dst.green = 0xFF & ~src.green;
        dst.blue  = 0xFF & ~src.blue;
        dst.alpha =  src.alpha;
    },
    
    /* Invert DEST */
    0xA: function (src, dst) {
        dst.red   = 0xFF & ~dst.red;
        dst.green = 0xFF & ~dst.green;
        dst.blue  = 0xFF & ~dst.blue;
    },

    /* AND */
    0x1: function (src, dst) {
        dst.red   =  ( src.red   &  dst.red);
        dst.green =  ( src.green &  dst.green);
        dst.blue  =  ( src.blue  &  dst.blue);
    },

    /* NAND */
    0xE: function (src, dst) {
        dst.red   = 0xFF & ~( src.red   &  dst.red);
        dst.green = 0xFF & ~( src.green &  dst.green);
        dst.blue  = 0xFF & ~( src.blue  &  dst.blue);
    },

    /* OR */
    0x7: function (src, dst) {
        dst.red   =  ( src.red   |  dst.red);
        dst.green =  ( src.green |  dst.green);
        dst.blue  =  ( src.blue  |  dst.blue);
    },

    /* NOR */
    0x8: function (src, dst) {
        dst.red   = 0xFF & ~( src.red   |  dst.red);
        dst.green = 0xFF & ~( src.green |  dst.green);
        dst.blue  = 0xFF & ~( src.blue  |  dst.blue);
    },

    /* XOR */
    0x6: function (src, dst) {
        dst.red   =  ( src.red   ^  dst.red);
        dst.green =  ( src.green ^  dst.green);
        dst.blue  =  ( src.blue  ^  dst.blue);
    },

    /* XNOR */
    0x9: function (src, dst) {
        dst.red   = 0xFF & ~( src.red   ^  dst.red);
        dst.green = 0xFF & ~( src.green ^  dst.green);
        dst.blue  = 0xFF & ~( src.blue  ^  dst.blue);
    },

    /* AND inverted source */
    0x4: function (src, dst) {
        dst.red   =  0xFF & (~src.red   &  dst.red);
        dst.green =  0xFF & (~src.green &  dst.green);
        dst.blue  =  0xFF & (~src.blue  &  dst.blue);
    },

    /* OR inverted source */
    0xD: function (src, dst) {
        dst.red   =  0xFF & (~src.red   |  dst.red);
        dst.green =  0xFF & (~src.green |  dst.green);
        dst.blue  =  0xFF & (~src.blue  |  dst.blue);
    },

    /* AND inverted destination */
    0x2: function (src, dst) {
        dst.red   =  0xFF & ( src.red   & ~dst.red);
        dst.green =  0xFF & ( src.green & ~dst.green);
        dst.blue  =  0xFF & ( src.blue  & ~dst.blue);
    },

    /* OR inverted destination */
    0xB: function (src, dst) {
        dst.red   =  0xFF & ( src.red   | ~dst.red);
        dst.green =  0xFF & ( src.green | ~dst.green);
        dst.blue  =  0xFF & ( src.blue  | ~dst.blue);
    }

};
