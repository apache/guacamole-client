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
 * A recording of a Guacamole session. Given a {@link Guacamole.Tunnel}, the
 * Guacamole.SessionRecording automatically handles incoming Guacamole
 * instructions, storing them for playback. Playback of the recording may be
 * controlled through function calls to the Guacamole.SessionRecording, even
 * while the recording has not yet finished being created or downloaded.
 *
 * @constructor
 * @param {Guacamole.Tunnel} tunnel
 *     The Guacamole.Tunnel from which the instructions of the recording should
 *     be read.
 */
Guacamole.SessionRecording = function SessionRecording(tunnel) {

    /**
     * Reference to this Guacamole.SessionRecording.
     *
     * @private
     * @type {Guacamole.SessionRecording}
     */
    var recording = this;

    /**
     * The minimum number of characters which must have been read between
     * keyframes.
     *
     * @private
     * @constant
     * @type {Number}
     */
    var KEYFRAME_CHAR_INTERVAL = 16384;

    /**
     * The minimum number of milliseconds which must elapse between keyframes.
     *
     * @private
     * @constant
     * @type {Number}
     */
    var KEYFRAME_TIME_INTERVAL = 5000;

    /**
     * The maximum amount of time to spend in any particular seek operation
     * before returning control to the main thread, in milliseconds. Seek
     * operations exceeding this amount of time will proceed asynchronously.
     *
     * @private
     * @constant
     * @type {Number}
     */
    var MAXIMUM_SEEK_TIME = 5;

    /**
     * All frames parsed from the provided tunnel.
     *
     * @private
     * @type {Guacamole.SessionRecording._Frame[]}
     */
    var frames = [];

    /**
     * All instructions which have been read since the last frame was added to
     * the frames array.
     *
     * @private
     * @type {Guacamole.SessionRecording._Frame.Instruction[]}
     */
    var instructions = [];

    /**
     * The approximate number of characters which have been read from the
     * provided tunnel since the last frame was flagged for use as a keyframe.
     *
     * @private
     * @type {Number}
     */
    var charactersSinceLastKeyframe = 0;

    /**
     * The timestamp of the last frame which was flagged for use as a keyframe.
     * If no timestamp has yet been flagged, this will be 0.
     *
     * @private
     * @type {Number}
     */
    var lastKeyframeTimestamp = 0;

    /**
     * Tunnel which feeds arbitrary instructions to the client used by this
     * Guacamole.SessionRecording for playback of the session recording.
     *
     * @private
     * @type {Guacamole.SessionRecording._PlaybackTunnel}
     */
    var playbackTunnel = new Guacamole.SessionRecording._PlaybackTunnel();

    /**
     * Guacamole.Client instance used for visible playback of the session
     * recording.
     *
     * @private
     * @type {Guacamole.Client}
     */
    var playbackClient = new Guacamole.Client(playbackTunnel);

    /**
     * The current frame rendered within the playback client. If no frame is
     * yet rendered, this will be -1.
     *
     * @private
     * @type {Number}
     */
    var currentFrame = -1;

    /**
     * The timestamp of the frame when playback began, in milliseconds. If
     * playback is not in progress, this will be null.
     *
     * @private
     * @type {Number}
     */
    var startVideoTimestamp = null;

    /**
     * The real-world timestamp when playback began, in milliseconds. If
     * playback is not in progress, this will be null.
     *
     * @private
     * @type {Number}
     */
    var startRealTimestamp = null;

    /**
     * The ID of the timeout which will continue the in-progress seek
     * operation. If no seek operation is in progress, the ID stored here (if
     * any) will not be valid.
     *
     * @private
     * @type {Number}
     */
    var seekTimeout = null;

    // Start playback client connected
    playbackClient.connect();

    // Hide cursor unless mouse position is received
    playbackClient.getDisplay().showCursor(false);

    // Read instructions from provided tunnel, extracting each frame
    tunnel.oninstruction = function handleInstruction(opcode, args) {

        // Store opcode and arguments for received instruction
        var instruction = new Guacamole.SessionRecording._Frame.Instruction(opcode, args.slice());
        instructions.push(instruction);
        charactersSinceLastKeyframe += instruction.getSize();

        // Once a sync is received, store all instructions since the last
        // frame as a new frame
        if (opcode === 'sync') {

            // Parse frame timestamp from sync instruction
            var timestamp = parseInt(args[0]);

            // Add a new frame containing the instructions read since last frame
            var frame = new Guacamole.SessionRecording._Frame(timestamp, instructions);
            frames.push(frame);

            // This frame should eventually become a keyframe if enough data
            // has been processed and enough recording time has elapsed, or if
            // this is the absolute first frame
            if (frames.length === 1 || (charactersSinceLastKeyframe >= KEYFRAME_CHAR_INTERVAL
                    && timestamp - lastKeyframeTimestamp >= KEYFRAME_TIME_INTERVAL)) {
                frame.keyframe = true;
                lastKeyframeTimestamp = timestamp;
                charactersSinceLastKeyframe = 0;
            }

            // Clear set of instructions in preparation for next frame
            instructions = [];

            // Notify that additional content is available
            if (recording.onprogress)
                recording.onprogress(recording.getDuration());

        }

    };

    /**
     * Converts the given absolute timestamp to a timestamp which is relative
     * to the first frame in the recording.
     *
     * @private
     * @param {Number} timestamp
     *     The timestamp to convert to a relative timestamp.
     *
     * @returns {Number}
     *     The difference in milliseconds between the given timestamp and the
     *     first frame of the recording, or zero if no frames yet exist.
     */
    var toRelativeTimestamp = function toRelativeTimestamp(timestamp) {

        // If no frames yet exist, all timestamps are zero
        if (frames.length === 0)
            return 0;

        // Calculate timestamp relative to first frame
        return timestamp - frames[0].timestamp;

    };

    /**
     * Searches through the given region of frames for the frame having a
     * relative timestamp closest to the timestamp given.
     *
     * @private
     * @param {Number} minIndex
     *     The index of the first frame in the region (the frame having the
     *     smallest timestamp).
     *
     * @param {Number} maxIndex
     *     The index of the last frame in the region (the frame having the
     *     largest timestamp).
     *
     * @param {Number} timestamp
     *     The relative timestamp to search for, where zero denotes the first
     *     frame in the recording.
     *
     * @returns {Number}
     *     The index of the frame having a relative timestamp closest to the
     *     given value.
     */
    var findFrame = function findFrame(minIndex, maxIndex, timestamp) {

        // Do not search if the region contains only one element
        if (minIndex === maxIndex)
            return minIndex;

        // Split search region into two halves
        var midIndex = Math.floor((minIndex + maxIndex) / 2);
        var midTimestamp = toRelativeTimestamp(frames[midIndex].timestamp);

        // If timestamp is within lesser half, search again within that half
        if (timestamp < midTimestamp && midIndex > minIndex)
            return findFrame(minIndex, midIndex - 1, timestamp);

        // If timestamp is within greater half, search again within that half
        if (timestamp > midTimestamp && midIndex < maxIndex)
            return findFrame(midIndex + 1, maxIndex, timestamp);

        // Otherwise, we lucked out and found a frame with exactly the
        // desired timestamp
        return midIndex;

    };

    /**
     * Replays the instructions associated with the given frame, sending those
     * instructions to the playback client.
     *
     * @private
     * @param {Number} index
     *     The index of the frame within the frames array which should be
     *     replayed.
     */
    var replayFrame = function replayFrame(index) {

        var frame = frames[index];

        // Replay all instructions within the retrieved frame
        for (var i = 0; i < frame.instructions.length; i++) {
            var instruction = frame.instructions[i];
            playbackTunnel.receiveInstruction(instruction.opcode, instruction.args);
        }

        // Store client state if frame is flagged as a keyframe
        if (frame.keyframe && !frame.clientState) {
            playbackClient.exportState(function storeClientState(state) {
                frame.clientState = state;
            });
        }

    };

    /**
     * Moves the playback position to the given frame, resetting the state of
     * the playback client and replaying frames as necessary. The seek
     * operation will proceed asynchronously. If a seek operation is already in
     * progress, that seek is first aborted. The progress of the seek operation
     * can be observed through the onseek handler and the provided callback.
     *
     * @private
     * @param {Number} index
     *     The index of the frame which should become the new playback
     *     position.
     *
     * @param {function} callback
     *     The callback to invoke once the seek operation has completed.
     *
     * @param {Number} [delay=0]
     *     The number of milliseconds that the seek operation should be
     *     scheduled to take.
     */
    var seekToFrame = function seekToFrame(index, callback, delay) {

        // Abort any in-progress seek
        abortSeek();

        // Replay frames asynchronously
        seekTimeout = window.setTimeout(function continueSeek() {

            var startIndex;

            // Back up until startIndex represents current state
            for (startIndex = index; startIndex >= 0; startIndex--) {

                var frame = frames[startIndex];

                // If we've reached the current frame, startIndex represents
                // current state by definition
                if (startIndex === currentFrame)
                    break;

                // If frame has associated absolute state, make that frame the
                // current state
                if (frame.clientState) {
                    playbackClient.importState(frame.clientState);
                    break;
                }

            }

            // Advance to frame index after current state
            startIndex++;

            var startTime = new Date().getTime();

            // Replay any applicable incremental frames
            for (; startIndex <= index; startIndex++) {

                // Stop seeking if the operation is taking too long
                var currentTime = new Date().getTime();
                if (currentTime - startTime >= MAXIMUM_SEEK_TIME)
                    break;

                replayFrame(startIndex);
            }

            // Current frame is now at requested index
            currentFrame = startIndex - 1;

            // Notify of changes in position
            if (recording.onseek)
                recording.onseek(recording.getPosition());

            // If the seek operation has not yet completed, schedule continuation
            if (currentFrame !== index)
                seekToFrame(index, callback,
                    Math.max(delay - (new Date().getTime() - startTime), 0));

            // Notify that the requested seek has completed
            else
                callback();

        }, delay || 0);

    };

    /**
     * Aborts the seek operation currently in progress, if any. If no seek
     * operation is in progress, this function has no effect.
     *
     * @private
     */
    var abortSeek = function abortSeek() {
        window.clearTimeout(seekTimeout);
    };

    /**
     * Advances playback to the next frame in the frames array and schedules
     * playback of the frame following that frame based on their associated
     * timestamps. If no frames exist after the next frame, playback is paused.
     *
     * @private
     */
    var continuePlayback = function continuePlayback() {

        // If frames remain after advancing, schedule next frame
        if (currentFrame + 1 < frames.length) {

            // Pull the upcoming frame
            var next = frames[currentFrame + 1];

            // Calculate the real timestamp corresponding to when the next
            // frame begins
            var nextRealTimestamp = next.timestamp - startVideoTimestamp + startRealTimestamp;

            // Calculate the relative delay between the current time and
            // the next frame start
            var delay = Math.max(nextRealTimestamp - new Date().getTime(), 0);

            // Advance to next frame after enough time has elapsed
            seekToFrame(currentFrame + 1, function frameDelayElapsed() {
                continuePlayback();
            }, delay);

        }

        // Otherwise stop playback
        else
            recording.pause();

    };

    /**
     * Fired when new frames have become available while the recording is
     * being downloaded.
     *
     * @event
     * @param {Number} duration
     *     The new duration of the recording, in milliseconds.
     */
    this.onprogress = null;

    /**
     * Fired whenever playback of the recording has started.
     *
     * @event
     */
    this.onplay = null;

    /**
     * Fired whenever playback of the recording has been paused. This may
     * happen when playback is explicitly paused with a call to pause(), or
     * when playback is implicitly paused due to reaching the end of the
     * recording.
     *
     * @event
     */
    this.onpause = null;

    /**
     * Fired whenever the playback position within the recording changes.
     *
     * @event
     * @param {Number} position
     *     The new position within the recording, in milliseconds.
     */
    this.onseek = null;

    /**
     * Connects the underlying tunnel, beginning download of the Guacamole
     * session. Playback of the Guacamole session cannot occur until at least
     * one frame worth of instructions has been downloaded.
     *
     * @param {String} data
     *     The data to send to the tunnel when connecting.
     */
    this.connect = function connect(data) {
        tunnel.connect(data);
    };

    /**
     * Disconnects the underlying tunnel, stopping further download of the
     * Guacamole session.
     */
    this.disconnect = function disconnect() {
        tunnel.disconnect();
    };

    /**
     * Returns the underlying display of the Guacamole.Client used by this
     * Guacamole.SessionRecording for playback. The display contains an Element
     * which can be added to the DOM, causing the display (and thus playback of
     * the recording) to become visible.
     *
     * @return {Guacamole.Display}
     *     The underlying display of the Guacamole.Client used by this
     *     Guacamole.SessionRecording for playback.
     */
    this.getDisplay = function getDisplay() {
        return playbackClient.getDisplay();
    };

    /**
     * Returns whether playback is currently in progress.
     *
     * @returns {Boolean}
     *     true if playback is currently in progress, false otherwise.
     */
    this.isPlaying = function isPlaying() {
        return !!startVideoTimestamp;
    };

    /**
     * Returns the current playback position within the recording, in
     * milliseconds, where zero is the start of the recording.
     *
     * @returns {Number}
     *     The current playback position within the recording, in milliseconds.
     */
    this.getPosition = function getPosition() {

        // Position is simply zero if playback has not started at all
        if (currentFrame === -1)
            return 0;

        // Return current position as a millisecond timestamp relative to the
        // start of the recording
        return toRelativeTimestamp(frames[currentFrame].timestamp);

    };

    /**
     * Returns the duration of this recording, in milliseconds. If the
     * recording is still being downloaded, this value will gradually increase.
     *
     * @returns {Number}
     *     The duration of this recording, in milliseconds.
     */
    this.getDuration = function getDuration() {

        // If no frames yet exist, duration is zero
        if (frames.length === 0)
            return 0;

        // Recording duration is simply the timestamp of the last frame
        return toRelativeTimestamp(frames[frames.length - 1].timestamp);

    };

    /**
     * Begins continuous playback of the recording downloaded thus far.
     * Playback of the recording will continue until pause() is invoked or
     * until no further frames exist. Playback is initially paused when a
     * Guacamole.SessionRecording is created, and must be explicitly started
     * through a call to this function. If playback is already in progress,
     * this function has no effect. If a seek operation is in progress,
     * playback resumes at the current position, and the seek is aborted as if
     * completed.
     */
    this.play = function play() {

        // If playback is not already in progress and frames remain,
        // begin playback
        if (!recording.isPlaying() && currentFrame + 1 < frames.length) {

            // Notify that playback is starting
            if (recording.onplay)
                recording.onplay();

            // Store timestamp of playback start for relative scheduling of
            // future frames
            var next = frames[currentFrame + 1];
            startVideoTimestamp = next.timestamp;
            startRealTimestamp = new Date().getTime();

            // Begin playback of video
            continuePlayback();

        }

    };

    /**
     * Seeks to the given position within the recording. If the recording is
     * currently being played back, playback will continue after the seek is
     * performed. If the recording is currently paused, playback will be
     * paused after the seek is performed. If a seek operation is already in
     * progress, that seek is first aborted. The seek operation will proceed
     * asynchronously.
     *
     * @param {Number} position
     *     The position within the recording to seek to, in milliseconds.
     *
     * @param {function} [callback]
     *     The callback to invoke once the seek operation has completed.
     */
    this.seek = function seek(position, callback) {

        // Do not seek if no frames exist
        if (frames.length === 0)
            return;

        // Pause playback, preserving playback state
        var originallyPlaying = recording.isPlaying();
        recording.pause();

        // Perform seek
        seekToFrame(findFrame(0, frames.length - 1, position), function restorePlaybackState() {

            // Restore playback state
            if (originallyPlaying)
                recording.play();

            // Notify that seek has completed
            if (callback)
                callback();

        });

    };

    /**
     * Pauses playback of the recording, if playback is currently in progress.
     * If playback is not in progress, this function has no effect. If a seek
     * operation is in progress, the seek is aborted. Playback is initially
     * paused when a Guacamole.SessionRecording is created, and must be
     * explicitly started through a call to play().
     */
    this.pause = function pause() {

        // Abort any in-progress seek / playback
        abortSeek();

        // Stop playback only if playback is in progress
        if (recording.isPlaying()) {

            // Notify that playback is stopping
            if (recording.onpause)
                recording.onpause();

            // Playback is stopped
            startVideoTimestamp = null;
            startRealTimestamp = null;

        }

    };

};

/**
 * A single frame of Guacamole session data. Each frame is made up of the set
 * of instructions used to generate that frame, and the timestamp as dictated
 * by the "sync" instruction terminating the frame. Optionally, a frame may
 * also be associated with a snapshot of Guacamole client state, such that the
 * frame can be rendered without replaying all previous frames.
 *
 * @private
 * @constructor
 * @param {Number} timestamp
 *     The timestamp of this frame, as dictated by the "sync" instruction which
 *     terminates the frame.
 *
 * @param {Guacamole.SessionRecording._Frame.Instruction[]} instructions
 *     All instructions which are necessary to generate this frame relative to
 *     the previous frame in the Guacamole session.
 */
Guacamole.SessionRecording._Frame = function _Frame(timestamp, instructions) {

    /**
     * Whether this frame should be used as a keyframe if possible. This value
     * is purely advisory. The stored clientState must eventually be manually
     * set for the frame to be used as a keyframe. By default, frames are not
     * keyframes.
     *
     * @type {Boolean}
     * @default false
     */
    this.keyframe = false;

    /**
     * The timestamp of this frame, as dictated by the "sync" instruction which
     * terminates the frame.
     *
     * @type {Number}
     */
    this.timestamp = timestamp;

    /**
     * All instructions which are necessary to generate this frame relative to
     * the previous frame in the Guacamole session.
     *
     * @type {Guacamole.SessionRecording._Frame.Instruction[]}
     */
    this.instructions = instructions;

    /**
     * A snapshot of client state after this frame was rendered, as returned by
     * a call to exportState(). If no such snapshot has been taken, this will
     * be null.
     *
     * @type {Object}
     * @default null
     */
    this.clientState = null;

};

/**
 * A Guacamole protocol instruction. Each Guacamole protocol instruction is
 * made up of an opcode and set of arguments.
 *
 * @private
 * @constructor
 * @param {String} opcode
 *     The opcode of this Guacamole instruction.
 *
 * @param {String[]} args
 *     All arguments associated with this Guacamole instruction.
 */
Guacamole.SessionRecording._Frame.Instruction = function Instruction(opcode, args) {

    /**
     * Reference to this Guacamole.SessionRecording._Frame.Instruction.
     *
     * @private
     * @type {Guacamole.SessionRecording._Frame.Instruction}
     */
    var instruction = this;

    /**
     * The opcode of this Guacamole instruction.
     *
     * @type {String}
     */
    this.opcode = opcode;

    /**
     * All arguments associated with this Guacamole instruction.
     *
     * @type {String[]}
     */
    this.args = args;

    /**
     * Returns the approximate number of characters which make up this
     * instruction. This value is only approximate as it excludes the length
     * prefixes and various delimiters used by the Guacamole protocol; only
     * the content of the opcode and each argument is taken into account.
     *
     * @returns {Number}
     *     The approximate size of this instruction, in characters.
     */
    this.getSize = function getSize() {

        // Init with length of opcode
        var size = instruction.opcode.length;

        // Add length of all arguments
        for (var i = 0; i < instruction.args.length; i++)
            size += instruction.args[i].length;

        return size;

    };

};

/**
 * A read-only Guacamole.Tunnel implementation which streams instructions
 * received through explicit calls to its receiveInstruction() function.
 *
 * @private
 * @constructor
 * @augments {Guacamole.Tunnel}
 */
Guacamole.SessionRecording._PlaybackTunnel = function _PlaybackTunnel() {

    /**
     * Reference to this Guacamole.SessionRecording._PlaybackTunnel.
     *
     * @private
     * @type {Guacamole.SessionRecording._PlaybackTunnel}
     */
    var tunnel = this;

    this.connect = function connect(data) {
        // Do nothing
    };

    this.sendMessage = function sendMessage(elements) {
        // Do nothing
    };

    this.disconnect = function disconnect() {
        // Do nothing
    };

    /**
     * Invokes this tunnel's oninstruction handler, notifying users of this
     * tunnel (such as a Guacamole.Client instance) that an instruction has
     * been received. If the oninstruction handler has not been set, this
     * function has no effect.
     *
     * @param {String} opcode
     *     The opcode of the Guacamole instruction.
     *
     * @param {String[]} args
     *     All arguments associated with this Guacamole instruction.
     */
    this.receiveInstruction = function receiveInstruction(opcode, args) {
        if (tunnel.oninstruction)
            tunnel.oninstruction(opcode, args);
    };

};
