
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
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

function GuacamoleClient(display) {

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
            display.className += " guac-hide-cursor";
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

        sendMessage("key:" +  keysym + "," + pressed + ";");
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


    function sendMouseState(mouseState) {

        // Do not send requests if not connected
        if (!isConnected())
            return;

        // Build mask
        var buttonMask = 0;
        if (mouseState.getLeft())   buttonMask |= 1;
        if (mouseState.getMiddle()) buttonMask |= 2;
        if (mouseState.getRight())  buttonMask |= 4;
        if (mouseState.getUp())     buttonMask |= 8;
        if (mouseState.getDown())   buttonMask |= 16;

        // Send message
        sendMessage("mouse:" + mouseState.getX() + "," + mouseState.getY() + "," + buttonMask + ";");
    }

    var sendingMessages = 0;
    var outputMessageBuffer = "";

    function sendMessage(message) {

        // Add event to queue, restart send loop if finished.
        outputMessageBuffer += message;
        if (sendingMessages == 0)
            sendPendingMessages();

    }

    function sendPendingMessages() {

        if (outputMessageBuffer.length > 0) {

            sendingMessages = 1;

            var message_xmlhttprequest = new XMLHttpRequest();
            message_xmlhttprequest.open("POST", "inbound");
            message_xmlhttprequest.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
            message_xmlhttprequest.setRequestHeader("Content-length", outputMessageBuffer.length);

            // Once response received, send next queued event.
            message_xmlhttprequest.onreadystatechange = function() {
                if (message_xmlhttprequest.readyState == 4)
                    sendPendingMessages();
            }

            message_xmlhttprequest.send(outputMessageBuffer);
            outputMessageBuffer = ""; // Clear buffer

        }
        else
            sendingMessages = 0;

    }


	/*****************************************/
	/*** Clipboard                         ***/
	/*****************************************/

    this.setClipboard = function(data) {

        // Do not send requests if not connected
        if (!isConnected())
            return;

        sendMessage("clipboard:" + escapeGuacamoleString(data) + ";");
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
            display.className += " guac-error";

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

    var clipboardHandler = null;
    var requests = 0;

    this.setClipboardHandler = function(handler) {
        clipboardHandler = handler;
    };


    function handleResponse(xmlhttprequest) {

        var nextRequest = null;

        var instructionStart = 0;
        var startIndex = 0;

        function parseResponse() {

            // Start next request as soon as possible
            if (xmlhttprequest.readyState >= 2 && nextRequest == null)
                nextRequest = makeRequest();

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
                
                while ((instructionEnd = current.indexOf(";", startIndex)) != -1) {

                    // Start next search at next instruction
                    startIndex = instructionEnd+1;

                    var instruction = current.substr(instructionStart,
                            instructionEnd - instructionStart);

                    instructionStart = startIndex;

                    var opcodeEnd = instruction.indexOf(":");

                    var opcode;
                    var parameters;
                    if (opcodeEnd == -1) {
                        opcode = instruction;
                        parameters = new Array();
                    }
                    else {
                        opcode = instruction.substr(0, opcodeEnd);
                        parameters = instruction.substr(opcodeEnd+1).split(",");
                    }

                    // If we're done parsing, handle the next response.
                    if (opcode.length == 0) {

                        if (isConnected()) {
                            delete xmlhttprequest;
                            if (nextRequest)
                                handleResponse(nextRequest);
                        }

                        break;
                    }

                    // Call instruction handler.
                    doInstruction(opcode, parameters);
                }

                // Start search at end of string.
                startIndex = current.length;

                delete instruction;
                delete parameters;

            }

        }

        xmlhttprequest.onreadystatechange = parseResponse;
        parseResponse();

    }


    function makeRequest() {

        // Download self
        var xmlhttprequest = new XMLHttpRequest();
        xmlhttprequest.open("POST", "outbound");
        xmlhttprequest.send(null); 

        return xmlhttprequest;

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

    var instructionHandlers = {

        "ready": function(parameters) {

            // If done drawing, send ready response
            if (background.isReady())
                sendMessage("ready;");

            // If not done drawing, set callback which will send response
            else
                background.setReadyHandler(function() {
                    sendMessage("ready;");
                    background.setReadyHandler(null);
                });

        },

        "error": function(parameters) {
            showError(unescapeGuacamoleString(parameters[0]));
        },

        "name": function(parameters) {
            document.title = unescapeGuacamoleString(parameters[0]);
        },

        "clipboard": function(parameters) {
            clipboardHandler(unescapeGuacamoleString(parameters[0]));
        },

        "size": function(parameters) {

            var width = parseInt(parameters[0]);
            var height = parseInt(parameters[1]);

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

            var x = parseInt(parameters[0]);
            var y = parseInt(parameters[1]);
            var w = parseInt(parameters[2]);
            var h = parseInt(parameters[3]);
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

            var x = parseInt(parameters[0]);
            var y = parseInt(parameters[1]);
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

            var srcX = parseInt(parameters[0]);
            var srcY = parseInt(parameters[1]);
            var srcWidth = parseInt(parameters[2]);
            var srcHeight = parseInt(parameters[3]);
            var dstX = parseInt(parameters[4]);
            var dstY = parseInt(parameters[5]);

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

            var x = parseInt(parameters[0]);
            var y = parseInt(parameters[1]);
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

        setState(STATE_CONNECTING);

        // Start tunnel and connect synchronously
        var connect_xmlhttprequest = new XMLHttpRequest();
        connect_xmlhttprequest.open("POST", "connect", false);
        connect_xmlhttprequest.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
        connect_xmlhttprequest.setRequestHeader("Content-length", 0);
        connect_xmlhttprequest.send(null);

        // Start reading data
        setState(STATE_WAITING);
        handleResponse(makeRequest());

        // Send "ready" message to server
        sendMessage("ready;");

    };

    
    function disconnect() {

        // Only attempt disconnection not disconnected.
        if (currentState != STATE_DISCONNECTED
                && currentState != STATE_DISCONNECTING) {

            var message = "disconnect;";
            setState(STATE_DISCONNECTING);

            // Send disconnect message (synchronously... as necessary until handoff is implemented)
            var disconnect_xmlhttprequest = new XMLHttpRequest();
            disconnect_xmlhttprequest.open("POST", "inbound", false);
            disconnect_xmlhttprequest.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
            disconnect_xmlhttprequest.setRequestHeader("Content-length", message.length);
            disconnect_xmlhttprequest.send(message);

            setState(STATE_DISCONNECTED);
        }

    }

    this.disconnect = disconnect;

}
