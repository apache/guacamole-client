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

import { Component, DoCheck, Input, ViewEncapsulation } from '@angular/core';
import FileSaver from 'file-saver';
import { GuacTranslateService } from '../../services/guac-translate.service';
import { ManagedFileTransferState } from '../../types/ManagedFileTransferState';
import { ManagedFileUpload } from '../../types/ManagedFileUpload';

/**
 * Component which displays an active file transfer, providing links for
 * downloads, if applicable.
 */
@Component({
    selector: 'guac-file-transfer',
    templateUrl: './guac-file-transfer.component.html',
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class GuacFileTransferComponent implements DoCheck {

    /**
     * The file transfer to display.
     */
    @Input({ required: true }) transfer!: ManagedFileUpload;

    /**
     * The translated error message for the current status code.
     */
    translatedErrorMessage?: string = '';

    /**
     * TODO
     * @private
     */
    private lastTransferStatusCode?: number;

    /**
     * Inject required services.
     */
    constructor(private guacTranslate: GuacTranslateService) {
    }

    /**
     * Returns the unit string that is most appropriate for the
     * number of bytes transferred thus far - either 'gb', 'mb', 'kb',
     * or 'b'.
     *
     * @returns
     *     The unit string that is most appropriate for the number of
     *     bytes transferred thus far.
     */
    getProgressUnit(): string {

        const bytes = this.transfer.progress || 0;

        // Gigabytes
        if (bytes > 1000000000)
            return 'gb';

        // Megabytes
        if (bytes > 1000000)
            return 'mb';

        // Kilobytes
        if (bytes > 1000)
            return 'kb';

        // Bytes
        return 'b';

    }

    /**
     * Returns the amount of data transferred thus far, in the units
     * returned by getProgressUnit().
     *
     * @returns
     *     The amount of data transferred thus far, in the units
     *     returned by getProgressUnit().
     */
    getProgressValue(): number | string | undefined {

        const bytes = this.transfer.progress;
        if (!bytes)
            return bytes;

        // Convert bytes to necessary units
        switch (this.getProgressUnit()) {

            // Gigabytes
            case 'gb':
                return (bytes / 1000000000).toFixed(1);

            // Megabytes
            case 'mb':
                return (bytes / 1000000).toFixed(1);

            // Kilobytes
            case 'kb':
                return (bytes / 1000).toFixed(1);

            // Bytes
            case 'b':
            default:
                return bytes;

        }

    }

    /**
     * Returns the percentage of bytes transferred thus far, if the
     * overall length of the file is known.
     *
     * @returns
     *     The percentage of bytes transferred thus far, if the
     *     overall length of the file is known.
     */
    getPercentDone(): number {
        return this.transfer.progress! / this.transfer.length! * 100;
    }

    /**
     * Determines whether the associated file transfer is in progress.
     *
     * @returns
     *     true if the file transfer is in progress, false otherwise.
     */
    isInProgress(): boolean {

        // Not in progress if there is no transfer
        if (!this.transfer)
            return false;

        // Determine in-progress status based on stream state
        switch (this.transfer.transferState.streamState) {

            // IDLE or OPEN file transfers are active
            case ManagedFileTransferState.StreamState.IDLE:
            case ManagedFileTransferState.StreamState.OPEN:
                return true;

            // All others are not active
            default:
                return false;

        }

    }

    /**
     * Returns whether the file associated with this file transfer can
     * be saved locally via a call to save().
     *
     * @returns
     *     true if a call to save() will result in the file being
     *     saved, false otherwise.
     */
    isSavable(): boolean {
        return !!(this.transfer as any).blob;
    }

    /**
     * Saves the downloaded file, if any. If this transfer is an upload
     * or the download is not yet complete, this function has no
     * effect.
     */
    save(): void {

        // Ignore if no blob exists
        if (!(this.transfer as any).blob)
            return;

        // Save file
        FileSaver.saveAs((this.transfer as any).blob, this.transfer.filename);

    }

    /**
     * Returns whether an error has occurred. If an error has occurred,
     * the transfer is no longer active, and the text of the error can
     * be read from getErrorText().
     *
     * @returns
     *     true if an error has occurred during transfer, false
     *     otherwise.
     */
    hasError(): boolean {
        return this.transfer.transferState.streamState === ManagedFileTransferState.StreamState.ERROR;
    }

    /**
     * Handle changes to the transfer status code.
     */
    ngDoCheck(): void {

        const currentTransferStatusCode = this.transfer.transferState.statusCode;

        if (this.lastTransferStatusCode !== currentTransferStatusCode) {

            // Determine translation name of error
            const errorName = 'CLIENT.ERROR_UPLOAD_' + currentTransferStatusCode.toString(16).toUpperCase();

            // Use translation string, or the default if no translation is found for this error code
            this.guacTranslate.translateWithFallback(errorName, 'CLIENT.ERROR_UPLOAD_DEFAULT').subscribe(
                translationResult => this.translatedErrorMessage = translationResult.message
            );

            // Remember the new status code
            this.lastTransferStatusCode = currentTransferStatusCode;

        }

    }


}
