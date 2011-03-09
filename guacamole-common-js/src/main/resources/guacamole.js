
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

function GuacamoleClient(display, tunnel) {

    var STATE_IDLE          = 0;
    var STATE_CONNECTING    = 1;
    var STATE_WAITING       = 2;
    var STATE_CONNECTED     = 3;
    var STATE_DISCONNECTING = 4;
    var STATE_DISCONNECTED  = 5;

    var currentState = STATE_IDLE;
    var stateChangeHandler = null;

    tunnel.setInstructionHandler(doInstruction);

    // Display must be relatively positioned for mouse to be handled properly
    display.style.position = "relative";

    function setState(state) {
        if (state != currentState) {
            currentState = state;
            if (stateChangeHandler)
                stateChangeHandler(currentState);
        }
    }

    this.setOnStateChangeHandler = function(handler) {
        stateChangeHandler = handler;
    }

    function isConnected() {
        return currentState == STATE_CONNECTED
            || currentState == STATE_WAITING;
    }

    var cursorImage = null;
    var cursorHotspotX = 0;
    var cursorHotspotY = 0;

    // FIXME: Make object. Clean up.
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

    this.sendKeyEvent = function(pressed, keysym) {
        // Do not send requests if not connected
        if (!isConnected())
            return;

        tunnel.sendMessage("key:" +  keysym + "," + pressed + ";");
    }

    this.sendMouseState = function(mouseState) {

        // Do not send requests if not connected
        if (!isConnected())
            return;

        // Draw client-side cursor
        if (cursorImage != null) {
            redrawCursor(
                mouseState.getX(),
                mouseState.getY()
            );
        }

        // Build mask
        var buttonMask = 0;
        if (mouseState.getLeft())   buttonMask |= 1;
        if (mouseState.getMiddle()) buttonMask |= 2;
        if (mouseState.getRight())  buttonMask |= 4;
        if (mouseState.getUp())     buttonMask |= 8;
        if (mouseState.getDown())   buttonMask |= 16;

        // Send message
        tunnel.sendMessage("mouse:" + mouseState.getX() + "," + mouseState.getY() + "," + buttonMask + ";");
    }

    this.setClipboard = function(data) {

        // Do not send requests if not connected
        if (!isConnected())
            return;

        tunnel.sendMessage("clipboard:" + tunnel.escapeGuacamoleString(data) + ";");
    }

    // Handlers

    var nameHandler = null;
    this.setNameHandler = function(handler) {
        nameHandler = handler;
    }

    var errorHandler = null;
    this.setErrorHandler = function(handler) {
        errorHandler = handler;
    };

    var clipboardHandler = null;
    this.setClipboardHandler = function(handler) {
        clipboardHandler = handler;
    };

    // Layers
    var displayWidth = 0;
    var displayHeight = 0;

    var layers = new Array();
    var buffers = new Array();
    var cursor = null;

    this.getLayers = function() {
        return layers;
    }

    function getLayer(index) {

        // If negative index, use buffer
        if (index < 0) {

            index = -1 - index;
            var buffer = buffers[index];

            // Create buffer if necessary
            if (buffer == null) {
                buffer = new Layer(0, 0);
                buffer.setAutosize(1);
                buffers[index] = buffer;
            }

            return buffer;
        }

        // If non-negative, use visible layer
        else {

            var layer = layers[index];
            if (layer == null) {

                // Add new layer
                layer = new Layer(displayWidth, displayHeight);
                layers[index] = layer;

                // (Re)-add existing layers in order
                for (var i=0; i<layers.length; i++) {
                    if (layers[i]) {

                        // If already present, remove
                        if (layers[i].parentNode === display)
                            display.removeChild(layers[i]);

                        // Add to end
                        display.appendChild(layers[i]);
                    }
                }

                // Add cursor layer last
                if (cursor != null) {
                    if (cursor.parentNode === display)
                        display.removeChild(cursor);
                    display.appendChild(cursor);
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
            if (errorHandler) errorHandler(tunnel.unescapeGuacamoleString(parameters[0]));
            disconnect();
        },

        "name": function(parameters) {
            if (nameHandler) nameHandler(tunnel.unescapeGuacamoleString(parameters[0]));
        },

        "clipboard": function(parameters) {
            if (clipboardHandler) clipboardHandler(tunnel.unescapeGuacamoleString(parameters[0]));
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

            var layer = parseInt(parameters[0]);
            var x = parseInt(parameters[1]);
            var y = parseInt(parameters[2]);
            var data = parameters[3];

            getLayer(layer).draw(
                x,
                y,
                "data:image/png;base64," + data
            );

            // If received first update, no longer waiting.
            if (currentState == STATE_WAITING)
                setState(STATE_CONNECTED);

        },

        "copy": function(parameters) {

            var srcL = parseInt(parameters[0]);
            var srcX = parseInt(parameters[1]);
            var srcY = parseInt(parameters[2]);
            var srcWidth = parseInt(parameters[3]);
            var srcHeight = parseInt(parameters[4]);
            var dstL = parseInt(parameters[5]);
            var dstX = parseInt(parameters[6]);
            var dstY = parseInt(parameters[7]);

            getLayer(dstL).copyRect(
                getLayer(srcL),
                srcX,
                srcY,
                srcWidth, 
                srcHeight, 
                dstX,
                dstY 
            );

        },

        "cursor": function(parameters) {

            var x = parseInt(parameters[0]);
            var y = parseInt(parameters[1]);
            var data = parameters[2];

            if (cursor == null) {
                cursor = new Layer(displayWidth, displayHeight);
                display.appendChild(cursor);
            }

            // Start cursor image load
            var image = new Image();
            image.onload = function() {
                cursorImage = image;
                cursorHotspotX = x;
                cursorHotspotY = y;
                redrawCursor(cursorRectX, cursorRectY);
            };
            image.src = "data:image/png;base64," + data

        }
      
    };


    function doInstruction(opcode, parameters) {

        var handler = instructionHandlers[opcode];
        if (handler)
            handler(parameters);

    }
        

    this.connect = function() {

        setState(STATE_CONNECTING);
        tunnel.connect();
        setState(STATE_WAITING);

    };

    
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

    this.disconnect = disconnect;

}
