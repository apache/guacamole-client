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

import { SimpleChange } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ManagedFilesystem } from '../../types/ManagedFilesystem';
import { FileComponent } from './file.component';

describe('FileComponent', () => {
    let fixture: ComponentFixture<FileComponent>;
    let component: FileComponent;
    let element: HTMLElement;

    /**
     * Creates a file of the given type.
     *
     * @param type
     *     The type of the file to create.
     *
     * @returns
     *     A new file with the given type.
     */
    function createFile(type: ManagedFilesystem.File.Type): ManagedFilesystem.File {
        return new ManagedFilesystem.File({
            mimetype  : 'text/plain',
            streamName: '/file',
            type      : type,
            name      : 'file'
        });
    }

    /**
     * Replaces the file displayed by the component, as the parent does
     * when the contents of a directory are reloaded.
     *
     * @param file
     *     The file to display.
     */
    function setFile(file: ManagedFilesystem.File): void {
        const previous = component.file;
        component.file = file;
        component.ngOnChanges({ file: new SimpleChange(previous, file, previous === null) });
        fixture.detectChanges();
    }

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [FileComponent]
        });
        fixture = TestBed.createComponent(FileComponent);
        component = fixture.componentInstance;
        setFile(createFile(ManagedFilesystem.File.Type.NORMAL));
        element = fixture.nativeElement.querySelector('.list-item');
    });

    it('should emit focusFile exactly once per click, even after the file was replaced', () => {

        const focusFile = vi.fn();
        component.focusFile.subscribe(focusFile);

        setFile(createFile(ManagedFilesystem.File.Type.NORMAL));
        setFile(createFile(ManagedFilesystem.File.Type.NORMAL));
        element.click();

        expect(focusFile).toHaveBeenCalledTimes(1);
    });

    it('should not download a file which is not focused', () => {

        const downloadFile = vi.fn();
        component.downloadFile.subscribe(downloadFile);

        setFile(createFile(ManagedFilesystem.File.Type.NORMAL));
        setFile(createFile(ManagedFilesystem.File.Type.NORMAL));
        element.click();

        expect(downloadFile).not.toHaveBeenCalled();
    });

    it('should download a focused file exactly once per click, even after the file was replaced', () => {

        const downloadFile = vi.fn();
        component.downloadFile.subscribe(downloadFile);

        const file = createFile(ManagedFilesystem.File.Type.NORMAL);
        setFile(createFile(ManagedFilesystem.File.Type.NORMAL));
        setFile(file);
        component.isFocused = true;
        element.click();

        expect(downloadFile).toHaveBeenCalledTimes(1);
        expect(downloadFile).toHaveBeenCalledWith(file);
    });

    it('should change the directory when a focused directory is clicked', () => {

        const changeDirectory = vi.fn();
        component.changeDirectory.subscribe(changeDirectory);

        const directory = createFile(ManagedFilesystem.File.Type.DIRECTORY);
        setFile(directory);
        component.isFocused = true;
        element.click();

        expect(changeDirectory).toHaveBeenCalledTimes(1);
        expect(changeDirectory).toHaveBeenCalledWith(directory);
    });

    it('should prevent text selection', () => {

        const event = new Event('selectstart', { bubbles: true, cancelable: true });
        element.dispatchEvent(event);

        expect(event.defaultPrevented).toBe(true);
    });
});
