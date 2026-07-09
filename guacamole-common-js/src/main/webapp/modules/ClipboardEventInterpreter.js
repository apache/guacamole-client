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
     * Clipboard transfer directions parsed from server-emitted "log"
     * instructions, keyed by stream index, awaiting the clipboard stream they
     * annotate. The server writes the direction log immediately before the
     * corresponding clipboard stream, so it is buffered here until the
     * matching clipboard instruction arrives.
     *
     * @private
     * @type {Object.<string, string>}
     */
    var pendingDirections = {};

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
            direction: pendingDirections[streamIndex] || null,
            timestamp: lastTimestamp
        };

        // The buffered direction, if any, has now been consumed
        delete pendingDirections[streamIndex];
    };

    /**
     * Handles a log instruction. When the log carries a clipboard direction
     * annotation ("clipboard stream=N direction=... mimetype=... [bytes=...]"),
     * the direction is buffered against its stream index so it can be attached
     * to the clipboard event once that stream arrives. All other log
     * instructions are ignored.
     *
     * @param {!string[]} args
     *     The arguments: [message]
     */
    this.handleLog = function handleLog(args) {
        var message = args[0];
        if (!message)
            return;

        var match = /^clipboard stream=(\d+) direction=(\S+)/.exec(message);
        if (match)
            pendingDirections[match[1]] = match[2];
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
     * Returns the number of decoded bytes represented by the given base64
     * string, without actually decoding it. Handles both padded and unpadded
     * base64; Guacamole blob payloads are newline-free, so length arithmetic
     * is exact.
     *
     * @private
     * @param {string} base64
     *     The base64-encoded string to measure.
     *
     * @returns {number}
     *     The size, in bytes, of the decoded data.
     */
    var base64ByteLength = function base64ByteLength(base64) {
        if (!base64)
            return 0;
        var padding = 0;
        if (base64.charAt(base64.length - 1) === '=') padding++;
        if (base64.charAt(base64.length - 2) === '=') padding++;
        return Math.max(0, Math.floor(base64.length * 3 / 4) - padding);
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

            var isImage = /^image\//i.test(stream.mimetype || '');

            var decodedData = '';
            var dataURL = null;
            var size = 0;

            // Image clipboard: keep the base64 payload intact and expose it
            // as a data: URL for inline preview. Do NOT run the text/UTF-8
            // decode used below - binary image bytes are not valid UTF-8 and
            // would otherwise collapse to the literal string '[Binary data]'.
            if (isImage) {
                // Lowercase the mimetype in the data: scheme - AngularJS's
                // img-src sanitizer whitelists "data:image/" case-sensitively,
                // so an uppercase mimetype would be rewritten to "unsafe:" and
                // silently fail to render.
                dataURL = 'data:' + (stream.mimetype || '').toLowerCase()
                        + ';base64,' + stream.data;
                size = base64ByteLength(stream.data);
            }

            // Text clipboard: decode the base64 data, then interpret as UTF-8
            else {
                try {
                    decodedData = atob(stream.data);
                    decodedData = decodeURIComponent(escape(decodedData));
                } catch (e) {
                    // If decoding fails, use raw decoded data or mark as binary
                    try {
                        decodedData = atob(stream.data);
                    } catch (e2) {
                        decodedData = '[Binary data]';
                    }
                }
            }

            // Create the clipboard event. A clipboard stream may arrive
            // before the first sync instruction has set lastTimestamp, in
            // which case stream.timestamp - startTimestamp is negative.
            // Clamp to 0 so pre-connection clipboard syncs land at the
            // start of the playback timeline rather than producing a
            // negative timestamp that breaks playerTimeService.formatTime().
            parsedEvents.push(new Guacamole.ClipboardEventInterpreter.ClipboardEvent({
                mimetype: stream.mimetype,
                data: decodedData,
                isImage: isImage,
                dataURL: dataURL,
                size: size,
                direction: stream.direction || null,
                timestamp: Math.max(0, stream.timestamp - startTimestamp)
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

    /**
     * Returns the number of clipboard streams that were opened but never
     * terminated by an "end" instruction. Such streams indicate clipboard
     * transfers that were only partially recorded (e.g. a truncated
     * recording), and whose data is therefore missing.
     *
     * @returns {!number}
     *     The number of incomplete clipboard streams still open at the end of
     *     parsing.
     */
    this.getIncompleteCount = function getIncompleteCount() {
        return Object.keys(activeStreams).length;
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
     * The clipboard content (decoded from base64). Empty for image clipboard
     * events, whose payload is exposed via dataURL instead.
     *
     * @type {!string}
     */
    this.data = template.data || '';

    /**
     * Whether this clipboard event carries image data (mimetype image/*)
     * rather than text.
     *
     * @type {!boolean}
     */
    this.isImage = template.isImage || false;

    /**
     * For image clipboard events, a data: URL suitable for direct use as an
     * <img> source. Null for text clipboard events.
     *
     * @type {string}
     */
    this.dataURL = template.dataURL || null;

    /**
     * The size, in bytes, of the clipboard payload. Only meaningful for
     * image clipboard events; 0 for text.
     *
     * @type {!number}
     */
    this.size = template.size || 0;

    /**
     * The direction of this clipboard transfer as annotated by the server,
     * either "guest-to-client" (data copied out of the guest) or
     * "client-to-guest" (data pasted into the guest), or null if the
     * recording carries no direction annotation.
     *
     * @type {string}
     */
    this.direction = template.direction || null;

    /**
     * The timestamp when this clipboard event occurred, relative to
     * the start of the recording.
     *
     * @type {!number}
     */
    this.timestamp = template.timestamp || 0;

};