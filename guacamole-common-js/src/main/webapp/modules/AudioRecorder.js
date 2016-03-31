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
 * Abstract audio recorder which streams arbitrary audio data to an underlying
 * Guacamole.OutputStream. It is up to implementations of this class to provide
 * some means of handling this Guacamole.OutputStream. Data produced by the
 * recorder is to be sent along the provided stream immediately.
 *
 * @constructor
 */
Guacamole.AudioRecorder = function AudioRecorder() {

    // AudioRecorder currently provides no functions

};

/**
 * Determines whether the given mimetype is supported by any built-in
 * implementation of Guacamole.AudioRecorder, and thus will be properly handled
 * by Guacamole.AudioRecorder.getInstance().
 *
 * @param {String} mimetype
 *     The mimetype to check.
 *
 * @returns {Boolean}
 *     true if the given mimetype is supported by any built-in
 *     Guacamole.AudioRecorder, false otherwise.
 */
Guacamole.AudioRecorder.isSupportedType = function isSupportedType(mimetype) {

    return Guacamole.RawAudioRecorder.isSupportedType(mimetype);

};

/**
 * Returns a list of all mimetypes supported by any built-in
 * Guacamole.AudioRecorder, in rough order of priority. Beware that only the
 * core mimetypes themselves will be listed. Any mimetype parameters, even
 * required ones, will not be included in the list. For example, "audio/L8" is
 * a supported raw audio mimetype that is supported, but it is invalid without
 * additional parameters. Something like "audio/L8;rate=44100" would be valid,
 * however (see https://tools.ietf.org/html/rfc4856).
 *
 * @returns {String[]}
 *     A list of all mimetypes supported by any built-in
 *     Guacamole.AudioRecorder, excluding any parameters.
 */
Guacamole.AudioRecorder.getSupportedTypes = function getSupportedTypes() {

    return Guacamole.RawAudioRecorder.getSupportedTypes();

};

/**
 * Returns an instance of Guacamole.AudioRecorder providing support for the
 * given audio format. If support for the given audio format is not available,
 * null is returned.
 *
 * @param {Guacamole.OutputStream} stream
 *     The Guacamole.OutputStream to send audio data through.
 *
 * @param {String} mimetype
 *     The mimetype of the audio data to be sent along the provided stream.
 *
 * @return {Guacamole.AudioRecorder}
 *     A Guacamole.AudioRecorder instance supporting the given mimetype and
 *     writing to the given stream, or null if support for the given mimetype
 *     is absent.
 */
Guacamole.AudioRecorder.getInstance = function getInstance(stream, mimetype) {

    // Use raw audio recorder if possible
    if (Guacamole.RawAudioRecorder.isSupportedType(mimetype))
        return new Guacamole.RawAudioRecorder(stream, mimetype);

    // No support for given mimetype
    return null;

};

/**
 * Implementation of Guacamole.AudioRecorder providing support for raw PCM
 * format audio. This recorder relies only on the Web Audio API and does not
 * require any browser-level support for its audio formats.
 *
 * @constructor
 * @augments Guacamole.AudioRecorder
 * @param {Guacamole.OutputStream} stream
 *     The Guacamole.OutputStream to write audio data to.
 *
 * @param {String} mimetype
 *     The mimetype of the audio data to send along the provided stream, which
 *     must be a "audio/L8" or "audio/L16" mimetype with necessary parameters,
 *     such as: "audio/L16;rate=44100,channels=2".
 */
Guacamole.RawAudioRecorder = function RawAudioRecorder(stream, mimetype) {

    /**
     * The format of audio this recorder will encode.
     *
     * @private
     * @type {Guacamole.RawAudioFormat}
     */
    var format = Guacamole.RawAudioFormat.parse(mimetype);

    /**
     * An instance of a Web Audio API AudioContext object, or null if the
     * Web Audio API is not supported.
     *
     * @private
     * @type {AudioContext}
     */
    var context = Guacamole.AudioContextFactory.getAudioContext();

    /**
     * A function which directly invokes the browser's implementation of
     * navigator.getUserMedia() with all provided parameters.
     *
     * @type Function
     */
    var getUserMedia = (navigator.getUserMedia
            || navigator.webkitGetUserMedia
            || navigator.mozGetUserMedia
            || navigator.msGetUserMedia).bind(navigator);

    /**
     * Guacamole.ArrayBufferWriter wrapped around the audio output stream
     * provided when this Guacamole.RawAudioRecorder was created.
     *
     * @private
     * @type {Guacamole.ArrayBufferWriter}
     */
    var writer = new Guacamole.ArrayBufferWriter(stream);

    /**
     * The type of typed array that will be used to represent each audio packet
     * internally. This will be either Int8Array or Int16Array, depending on
     * whether the raw audio format is 8-bit or 16-bit.
     *
     * @private
     * @constructor
     */
    var SampleArray = (format.bytesPerSample === 1) ? window.Int8Array : window.Int16Array;

    /**
     * The maximum absolute value of any sample within a raw audio packet sent
     * by this audio recorder. This depends only on the size of each sample,
     * and will be 128 for 8-bit audio and 32768 for 16-bit audio.
     *
     * @private
     * @type {Number}
     */
    var maxSampleValue = (format.bytesPerSample === 1) ? 128 : 32768;

    /**
     * The size of audio buffer to request from the Web Audio API when
     * recording audio. This must be a power of two between 256 and 16384
     * inclusive, as required by AudioContext.createScriptProcessor().
     *
     * @private
     * @type {Number}
     */
    var bufferSize = format.bytesPerSample * 4096;

    /**
     * Converts the given AudioBuffer into an audio packet, ready for streaming
     * along the underlying output stream. Unlike the raw audio packets used by
     * this audio recorder, AudioBuffers require floating point samples and are
     * split into isolated planes of channel-specific data.
     *
     * @private
     * @param {AudioBuffer} audioBuffer
     *     The Web Audio API AudioBuffer that should be converted to a raw
     *     audio packet.
     *
     * @returns {SampleArray}
     *     A new raw audio packet containing the audio data from the provided
     *     AudioBuffer.
     */
    var toSampleArray = function toSampleArray(audioBuffer) {

        // Get array for raw PCM storage
        var data = new SampleArray(audioBuffer.length);

        // Convert each channel
        for (var channel = 0; channel < format.channels; channel++) {

            var audioData = audioBuffer.getChannelData(channel);

            // Fill array with data from audio buffer channel
            var offset = channel;
            for (var i = 0; i < audioData.length; i++) {
                data[offset] = audioData[i] * maxSampleValue;
                offset += format.channels;
            }

        }

        return data;

    };

    // Once audio stream is successfully open, request and begin reading audio
    writer.onack = function audioStreamAcknowledged(status) {

        // Abort stream if rejected
        if (status.code !== Guacamole.Status.Code.SUCCESS) {
            writer.sendEnd();
            return;
        }

        // Attempt to retrieve an audio input stream from the browser
        getUserMedia({ 'audio' : true }, function streamReceived(mediaStream) {

            // Create processing node which receives appropriately-sized audio buffers
            var processor = context.createScriptProcessor(bufferSize, format.channels, format.channels);
            processor.connect(context.destination);

            // Send blobs when audio buffers are received
            processor.onaudioprocess = function processAudio(e) {
                writer.sendData(toSampleArray(e.inputBuffer));
            };

            // Connect processing node to user's audio input source
            var source = context.createMediaStreamSource(mediaStream);
            source.connect(processor);

        }, function streamDenied() {

            // Simply end stream if audio access is not allowed
            writer.sendEnd();

        });

    };

};

Guacamole.RawAudioRecorder.prototype = new Guacamole.AudioRecorder();

/**
 * Determines whether the given mimetype is supported by
 * Guacamole.RawAudioRecorder.
 *
 * @param {String} mimetype
 *     The mimetype to check.
 *
 * @returns {Boolean}
 *     true if the given mimetype is supported by Guacamole.RawAudioRecorder,
 *     false otherwise.
 */
Guacamole.RawAudioRecorder.isSupportedType = function isSupportedType(mimetype) {

    // No supported types if no Web Audio API
    if (!Guacamole.AudioContextFactory.getAudioContext())
        return false;

    return Guacamole.RawAudioFormat.parse(mimetype) !== null;

};

/**
 * Returns a list of all mimetypes supported by Guacamole.RawAudioRecorder. Only
 * the core mimetypes themselves will be listed. Any mimetype parameters, even
 * required ones, will not be included in the list. For example, "audio/L8" is
 * a raw audio mimetype that may be supported, but it is invalid without
 * additional parameters. Something like "audio/L8;rate=44100" would be valid,
 * however (see https://tools.ietf.org/html/rfc4856).
 *
 * @returns {String[]}
 *     A list of all mimetypes supported by Guacamole.RawAudioRecorder,
 *     excluding any parameters. If the necessary JavaScript APIs for recording
 *     raw audio are absent, this list will be empty.
 */
Guacamole.RawAudioRecorder.getSupportedTypes = function getSupportedTypes() {

    // No supported types if no Web Audio API
    if (!Guacamole.AudioContextFactory.getAudioContext())
        return [];

    // We support 8-bit and 16-bit raw PCM
    return [
        'audio/L8',
        'audio/L16'
    ];

};
