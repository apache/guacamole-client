
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

// Guacamole namespace
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

    // Create display
    var display = document.createElement("div");
    display.style.position = "relative";
    display.style.width = displayWidth + "px";
    display.style.height = displayHeight + "px";

    // Create default layer
    var default_layer_container = new Guacamole.Client.LayerContainer(displayWidth, displayHeight);

    // Position default layer
    var default_layer_container_element = default_layer_container.getElement();
    default_layer_container_element.style.position = "absolute";
    default_layer_container_element.style.left = "0px";
    default_layer_container_element.style.top  = "0px";

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

    // Initially, only default layer exists
    var layers =  [default_layer_container];

    // No initial buffers
    var buffers = [];

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

        var element = cursor.getElement();

        // Update rect
        element.style.left = (x - cursorHotspotX) + "px";
        element.style.top  = (y - cursorHotspotY) + "px";

        // Update stored position
        cursorX = x;
        cursorY = y;

    }

    guac_client.getDisplay = function() {
        return display;
    };

    guac_client.sendKeyEvent = function(pressed, keysym) {
        // Do not send requests if not connected
        if (!isConnected())
            return;

        tunnel.sendMessage("key", keysym, pressed);
    };

    guac_client.sendMouseState = function(mouseState) {

        // Do not send requests if not connected
        if (!isConnected())
            return;

        // Update client-side cursor
        moveCursor(
            mouseState.x,
            mouseState.y
        );

        // Build mask
        var buttonMask = 0;
        if (mouseState.left)   buttonMask |= 1;
        if (mouseState.middle) buttonMask |= 2;
        if (mouseState.right)  buttonMask |= 4;
        if (mouseState.up)     buttonMask |= 8;
        if (mouseState.down)   buttonMask |= 16;

        // Send message
        tunnel.sendMessage("mouse", mouseState.x, mouseState.y, buttonMask);
    };

    guac_client.setClipboard = function(data) {

        // Do not send requests if not connected
        if (!isConnected())
            return;

        tunnel.sendMessage("clipboard", data);
    };

    // Handlers
    guac_client.onstatechange = null;
    guac_client.onname = null;
    guac_client.onerror = null;
    guac_client.onclipboard = null;

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

    var instructionHandlers = {

        "error": function(parameters) {
            if (guac_client.onerror) guac_client.onerror(parameters[0]);
            guac_client.disconnect();
        },

        "name": function(parameters) {
            if (guac_client.onname) guac_client.onname(parameters[0]);
        },

        "clipboard": function(parameters) {
            if (guac_client.onclipboard) guac_client.onclipboard(parameters[0]);
        },

        "size": function(parameters) {

            var layer_index = parseInt(parameters[0]);
            var width = parseInt(parameters[1]);
            var height = parseInt(parameters[2]);

            // Only valid for layers (buffers auto-resize)
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

                }

            } // end if layer (not buffer)

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
                var layer_container = getLayerContainer(layer_index).getElement();
                var parent = getLayerContainer(parent_index).getElement();

                // Set parent if necessary
                if (!(layer_container.parentNode === parent))
                    parent.appendChild(layer_container);

                // Move layer
                layer_container.style.left   = x + "px";
                layer_container.style.top    = y + "px";
                layer_container.style.zIndex = z;

            }

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

            // If received first update, no longer waiting.
            if (currentState == STATE_WAITING)
                setState(STATE_CONNECTED);

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

        "rect": function(parameters) {

            var layer = getLayer(parseInt(parameters[0]));
            var x = parseInt(parameters[1]);
            var y = parseInt(parameters[2]);
            var w = parseInt(parameters[3]);
            var h = parseInt(parameters[4]);

            layer.rect(x, y, w, h);

        },

        "clip": function(parameters) {

            var layer = getLayer(parseInt(parameters[0]));

            layer.clip();

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
            cursor.getLayer().copyRect(
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

        }
      
    };


    tunnel.oninstruction = function(opcode, parameters) {

        var handler = instructionHandlers[opcode];
        if (handler)
            handler(parameters);

    };


    guac_client.disconnect = function() {

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
    
    guac_client.connect = function(data) {

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
    layer_container.resize = function(width, height) {

        // Resize layer
        layer.resize(width, height);

        // Resize containing div
        div.style.width = width + "px";
        div.style.height = height + "px";

    };
  
    /**
     * Returns the Layer contained within this LayerContainer.
     * @returns {Guacamole.Layer} The Layer contained within this LayerContainer.
     */
    layer_container.getLayer = function() {
        return layer;
    };

    /**
     * Returns the element containing the Layer within this LayerContainer.
     * @returns {Element} The element containing the Layer within this LayerContainer.
     */
    layer_container.getElement = function() {
        return div;
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
