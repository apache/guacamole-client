/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

var Guacamole = Guacamole || {};

/**
 * A writer which automatically writes to the given output stream with the
 * contents of a local files, supplied as standard File objects.
 *
 * @constructor
 * @param {Guacamole.OutputStream} stream
 *     The stream that data will be written to.
 */
Guacamole.FileWriter = function FileWriter(stream) {

    /**
     * Reference to this Guacamole.FileWriter.
     *
     * @private
     * @type {Guacamole.FileWriter}
     */
    var guacWriter = this;

    /**
     * Wrapped Guacamole.ArrayBufferWriter which will be used to send any
     * provided file data.
     *
     * @private
     * @type {Guacamole.ArrayBufferWriter}
     */
    var arrayBufferWriter = new Guacamole.ArrayBufferWriter(stream);

    // Initially, simply call onack for acknowledgements
    arrayBufferWriter.onack = function(status) {
        if (guacWriter.onack)
            guacWriter.onack(status);
    };

    /**
     * Browser-independent implementation of Blob.slice() which uses an end
     * offset to determine the span of the resulting slice, rather than a
     * length.
     *
     * @private
     * @param {Blob} blob
     *     The Blob to slice.
     *
     * @param {Number} start
     *     The starting offset of the slice, in bytes, inclusive.
     *
     * @param {Number} end
     *     The ending offset of the slice, in bytes, exclusive.
     *
     * @returns {Blob}
     *     A Blob containing the data within the given Blob starting at
     *     <code>start</code> and ending at <code>end - 1</code>.
     */
    var slice = function slice(blob, start, end) {

        // Use prefixed implementations if necessary
        var sliceImplementation = (
                blob.slice
             || blob.webkitSlice
             || blob.mozSlice
        ).bind(blob);

        var length = end - start;

        // The old Blob.slice() was length-based (not end-based). Try the
        // length version first, if the two calls are not equivalent.
        if (length !== end) {

            // If the result of the slice() call matches the expected length,
            // trust that result. It must be correct.
            var sliceResult = sliceImplementation(start, length);
            if (sliceResult.size === length)
                return sliceResult;

        }

        // Otherwise, use the most-recent standard: end-based slice()
        return sliceImplementation(start, end);

    };

    /**
     * Sends the contents of the given file over the underlying stream.
     *
     * @param {File} file
     *     The file to send.
     */
    this.sendFile = function sendFile(file) {

        var offset = 0;
        var reader = new FileReader();

        /**
         * Reads the next chunk of the file provided to
         * [sendFile()]{@link Guacamole.FileWriter#sendFile}. The chunk itself
         * is read asynchronously, and will not be available until
         * reader.onload fires.
         *
         * @private
         */
        var readNextChunk = function readNextChunk() {

            // If no further chunks remain, inform of completion and stop
            if (offset >= file.size) {

                // Fire completion event for completed file
                if (guacWriter.oncomplete)
                    guacWriter.oncomplete(file);

                // No further chunks to read
                return;

            }

            // Obtain reference to next chunk as a new blob
            var chunk = slice(file, offset, offset + arrayBufferWriter.blobLength);
            offset += arrayBufferWriter.blobLength;

            // Attempt to read the file contents represented by the blob into
            // a new array buffer
            reader.readAsArrayBuffer(chunk);

        };

        // Send each chunk over the stream, continue reading the next chunk
        reader.onload = function chunkLoadComplete() {

            // Send the successfully-read chunk
            arrayBufferWriter.sendData(reader.result);

            // Continue sending more chunks after the latest chunk is
            // acknowledged
            arrayBufferWriter.onack = function sendMoreChunks(status) {

                if (guacWriter.onack)
                    guacWriter.onack(status);

                // Abort transfer if an error occurs
                if (status.isError())
                    return;

                // Inform of file upload progress via progress events
                if (guacWriter.onprogress)
                    guacWriter.onprogress(file, offset - arrayBufferWriter.blobLength);

                // Queue the next chunk for reading
                readNextChunk();

            };

        };

        // If an error prevents further reading, inform of error and stop
        reader.onerror = function chunkLoadFailed() {

            // Fire error event, including the context of the error
            if (guacWriter.onerror)
                guacWriter.onerror(file, offset, reader.error);

        };

        // Begin reading the first chunk
        readNextChunk();

    };

    /**
     * Signals that no further text will be sent, effectively closing the
     * stream.
     */
    this.sendEnd = function sendEnd() {
        arrayBufferWriter.sendEnd();
    };

    /**
     * Fired for received data, if acknowledged by the server.
     *
     * @event
     * @param {Guacamole.Status} status
     *     The status of the operation.
     */
    this.onack = null;

    /**
     * Fired when an error occurs reading a file passed to
     * [sendFile()]{@link Guacamole.FileWriter#sendFile}. The file transfer for
     * the given file will cease, but the stream will remain open.
     *
     * @event
     * @param {File} file
     *     The file that was being read when the error occurred.
     *
     * @param {Number} offset
     *     The offset of the failed read attempt within the file, in bytes.
     *
     * @param {DOMError} error
     *     The error that occurred.
     */
    this.onerror = null;

    /**
     * Fired for each successfully-read chunk of file data as a file is being
     * sent via [sendFile()]{@link Guacamole.FileWriter#sendFile}.
     *
     * @event
     * @param {File} file
     *     The file that is being read.
     *
     * @param {Number} offset
     *     The offset of the read that just succeeded.
     */
    this.onprogress = null;

    /**
     * Fired when a file passed to
     * [sendFile()]{@link Guacamole.FileWriter#sendFile} has finished being
     * sent.
     *
     * @event
     * @param {File} file
     *     The file that was sent.
     */
    this.oncomplete = null;

};
