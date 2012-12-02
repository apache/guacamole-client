
/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is guacamole-common-js.
 *
 * The Initial Developer of the Original Code is
 * Michael Jumper.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Matt Hortman
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

/**
 * Namespace for all Guacamole JavaScript objects.
 * @namespace
 */
var Guacamole = Guacamole || {};

/**
 * Simple Guacamole protocol parser that invokes an oninstruction event when
 * full instructions are available from data received via receive().
 * 
 * @constructor
 */
Guacamole.Parser = function() {

    /**
     * Reference to this parser.
     * @private
     */
    var parser = this;

    /**
     * Current buffer of received data. This buffer grows until a full
     * element is available. After a full element is available, that element
     * is flushed into the element buffer.
     * 
     * @private
     */
    var buffer = "";

    /**
     * Buffer of all received, complete elements. After an entire instruction
     * is read, this buffer is flushed, and a new instruction begins.
     * 
     * @private
     */
    var element_buffer = [];

    // The location of the last element's terminator
    var element_end = -1;

    // Where to start the next length search or the next element
    var start_index = 0;

    /**
     * Appends the given instruction data packet to the internal buffer of
     * this Guacamole.Parser, executing all completed instructions at
     * the beginning of this buffer, if any.
     *
     * @param {String} packet The instruction data to append.
     */
    this.receive = function(packet) {

        // Truncate buffer as necessary
        if (start_index > 4096 && element_end >= start_index) {

            buffer = buffer.substring(start_index);

            // Reset parse relative to truncation
            element_end -= start_index;
            start_index = 0;

        }

        // Append data to buffer
        buffer += packet;

        // While search is within currently received data
        while (element_end < buffer.length) {

            // If we are waiting for element data
            if (element_end >= start_index) {

                // We now have enough data for the element. Parse.
                var element = buffer.substring(start_index, element_end);
                var terminator = buffer.substring(element_end, element_end+1);

                // Add element to array
                element_buffer.push(element);

                // If last element, handle instruction
                if (terminator == ";") {

                    // Get opcode
                    var opcode = element_buffer.shift();

                    // Call instruction handler.
                    if (parser.oninstruction != null)
                        parser.oninstruction(opcode, element_buffer);

                    // Clear elements
                    element_buffer.length = 0;

                }
                else if (terminator != ',')
                    throw new Error("Illegal terminator.");

                // Start searching for length at character after
                // element terminator
                start_index = element_end + 1;

            }

            // Search for end of length
            var length_end = buffer.indexOf(".", start_index);
            if (length_end != -1) {

                // Parse length
                var length = parseInt(buffer.substring(element_end+1, length_end));
                if (length == NaN)
                    throw new Error("Non-numeric character in element length.");

                // Calculate start of element
                start_index = length_end + 1;

                // Calculate location of element terminator
                element_end = start_index + length;

            }
            
            // If no period yet, continue search when more data
            // is received
            else {
                start_index = buffer.length;
                break;
            }

        } // end parse loop

    };

    /**
     * Fired once for every complete Guacamole instruction received, in order.
     * 
     * @event
     * @param {String} opcode The Guacamole instruction opcode.
     * @param {Array} parameters The parameters provided for the instruction,
     *                           if any.
     */
    this.oninstruction = null;

};


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
    var default_layer_container = new Guacamole.Client.LayerContainer(displayWidth, displayHeight);

    // Position default layer
    var default_layer_container_element = default_layer_container.getElement();
    default_layer_container_element.style.position = "absolute";
    default_layer_container_element.style.left = "0px";
    default_layer_container_element.style.top  = "0px";
    default_layer_container_element.style.overflow = "hidden";

    // Create cursor layer
    var cursor = new Guacamole.Client.LayerContainer(0, 0);
    cursor.getLayer().setChannelMask(Guacamole.Layer.SRC);

    // Position cursor layer
    var cursor_element = cursor.getElement();
    cursor_element.style.position = "absolute";
    cursor_element.style.left = "0px";
    cursor_element.style.top  = "0px";

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

    tunnel.onerror = function(message) {
        if (guac_client.onerror)
            guac_client.onerror(message);
    };

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
     * @param {String} data The data to send as the clipboard contents.
     */
    this.setClipboard = function(data) {

        // Do not send requests if not connected
        if (!isConnected())
            return;

        tunnel.sendMessage("clipboard", data);
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
     * @param {String} error A human-readable description of the error.
     */
    this.onerror = null;

    /**
     * Fired when the clipboard of the remote client is changing.
     * 
     * @event
     * @param {String} data The new text data of the remote clipboard.
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
     * Fired when a file is received. Note that this will contain the entire
     * data of the file.
     * 
     * @event
     * @param {String} name A human-readable name describing the file.
     * @param {String} mimetype The mimetype of the file received.
     * @param {String} data The actual entire contents of the file,
     *                      base64-encoded.
     */
    this.onfile = null;

    // Layers
    function getBufferLayer(index) {

        index = -1 - index;
        var buffer = buffers[index];

        // Create buffer if necessary
        if (buffer == null) {
            buffer = new Guacamole.Layer(0, 0);
            buffer.autosize = 1;
            buffers[index] = buffer;
        }

        return buffer;

    }

    function getLayerContainer(index) {

        var layer = layers[index];
        if (layer == null) {

            // Add new layer
            layer = new Guacamole.Client.LayerContainer(displayWidth, displayHeight);
            layers[index] = layer;

            // Get and position layer
            var layer_element = layer.getElement();
            layer_element.style.position = "absolute";
            layer_element.style.left = "0px";
            layer_element.style.top = "0px";
            layer_element.style.overflow = "hidden";

            // Add to default layer container
            default_layer_container.getElement().appendChild(layer_element);

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

            var channel = getAudioChannel(parseInt(parameters[0]));
            var mimetype = parameters[1];
            var duration = parseFloat(parameters[2]);
            var data = parameters[3];

            channel.play(mimetype, duration, data);

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
            if (guac_client.onclipboard) guac_client.onclipboard(parameters[0]);
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

                // Get container element
                var layer_container = getLayerContainer(layer_index).getElement();

                // Remove from parent
                layer_container.parentNode.removeChild(layer_container);

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

                // Get container element
                var layer_container = getLayerContainer(layer_index).getElement();

                // Set layer transform 
                layer_container.transform(a, b, c, d, e, f);

             }

        },
 
        "error": function(parameters) {
            if (guac_client.onerror) guac_client.onerror(parameters[0]);
            guac_client.disconnect();
        },

        "file": function(parameters) {
            if (guac_client.onfile)
                guac_client.onfile(
                    parameters[0], // Name
                    parameters[1], // Mimetype
                    parameters[2]  // Data
                );
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
                var layer_container_element = layer_container.getElement();
                var parent = getLayerContainer(parent_index).getElement();

                // Set parent if necessary
                if (!(layer_container_element.parentNode === parent))
                    parent.appendChild(layer_container_element);

                // Move layer
                layer_container.translate(x, y);
                layer_container_element.style.zIndex = z;

            }

        },

        "name": function(parameters) {
            if (guac_client.onname) guac_client.onname(parameters[0]);
        },

        "nest": function(parameters) {
            var parser = getParser(parseInt(parameters[0]));
            parser.receive(parameters[1]);
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

                // Get container element
                var layer_container = getLayerContainer(layer_index).getElement();

                // Set layer opacity
                layer_container.style.opacity = a/255.0;

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

                var layer = layers[i].getLayer();
                if (layer && !layer.isReady()) {
                    layersToSync++;
                    layer.sync(syncLayer);
                }

            }

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

        },

        "transfer": function(parameters) {

            var srcL = getLayer(parseInt(parameters[0]));
            var srcX = parseInt(parameters[1]);
            var srcY = parseInt(parameters[2]);
            var srcWidth = parseInt(parameters[3]);
            var srcHeight = parseInt(parameters[4]);
            var transferFunction = Guacamole.Client.DefaultTransferFunction[parameters[5]];
            var dstL = getLayer(parseInt(parameters[6]));
            var dstX = parseInt(parameters[7]);
            var dstY = parseInt(parameters[8]);

            dstL.transfer(
                srcL,
                srcX,
                srcY,
                srcWidth, 
                srcHeight, 
                dstX,
                dstY,
                transferFunction
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

            var layer = getLayer(parseInt(parameters[0]));
            var mimetype = parameters[1];
            var duration = parseFloat(parameters[2]);
            var data = parameters[3];

            layer.play(mimetype, duration, "data:" + mimetype + ";base64," + data);

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
     */
    this.connect = function(data) {

        setState(STATE_CONNECTING);

        try {
            tunnel.connect(data);
        }
        catch (e) {
            setState(STATE_IDLE);
            throw e;
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
       
        // Get source and destination canvases
        var source = getLayer(0).getCanvas();
        var canvas = document.createElement("canvas");

        // Set dimensions
        canvas.width = source.width;
        canvas.height = source.height;

        // Copy image from source
        var context = canvas.getContext("2d");
        context.drawImage(source, 0, 0);
        
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
 * @param {Number} width The width of the Layer, in pixels. The canvas element
 *                       backing this Layer will be given this width.
 *                       
 * @param {Number} height The height of the Layer, in pixels. The canvas element
 *                        backing this Layer will be given this height.
 */
Guacamole.Client.LayerContainer = function(width, height) {

    /**
     * Reference to this LayerContainer.
     * @private
     */
    var layer_container = this;

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
