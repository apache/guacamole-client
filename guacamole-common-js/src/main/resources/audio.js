
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
     * Packet queue.
     */
    var packets = [];

    /**
     * Whether this channel is currently playing sound.
     */
    var playing = false;

    /**
     * Advances to the next audio packet, if any, and plays it.
     */
    function advance() {

        // If packets remain, play next
        if (packets.length != 0) {
            var packet = packets.shift();
            packet.play();
            window.setTimeout(advance, packet.duration);
        }

        // Otherwise, no longer playing
        else
            playing = false;

    }

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
            new Guacamole.AudioChannel.Packet(mimetype, duration, data);

        // If currently playing sound, add packet to queue
        if (playing)
            packets.push(packet);

        // Otherwise, play now, flag channel as playing
        else {
            playing = true;
            packet.play();
            window.setTimeout(advance, packet.duration);
        }

    };

};

/**
 * Abstract representation of an audio packet.
 * 
 * @constructor
 * 
 * @param {String} mimetype The mimetype of the data contained by this packet.
 * @param {Number} duration The duration of the data contained by this packet.
 * @param {String} data The base64-encoded sound data contained by this packet.
 */
Guacamole.AudioChannel.Packet = function(mimetype, duration, data) {

    // Build data URI
    var data_uri = "data:" + mimetype + ";base64," + data;
   
    // Create audio element to house and play the data
    var audio = new Audio();
    audio.src = data_uri;
  
    /**
     * The duration of this packet, in milliseconds.
     */
    this.duration = duration;

    /**
     * Plays the sound data contained by this packet immediately.
     */
    this.play = function() {
        audio.play();
    };

};
