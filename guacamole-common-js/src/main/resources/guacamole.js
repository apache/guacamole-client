
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
    
    var currentTimestamp = 0;
    var pingInterval = null;

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

        tunnel.sendMessage("key", keysym, pressed);
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
                if (layersToSync == 0) {
                    if (timestamp != currentTimestamp) {
                        tunnel.sendMessage("sync", timestamp);
                        currentTimestamp = timestamp;
                    }
                }

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

