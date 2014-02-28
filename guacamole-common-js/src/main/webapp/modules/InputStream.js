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
 * An input stream abstraction used by the Guacamole client to facilitate
 * transfer of files or other binary data.
 * 
 * @constructor
 * @param {String} mimetype The mimetype of the data this stream will receive.
 */
Guacamole.InputStream = function(mimetype) {

    /**
     * The mimetype of the data contained within this blob.
     */
    this.mimetype = mimetype;

    /**
     * Receives the given base64-encoded data.
     * 
     * @param {String} data The received base64 data.
     */
    this.receive = function(data) {};

    /**
     * Closes this Guacamole.InputStream such that no further data will be
     * written.
     */
    this.close = function() {};

};

/**
 * An input stream which receives all data packets as individual ArrayBuffer
 * objects.
 * 
 * @constructor
 * @param {String} mimetype The mimetype of the data this stream will receive.
 */
Guacamole.ArrayBufferInputStream = function(mimetype) {

    /**
     * Reference to this Guacamole.InputStream.
     * @private
     */
    var guac_stream = this;

    /*
     * This is an input stream.
     */
    Guacamole.InputStream.apply(this, [mimetype]);

    // Receive implementation
    this.receive = function(data) {

        // Convert to ArrayBuffer
        var binary = window.atob(data);
        var arrayBuffer = new ArrayBuffer(binary.length);
        var bufferView = new Uint8Array(arrayBuffer);

        for (var i=0; i<binary.length; i++)
            bufferView[i] = binary.charCodeAt(i);

        // Call handler, if present
        if (guac_stream.onreceive)
            guac_stream.onreceive(arrayBuffer);

    };

    // Close implementation
    this.close = function() {

        // Call handler, if present
        if (guac_stream.onclose)
            guac_stream.onclose();

        // NOTE: Currently not enforced.

    };

    /**
     * Fired once for every blob of data received.
     * 
     * @event
     * @param {ArrayBuffer} buffer The data packet received.
     */
    this.onreceive = null;

    /**
     * Fired once this stream is finished and no further data will be written.
     * @event
     */
    this.onclose = null;

};

Guacamole.ArrayBufferInputStream.prototype = new Guacamole.InputStream();

/**
 * An input stream which continuously builds a single blob by appending each
 * individual blob received. Only the size of each blob received is exposed.
 * 
 * @constructor
 * @augments Guacamole.InputStream
 * @param {String} mimetype The mimetype of the data this stream will receive.
 */
Guacamole.BlobInputStream = function(mimetype) {

    /**
     * Reference to this Guacamole.InputStream.
     * @private
     */
    var guac_stream = this;

    /**
     * The length of this Guacamole.InputStream in bytes.
     * @private
     */
    var length = 0;

    /*
     * This is an input stream.
     */
    Guacamole.InputStream.apply(this, [mimetype]);

    // Get blob builder
    var blob_builder;
    if      (window.BlobBuilder)       blob_builder = new BlobBuilder();
    else if (window.WebKitBlobBuilder) blob_builder = new WebKitBlobBuilder();
    else if (window.MozBlobBuilder)    blob_builder = new MozBlobBuilder();
    else
        blob_builder = new (function() {

            var blobs = [];

            /** @ignore */
            this.append = function(data) {
                blobs.push(new Blob([data], {"type": mimetype}));
            };

            /** @ignore */
            this.getBlob = function() {
                return new Blob(blobs, {"type": mimetype});
            };

        })();

    // Receive implementation
    this.receive = function(data) {

        // Convert to ArrayBuffer
        var binary = window.atob(data);
        var arrayBuffer = new ArrayBuffer(binary.length);
        var bufferView = new Uint8Array(arrayBuffer);

        for (var i=0; i<binary.length; i++)
            bufferView[i] = binary.charCodeAt(i);

        blob_builder.append(arrayBuffer);
        length += arrayBuffer.byteLength;

        // Call handler, if present
        if (guac_stream.onprogress)
            guac_stream.onprogress(arrayBuffer.byteLength);

    };

    // Close implementation
    this.close = function() {

        // Call handler, if present
        if (guac_stream.onclose)
            guac_stream.onclose();

        // NOTE: Currently not enforced.

    };

    /**
     * Returns the current length of this Guacamole.InputStream, in bytes.
     * @return {Number} The current length of this Guacamole.InputStream.
     */
    this.getLength = function() {
        return length;
    };

    /**
     * Returns the contents of this Guacamole.InputStream as a Blob.
     * @return {Blob} The contents of this Guacamole.InputStream.
     */
    this.getBlob = function() {
        return blob_builder.getBlob();
    };

    /**
     * Fired once for every blob of data received.
     * 
     * @event
     * @param {Number} length The number of bytes received.
     */
    this.onprogress = null;

    /**
     * Fired once this stream is finished and no further data will be written.
     * @event
     */
    this.onclose = null;

};

Guacamole.BlobInputStream.prototype = new Guacamole.InputStream();

/**
 * An input stream which receives strictly text data.
 * 
 * @constructor
 * @param {String} mimetype The mimetype of the data this stream will receive.
 */
Guacamole.StringInputStream = function(mimetype) {

    /**
     * Reference to this Guacamole.InputStream.
     * @private
     */
    var guac_stream = this;

    /*
     * This is an input stream.
     */
    Guacamole.InputStream.apply(this, [mimetype]);

    // Receive implementation
    this.receive = function(data) {

        // Convert to string 
        var text = window.atob(data);

        // Call handler, if present
        if (guac_stream.onreceive)
            guac_stream.onreceive(text);

    };

    // Close implementation
    this.close = function() {

        // Call handler, if present
        if (guac_stream.onclose)
            guac_stream.onclose();

        // NOTE: Currently not enforced.

    };

    /**
     * Fired once for every blob of data received.
     * 
     * @event
     * @param {String} text The data packet received.
     */
    this.onreceive = null;

    /**
     * Fired once this stream is finished and no further data will be written.
     * @event
     */
    this.onclose = null;

};

Guacamole.StringInputStream.prototype = new Guacamole.InputStream();
