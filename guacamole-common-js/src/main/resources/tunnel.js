
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
 * Core object providing abstract communication for Guacamole. This object
 * is a null implementation whose functions do nothing. Guacamole applications
 * should use {@link Guacamole.HTTPTunnel} instead, or implement their own tunnel based
 * on this one.
 * 
 * @constructor
 * @see Guacamole.HTTPTunnel
 */
Guacamole.Tunnel = function() {

    /**
     * Connect to the tunnel with the given optional data. This data is
     * typically used for authentication. The format of data accepted is
     * up to the tunnel implementation.
     * 
     * @param {String} data The data to send to the tunnel when connecting.
     */
    this.connect = function(data) {};
    
    /**
     * Disconnect from the tunnel.
     */
    this.disconnect = function() {};
    
    /**
     * Send the given message through the tunnel to the service on the other
     * side. All messages are guaranteed to be received in the order sent.
     * 
     * @param {String} message The message to send to the service on the other
     *                         side of the tunnel.
     */
    this.sendMessage = function(message) {};
    
    /**
     * Fired whenever an error is encountered by the tunnel.
     * 
     * @event
     * @param {String} message A human-readable description of the error that
     *                         occurred.
     */
    this.onerror = null;

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
 * Guacamole Tunnel implemented over HTTP via XMLHttpRequest.
 * 
 * @constructor
 * @augments Guacamole.Tunnel
 * @param {String} tunnelURL The URL of the HTTP tunneling service.
 */
Guacamole.HTTPTunnel = function(tunnelURL) {

    /**
     * Reference to this HTTP tunnel.
     */
    var tunnel = this;

    var tunnel_uuid;

    var TUNNEL_CONNECT = tunnelURL + "?connect";
    var TUNNEL_READ    = tunnelURL + "?read:";
    var TUNNEL_WRITE   = tunnelURL + "?write:";

    var STATE_IDLE          = 0;
    var STATE_CONNECTED     = 1;
    var STATE_DISCONNECTED  = 2;

    var currentState = STATE_IDLE;

    var POLLING_ENABLED     = 1;
    var POLLING_DISABLED    = 0;

    // Default to polling - will be turned off automatically if not needed
    var pollingMode = POLLING_ENABLED;

    var sendingMessages = false;
    var outputMessageBuffer = "";

    this.sendMessage = function(message) {

        // Do not attempt to send messages if not connected
        if (currentState != STATE_CONNECTED)
            return;

        // Add event to queue, restart send loop if finished.
        outputMessageBuffer += message;
        if (!sendingMessages)
            sendPendingMessages();

    };

    function sendPendingMessages() {

        if (outputMessageBuffer.length > 0) {

            sendingMessages = true;

            var message_xmlhttprequest = new XMLHttpRequest();
            message_xmlhttprequest.open("POST", TUNNEL_WRITE + tunnel_uuid);
            message_xmlhttprequest.setRequestHeader("Content-type", "application/x-www-form-urlencoded");

            // Once response received, send next queued event.
            message_xmlhttprequest.onreadystatechange = function() {
                if (message_xmlhttprequest.readyState == 4)
                    sendPendingMessages();
            }

            message_xmlhttprequest.send(outputMessageBuffer);
            outputMessageBuffer = ""; // Clear buffer

        }
        else
            sendingMessages = false;

    }


    function handleResponse(xmlhttprequest) {

        var interval = null;
        var nextRequest = null;

        var dataUpdateEvents = 0;

        // The location of the last element's terminator
        var elementEnd = -1;

        // Where to start the next length search or the next element
        var startIndex = 0;

        // Parsed elements
        var elements = new Array();

        function parseResponse() {

            // Do not handle responses if not connected
            if (currentState != STATE_CONNECTED) {
                
                // Clean up interval if polling
                if (interval != null)
                    clearInterval(interval);
                
                return;
            }

            // Start next request as soon as possible IF request was successful
            if (xmlhttprequest.readyState >= 2 && nextRequest == null && xmlhttprequest.status == 200)
                nextRequest = makeRequest();

            // Parse stream when data is received and when complete.
            if (xmlhttprequest.readyState == 3 ||
                xmlhttprequest.readyState == 4) {

                // Also poll every 30ms (some browsers don't repeatedly call onreadystatechange for new data)
                if (pollingMode == POLLING_ENABLED) {
                    if (xmlhttprequest.readyState == 3 && interval == null)
                        interval = setInterval(parseResponse, 30);
                    else if (xmlhttprequest.readyState == 4 && interval != null)
                        clearInterval(interval);
                }

                // If canceled, stop transfer
                if (xmlhttprequest.status == 0) {
                    tunnel.disconnect();
                    return;
                }

                // Halt on error during request
                else if (xmlhttprequest.status != 200) {

                    // Get error message (if any)
                    var message = xmlhttprequest.getResponseHeader("X-Guacamole-Error-Message");
                    if (!message)
                        message = "Internal server error";

                    // Call error handler
                    if (tunnel.onerror) tunnel.onerror(message);

                    // Finish
                    tunnel.disconnect();
                    return;
                }

                var current = xmlhttprequest.responseText;

                // While search is within currently received data
                while (elementEnd < current.length) {

                    // If we are waiting for element data
                    if (elementEnd >= startIndex) {

                        // We now have enough data for the element. Parse.
                        var element = current.substring(startIndex, elementEnd);
                        var terminator = current.substring(elementEnd, elementEnd+1);

                        // Add element to array
                        elements.push(element);

                        // If last element, handle instruction
                        if (terminator == ";") {

                            // Get opcode
                            var opcode = elements.shift();

                            // Call instruction handler.
                            if (tunnel.oninstruction != null)
                                tunnel.oninstruction(opcode, elements);

                            // Clear elements
                            elements.length = 0;

                        }

                        // Start searching for length at character after
                        // element terminator
                        startIndex = elementEnd + 1;

                    }

                    // Search for end of length
                    var lengthEnd = current.indexOf(".", startIndex);
                    if (lengthEnd != -1) {

                        // Parse length
                        var length = parseInt(current.substring(elementEnd+1, lengthEnd));

                        // If we're done parsing, handle the next response.
                        if (length == 0) {

                            // Clean up interval if polling
                            if (interval != null)
                                clearInterval(interval);
                           
                            // Clean up object
                            xmlhttprequest.onreadystatechange = null;
                            xmlhttprequest.abort();

                            // Start handling next request
                            if (nextRequest)
                                handleResponse(nextRequest);

                            // Done parsing
                            break;

                        }

                        // Calculate start of element
                        startIndex = lengthEnd + 1;

                        // Calculate location of element terminator
                        elementEnd = startIndex + length;

                    }
                    
                    // If no period yet, continue search when more data
                    // is received
                    else {
                        startIndex = current.length;
                        break;
                    }

                } // end parse loop

            }

        }

        // If response polling enabled, attempt to detect if still
        // necessary (via wrapping parseResponse())
        if (pollingMode == POLLING_ENABLED) {
            xmlhttprequest.onreadystatechange = function() {

                // If we receive two or more readyState==3 events,
                // there is no need to poll.
                if (xmlhttprequest.readyState == 3) {
                    dataUpdateEvents++;
                    if (dataUpdateEvents >= 2) {
                        pollingMode = POLLING_DISABLED;
                        xmlhttprequest.onreadystatechange = parseResponse;
                    }
                }

                parseResponse();
            }
        }

        // Otherwise, just parse
        else
            xmlhttprequest.onreadystatechange = parseResponse;

        parseResponse();

    }


    function makeRequest() {

        // Download self
        var xmlhttprequest = new XMLHttpRequest();
        xmlhttprequest.open("POST", TUNNEL_READ + tunnel_uuid);
        xmlhttprequest.send(null);

        return xmlhttprequest;

    }

    this.connect = function(data) {

        // Start tunnel and connect synchronously
        var connect_xmlhttprequest = new XMLHttpRequest();
        connect_xmlhttprequest.open("POST", TUNNEL_CONNECT, false);
        connect_xmlhttprequest.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
        connect_xmlhttprequest.send(data);

        // If failure, throw error
        if (connect_xmlhttprequest.status != 200) {

            var message = connect_xmlhttprequest.getResponseHeader("X-Guacamole-Error-Message");
            if (!message)
                message = "Internal error";

            throw new Error(message);

        }

        // Get UUID from response
        tunnel_uuid = connect_xmlhttprequest.responseText;

        // Start reading data
        currentState = STATE_CONNECTED;
        handleResponse(makeRequest());

    };

    this.disconnect = function() {
        currentState = STATE_DISCONNECTED;
    };

};

Guacamole.HTTPTunnel.prototype = new Guacamole.Tunnel();


/**
 * Guacamole Tunnel implemented over WebSocket via XMLHttpRequest.
 * 
 * @constructor
 * @augments Guacamole.Tunnel
 * @param {String} tunnelURL The URL of the WebSocket tunneling service.
 */
Guacamole.WebSocketTunnel = function(tunnelURL) {

    /**
     * Reference to this WebSocket tunnel.
     */
    var tunnel = this;

    /**
     * The WebSocket used by this tunnel.
     */
    var socket = null;

    /**
     * The WebSocket protocol corresponding to the protocol used for the current
     * location.
     */
    var ws_protocol = {
        "http:":  "ws:",
        "https:": "wss:"
    };

    var STATE_IDLE          = 0;
    var STATE_CONNECTED     = 1;
    var STATE_DISCONNECTED  = 2;

    var currentState = STATE_IDLE;
    
    // Transform current URL to WebSocket URL

    // If not already a websocket URL
    if (   tunnelURL.substring(0, 3) != "ws:"
        && tunnelURL.substring(0, 4) != "wss:") {

        var protocol = ws_protocol[window.location.protocol];

        // If absolute URL, convert to absolute WS URL
        if (tunnelURL.substring(0, 1) == "/")
            tunnelURL =
                protocol
                + "//" + window.location.host
                + tunnelURL;

        // Otherwise, construct absolute from relative URL
        else {

            // Get path from pathname
            var slash = window.location.pathname.lastIndexOf("/");
            var path  = window.location.pathname.substring(0, slash + 1);

            // Construct absolute URL
            tunnelURL =
                protocol
                + "//" + window.location.host
                + path
                + tunnelURL;

        }

    }

    this.sendMessage = function(message) {

        // Do not attempt to send messages if not connected
        if (currentState != STATE_CONNECTED)
            return;

        socket.send(message);

    };

    this.connect = function(data) {

        // Connect socket
        socket = new WebSocket(tunnelURL + "?" + data, "guacamole");

        socket.onopen = function(event) {
            currentState = STATE_CONNECTED;
        };

        socket.onmessage = function(event) {

            var message = event.data;

            var instructions = message.split(";");
            for (var i=0; i<instructions.length; i++) {

                var instruction = instructions[i];

                var opcodeEnd = instruction.indexOf(":");
                if (opcodeEnd == -1)
                    opcodeEnd = instruction.length;

                var opcode = instruction.substring(0, opcodeEnd);
                var parameters = instruction.substring(opcodeEnd+1).split(",");

                if (tunnel.oninstruction)
                    tunnel.oninstruction(opcode, parameters);

            }

        };

    };

    this.disconnect = function() {
        currentState = STATE_DISCONNECTED;
        socket.close();
    };

};

Guacamole.WebSocketTunnel.prototype = new Guacamole.Tunnel();

