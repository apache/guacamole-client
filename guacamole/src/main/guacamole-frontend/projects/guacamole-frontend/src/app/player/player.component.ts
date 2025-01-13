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

import {
    Component,
    HostListener,
    Input,
    OnChanges,
    OnDestroy,
    signal,
    SimpleChanges,
    ViewEncapsulation
} from '@angular/core';
import { GuacEventService } from 'guacamole-frontend-lib';
import { GuacFrontendEventArguments } from '../events/types/GuacFrontendEventArguments';
import { KeyEventDisplayService, TextBatch } from './services/key-event-display.service';
import { PlayerHeatmapService } from './services/player-heatmap.service';
import { PlayerTimeService } from './services/player-time.service';
import debounce from 'lodash/debounce';

/**
 * The number of milliseconds after the last detected mouse activity after
 * which the associated CSS class should be removed.
 */
const MOUSE_CLEANUP_DELAY = 4000;

/**
 * The number of milliseconds after the last detected mouse activity before
 * the cleanup timer to remove the associated CSS class should be scheduled.
 */
const MOUSE_DEBOUNCE_DELAY = 250;

/**
 * The number of milliseconds, after the debounce delay, before the mouse
 * activity cleanup timer should run.
 */
const MOUSE_CLEANUP_TIMER_DELAY = MOUSE_CLEANUP_DELAY - MOUSE_DEBOUNCE_DELAY;

/**
 * Component which plays back session recordings. This directive emits the
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
@Component({
    selector: 'guac-player',
    templateUrl: './player.component.html',
    encapsulation: ViewEncapsulation.None,
    host: {
        '[class.recent-mouse-movement]': 'recentMouseMovement()'
    }
})
export class PlayerComponent implements OnChanges, OnDestroy {

    /**
     * A Blob containing the Guacamole session recording to load.
     */
    @Input('guacSrc') src?: Blob | Guacamole.Tunnel;

    /**
     * Guacamole.SessionRecording instance to be used to playback the
     * session recording given via $scope.src. If the recording has not
     * yet been loaded, this will be null.
     */
    recording: Guacamole.SessionRecording | null = null;

    /**
     * The current playback position, in milliseconds. If a seek request is
     * in progress, this will be the desired playback position of the
     * pending request.
     */
    playbackPosition = 0;

    /**
     * The key of the translation string that describes the operation
     * currently running in the background, or null if no such operation is
     * running.
     */
    operationMessage: string | null = null;

    /**
     * The current progress toward completion of the operation running in
     * the background, where 0 represents no progress and 1 represents full
     * completion. If no such operation is running, this value has no
     * meaning.
     */
    operationProgress = 0;

    /**
     * The position within the recording of the current seek operation, in
     * milliseconds. If a seek request is not in progress, this will be
     * undefined.
     */
    seekPosition: number | null = null;

    /**
     * Any batches of text typed during the recording.
     */
    textBatches: TextBatch[] = [];

    /**
     * Whether or not the key log viewer should be displayed. False by
     * default unless explicitly enabled by user interaction.
     */
    showKeyLog = false;

    /**
     * The height, in pixels, of the SVG heatmap paths. Note that this is not
     * necessarily the actual rendered height, just the initial size of the
     * SVG path before any styling is applied.
     */
    HEATMAP_HEIGHT = 100;

    /**
     * The width, in pixels, of the SVG heatmap paths. Note that this is not
     * necessarily the actual rendered width, just the initial size of the
     * SVG path before any styling is applied.
     */
    HEATMAP_WIDTH = 1000;

    /**
     * The maximum number of key events per millisecond to display in the
     * key event heatmap. Any key event rates exceeding this value will be
     * capped at this rate to ensure that unsually large spikes don't make
     * swamp the rest of the data.
     *
     * Note: This is 6 keys per second (events include both presses and
     * releases) - equivalent to ~88 words per minute typed.
     */
    private readonly KEY_EVENT_RATE_CAP = 12 / 1000;

    /**
     * The maximum number of frames per millisecond to display in the
     * frame heatmap. Any frame rates exceeding this value will be
     * capped at this rate to ensure that unsually large spikes don't make
     * swamp the rest of the data.
     */
    private readonly FRAME_RATE_CAP = 10 / 1000;

    /**
     * An SVG path describing a smoothed curve that visualizes the relative
     * number of frames rendered throughout the recording - i.e. a heatmap
     * of screen updates.
     */
    frameHeatmap = '';

    /**
     * An SVG path describing a smoothed curve that visualizes the relative
     * number of key events recorded throughout the recording - i.e. a
     * heatmap of key events.
     */
    keyHeatmap = '';

    /**
     * Whether a seek request is currently in progress. A seek request is
     * in progress if the user is attempting to change the current playback
     * position (the user is manipulating the playback position slider).
     */
    private pendingSeekRequest = false;

    /**
     * Whether playback should be resumed (play() should be invoked on the
     * recording) once the current seek request is complete. This value
     * only has meaning if a seek request is pending.
     */
    private resumeAfterSeekRequest = false;

    /**
     * A scheduled timer to clean up the mouse activity CSS class, or null
     * if no timer is scheduled.
     */
    private mouseActivityTimer: number | null = null;

    /**
     * The recording-relative timestamp of each frame of the recording that
     * has been processed so far.
     */
    private frameTimestamps: number[] = [];

    /**
     * The recording-relative timestamp of each text event that has been
     * processed so far.
     */
    private keyTimestamps: number[] = [];

    /**
     * Whether the mouse has moved recently.
     */
    recentMouseMovement = signal(false);

    /**
     * The operation that should be performed when the cancel button is
     * clicked.
     */
    cancelOperation: () => void = () => {
    };

    /**
     * Inject required services.
     */
    constructor(private guacEventService: GuacEventService<GuacFrontendEventArguments>,
                private keyEventDisplayService: KeyEventDisplayService,
                private playerHeatmapService: PlayerHeatmapService,
                private playerTimeService: PlayerTimeService) {
    }

    /**
     * Return true if any batches of key event logs are available for this
     * recording, or false otherwise.
     *
     * @return
     *     True if any batches of key event logs are avaiable for this
     *     recording, or false otherwise.
     */
    hasTextBatches(): boolean {
        return this.textBatches.length >= 0;
    }

    /**
     * Toggle the visibility of the text key log viewer.
     */
    toggleKeyLogView(): void {
        this.showKeyLog = !this.showKeyLog;
    }

    /**
     * @borrows PlayerTimeService.formatTime
     */
    formatTime = this.playerTimeService.formatTime;

    /**
     * Pauses playback and decouples the position slider from current
     * playback position, allowing the user to manipulate the slider
     * without interference. Playback state will be resumed following a
     * call to commitSeekRequest().
     */
    beginSeekRequest(): void {

        // If a recording is present, pause and save state if we haven't
        // already done so
        if (this.recording && !this.pendingSeekRequest) {
            this.resumeAfterSeekRequest = this.recording.isPlaying();
            this.recording.pause();
        }

        // Flag seek request as in progress
        this.pendingSeekRequest = true;

    }

    /**
     * Restores the playback state at the time beginSeekRequest() was
     * called and resumes coupling between the playback position slider and
     * actual playback position.
     */
    commitSeekRequest(): void {

        // If a recording is present and there is an active seek request,
        // restore the playback state at the time that request began and
        // begin seeking to the requested position
        if (this.recording && this.pendingSeekRequest)
            this.seekToPlaybackPosition();

        // Flag seek request as completed
        this.pendingSeekRequest = false;

    }

    /**
     * Seek the recording to the specified position within the recording,
     * in milliseconds.
     *
     * @param timestamp
     *      The position to seek to within the current record,
     *      in milliseconds.
     */
    seekToTimestamp(timestamp: number): void {

        // Set the timestamp and seek to it
        this.playbackPosition = timestamp;
        this.seekToPlaybackPosition();

    }

    /**
     * Seek the recording to the current playback position value.
     */
    seekToPlaybackPosition() {

        this.seekPosition = null;
        this.operationMessage = 'PLAYER.INFO_SEEK_IN_PROGRESS';
        this.operationProgress = 0;

        // Cancel seek when requested, updating playback position if
        // that position changed
        this.cancelOperation = () => {
            this.recording!.cancel();
            this.playbackPosition = this.seekPosition || this.playbackPosition;
        };

        this.resumeAfterSeekRequest && this.recording!.play();
        this.recording!.seek(this.playbackPosition, () => {
            this.seekPosition = null;
            this.operationMessage = null;
        });
    }

    /**
     * Toggles the current playback state. If playback is currently paused,
     * playback is resumed. If playback is currently active, playback is
     * paused. If no recording has been loaded, this function has no
     * effect.
     */
    togglePlayback(): void {
        if (this.recording) {
            if (this.recording.isPlaying())
                this.recording.pause();
            else
                this.recording.play();
        }
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes['src']) {

            // Reset position and seek state
            this.pendingSeekRequest = false;
            this.playbackPosition = 0;

            // Stop loading the current recording, if any
            if (this.recording) {
                this.recording.pause();
                this.recording.abort();
            }

            // If no recording is provided, reset to empty
            if (!this.src)
                this.recording = null;

            // Otherwise, begin loading the provided recording
            else {

                this.recording = new Guacamole.SessionRecording(this.src);

                // Begin downloading the recording
                this.recording.connect();

                // Notify listeners and set any heatmap paths
                // when the recording is completely loaded
                this.recording.onload = () => {
                    this.operationMessage = null;
                    this.guacEventService.broadcast('guacPlayerLoaded');

                    const recordingDuration = this.recording!.getDuration();

                    // Generate heat maps for rendered frames and typed text
                    this.frameHeatmap = (
                        this.playerHeatmapService.generateHeatmapPath(
                            this.frameTimestamps, recordingDuration, this.FRAME_RATE_CAP,
                            this.HEATMAP_HEIGHT, this.HEATMAP_WIDTH));
                    this.keyHeatmap = (
                        this.playerHeatmapService.generateHeatmapPath(
                            this.keyTimestamps, recordingDuration, this.KEY_EVENT_RATE_CAP,
                            this.HEATMAP_HEIGHT, this.HEATMAP_WIDTH));
                };

                // Notify listeners if an error occurs
                this.recording.onerror = (message) => {
                    this.operationMessage = null;
                    this.guacEventService.broadcast('guacPlayerError', { message });
                };

                // Notify listeners when additional recording data has been
                // loaded
                this.recording.onprogress = (duration, current) => {
                    this.operationProgress = (this.src as unknown as any)?.size ? current / (this.src as unknown as any).size : 0;
                    this.guacEventService.broadcast('guacPlayerProgress', { duration, current });

                    // Store the timestamp of the just-received frame
                    this.frameTimestamps.push(duration);
                };

                // Notify listeners when playback has started/resumed
                this.recording.onplay = () => {
                    this.guacEventService.broadcast('guacPlayerPlay');
                };

                // Notify listeners when playback has paused
                this.recording.onpause = () => {
                    this.guacEventService.broadcast('guacPlayerPause');
                };

                // @ts-ignore TODO: Remove when guacamole-common-js 1.6.0 is released and the types are updated
                // Extract key events from the recording
                this.recording.onkeyevents = (events) => {

                    // Convert to a display-optimized format
                    this.textBatches = (
                        this.keyEventDisplayService.parseEvents(events));

                    // @ts-ignore TODO: Remove when guacamole-common-js 1.6.0 is released and the types are updated
                    this.keyTimestamps = events.map(event => event.timestamp);

                };

                // Notify listeners when current position within the recording
                // has changed
                this.recording.onseek = (position, current, total) => {

                    // Update current playback position while playing
                    if (this.recording?.isPlaying())
                        this.playbackPosition = position;

                    // Update seek progress while seeking
                    else {
                        this.seekPosition = position;
                        this.operationProgress = current / total;
                    }

                    this.guacEventService.broadcast('guacPlayerSeek', { position });

                };

                this.operationMessage = 'PLAYER.INFO_LOADING_RECORDING';
                this.operationProgress = 0;

                this.cancelOperation = () => {
                    this.recording?.abort();
                    this.operationMessage = null;
                };

                this.guacEventService.broadcast('guacPlayerLoading');
            }
        }
    }

    /**
     * Clean up resources when player is destroyed
     */
    ngOnDestroy() {
        this.recording?.pause();
        this.recording?.abort();
        this.mouseActivityTimer !== null && window.clearTimeout(this.mouseActivityTimer);
    }

    /**
     * Clean up the mouse movement class after no mouse activity has been
     * detected for the appropriate time period.
     */
    private readonly scheduleCleanupTimeout = debounce(() =>
            this.mouseActivityTimer = window.setTimeout(() => {
                this.mouseActivityTimer = null;
                this.recentMouseMovement.set(false);
            }, MOUSE_CLEANUP_TIMER_DELAY),

        /*
         * Only schedule the cleanup task after the mouse hasn't moved
         * for a reasonable amount of time to ensure that the number of
         * created cleanup timers remains reasonable.
         */
        MOUSE_DEBOUNCE_DELAY);

    /*
     * When the mouse moves inside the player, add a CSS class signifying
     * recent mouse movement, to be automatically cleaned up after a period
     * of time with no detected mouse movement.
     */
    @HostListener('mousemove')
    onMouseMove() {

        // Clean up any existing cleanup timer
        if (this.mouseActivityTimer !== null) {
            window.clearTimeout(this.mouseActivityTimer);
            this.mouseActivityTimer = null;
        }

        // Add the marker CSS class, and schedule its removal
        this.recentMouseMovement.set(true);
        this.scheduleCleanupTimeout();

    }

}
