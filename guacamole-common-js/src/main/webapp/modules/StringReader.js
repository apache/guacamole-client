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
 * A reader which automatically handles the given input stream, returning
 * strictly text data. Note that this object will overwrite any installed event
 * handlers on the given Guacamole.InputStream.
 * 
 * @constructor
 * @param {Guacamole.InputStream} stream The stream that data will be read
 *                                       from.
 */
Guacamole.StringReader = function(stream) {

    /**
     * Reference to this Guacamole.InputStream.
     * @private
     */
    var guac_reader = this;

    // Receive blobs as strings
    stream.onblob = function(data) {

        // Convert to string 
        var text = window.atob(data);

        // Call handler, if present
        if (guac_reader.ontext)
            guac_reader.ontext(text);

    };

    // Simply call onend when end received
    stream.onend = function() {
        if (guac_reader.onend)
            guac_reader.onend();
    };

    /**
     * Fired once for every blob of text data received.
     * 
     * @event
     * @param {String} text The data packet received.
     */
    this.ontext = null;

    /**
     * Fired once this stream is finished and no further data will be written.
     * @event
     */
    this.onend = null;

};