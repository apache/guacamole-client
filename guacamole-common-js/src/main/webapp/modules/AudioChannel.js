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
 * Abstract audio channel which queues and plays arbitrary audio data.
 *
 * @constructor
 */
Guacamole.AudioChannel = function AudioChannel() {

    /**
     * Reference to this AudioChannel.
     *
     * @private
     * @type Guacamole.AudioChannel
     */
    var channel = this;

    /**
     * The earliest possible time that the next packet could play without
     * overlapping an already-playing packet, in milliseconds.
     *
     * @private
     * @type Number
     */
    var nextPacketTime = Guacamole.AudioChannel.getTimestamp();

    /**
     * The last time that sync() was called, in milliseconds. If sync() has
     * never been called, this will be the time the Guacamole.AudioChannel
     * was created.
     *
     * @type Number
     */
    var lastSync = nextPacketTime;

    /**
     * Notifies this Guacamole.AudioChannel that all audio up to the current
     * point in time has been given via play(), and that any difference in time
     * between queued audio packets and the current time can be considered
     * latency.
     */
    this.sync = function sync() {

        // Calculate elapsed time since last sync
        var now = Guacamole.AudioChannel.getTimestamp();
        var elapsed = now - lastSync;

        // Reschedule future playback time such that playback latency is
        // bounded within the duration of the last audio frame
        nextPacketTime = Math.min(nextPacketTime, now + elapsed);

        // Record sync time
        lastSync = now;

    };

    /**
     * Queues up the given data for playing by this channel once all previously
     * queued data has been played. If no data has been queued, the data will
     * play immediately.
     * 
     * @param {String} mimetype
     *     The mimetype of the audio data provided.
     *
     * @param {Number} duration
     *     The duration of the data provided, in milliseconds.
     *
     * @param {Blob} data
     *     The blob of audio data to play.
     */
    this.play = function play(mimetype, duration, data) {

        var packet = new Guacamole.AudioChannel.Packet(mimetype, data);

        // Determine exactly when packet CAN play
        var packetTime = Guacamole.AudioChannel.getTimestamp();
        if (nextPacketTime < packetTime)
            nextPacketTime = packetTime;

        // Schedule packet
        packet.play(nextPacketTime);

        // Update timeline
        nextPacketTime += duration;

    };

};

// Define context if available
if (window.AudioContext) {
    try {Guacamole.AudioChannel.context = new AudioContext();}
    catch (e){}
}

// Fallback to Webkit-specific AudioContext implementation
else if (window.webkitAudioContext) {
    try {Guacamole.AudioChannel.context = new webkitAudioContext();}
    catch (e){}
}

/**
 * Returns a base timestamp which can be used for scheduling future audio
 * playback. Scheduling playback for the value returned by this function plus
 * N will cause the associated audio to be played back N milliseconds after
 * the function is called.
 *
 * @return {Number} An arbitrary channel-relative timestamp, in milliseconds.
 */
Guacamole.AudioChannel.getTimestamp = function() {

    // If we have an audio context, use its timestamp
    if (Guacamole.AudioChannel.context)
        return Guacamole.AudioChannel.context.currentTime * 1000;

    // If we have high-resolution timers, use those
    if (window.performance) {

        if (window.performance.now)
            return window.performance.now();

        if (window.performance.webkitNow)
            return window.performance.webkitNow();
        
    }

    // Fallback to millisecond-resolution system time
    return new Date().getTime();

};

/**
 * Abstract representation of an audio packet.
 * 
 * @constructor
 * 
 * @param {String} mimetype The mimetype of the data contained by this packet.
 * @param {Blob} data The blob of sound data contained by this packet.
 */
Guacamole.AudioChannel.Packet = function(mimetype, data) {

    /**
     * Schedules this packet for playback at the given time.
     *
     * @function
     * @param {Number} when The time this packet should be played, in
     *                      milliseconds.
     */
    this.play = function(when) { /* NOP */ }; // Defined conditionally depending on support

    // If audio API available, use it.
    if (Guacamole.AudioChannel.context) {

        var readyBuffer = null;

        // By default, when decoding finishes, store buffer for future
        // playback
        var handleReady = function(buffer) {
            readyBuffer = buffer;
        };

        // Read data and start decoding
        var reader = new FileReader();
        reader.onload = function() {
            Guacamole.AudioChannel.context.decodeAudioData(
                reader.result,
                function(buffer) { handleReady(buffer); }
            );
        };
        reader.readAsArrayBuffer(data);

        // Set up buffer source
        var source = Guacamole.AudioChannel.context.createBufferSource();
        source.connect(Guacamole.AudioChannel.context.destination);

        // Use noteOn() instead of start() if necessary
        if (!source.start)
            source.start = source.noteOn;

        var play_when;

        function playDelayed(buffer) {
            source.buffer = buffer;
            source.start(play_when / 1000);
        }

        /** @ignore */
        this.play = function(when) {
            
            play_when = when;
            
            // If buffer available, play it NOW
            if (readyBuffer)
                playDelayed(readyBuffer);

            // Otherwise, play when decoded
            else
                handleReady = playDelayed;

        };

    }

    else {

        var play_on_load = false;

        // Create audio element to house and play the data
        var audio = null;
        try { audio = new Audio(); }
        catch (e) {}

        if (audio) {

            // Read data and start decoding
            var reader = new FileReader();
            reader.onload = function() {

                var binary = "";
                var bytes = new Uint8Array(reader.result);

                // Produce binary string from bytes in buffer
                for (var i=0; i<bytes.byteLength; i++)
                    binary += String.fromCharCode(bytes[i]);

                // Convert to data URI 
                audio.src = "data:" + mimetype + ";base64," + window.btoa(binary);

                // Play if play was attempted but packet wasn't loaded yet
                if (play_on_load)
                    audio.play();

            };
            reader.readAsArrayBuffer(data);
       
            function play() {

                // If audio data is ready, play now
                if (audio.src)
                    audio.play();

                // Otherwise, play when loaded
                else
                    play_on_load = true;

            }
            
            /** @ignore */
            this.play = function(when) {
                
                // Calculate time until play
                var now = Guacamole.AudioChannel.getTimestamp();
                var delay = when - now;
                
                // Play now if too late
                if (delay < 0)
                    play();

                // Otherwise, schedule later playback
                else
                    window.setTimeout(play, delay);

            };

        }

    }

};
