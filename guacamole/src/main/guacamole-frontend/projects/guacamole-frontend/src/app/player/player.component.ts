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
    ChangeDetectorRef,
    Component,
    Input,
    OnChanges,
    OnDestroy,
    SimpleChanges,
    ViewEncapsulation
} from '@angular/core';
import { GuacEventService } from 'guacamole-frontend-lib';
import {
    GuacFrontendEventArguments
} from '../events/types/GuacFrontendEventArguments';

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
    encapsulation: ViewEncapsulation.None
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
    playbackPosition: number = 0;

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
    operationProgress: number = 0;

    /**
     * The position within the recording of the current seek operation, in
     * milliseconds. If a seek request is not in progress, this will be
     * undefined.
     */
    seekPosition: number | null = null;

    /**
     * Whether a seek request is currently in progress. A seek request is
     * in progress if the user is attempting to change the current playback
     * position (the user is manipulating the playback position slider).
     */
    pendingSeekRequest: boolean = false;

    /**
     * Whether playback should be resumed (play() should be invoked on the
     * recording) once the current seek request is complete. This value
     * only has meaning if a seek request is pending.
     */
    resumeAfterSeekRequest: boolean = false;

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
                private cdr: ChangeDetectorRef) {
    }

    /**
     * Formats the given number as a decimal string, adding leading zeroes
     * such that the string contains at least two digits. The given number
     * MUST NOT be negative.
     *
     * @param value
     *     The number to format.
     *
     * @returns
     *     The decimal string representation of the given value, padded
     *     with leading zeroes up to a minimum length of two digits.
     */
    zeroPad(value: number): string {
        return value > 9 ? value.toString() : '0' + value;
    }

    /**
     * Formats the given quantity of milliseconds as days, hours, minutes,
     * and whole seconds, separated by colons (DD:HH:MM:SS). Hours are
     * included only if the quantity is at least one hour, and days are
     * included only if the quantity is at least one day. All included
     * groups are zero-padded to two digits with the exception of the
     * left-most group.
     *
     * @param value
     *     The time to format, in milliseconds.
     *
     * @returns
     *     The given quantity of milliseconds formatted as "DD:HH:MM:SS".
     */
    formatTime(value: number): string {

        // Round provided value down to whole seconds
        value = Math.floor((value || 0) / 1000);

        // Separate seconds into logical groups of seconds, minutes,
        // hours, etc.
        const groups: (number | string)[] = [1, 24, 60, 60];
        for (let i = groups.length - 1; i >= 0; i--) {
            const placeValue = groups[i];
            groups[i] = this.zeroPad(value % (placeValue as number));
            value = Math.floor(value / (placeValue as number));
        }

        // Format groups separated by colons, stripping leading zeroes and
        // groups which are entirely zeroes, leaving at least minutes and
        // seconds
        const formatted = groups.join(':');
        return /^[0:]*([0-9]{1,2}(?::[0-9]{2})+)$/.exec(formatted)![1];

    }

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
        if (this.recording && this.pendingSeekRequest) {

            this.seekPosition = null;
            this.operationMessage = 'PLAYER.INFO_SEEK_IN_PROGRESS';
            this.operationProgress = 0;

            // Cancel seek when requested, updating playback position if
            // that position changed
            this.cancelOperation = () => {
                this.recording?.cancel();
                this.playbackPosition = this.seekPosition || this.playbackPosition;
            };

            this.resumeAfterSeekRequest && this.recording.play();
            this.recording.seek(this.playbackPosition, () => {
                this.operationMessage = null;
                // TODO $scope.$evalAsync();
                this.cdr.detectChanges();

            });

        }

        // Flag seek request as completed
        this.pendingSeekRequest = false;

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
    };

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

                // Notify listeners when the recording is completely loaded
                this.recording.onload = () => {
                    this.operationMessage = null;
                    this.guacEventService.broadcast('guacPlayerLoaded');
                    this.cdr.detectChanges();
                };

                // Notify listeners if an error occurs
                this.recording.onerror = (message) => {
                    this.operationMessage = null;
                    this.guacEventService.broadcast('guacPlayerError', {message});
                    this.cdr.detectChanges();
                };

                // Notify listeners when additional recording data has been
                // loaded
                this.recording.onprogress = (duration, current) => {
                    this.operationProgress = (this.src as unknown as any)?.size ? current / (this.src as unknown as any).size : 0;
                    this.guacEventService.broadcast('guacPlayerProgress', {duration, current});
                    this.cdr.detectChanges();
                };

                // Notify listeners when playback has started/resumed
                this.recording.onplay = () => {
                    this.guacEventService.broadcast('guacPlayerPlay');
                    this.cdr.detectChanges();
                };

                // Notify listeners when playback has paused
                this.recording.onpause = () => {
                    this.guacEventService.broadcast('guacPlayerPause');
                    this.cdr.detectChanges();
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

                    this.guacEventService.broadcast('guacPlayerSeek', {position});
                    this.cdr.detectChanges();

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

    ngOnDestroy() {
        this.recording?.pause();
        this.recording?.abort();
    }

}
