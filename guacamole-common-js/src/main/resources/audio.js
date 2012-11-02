
/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is guacamole-common-js.
 *
 * The Initial Developer of the Original Code is
 * Michael Jumper.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

/**
 * Namespace for all Guacamole JavaScript objects.
 * @namespace
 */
var Guacamole = Guacamole || {};

/**
 * Abstract audio channel which queues and plays arbitrary audio data.
 * @constructor
 */
Guacamole.AudioChannel = function() {

    /**
     * Reference to this AudioChannel.
     * @private
     */
    var channel = this;

    /**
     * When the next packet should play.
     */
    var next_packet_time = 0;

    /**
     * Queues up the given data for playing by this channel once all previously
     * queued data has been played. If no data has been queued, the data will
     * play immediately.
     * 
     * @param {String} mimetype The mimetype of the data provided.
     * @param {Number} duration The duration of the data provided, in
     *                          milliseconds.
     * @param {String} data The base64-encoded data to play.
     */
    this.play = function(mimetype, duration, data) {

        var packet =
            new Guacamole.AudioChannel.Packet(mimetype, data);

        var now;
        if (Guacamole.AudioChannel.context)
            now = Guacamole.AudioChannel.context.currentTime * 1000;
        else
            now = new Date().getTime();

        // If underflow is detected, delay start
        if (next_packet_time < now)
            next_packet_time = now + 50;

        // Schedule next packet
        packet.play(next_packet_time);
        next_packet_time += duration;

    };

};

// Define context if available
if (window.webkitAudioContext) {
    Guacamole.AudioChannel.context = new webkitAudioContext();
}

/**
 * Abstract representation of an audio packet.
 * 
 * @constructor
 * 
 * @param {String} mimetype The mimetype of the data contained by this packet.
 * @param {String} data The base64-encoded sound data contained by this packet.
 */
Guacamole.AudioChannel.Packet = function(mimetype, data) {

    // If audio API available, use it.
    if (Guacamole.AudioChannel.context) {

        var readyBuffer = null;

        // By default, when decoding finishes, store buffer for future
        // playback
        var handleReady = function(buffer) {
            readyBuffer = buffer;
        };

        // Convert to ArrayBuffer
        var binary = window.atob(data);
        var arrayBuffer = new ArrayBuffer(binary.length);
        var bufferView = new Uint8Array(arrayBuffer);

        for (var i=0; i<binary.length; i++)
            bufferView[i] = binary.charCodeAt(i);

        // Get context and start decoding
        Guacamole.AudioChannel.context.decodeAudioData(
            arrayBuffer,
            function(buffer) { handleReady(buffer); }
        );

        // Set up buffer source
        var source = Guacamole.AudioChannel.context.createBufferSource();
        source.connect(Guacamole.AudioChannel.context.destination);

        var play_when;

        function playDelayed(buffer) {
            source.buffer = buffer;
            source.noteOn(play_when / 1000);
        }

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

        // Build data URI
        var data_uri = "data:" + mimetype + ";base64," + data;
       
        // Create audio element to house and play the data
        var audio = new Audio();
        audio.src = data_uri;
      
        /**
         * Plays the sound data contained by this packet immediately.
         */
        this.play = function(when) {
            
            // Calculate time until play
            var now = new Date().getTime();
            var delay = when - now;
            
            // Play now if too late
            if (delay < 0)
                audio.play();

            // Otherwise, schedule later playback
            else
                window.setTimeout(function() {
                    audio.play();
                }, delay);

        };

    }

};
