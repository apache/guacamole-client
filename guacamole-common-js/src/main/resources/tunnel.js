
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

function GuacamoleHTTPTunnel(tunnelURL) {

    var TUNNEL_CONNECT = tunnelURL + "?connect";
    var TUNNEL_READ    = tunnelURL + "?read";
    var TUNNEL_WRITE   = tunnelURL + "?write";

    var pollResponse = 1; // Default to polling - will be turned off automatically if not needed
    var instructionHandler = null;

    var sendingMessages = 0;
    var outputMessageBuffer = "";

    function sendMessage(message) {

        // Add event to queue, restart send loop if finished.
        outputMessageBuffer += message;
        if (sendingMessages == 0)
            sendPendingMessages();

    };

    function sendPendingMessages() {

        if (outputMessageBuffer.length > 0) {

            sendingMessages = 1;

            var message_xmlhttprequest = new XMLHttpRequest();
            message_xmlhttprequest.open("POST", TUNNEL_WRITE);
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


    function handleResponse(xmlhttprequest) {

        var interval = null;
        var nextRequest = null;

        var dataUpdateEvents = 0;
        var instructionStart = 0;
        var startIndex = 0;

        function parseResponse() {

            // Start next request as soon as possible
            if (xmlhttprequest.readyState >= 2 && nextRequest == null)
                nextRequest = makeRequest();

            // Parse stream when data is received and when complete.
            if (xmlhttprequest.readyState == 3 ||
                xmlhttprequest.readyState == 4) {

                // Also poll every 30ms (some browsers don't repeatedly call onreadystatechange for new data)
                if (pollResponse == 1) {
                    if (xmlhttprequest.readyState == 3 && interval == null)
                        interval = setInterval(parseResponse, 30);
                    else if (xmlhttprequest.readyState == 4 && interval != null)
                        clearInterval(interval);
                }

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

                        delete xmlhttprequest;
                        if (nextRequest)
                            handleResponse(nextRequest);

                        break;
                    }

                    // Call instruction handler.
                    if (instructionHandler != null)
                        instructionHandler(opcode, parameters);
                }

                // Start search at end of string.
                startIndex = current.length;

                delete instruction;
                delete parameters;

            }

        }

        // If response polling enabled, attempt to detect if still
        // necessary (via wrapping parseResponse())
        if (pollResponse == 1) {
            xmlhttprequest.onreadystatechange = function() {

                // If we receive two or more readyState==3 events,
                // there is no need to poll.
                if (xmlhttprequest.readyState == 3) {
                    dataUpdateEvents++;
                    if (dataUpdateEvents >= 2) {
                        pollResponse = 0;
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
        xmlhttprequest.open("POST", TUNNEL_READ);
        xmlhttprequest.send(null);

        return xmlhttprequest;

    }

    function connect() {

        // Start tunnel and connect synchronously
        var connect_xmlhttprequest = new XMLHttpRequest();
        connect_xmlhttprequest.open("POST", TUNNEL_CONNECT, false);
        connect_xmlhttprequest.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
        connect_xmlhttprequest.setRequestHeader("Content-length", 0);
        connect_xmlhttprequest.send(null);

        // Start reading data
        handleResponse(makeRequest());

    };

    // External API
    this.connect = connect;
    this.sendMessage = sendMessage;
    this.setInstructionHandler = function(handler) {
        instructionHandler = handler;
    }

    this.escapeGuacamoleString = function(str) {

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

    this.unescapeGuacamoleString = function(str) {

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

}
