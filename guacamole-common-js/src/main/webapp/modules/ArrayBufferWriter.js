/*
 * Copyright (C) 2013 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

var Guacamole = Guacamole || {};

/**
 * A writer which automatically writes to the given output stream with arbitrary
 * binary data, supplied as ArrayBuffers.
 * 
 * @constructor
 * @param {Guacamole.OutputStream} stream The stream that data will be written
 *                                        to.
 */
Guacamole.ArrayBufferWriter = function(stream) {

    /**
     * Reference to this Guacamole.StringWriter.
     * @private
     */
    var guac_writer = this;

    // Simply call onack for acknowledgements
    stream.onack = function(status) {
        if (guac_writer.onack)
            guac_writer.onack(status);
    };

    /**
     * Sends the given data.
     * 
     * @param {ArrayBuffer} data The data to send.
     */
    this.sendData = function(data) {

        var bytes = new Uint8Array(data);
        var binary = "";

        // Produce binary string from bytes in buffer
        for (var i=0; i<bytes.byteLength; i++)
            binary += String.fromCharCode(bytes[i]);

        // Send as base64
        stream.sendBlob(window.btoa(binary));

    };

    /**
     * Signals that no further text will be sent, effectively closing the
     * stream.
     */
    this.sendEnd = function() {
        stream.sendEnd();
    };

    /**
     * Fired for received data, if acknowledged by the server.
     * @event
     * @param {Guacamole.Status} status The status of the operation.
     */
    this.onack = null;

};