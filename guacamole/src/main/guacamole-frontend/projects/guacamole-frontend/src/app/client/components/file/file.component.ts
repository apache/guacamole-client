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
    EventEmitter,
    HostListener,
    Input,
    OnChanges,
    Output,
    SimpleChanges,
    ViewEncapsulation,
    ChangeDetectionStrategy
} from '@angular/core';
import { ManagedFilesystem } from '../../types/ManagedFilesystem';

/**
 * This component displays a single file in a file browser. It shows the
 * file's name, icon, and type, and allows the user to interact with the
 * file by clicking on it.
 *
 * If the file is a directory, the user can click on it to change the
 * current directory of the file browser. If the file is a normal file,
 * the user can click on it to initiate a download.
 */
@Component({
    selector: 'guac-file',
    templateUrl: './file.component.html',
    encapsulation: ViewEncapsulation.None,
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class FileComponent implements OnChanges {

    /**
     * The file to be displayed by the component.
     */
    @Input({ required: true }) file: ManagedFilesystem.File | null = null;

    /**
     * Indicates whether the file is currently focused. If true, the next
     * click on the file will trigger a changeDirectory or downloadFile
     * event based on the type of the file.
     */
    @Input({ required: true }) isFocused = false;

    /**
     * Event emitted when the file is clicked and should be focused.
     */
    @Output() focusFile: EventEmitter<void> = new EventEmitter<void>();

    /**
     * Event emitted when the current directory should be changed.
     * The event payload is the directory to change to.
     */
    @Output() changeDirectory: EventEmitter<ManagedFilesystem.File> = new EventEmitter<ManagedFilesystem.File>();

    /**
     * Event emitted when a file should be downloaded.
     * The event payload is the file to download.
     */
    @Output() downloadFile: EventEmitter<ManagedFilesystem.File> = new EventEmitter<ManagedFilesystem.File>();

    /**
     * The file action to execute when a focused file is clicked.
     */
    fileAction: (() => void) | null = null;

    /**
     * The CSS class to apply to the file depending on its type.
     */
    clazz: string | null = null;

    /**
     * Calls setupComponent() when the file changes.
     */
    ngOnChanges(changes: SimpleChanges): void {

        if (changes['file']) {

            const file = changes['file'].currentValue as ManagedFilesystem.File | null;

            if (file)
                this.setupComponent();
        }

    }

    /**
     * Sets up the component by determining the CSS class and the file action
     * for the current file.
     */
    setupComponent(): void {

        if (!this.file)
            return;

        // Double-clicking on unknown file types will do nothing
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        let fileAction = () => {
        };

        let clazz = '';

        // Change current directory when directories are clicked
        if (this.isDirectory(this.file)) {
            clazz = 'directory';
            fileAction = () => {
                if (this.file)
                    this.changeDirectory.emit(this.file);
            };
        }

        // Initiate downloads when normal files are clicked
        else if (this.isNormalFile(this.file)) {
            clazz = 'normal-file';
            fileAction = () => {
                if (this.file)
                    this.downloadFile.emit(this.file);
            };
        }

        this.clazz = clazz;
        this.fileAction = fileAction;
    }

    /**
     * Handles a click on the file. Executes the file action if the file is
     * already focused, otherwise requests that the file be focused.
     */
    @HostListener('click')
    handleClick(): void {

        // Fire file-specific action if already focused
        if (this.fileAction && this.isFocused)
            this.fileAction();

        // Otherwise mark as focused
        else
            this.focusFile.emit();

    }

    /**
     * Prevents text selection during navigation.
     *
     * @param e
     *     The selectstart event.
     */
    @HostListener('selectstart', ['$event'])
    handleSelectStart(e: Event): void {
        e.preventDefault();
        e.stopPropagation();
    }

    /**
     * Returns whether the given file is a normal file.
     *
     * @param file
     *     The file to test.
     *
     * @returns
     *     true if the given file is a normal file, false otherwise.
     */
    isNormalFile(file: ManagedFilesystem.File): boolean {
        return file.type === ManagedFilesystem.File.Type.NORMAL;
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


}
