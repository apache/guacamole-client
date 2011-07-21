
/*
 *  Guacamole - Clientless Remote Desktop
 *  Copyright (C) 2010  Michael Jumper
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 */

// Guacamole namespace
var Guacamole = Guacamole || {};

/**
 * Guacamole protocol client. Given a display element and {@link Guacamole.Tunnel},
 * automatically handles incoming and outgoing Guacamole instructions via the
 * provided tunnel, updating the display using one or more canvas elements.
 * 
 * @constructor
 * @param {Element} display The display element to add canvas elements to.
 * @param {Guacamole.Tunnel} tunnel The tunnel to use to send and receive
 *                                  Guacamole instructions.
 */
Guacamole.Client = function(display, tunnel) {

    var guac_client = this;

    var STATE_IDLE          = 0;
    var STATE_CONNECTING    = 1;
    var STATE_WAITING       = 2;
    var STATE_CONNECTED     = 3;
    var STATE_DISCONNECTING = 4;
    var STATE_DISCONNECTED  = 5;

    var currentState = STATE_IDLE;

    tunnel.oninstruction = doInstruction;

    tunnel.onerror = function(message) {
        if (guac_client.onerror)
            guac_client.onerror(message);
    };

    // Display must be relatively positioned for mouse to be handled properly
    display.style.position = "relative";

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

    var cursorImage = null;
    var cursorHotspotX = 0;
    var cursorHotspotY = 0;

    var cursorRectX = 0;
    var cursorRectY = 0;
    var cursorRectW = 0;
    var cursorRectH = 0;

    var cursorHidden = 0;

    function redrawCursor(x, y) {

        // Hide hardware cursor
        if (cursorHidden == 0) {
            display.className += " guac-hide-cursor";
            cursorHidden = 1;
        }

        // Erase old cursor
        cursor.clearRect(cursorRectX, cursorRectY, cursorRectW, cursorRectH);

        // Update rect
        cursorRectX = x - cursorHotspotX;
        cursorRectY = y - cursorHotspotY;
        cursorRectW = cursorImage.width;
        cursorRectH = cursorImage.height;

        // Draw new cursor
        cursor.drawImage(cursorRectX, cursorRectY, cursorImage);
    }

    guac_client.sendKeyEvent = function(pressed, keysym) {
        // Do not send requests if not connected
        if (!isConnected())
            return;

        tunnel.sendMessage("key:" +  keysym + "," + pressed + ";");
    };

    guac_client.sendMouseState = function(mouseState) {

        // Do not send requests if not connected
        if (!isConnected())
            return;

        // Draw client-side cursor
        if (cursorImage != null) {
            redrawCursor(
                mouseState.x,
                mouseState.y
            );
        }

        // Build mask
        var buttonMask = 0;
        if (mouseState.left)   buttonMask |= 1;
        if (mouseState.middle) buttonMask |= 2;
        if (mouseState.right)  buttonMask |= 4;
        if (mouseState.up)     buttonMask |= 8;
        if (mouseState.down)   buttonMask |= 16;

        // Send message
        tunnel.sendMessage("mouse:" + mouseState.x + "," + mouseState.y + "," + buttonMask + ";");
    };

    guac_client.setClipboard = function(data) {

        // Do not send requests if not connected
        if (!isConnected())
            return;

        tunnel.sendMessage("clipboard:" + escapeGuacamoleString(data) + ";");
    };

    // Handlers
    guac_client.onstatechange = null;
    guac_client.onname = null;
    guac_client.onerror = null;
    guac_client.onclipboard = null;

    // Layers
    var displayWidth = 0;
    var displayHeight = 0;

    var layers = new Array();
    var buffers = new Array();
    var cursor = null;

    guac_client.getLayers = function() {
        return layers;
    };

    function getLayer(index) {

        // If negative index, use buffer
        if (index < 0) {

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

        // If non-negative, use visible layer
        else {

            var layer = layers[index];
            if (layer == null) {

                // Add new layer
                layer = new Guacamole.Layer(displayWidth, displayHeight);
                
                // Set layer position
                var canvas = layer.getCanvas();
                canvas.style.position = "absolute";
                canvas.style.left = "0px";
                canvas.style.top = "0px";

                layers[index] = layer;

                // (Re)-add existing layers in order
                for (var i=0; i<layers.length; i++) {
                    if (layers[i]) {

                        // If already present, remove
                        if (layers[i].parentNode === display)
                            display.removeChild(layers[i].getCanvas());

                        // Add to end
                        display.appendChild(layers[i].getCanvas());
                    }
                }

                // Add cursor layer last
                if (cursor != null) {
                    if (cursor.parentNode === display)
                        display.removeChild(cursor.getCanvas());
                    display.appendChild(cursor.getCanvas());
                }

            }
            else {
                // Reset size
                layer.resize(displayWidth, displayHeight);
            }

            return layer;
        }

    }

    var instructionHandlers = {

        "error": function(parameters) {
            if (guac_client.onerror) guac_client.onerror(unescapeGuacamoleString(parameters[0]));
            disconnect();
        },

        "name": function(parameters) {
            if (guac_client.onname) guac_client.onname(unescapeGuacamoleString(parameters[0]));
        },

        "clipboard": function(parameters) {
            if (guac_client.onclipboard) guac_client.onclipboard(unescapeGuacamoleString(parameters[0]));
        },

        "size": function(parameters) {

            displayWidth = parseInt(parameters[0]);
            displayHeight = parseInt(parameters[1]);

            // Update (set) display size
            display.style.width = displayWidth + "px";
            display.style.height = displayHeight + "px";

            // Set cursor layer width/height
            if (cursor != null)
                cursor.resize(displayWidth, displayHeight);

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

            dstL.copyRect(
                srcL,
                srcX,
                srcY,
                srcWidth, 
                srcHeight, 
                dstX,
                dstY 
            );

        },

        "rect": function(parameters) {

            var channelMask = parseInt(parameters[0]);
            var layer = getLayer(parseInt(parameters[1]));
            var x = parseInt(parameters[2]);
            var y = parseInt(parameters[3]);
            var w = parseInt(parameters[4]);
            var h = parseInt(parameters[5]);
            var r = parseInt(parameters[6]);
            var g = parseInt(parameters[7]);
            var b = parseInt(parameters[8]);
            var a = parseInt(parameters[9]);

            layer.setChannelMask(channelMask);

            layer.drawRect(
                x, y, w, h,
                r, g, b, a
            );

        },

        "clip": function(parameters) {

            var layer = getLayer(parseInt(parameters[0]));
            var x = parseInt(parameters[1]);
            var y = parseInt(parameters[2]);
            var w = parseInt(parameters[3]);
            var h = parseInt(parameters[4]);

            layer.clipRect(x, y, w, h);

        },

        "cursor": function(parameters) {

            var x = parseInt(parameters[0]);
            var y = parseInt(parameters[1]);
            var data = parameters[2];

            if (cursor == null) {
                cursor = new Guacamole.Layer(displayWidth, displayHeight);
                
                var canvas = cursor.getCanvas();
                canvas.style.position = "absolute";
                canvas.style.left = "0px";
                canvas.style.top = "0px";

                display.appendChild(canvas);
            }

            // Start cursor image load
            var image = new Image();
            image.onload = function() {
                cursorImage = image;

                var cursorX = cursorRectX + cursorHotspotX;
                var cursorY = cursorRectY + cursorHotspotY;

                cursorHotspotX = x;
                cursorHotspotY = y;

                redrawCursor(cursorX, cursorY);
            };
            image.src = "data:image/png;base64," + data

        },

        "sync": function(parameters) {

            var timestamp = parameters[0];

            // When all layers have finished rendering all instructions
            // UP TO THIS POINT IN TIME, send sync response.

            var layersToSync = 0;
            function syncLayer() {

                layersToSync--;

                // Send sync response when layers are finished
                if (layersToSync == 0)
                    tunnel.sendMessage("sync:" + timestamp + ";");

            }

            // Count active, not-ready layers and install sync tracking hooks
            for (var i=0; i<layers.length; i++) {

                var layer = layers[i];
                if (layer && !layer.isReady()) {
                    layersToSync++;
                    layer.sync(syncLayer);
                }

            }

            // If all layers are ready, then we didn't install any hooks.
            // Send sync message now,
            if (layersToSync == 0)
                tunnel.sendMessage("sync:" + timestamp + ";");

        }
      
    };


    function doInstruction(opcode, parameters) {

        var handler = instructionHandlers[opcode];
        if (handler)
            handler(parameters);

    }


    function disconnect() {

        // Only attempt disconnection not disconnected.
        if (currentState != STATE_DISCONNECTED
                && currentState != STATE_DISCONNECTING) {

            setState(STATE_DISCONNECTING);
            tunnel.sendMessage("disconnect;");
            tunnel.disconnect();
            setState(STATE_DISCONNECTED);
        }

    }

    function escapeGuacamoleString(str) {

        var escapedString = "";

        for (var i=0; i<str.length; i++) {

            var c = str.charAt(i);
            if (c == ",")
                escapedString += "\\c";
            else if (c == ";")
                escapedString += "\\s";
            else if (c == "\\")
                escapedString += "\\\\";
            else
                escapedString += c;

        }

        return escapedString;

    }

    function unescapeGuacamoleString(str) {

        var unescapedString = "";

        for (var i=0; i<str.length; i++) {

            var c = str.charAt(i);
            if (c == "\\" && i<str.length-1) {

                var escapeChar = str.charAt(++i);
                if (escapeChar == "c")
                    unescapedString += ",";
                else if (escapeChar == "s")
                    unescapedString += ";";
                else if (escapeChar == "\\")
                    unescapedString += "\\";
                else
                    unescapedString += "\\" + escapeChar;

            }
            else
                unescapedString += c;

        }

        return unescapedString;

    }

    guac_client.disconnect = disconnect;
    guac_client.connect = function(data) {

        setState(STATE_CONNECTING);

        try {
            tunnel.connect(data);
        }
        catch (e) {
            setState(STATE_IDLE);
            throw e;
        }

        setState(STATE_WAITING);
    };

    guac_client.escapeGuacamoleString   = escapeGuacamoleString;
    guac_client.unescapeGuacamoleString = unescapeGuacamoleString;

}
