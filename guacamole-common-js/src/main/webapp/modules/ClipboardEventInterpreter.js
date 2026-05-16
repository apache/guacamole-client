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
 * An interpreter for clipboard events within a Guacamole session recording.
 * Clipboard data arrives as a sequence of instructions: clipboard (declares
 * stream and mimetype), blob (contains base64 data), and end (terminates stream).
 *
 * @constructor
 * @param {number} [startTimestamp=0]
 *     The starting timestamp for the recording. Event timestamps will be
 *     relative to this value.
 */
Guacamole.ClipboardEventInterpreter = function ClipboardEventInterpreter(startTimestamp) {

    if (startTimestamp === undefined || startTimestamp === null)
        startTimestamp = 0;

    /**
     * All clipboard events parsed so far.
     *
     * @private
     * @type {!Guacamole.ClipboardEventInterpreter.ClipboardEvent[]}
     */
    var parsedEvents = [];

    /**
     * Map of active clipboard streams, keyed by stream index.
     * Each entry tracks the mimetype and accumulated base64 data.
     *
     * @private
     * @type {Object.<string, {mimetype: string, data: string, timestamp: number}>}
     */
    var activeStreams = {};

    /**
     * The timestamp of the most recent instruction, used for events
     * that don't have their own timestamp.
     *
     * @private
     * @type {number}
     */
    var lastTimestamp = 0;

    /**
     * Updates the last known timestamp.
     *
     * @param {number} timestamp
     *     The absolute timestamp from a sync instruction or key event.
     */
    this.setTimestamp = function setTimestamp(timestamp) {
        lastTimestamp = timestamp;
    };

    /**
     * Handles a clipboard instruction, which begins a new clipboard stream.
     *
     * @param {!string[]} args
     *     The arguments: [stream_index, mimetype]
     */
    this.handleClipboard = function handleClipboard(args) {
        var streamIndex = args[0];
        var mimetype = args[1];

        activeStreams[streamIndex] = {
            mimetype: mimetype,
            data: '',
            timestamp: lastTimestamp
        };
    };

    /**
     * Handles a blob instruction, which contains base64-encoded data
     * for an active stream.
     *
     * @param {!string[]} args
     *     The arguments: [stream_index, base64_data]
     */
    this.handleBlob = function handleBlob(args) {
        var streamIndex = args[0];
        var base64Data = args[1];

        var stream = activeStreams[streamIndex];
        if (stream)
            stream.data += base64Data;
    };

    /**
     * Handles an end instruction, which completes a clipboard stream
     * and creates the final clipboard event.
     *
     * @param {!string[]} args
     *     The arguments: [stream_index]
     */
    this.handleEnd = function handleEnd(args) {
        var streamIndex = args[0];

        var stream = activeStreams[streamIndex];
        if (stream) {
            // Decode the base64 data
            var decodedData = '';
            try {
                decodedData = atob(stream.data);
                // Handle UTF-8 decoding
                decodedData = decodeURIComponent(escape(decodedData));
            } catch (e) {
                // If decoding fails, use raw decoded data or mark as binary
                try {
                    decodedData = atob(stream.data);
                } catch (e2) {
                    decodedData = '[Binary data]';
                }
            }

            // Create the clipboard event
            parsedEvents.push(new Guacamole.ClipboardEventInterpreter.ClipboardEvent({
                mimetype: stream.mimetype,
                data: decodedData,
                timestamp: stream.timestamp - startTimestamp
            }));

            // Clean up the stream
            delete activeStreams[streamIndex];
        }
    };

    /**
     * Returns all parsed clipboard events.
     *
     * @returns {Guacamole.ClipboardEventInterpreter.ClipboardEvent[]}
     *     All clipboard events parsed so far.
     */
    this.getEvents = function getEvents() {
        return parsedEvents;
    };

};

/**
 * A single clipboard event from a recording.
 *
 * @constructor
 * @param {Guacamole.ClipboardEventInterpreter.ClipboardEvent|object} [template={}]
 *     The object whose properties should be copied.
 */
Guacamole.ClipboardEventInterpreter.ClipboardEvent = function ClipboardEvent(template) {

    template = template || {};

    /**
     * The mimetype of the clipboard data.
     *
     * @type {!string}
     */
    this.mimetype = template.mimetype || 'text/plain';

    /**
     * The clipboard content (decoded from base64).
     *
     * @type {!string}
     */
    this.data = template.data || '';

    /**
     * The timestamp when this clipboard event occurred, relative to
     * the start of the recording.
     *
     * @type {!number}
     */
    this.timestamp = template.timestamp || 0;

};