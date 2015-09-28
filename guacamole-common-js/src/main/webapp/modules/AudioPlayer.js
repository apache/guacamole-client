/*
 * Copyright (C) 2015 Glyptodon LLC
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
 * Abstract audio player which accepts, queues and plays back arbitrary audio
 * data. It is up to implementations of this class to provide some means of
 * handling a provided Guacamole.InputStream. Data received along the provided
 * stream is to be played back immediately.
 *
 * @constructor
 */
Guacamole.AudioPlayer = function AudioPlayer() {

    /**
     * Notifies this Guacamole.AudioPlayer that all audio up to the current
     * point in time has been given via the underlying stream, and that any
     * difference in time between queued audio data and the current time can be
     * considered latency.
     */
    this.sync = function sync() {
        // Default implementation - do nothing
    };

};

/**
 * Returns a base timestamp which can be used for scheduling future audio
 * playback. Scheduling playback for the value returned by this function plus
 * N will cause the associated audio to be played back N milliseconds after
 * the function is called.
 *
 * @return {Number}
 *     An arbitrary relative timestamp, in milliseconds.
 */
Guacamole.AudioPlayer.getTimestamp = function() {

    // If we have high-resolution timers, use those
    if (window.performance) {
        var now = performance.now || performance.webkitNow();
        return now();
    }

    // Fallback to millisecond-resolution system time
    return Date.now();

};

/**
 * Implementation of Guacamole.AudioPlayer providing support for raw PCM format
 * audio. This player relies only on the Web Audio API and does not require any
 * browser-level support for its audio formats.
 *
 * @constructor
 * @augments Guacamole.AudioPlayer
 * @param {Guacamole.InputStream} stream
 *     The Guacamole.InputStream to read audio data from.
 *
 * @param {String} mimetype
 *     The mimetype of the audio data in the provided stream, which must be a
 *     "audio/L8" or "audio/L16" mimetype with necessary parameters, such as:
 *     "audio/L16;rate=44100,channels=2".
 */
Guacamole.RawAudioPlayer = function RawAudioPlayer(stream, mimetype) {

    /**
     * The format of audio this player will decode.
     *
     * @type Guacamole.RawAudioPlayer._Format
     */
    var format = Guacamole.RawAudioPlayer._Format.parse(mimetype);

    /**
     * An instance of a Web Audio API AudioContext object, or null if the
     * Web Audio API is not supported.
     *
     * @type AudioContext
     */
    var context = (function getAudioContext() {

        // Fallback to Webkit-specific AudioContext implementation
        var AudioContext = window.AudioContext || window.webkitAudioContext;

        // Get new AudioContext instance if Web Audio API is supported
        if (AudioContext) {
            try {
                return new AudioContext();
            }
            catch (e) {
                // Do not use Web Audio API if not allowed by browser
            }
        }

        // Web Audio API not supported
        return null;

    })();

    /**
     * Returns a base timestamp which can be used for scheduling future audio
     * playback. Scheduling playback for the value returned by this function plus
     * N will cause the associated audio to be played back N milliseconds after
     * the function is called.
     *
     * @return {Number}
     *     An arbitrary relative timestamp, in milliseconds.
     */
    var getTimestamp = function getTimestamp() {

        // If we have an audio context, use its timestamp
        if (context)
            return context.currentTime * 1000;

        // Otherwise, use the internal timestamp implementation
        return Guacamole.AudioPlayer.getTimestamp();

    };

    /**
     * The earliest possible time that the next packet could play without
     * overlapping an already-playing packet, in milliseconds.
     *
     * @private
     * @type Number
     */
    var nextPacketTime = getTimestamp();

    /**
     * The last time that sync() was called, in milliseconds. If sync() has
     * never been called, this will be the time the Guacamole.AudioPlayer
     * was created.
     *
     * @type Number
     */
    var lastSync = nextPacketTime;

    /**
     * Guacamole.ArrayBufferReader wrapped around the audio input stream
     * provided with this Guacamole.RawAudioPlayer was created.
     *
     * @type Guacamole.ArrayBufferReader
     */
    var reader = new Guacamole.ArrayBufferReader(stream);

    // Play each received raw packet of audio immediately
    reader.ondata = function playReceivedAudio(data) {

        // Calculate total number of samples
        var samples = data.byteLength / format.channels / format.bytesPerSample;

        // Calculate overall duration (in milliseconds)
        var duration = samples * 1000 / format.rate;

        // Determine exactly when packet CAN play
        var packetTime = getTimestamp();
        if (nextPacketTime < packetTime)
            nextPacketTime = packetTime;

        // Obtain typed array view based on defined bytes per sample
        var maxValue;
        var source;
        if (format.bytesPerSample === 1) {
            source = new Int8Array(data);
            maxValue = 128;
        }
        else {
            source = new Int16Array(data);
            maxValue = 32768;
        }

        // Get audio buffer for specified format
        var audioBuffer = context.createBuffer(format.channels, samples, format.rate);

        // Convert each channel
        for (var channel = 0; channel < format.channels; channel++) {

            var audioData = audioBuffer.getChannelData(channel);

            // Fill audio buffer with data for channel
            var offset = channel;
            for (var i = 0; i < samples; i++) {
                audioData[i] = source[offset] / maxValue;
                offset += format.channels;
            }

        }

        // Set up buffer source
        var source = context.createBufferSource();
        source.connect(context.destination);

        // Use noteOn() instead of start() if necessary
        if (!source.start)
            source.start = source.noteOn;

        // Schedule packet
        source.buffer = audioBuffer;
        source.start(nextPacketTime / 1000);

        // Update timeline
        nextPacketTime += duration;

    };

    /** @override */
    this.sync = function sync() {

        // Calculate elapsed time since last sync
        var now = getTimestamp();
        var elapsed = now - lastSync;

        // Reschedule future playback time such that playback latency is
        // bounded within the duration of the last audio frame
        nextPacketTime = Math.min(nextPacketTime, now + elapsed);

        // Record sync time
        lastSync = now;

    };

};

Guacamole.RawAudioPlayer.prototype = new Guacamole.AudioPlayer();

/**
 * A description of the format of raw PCM audio received by a
 * Guacamole.RawAudioPlayer. This object describes the number of bytes per
 * sample, the number of channels, and the overall sample rate.
 *
 * @private
 * @constructor
 * @param {Guacamole.RawAudioPlayer._Format|Object} template
 *     The object whose properties should be copied into the corresponding
 *     properties of the new Guacamole.RawAudioPlayer._Format.
 */
Guacamole.RawAudioPlayer._Format = function _Format(template) {

    /**
     * The number of bytes in each sample of audio data. This value is
     * independent of the number of channels.
     *
     * @type Number
     */
    this.bytesPerSample = template.bytesPerSample;

    /**
     * The number of audio channels (ie: 1 for mono, 2 for stereo).
     *
     * @type Number
     */
    this.channels = template.channels;

    /**
     * The number of samples per second, per channel.
     *
     * @type Number
     */
    this.rate = template.rate;

};

/**
 * Parses the given mimetype, returning a new Guacamole.RawAudioPlayer._Format
 * which describes the type of raw audio data represented by that mimetype. If
 * the mimetype is not supported by Guacamole.RawAudioPlayer, null is returned.
 *
 * @private
 * @param {String} mimetype
 *     The audio mimetype to parse.
 *
 * @returns {Guacamole.RawAudioPlayer._Format}
 *     A new Guacamole.RawAudioPlayer._Format which describes the type of raw
 *     audio data represented by the given mimetype, or null if the given
 *     mimetype is not supported.
 */
Guacamole.RawAudioPlayer._Format.parse = function parseFormat(mimetype) {

    var bytesPerSample;

    // Rate is absolutely required - if null is still present later, the
    // mimetype must not be supported
    var rate = null;

    // Default for both "audio/L8" and "audio/L16" is one channel
    var channels = 1;

    // "audio/L8" has one byte per sample
    if (mimetype.substring(0, 9) === 'audio/L8;') {
        mimetype = mimetype.substring(9);
        bytesPerSample = 1;
    }

    // "audio/L16" has two bytes per sample
    else if (mimetype.substring(0, 10) === 'audio/L16;') {
        mimetype = mimetype.substring(10);
        bytesPerSample = 2;
    }

    // All other types are unsupported
    else
        return null;

    // Parse all parameters
    var parameters = mimetype.split(',');
    for (var i = 0; i < parameters.length; i++) {

        var parameter = parameters[i];

        // All parameters must have an equals sign separating name from value
        var equals = parameter.indexOf('=');
        if (equals === -1)
            return null;

        // Parse name and value from parameter string
        var name  = parameter.substring(0, equals);
        var value = parameter.substring(equals+1);

        // Handle each supported parameter
        switch (name) {

            // Number of audio channels
            case 'channels':
                channels = parseInt(value);
                break;

            // Sample rate
            case 'rate':
                rate = parseInt(value);
                break;

            // All other parameters are unsupported
            default:
                return null;

        }

    };

    // The rate parameter is required
    if (rate === null)
        return null;

    // Return parsed format details
    return new Guacamole.RawAudioPlayer._Format({
        bytesPerSample : bytesPerSample,
        channels       : channels,
        rate           : rate
    });

};

/**
 * Determines whether the given mimetype is supported by
 * Guacamole.RawAudioPlayer.
 *
 * @param {String} mimetype
 *     The mimetype to check.
 *
 * @returns {Boolean}
 *     true if the given mimetype is supported by Guacamole.RawAudioPlayer,
 *     false otherwise.
 */
Guacamole.RawAudioPlayer.isSupportedType = function isSupportedType(mimetype) {

    // No supported types if no Web Audio API
    if (!window.AudioContext && !window.webkitAudioContext)
        return false;

    return Guacamole.RawAudioPlayer._Format.parse(mimetype) !== null;

};

/**
 * Returns a list of all mimetypes supported by Guacamole.RawAudioPlayer. Only
 * the core mimetypes themselves will be listed. Any mimetype parameters, even
 * required ones, will not be included in the list. For example, "audio/L8" is
 * a raw audio mimetype that may be supported, but it is invalid without
 * additional parameters. Something like "audio/L8;rate=44100" would be valid,
 * however (see https://tools.ietf.org/html/rfc4856).
 *
 * @returns {String[]}
 *     A list of all mimetypes supported by Guacamole.RawAudioPlayer, excluding
 *     any parameters. If the necessary JavaScript APIs for playing raw audio
 *     are absent, this list will be empty.
 */
Guacamole.RawAudioPlayer.getSupportedTypes = function getSupportedTypes() {

    // No supported types if no Web Audio API
    if (!window.AudioContext && !window.webkitAudioContext)
        return [];

    // We support 8-bit and 16-bit raw PCM
    return [
        'audio/L8',
        'audio/L16'
    ];

};
