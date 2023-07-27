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

/*
 * NOTE: This session recording player implementation is based on the Session
 * Recording Player for Glyptodon Enterprise which is available at
 * https://github.com/glyptodon/glyptodon-enterprise-player under the
 * following license:
 *
 * Copyright (C) 2019 Glyptodon, Inc.
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

/**
 * Directive which plays back session recordings. This directive emits the
 * following events based on state changes within the current recording:
 *
 *     "guacPlayerLoading":
 *         A new recording has been selected and is now loading.
 *
 *     "guacPlayerError":
 *         The current recording cannot be loaded or played due to an error.
 *         The recording may be unreadable (lack of permissions) or corrupt
 *         (protocol error).
 *
 *     "guacPlayerProgress"
 *         Additional data has been loaded for the current recording and the
 *         recording's duration has changed. The new duration in milliseconds
 *         and the number of bytes loaded so far are passed to the event.
 *
 *     "guacPlayerLoaded"
 *         The current recording has finished loading.
 *
 *     "guacPlayerPlay"
 *         Playback of the current recording has started or has been resumed.
 *
 *     "guacPlayerPause"
 *         Playback of the current recording has been paused.
 *
 *     "guacPlayerSeek"
 *         The playback position of the current recording has changed. The new
 *         position within the recording is passed to the event as the number
 *         of milliseconds since the start of the recording.
 */
angular.module('player').directive('guacPlayer', ['$injector', function guacPlayer($injector) {

    // Required services
    const playerTimeService = $injector.get('playerTimeService');

    // Required types
    const TextBatch = $injector.get('TextBatch');

    const config = {
        restrict : 'E',
        templateUrl : 'app/player/templates/player.html'
    };

    config.scope = {

        /**
         * A Blob containing the Guacamole session recording to load.
         *
         * @type {!Blob|Guacamole.Tunnel}
         */
        src : '='

    };

    config.controller = ['$scope', '$element', '$injector',
        function guacPlayerController($scope) {

        /**
         * Guacamole.SessionRecording instance to be used to playback the
         * session recording given via $scope.src. If the recording has not
         * yet been loaded, this will be null.
         *
         * @type {Guacamole.SessionRecording}
         */
        $scope.recording = null;

        /**
         * The current playback position, in milliseconds. If a seek request is
         * in progress, this will be the desired playback position of the
         * pending request.
         *
         * @type {!number}
         */
        $scope.playbackPosition = 0;

        /**
         * The key of the translation string that describes the operation
         * currently running in the background, or null if no such operation is
         * running.
         *
         * @type {string}
         */
        $scope.operationMessage = null;

        /**
         * The current progress toward completion of the operation running in
         * the background, where 0 represents no progress and 1 represents full
         * completion. If no such operation is running, this value has no
         * meaning.
         *
         * @type {!number}
         */
        $scope.operationProgress = 0;

        /**
         * The position within the recording of the current seek operation, in
         * milliseconds. If a seek request is not in progress, this will be
         * null.
         *
         * @type {number}
         */
        $scope.seekPosition = null;

        /**
         * Any batches of text typed during the recording.
         *
         * @type {TextBatch[]}
         */
        $scope.textBatches = [];

        /**
         * Whether or not the key log viewer should be displayed. False by
         * default unless explicitly enabled by user interaction.
         *
         * @type {boolean}
         */
        $scope.showKeyLog = false;

        /**
         * Whether a seek request is currently in progress. A seek request is
         * in progress if the user is attempting to change the current playback
         * position (the user is manipulating the playback position slider).
         *
         * @type {boolean}
         */
        var pendingSeekRequest = false;

        /**
         * Whether playback should be resumed (play() should be invoked on the
         * recording) once the current seek request is complete. This value
         * only has meaning if a seek request is pending.
         *
         * @type {boolean}
         */
        var resumeAfterSeekRequest = false;

        /**
         * Return true if any batches of key event logs are available for this
         * recording, or false otherwise.
         *
         * @return
         *     True if any batches of key event logs are avaiable for this
         *     recording, or false otherwise.
         */
        $scope.hasTextBatches = function hasTextBatches () {
            return $scope.textBatches.length >= 0;
        };

        /**
         * Toggle the visibility of the text key log viewer.
         */
        $scope.toggleKeyLogView = function toggleKeyLogView() {
            $scope.showKeyLog = !$scope.showKeyLog;
        };

        /**
         * @borrows playerTimeService.formatTime
         */
        $scope.formatTime = playerTimeService.formatTime;

        /**
         * Pauses playback and decouples the position slider from current
         * playback position, allowing the user to manipulate the slider
         * without interference. Playback state will be resumed following a
         * call to commitSeekRequest().
         */
        $scope.beginSeekRequest = function beginSeekRequest() {

            // If a recording is present, pause and save state if we haven't
            // already done so
            if ($scope.recording && !pendingSeekRequest) {
                resumeAfterSeekRequest = $scope.recording.isPlaying();
                $scope.recording.pause();
            }

            // Flag seek request as in progress
            pendingSeekRequest = true;

        };

        /**
         * Restores the playback state at the time beginSeekRequest() was
         * called and resumes coupling between the playback position slider and
         * actual playback position.
         */
        $scope.commitSeekRequest = function commitSeekRequest() {

            // If a recording is present and there is an active seek request,
            // restore the playback state at the time that request began and
            // begin seeking to the requested position
            if ($scope.recording && pendingSeekRequest)
                $scope.seekToPlaybackPosition();

            // Flag seek request as completed
            pendingSeekRequest = false;

        };

        /**
         * Seek the recording to the specified position within the recording,
         * in milliseconds.
         *
         * @param {Number} timestamp
         *      The position to seek to within the current record,
         *      in milliseconds.
         */
        $scope.seekToTimestamp = function seekToTimestamp(timestamp) {

            // Set the timestamp and seek to it
            $scope.playbackPosition = timestamp;
            $scope.seekToPlaybackPosition();

        };

        /**
         * Seek the recording to the current playback position value.
         */
        $scope.seekToPlaybackPosition = function seekToPlaybackPosition() {

            $scope.seekPosition = null;
            $scope.operationMessage = 'PLAYER.INFO_SEEK_IN_PROGRESS';
            $scope.operationProgress = 0;

            // Cancel seek when requested, updating playback position if
            // that position changed
            $scope.cancelOperation = function abortSeek() {
                $scope.recording.cancel();
                $scope.playbackPosition = $scope.seekPosition || $scope.playbackPosition;
            };

            resumeAfterSeekRequest && $scope.recording.play();
            $scope.recording.seek($scope.playbackPosition, function seekComplete() {
                $scope.seekPosition = null;
                $scope.operationMessage = null;
                $scope.$evalAsync();
            });
        };

        /**
         * Toggles the current playback state. If playback is currently paused,
         * playback is resumed. If playback is currently active, playback is
         * paused. If no recording has been loaded, this function has no
         * effect.
         */
        $scope.togglePlayback = function togglePlayback() {
            if ($scope.recording) {
                if ($scope.recording.isPlaying())
                    $scope.recording.pause();
                else
                    $scope.recording.play();
            }
        };

        // Automatically load the requested session recording
        $scope.$watch('src', function srcChanged(src) {

            // Reset position and seek state
            pendingSeekRequest = false;
            $scope.playbackPosition = 0;

            // Stop loading the current recording, if any
            if ($scope.recording) {
                $scope.recording.pause();
                $scope.recording.abort();
            }

            // If no recording is provided, reset to empty
            if (!src)
                $scope.recording = null;

            // Otherwise, begin loading the provided recording
            else {

                $scope.recording = new Guacamole.SessionRecording(src);

                // Begin downloading the recording
                $scope.recording.connect();

                // Notify listeners when the recording is completely loaded
                $scope.recording.onload = function recordingLoaded() {
                    $scope.operationMessage = null;
                    $scope.$emit('guacPlayerLoaded');
                    $scope.$evalAsync();
                };

                // Notify listeners if an error occurs
                $scope.recording.onerror = function recordingFailed(message) {
                    $scope.operationMessage = null;
                    $scope.$emit('guacPlayerError', message);
                    $scope.$evalAsync();
                };

                // Notify listeners when additional recording data has been
                // loaded
                $scope.recording.onprogress = function recordingLoadProgressed(duration, current) {
                    $scope.operationProgress = src.size ? current / src.size : 0;
                    $scope.$emit('guacPlayerProgress', duration, current);
                    $scope.$evalAsync();
                };

                // Notify listeners when playback has started/resumed
                $scope.recording.onplay = function playbackStarted() {
                    $scope.$emit('guacPlayerPlay');
                    $scope.$evalAsync();
                };

                // Notify listeners when playback has paused
                $scope.recording.onpause = function playbackPaused() {
                    $scope.$emit('guacPlayerPause');
                    $scope.$evalAsync();
                };

                // Append any extracted batches of typed text
                $scope.recording.ontext = function appendTextBatch(batch) {

                    // Convert to the display-optimized TextBatch type
                    $scope.textBatches.push(new TextBatch(batch));

                };

                // Notify listeners when current position within the recording
                // has changed
                $scope.recording.onseek = function positionChanged(position, current, total) {

                    // Update current playback position while playing
                    if ($scope.recording.isPlaying())
                        $scope.playbackPosition = position;

                    // Update seek progress while seeking
                    else {
                        $scope.seekPosition = position;
                        $scope.operationProgress = current / total;
                    }

                    $scope.$emit('guacPlayerSeek', position);
                    $scope.$evalAsync();

                };

                $scope.operationMessage = 'PLAYER.INFO_LOADING_RECORDING';
                $scope.operationProgress = 0;

                $scope.cancelOperation = function abortLoad() {
                    $scope.recording.abort();
                    $scope.operationMessage = null;
                };

                $scope.$emit('guacPlayerLoading');

            }

        });

        // Clean up resources when player is destroyed
        $scope.$on('$destroy', function playerDestroyed() {
            $scope.recording.pause();
            $scope.recording.abort();
        });

    }];

    return config;

}]);
