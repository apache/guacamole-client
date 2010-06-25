
/*
 *  Guacamole - Pure JavaScript/HTML VNC Client
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
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

function VNCClient(display) {

    var STATE_IDLE          = 0;
    var STATE_CONNECTING    = 1;
    var STATE_WAITING       = 2;
    var STATE_CONNECTED     = 3;
    var STATE_DISCONNECTING = 4;
    var STATE_DISCONNECTED  = 5;

    var currentState = STATE_IDLE;
    var stateChangeHandler = null;

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

    var keyIndex = 0;
    var xmlIndex = 0;

    // Layers
    var background = null;
    var cursor = null;

    var cursorImage = null;
    var cursorHotspotX = 0;
    var cursorHotspotY = 0;


    // FIXME: Make object. Clean up.
    var cursorRectX = 0;
    var cursorRectY = 0;
    var cursorRectW = 0;
    var cursorRectH = 0;

    var cursorHidden = 0;

    function redrawCursor() {

        // Hide hardware cursor
        if (cursorHidden == 0) {
            display.className += " hideCursor";
            cursorHidden = 1;
        }

        // Erase old cursor
        cursor.clearRect(cursorRectX, cursorRectY, cursorRectW, cursorRectH);

        // Update rect
        cursorRectX = mouse.getX() - cursorHotspotX;
        cursorRectY = mouse.getY() - cursorHotspotY;
        cursorRectW = cursorImage.width;
        cursorRectH = cursorImage.height;

        // Draw new cursor
        cursor.drawImage(cursorRectX, cursorRectY, cursorImage);
    }




	/*****************************************/
	/*** Keyboard                          ***/
	/*****************************************/

    var keyboard = new GuacamoleKeyboard(document);

    this.disableKeyboard = function() {
        keyboard.setKeyPressedHandler(null);
        keyboard.setKeyReleasedHandler(null);
    };

    this.enableKeyboard = function() {
        keyboard.setKeyPressedHandler(
            function (keysym) {
                sendKeyEvent(1, keysym);
            }
        );

        keyboard.setKeyReleasedHandler(
            function (keysym) {
                sendKeyEvent(0, keysym);
            }
        );
    };

    // Enable keyboard by default
    this.enableKeyboard();

    function sendKeyEvent(pressed, keysym) {

        // Do not send requests if not connected
        if (!isConnected())
            return;

        var key_xmlhttprequest = new XMLHttpRequest();
        key_xmlhttprequest.open("GET",
            "key?index=" + (keyIndex++)
                + "&pressed=" + pressed
                + "&keysym=" + keysym);
        key_xmlhttprequest.send(null);
    }

    this.pressKey = function(keysym) {
        sendKeyEvent(1, keysym);
    };

    this.releaseKey = function(keysym) {
        sendKeyEvent(0, keysym);
    };


	/*****************************************/
	/*** Mouse                             ***/
	/*****************************************/

    var mouse = new GuacamoleMouse(display);
    mouse.setButtonPressedHandler(
        function(mouseState) {
            sendMouseState(mouseState);
        }
    );

    mouse.setButtonReleasedHandler(
        function(mouseState) {
            sendMouseState(mouseState);
        }
    );

    mouse.setMovementHandler(
        function(mouseState) {

            // Draw client-side cursor
            if (cursorImage != null) {
                redrawCursor();
            }

            sendMouseState(mouseState);
        }
    );


    var sendingMouseEvents = 0;
    var mouseEventBuffer = "";

    function sendMouseState(mouseState) {

        // Do not send requests if not connected
        if (!isConnected())
            return;

        // Add event to queue, restart send loop if finished.
        if (mouseEventBuffer.length > 0) mouseEventBuffer += "&";
        mouseEventBuffer += "event=" + mouseState.toString();
        if (sendingMouseEvents == 0)
            sendPendingMouseEvents();

    }

    function sendPendingMouseEvents() {

        // Do not send requests if not connected
        if (!isConnected())
            return;

        if (mouseEventBuffer.length > 0) {

            sendingMouseEvents = 1;

            var mouse_xmlhttprequest = new XMLHttpRequest();
            mouse_xmlhttprequest.open("GET", "pointer?" + mouseEventBuffer);
            mouseEventBuffer = ""; // Clear buffer

            var eventSendStart = new Date().getTime();

            // Once response received, send next queued event.
            mouse_xmlhttprequest.onreadystatechange = function() {

                // Update round-trip-time figures
                if (mouse_xmlhttprequest.readyState == 2) {
                    var eventSendEnd = new Date().getTime();
                    totalRoundTrip += eventSendEnd - eventSendStart;
                    roundTripSamples++;
                }

                if (mouse_xmlhttprequest.readyState == 4) {
                    sendPendingMouseEvents();
                }
            };

            mouse_xmlhttprequest.send(null);
        }
        else
            sendingMouseEvents = 0;

    }


	/*****************************************/
	/*** Clipboard                         ***/
	/*****************************************/

    this.setClipboard = function(data) {

        // Do not send requests if not connected
        if (!isConnected())
            return;

        var clipboard_xmlhttprequest = new XMLHttpRequest();
        clipboard_xmlhttprequest.open("POST", "clipboard");

        var sendStart = new Date().getTime();

        // Update round trip metrics
        clipboard_xmlhttprequest.onreadystatechange = function() {

            // Update round-trip-time figures
            if (clipboard_xmlhttprequest.readyState == 2) {
                var sendEnd = new Date().getTime();
                totalRoundTrip += sendEnd - sendStart;
                roundTripSamples++;
            }

        };

        clipboard_xmlhttprequest.send(data);

    }


    function desaturateFilter(data, width, height) {

        for (var i=0; i<data.length; i+=4) {

            // Get RGB values
            var r = data[i];
            var g = data[i+1];
            var b = data[i+2];

            // Desaturate
            var v = Math.max(r, g, b) / 2;
            data[i]   = v;
            data[i+1] = v;
            data[i+2] = v;

        }

    }


    var errorHandler = null;
    this.setErrorHandler = function(handler) {
        errorHandler = handler;
    };

    var errorEncountered = 0;
    function showError(error) {
        // Only display first error (avoid infinite error loops)
        if (errorEncountered == 0) {
            errorEncountered = 1;

            disconnect();

            // In case nothing has been rendered yet, use error style
            display.className += " error";

            // Show error by desaturating display
            if (background)
                background.filter(desaturateFilter);

            if (errorHandler)
                errorHandler(error);
        }
    }

    function handleErrors(message) {
        var errors = message.getErrors();
        for (var errorIndex=0; errorIndex<errors.length; errorIndex++)
            showError(errors[errorIndex].getMessage());
    }

    // Data transfer statistics
    var totalTransferred = 0;
    var totalTime = 0;
    var totalRoundTrip = 0;
    var roundTripSamples = 0;

    var clipboardHandler = null;

    this.setClipboardHandler = function(handler) {
        clipboardHandler = handler;
    };


    function handleResponse(xmlhttprequest) {

        var start = null; // Start of download time
        var startOffset = null;

        var instructionStart = 0;
        var startIndex = 0;

        // Make request for next before it's too late
        var nextRequest = null;
        if (xmlhttprequest.readyState >= 2)
            nextRequest = makeRequest();

        function parseResponse() {

            // Make request the moment we receive headers
            // If the event handler isn't set by the time headers are available, we will
            // already have made this request.
            if (xmlhttprequest.readyState == 2) {
                if (nextRequest == null)
                    nextRequest = makeRequest();

                start = new Date().getTime();
                startOffset = 0;
            }

            // Parse stream when data is received and when complete.
            if (xmlhttprequest.readyState == 3 ||
                xmlhttprequest.readyState == 4) {

                // Halt on error during request
                if (xmlhttprequest.status == 0) {
                    showError("Request canceled by browser.");
                    return;
                }
                else if (xmlhttprequest.status != 200) {
                    showError("Error during request (HTTP " + xmlhttprequest.status + "): " + xmlhttprequest.statusText);
                    return;
                }

                var current = xmlhttprequest.responseText;
                var instructionEnd;
                
                if (start == null) {
                    start = new Date().getTime();
                    startOffset = current.length;
                }

                while ((instructionEnd = current.indexOf(";", startIndex)) != -1) {

                    // Start next search at next instruction
                    startIndex = instructionEnd+1;

                    var instruction = current.substr(instructionStart,
                            instructionEnd - instructionStart);

                    instructionStart = startIndex;

                    var opcodeEnd = instruction.indexOf(":");

                    var opcode = instruction.substr(0, opcodeEnd);
                    var parameters = instruction.substr(opcodeEnd+1).split(",");

                    // Call instruction handler.
                    doInstruction(opcode, parameters);
                }

                // Start search at end of string.
                startIndex = current.length;

                delete instruction;
                delete parameters;

                // If we're done parsing, handle the next response.
                if (xmlhttprequest.readyState == 4 && isConnected()) {

                    // If we got the start time, do statistics.
                    if (start) {
                        var end = new Date().getTime();
                        var duration = end - start;
                        var length = current.length;

                        totalTime += duration;
                        totalTransferred += length;
                    }

                    delete xmlhttprequest;
                    handleResponse(nextRequest);
                }

            }

        };

        xmlhttprequest.onreadystatechange = parseResponse;

        // Handle what we have so far.
        parseResponse();
    }

    function makeRequest() {

        // Calculate message limit as number of bytes likely to be transferred
        // in one round trip.
        var messageLimit;
        if (totalTime == 0 || roundTripSamples == 0)
            messageLimit = 10240; // Default to a reasonable 10k
        else
            messageLimit = Math.round(totalRoundTrip * totalTransferred / totalTime / roundTripSamples);

        // Download self
        var xmlhttprequest = new XMLHttpRequest();
        xmlhttprequest.open("GET", "instructions?messageLimit=" + messageLimit);
        xmlhttprequest.send(null); 

        return xmlhttprequest;

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

    var instructionHandlers = {

        "error": function(parameters) {
            showError(unescapeGuacamoleString(parameters[0]));
        },

        "name": function(parameters) {
            document.title = "Guacamole (" + unescapeGuacamoleString(parameters[0]) + ")";
        },

        "clipboard": function(parameters) {
            clipboardHandler(unescapeGuacamoleString(parameters[0]));
        },

        "size": function(parameters) {

            var width = parameters[0];
            var height = parameters[1];

            // Update (set) display size
            if (display && (background == null || cursor == null)) {
                display.style.width = width + "px";
                display.style.height = height + "px";

                background = new Layer(width, height);
                cursor = new Layer(width, height);

                display.appendChild(background);
                display.appendChild(cursor);
            }

        },

        "rect": function(parameters) {

            var x = parameters[0];
            var y = parameters[1];
            var w = parameters[2];
            var h = parameters[3];
            var color = parameters[4];

            background.drawRect(
                x,
                y,
                w,
                h,
                color
            );

        },

        "png": function(parameters) {

            var x = parameters[0];
            var y = parameters[1];
            var data = parameters[2];

            background.draw(
                x,
                y,
                "data:image/png;base64," + data
            );

            // If received first update, no longer waiting.
            if (currentState == STATE_WAITING)
                setState(STATE_CONNECTED);

        },

        "copy": function(parameters) {

            var srcX = parameters[0];
            var srcY = parameters[1];
            var srcWidth = parameters[2];
            var srcHeight = parameters[3];
            var dstX = parameters[4];
            var dstY = parameters[5];

            background.copyRect(
                srcX,
                srcY,
                srcWidth, 
                srcHeight, 
                dstX,
                dstY 
            );

        },

        "cursor": function(parameters) {

            var x = parameters[0];
            var y = parameters[1];
            var data = parameters[2];

            // Start cursor image load
            var image = new Image();
            image.onload = function() {
                cursorImage = image;
                cursorHotspotX = x;
                cursorHotspotY = y;
                redrawCursor();
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

        // Attempt connection
        var connect_xmlhttprequest = new XMLHttpRequest();
        connect_xmlhttprequest.open("GET", "connect", false);

        setState(STATE_CONNECTING);
        connect_xmlhttprequest.send(null);

        // Handle result (and check for errors) 
        var message = new GuacamoleMessage(connect_xmlhttprequest.responseXML);
        if (!message.hasErrors()) {
            setState(STATE_WAITING);
            handleResponse(makeRequest()); // Start stream if connection successful
        }
        else
            handleErrors(message);

    };

    function disconnect() {

        // Only attempt disconnection not disconnected.
        if (currentState != STATE_DISCONNECTED
                && currentState != STATE_DISCONNECTING) {

            setState(STATE_DISCONNECTING);

            // Attempt disdisconnection
            var disconnect_xmlhttprequest = new XMLHttpRequest();
            disconnect_xmlhttprequest.open("GET", "disconnect", false);
            disconnect_xmlhttprequest.send(null);

            // Handle result (and check for errors) 
            var message = new GuacamoleMessage(disconnect_xmlhttprequest.responseXML);
            handleErrors(message);

            setState(STATE_DISCONNECTED);
        }

    }

    this.disconnect = disconnect;

}
