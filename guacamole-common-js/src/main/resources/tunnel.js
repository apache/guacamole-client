
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

/**
 * Namespace for all Guacamole JavaScript objects.
 * @namespace
 */
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
     * @param {...} elements The elements of the message to send to the
     *                       service on the other side of the tunnel.
     */
    this.sendMessage = function(elements) {};
    
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
     * @private
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

    this.sendMessage = function() {

        // Do not attempt to send messages if not connected
        if (currentState != STATE_CONNECTED)
            return;

        // Do not attempt to send empty messages
        if (arguments.length == 0)
            return;

        /**
         * Converts the given value to a length/string pair for use as an
         * element in a Guacamole instruction.
         * 
         * @private
         * @param value The value to convert.
         * @return {String} The converted value. 
         */
        function getElement(value) {
            var string = new String(value);
            return string.length + "." + string; 
        }

        // Initialized message with first element
        var message = getElement(arguments[0]);

        // Append remaining elements
        for (var i=1; i<arguments.length; i++)
            message += "," + getElement(arguments[i]);

        // Final terminator
        message += ";";

        // Add message to buffer
        outputMessageBuffer += message;

        // Send if not currently sending
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
                if (message_xmlhttprequest.readyState == 4) {

                    // If an error occurs during send, handle it
                    if (message_xmlhttprequest.status != 200)
                        handleHTTPTunnelError(message_xmlhttprequest);

                    // Otherwise, continue the send loop
                    else
                        sendPendingMessages();

                }
            }

            message_xmlhttprequest.send(outputMessageBuffer);
            outputMessageBuffer = ""; // Clear buffer

        }
        else
            sendingMessages = false;

    }

    function getHTTPTunnelErrorMessage(xmlhttprequest) {

        var status = xmlhttprequest.status;

        // Special cases
        if (status == 0)   return "Disconnected";
        if (status == 200) return "Success";
        if (status == 403) return "Unauthorized";
        if (status == 404) return "Connection closed"; /* While it may be more
                                                        * accurate to say the
                                                        * connection does not
                                                        * exist, it is confusing
                                                        * to the user.
                                                        * 
                                                        * In general, this error
                                                        * will only happen when
                                                        * the tunnel does not
                                                        * exist, which happens
                                                        * after the connection
                                                        * is closed and the
                                                        * tunnel is detached.
                                                        */
        // Internal server errors
        if (status >= 500 && status <= 599) return "Server error";

        // Otherwise, unknown
        return "Unknown error";

    }

    function handleHTTPTunnelError(xmlhttprequest) {

        // Get error message
        var message = getHTTPTunnelErrorMessage(xmlhttprequest);

        // Call error handler
        if (tunnel.onerror) tunnel.onerror(message);

        // Finish
        tunnel.disconnect();

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

            // Do not parse response yet if not ready
            if (xmlhttprequest.readyState < 2) return;

            // Attempt to read status
            var status;
            try { status = xmlhttprequest.status; }

            // If status could not be read, assume successful.
            catch (e) { status = 200; }

            // Start next request as soon as possible IF request was successful
            if (nextRequest == null && status == 200)
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
                    handleHTTPTunnelError(xmlhttprequest);
                    return;
                }

                // Attempt to read in-progress data
                var current;
                try { current = xmlhttprequest.responseText; }

                // Do not attempt to parse if data could not be read
                catch (e) { return; }

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

    /**
     * Arbitrary integer, unique for each tunnel read request.
     * @private
     */
    var request_id = 0;

    function makeRequest() {

        // Make request, increment request ID
        var xmlhttprequest = new XMLHttpRequest();
        xmlhttprequest.open("GET", TUNNEL_READ + tunnel_uuid + ":" + (request_id++));
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
            var message = getHTTPTunnelErrorMessage(connect_xmlhttprequest);
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
     * @private
     */
    var tunnel = this;

    /**
     * The WebSocket used by this tunnel.
     * @private
     */
    var socket = null;

    /**
     * The WebSocket protocol corresponding to the protocol used for the current
     * location.
     * @private
     */
    var ws_protocol = {
        "http:":  "ws:",
        "https:": "wss:"
    };

    var status_code = {
        1000: "Connection closed normally.",
        1001: "Connection shut down.",
        1002: "Protocol error.",
        1003: "Invalid data.",
        1004: "[UNKNOWN, RESERVED]",
        1005: "No status code present.",
        1006: "Connection closed abnormally.",
        1007: "Inconsistent data type.",
        1008: "Policy violation.",
        1009: "Message too large.",
        1010: "Extension negotiation failed."
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

    this.sendMessage = function(elements) {

        // Do not attempt to send messages if not connected
        if (currentState != STATE_CONNECTED)
            return;

        // Do not attempt to send empty messages
        if (arguments.length == 0)
            return;

        /**
         * Converts the given value to a length/string pair for use as an
         * element in a Guacamole instruction.
         * 
         * @private
         * @param value The value to convert.
         * @return {String} The converted value. 
         */
        function getElement(value) {
            var string = new String(value);
            return string.length + "." + string; 
        }

        // Initialized message with first element
        var message = getElement(arguments[0]);

        // Append remaining elements
        for (var i=1; i<arguments.length; i++)
            message += "," + getElement(arguments[i]);

        // Final terminator
        message += ";";

        socket.send(message);

    };

    this.connect = function(data) {

        // Connect socket
        socket = new WebSocket(tunnelURL + "?" + data, "guacamole");

        socket.onopen = function(event) {
            currentState = STATE_CONNECTED;
        };

        socket.onclose = function(event) {

            // If connection closed abnormally, signal error.
            if (event.code != 1000 && tunnel.onerror)
                tunnel.onerror(status_code[event.code]);

        };
        
        socket.onerror = function(event) {

            // Call error handler
            if (tunnel.onerror) tunnel.onerror(event.data);

        };

        socket.onmessage = function(event) {

            var message = event.data;
            var startIndex = 0;
            var elementEnd;

            var elements = [];

            do {

                // Search for end of length
                var lengthEnd = message.indexOf(".", startIndex);
                if (lengthEnd != -1) {

                    // Parse length
                    var length = parseInt(message.substring(elementEnd+1, lengthEnd));

                    // Calculate start of element
                    startIndex = lengthEnd + 1;

                    // Calculate location of element terminator
                    elementEnd = startIndex + length;

                }
                
                // If no period, incomplete instruction.
                else
                    throw new Error("Incomplete instruction.");

                // We now have enough data for the element. Parse.
                var element = message.substring(startIndex, elementEnd);
                var terminator = message.substring(elementEnd, elementEnd+1);

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

            } while (startIndex < message.length);

        };

    };

    this.disconnect = function() {
        currentState = STATE_DISCONNECTED;
        socket.close();
    };

};

Guacamole.WebSocketTunnel.prototype = new Guacamole.Tunnel();


/**
 * Guacamole Tunnel which cycles between all specified tunnels until
 * no tunnels are left. Another tunnel is used if an error occurs but
 * no instructions have been received. If an instruction has been
 * received, or no tunnels remain, the error is passed directly out
 * through the onerror handler (if defined).
 * 
 * @constructor
 * @augments Guacamole.Tunnel
 * @param {...} tunnel_chain The tunnels to use, in order of priority.
 */
Guacamole.ChainedTunnel = function(tunnel_chain) {

    /**
     * Reference to this chained tunnel.
     * @private
     */
    var chained_tunnel = this;

    /**
     * The currently wrapped tunnel, if any.
     * @private
     */
    var current_tunnel = null;

    /**
     * Data passed in via connect(), to be used for
     * wrapped calls to other tunnels' connect() functions.
     * @private
     */
    var connect_data;

    /**
     * Array of all tunnels passed to this ChainedTunnel through the
     * constructor arguments.
     * @private
     */
    var tunnels = [];

    // Load all tunnels into array
    for (var i=0; i<arguments.length; i++)
        tunnels.push(arguments[i]);

    /**
     * Sets the current tunnel.
     * 
     * @private
     * @param {Guacamole.Tunnel} tunnel The tunnel to set as the current tunnel.
     */
    function attach(tunnel) {

        // Clear handlers of current tunnel, if any
        if (current_tunnel) {
            current_tunnel.onerror = null;
            current_tunnel.oninstruction = null;
        }

        // Set own functions to tunnel's functions
        chained_tunnel.disconnect    = tunnel.disconnect;
        chained_tunnel.sendMessage   = tunnel.sendMessage;
        
        // Record current tunnel
        current_tunnel = tunnel;

        // Wrap own oninstruction within current tunnel
        current_tunnel.oninstruction = function(opcode, elements) {
            
            // Invoke handler
            chained_tunnel.oninstruction(opcode, elements);

            // Use handler permanently from now on
            current_tunnel.oninstruction = chained_tunnel.oninstruction;

            // Pass through errors (without trying other tunnels)
            current_tunnel.onerror = chained_tunnel.onerror;
            
        }

        // Attach next tunnel on error
        current_tunnel.onerror = function(message) {

            // Get next tunnel
            var next_tunnel = tunnels.shift();

            // If there IS a next tunnel, try using it.
            if (next_tunnel)
                attach(next_tunnel);

            // Otherwise, call error handler
            else if (chained_tunnel.onerror)
                chained_tunnel.onerror(message);

        };

        try {
            
            // Attempt connection
            current_tunnel.connect(connect_data);
            
        }
        catch (e) {
            
            // Call error handler of current tunnel on error
            current_tunnel.onerror(e.message);
            
        }


    }

    this.connect = function(data) {
       
        // Remember connect data
        connect_data = data;

        // Get first tunnel
        var next_tunnel = tunnels.shift();

        // Attach first tunnel
        if (next_tunnel)
            attach(next_tunnel);

        // If there IS no first tunnel, error
        else if (chained_tunnel.onerror)
            chained_tunnel.onerror("No tunnels to try.");

    };
    
};

Guacamole.ChainedTunnel.prototype = new Guacamole.Tunnel();
