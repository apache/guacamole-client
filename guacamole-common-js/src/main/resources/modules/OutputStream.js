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
 * Abstract stream which can receive data.
 * 
 * @constructor
 * @param {Guacamole.Client} client The client owning this stream.
 * @param {Number} index The index of this stream.
 */
Guacamole.OutputStream = function(client, index) {

    /**
     * Reference to this stream.
     * @private
     */
    var guac_stream = this;

    /**
     * The index of this stream.
     * @type Number
     */
    this.index = index;

    /**
     * Fired when the stream is being closed due to an error.
     * 
     * @param {String} reason A human-readable reason describing the error.
     * @param {Number} code The error code associated with the error.
     */
    this.onerror = null;

    /**
     * Fired whenever an acknowledgement is received from the server, indicating
     * that a stream operation has completed.
     * 
     * @event
     * @param {String} message A human-readable status message related to the
     *                         operation performed.
     * @param {Number} code The error code associated with the operation.
     */
    this.onack = null;

    /**
     * Writes the given base64-encoded data to this stream as a blob.
     * 
     * @param {String} data The base64-encoded data to send.
     */
    this.write = function(data) {
        client.sendBlob(guac_stream.index, data);
    };

    /**
     * Closes this stream.
     */
    this.close = function() {
        client.endStream(guac_stream.index);
    };

};
