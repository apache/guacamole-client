

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
    computed,
    DestroyRef,
    Input,
    OnChanges,
    OnInit,
    Signal,
    SimpleChanges,
    ViewEncapsulation
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { GuacEventService } from 'guacamole-frontend-lib';
import { GuacFrontendEventArguments } from '../../../events/types/GuacFrontendEventArguments';
import { ManagedFilesystemService } from '../../services/managed-filesystem.service';
import { ManagedFilesystem } from '../../types/ManagedFilesystem';

/**
 * A component which displays the contents of a filesystem received through the
 * Guacamole client.
 */
@Component({
    selector     : 'guac-file-browser',
    templateUrl  : './guac-file-browser.component.html',
    encapsulation: ViewEncapsulation.None
})
export class GuacFileBrowserComponent implements OnInit, OnChanges {

    /**
     * The filesystem to display.
     */
    @Input({ required: true }) filesystem: ManagedFilesystem | null = null;

    /**
     * Signal which fires whenever the files that should be displayed change.
     */
    files: Signal<ManagedFilesystem.File[]> | null = null;

    /**
     * The index of the file which is currently focused.
     */
    protected focusedFileIndex = -1;

    /**
     * Inject required services.
     */
    constructor(private managedFilesystemService: ManagedFilesystemService,
                private guacEventService: GuacEventService<GuacFrontendEventArguments>,
                private destroyRef: DestroyRef) {
    }

    /**
     * TOOD
     */
    ngOnInit(): void {

        // Refresh file browser when any upload completes
        this.guacEventService.on('guacUploadComplete')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(() => {

                // Refresh filesystem, if it exists
                if (this.filesystem)
                    this.managedFilesystemService.refresh(this.filesystem, this.filesystem.currentDirectory());

            });
    }


    /**
     * If the filesystem changes, update the files.
     */
    ngOnChanges(changes: SimpleChanges): void {
        if (changes['filesystem']) {

            const filesystem = changes['filesystem'].currentValue as ManagedFilesystem | null;

            if (!filesystem)
                return;

            // Update files when the current directory or the files within it change
            this.files = computed(() => {
                return this.sortFiles(filesystem.currentDirectory().files());
            });

        }
    }

    /**
     * Returns whether the given file is a directory.
     *
     * @param file
     *     The file to test.
     *
     * @returns
     *     true if the given file is a directory, false otherwise.
     */
    isDirectory(file: ManagedFilesystem.File): boolean {
        return file.type === ManagedFilesystem.File.Type.DIRECTORY;
    }

    /**
     * Changes the currently-displayed directory to the given
     * directory.
     *
     * @param file
     *     The directory to change to.
     */
    changeDirectory(file: ManagedFilesystem.File): void {
        if (this.filesystem)
            this.managedFilesystemService.changeDirectory(this.filesystem, file);
    }

    /**
     * Initiates a download of the given file. The progress of the
     * download can be observed through guacFileTransferManager.
     *
     * @param file
     *     The file to download.
     */
    downloadFile(file: ManagedFilesystem.File): void {
        if (this.filesystem)
            this.managedFilesystemService.downloadFile(this.filesystem, file.streamName);
    }

    /**
     * Sets the given file as focused, such that it will be highlighted.
     *
     * @param index
     *     The index of the file to focus.
     */
    focusFile(index: number): void {
        this.focusedFileIndex = index;
    }

    /**
     * trackBy Function for the list of files.
     */
    trackByStreamName(index: number, file: ManagedFilesystem.File): string {
        return file.streamName;
    }

    /**
     * Sorts the given map of files, returning an array of those files
     * grouped by file type (directories first, followed by non-
     * directories) and sorted lexicographically.
     *
     * @param files
     *     The map of files to sort.
     *
     * @returns
     *     An array of all files in the given map, sorted
     *     lexicographically with directories first, followed by non-
     *     directories.
     */
    private sortFiles(files: Record<string, ManagedFilesystem.File>): ManagedFilesystem.File[] {

        // Get all given files as an array
        const unsortedFiles = [];
        for (const name in files)
            unsortedFiles.push(files[name]);

        // Sort files - directories first, followed by all other files
        // sorted by name
        return unsortedFiles.sort((a, b) => {

            // Directories come before non-directories
            if (this.isDirectory(a) && !this.isDirectory(b))
                return -1;

            // Non-directories come after directories
            if (!this.isDirectory(a) && this.isDirectory(b))
                return 1;

            // All other combinations are sorted by name
            return a.name!.localeCompare(b.name!);

        });

    }

}
