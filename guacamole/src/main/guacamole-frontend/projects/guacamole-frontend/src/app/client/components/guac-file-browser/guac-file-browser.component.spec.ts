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

import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ManagedFilesystemService } from '../../services/managed-filesystem.service';
import { ManagedFilesystem } from '../../types/ManagedFilesystem';
import { FileComponent } from '../file/file.component';
import { GuacFileBrowserComponent } from './guac-file-browser.component';

describe('GuacFileBrowserComponent', () => {
    let fixture: ComponentFixture<GuacFileBrowserComponent>;
    let filesystem: ManagedFilesystem;
    let managedFilesystemService: {
        refresh: ReturnType<typeof vi.fn>;
        changeDirectory: ReturnType<typeof vi.fn>;
        downloadFile: ReturnType<typeof vi.fn>;
    };

    /**
     * Creates a directory entry or normal file within the given directory.
     *
     * @param name
     *     The name of the file.
     *
     * @param type
     *     The type of the file.
     *
     * @returns
     *     A new file with the given name and type.
     */
    function createFile(name: string, type: ManagedFilesystem.File.Type): ManagedFilesystem.File {
        return new ManagedFilesystem.File({
            mimetype  : 'text/plain',
            streamName: '/' + name,
            type      : type,
            name      : name
        });
    }

    /**
     * Replaces the contents of the current directory with new file objects
     * of the given names, as the service does when a directory is reloaded.
     *
     * @param names
     *     The names of the normal files within the directory.
     */
    function reloadDirectory(...names: string[]): void {
        const files: Record<string, ManagedFilesystem.File> = {};
        for (const name of names)
            files[name] = createFile(name, ManagedFilesystem.File.Type.NORMAL);
        filesystem.currentDirectory().files.set(files);
        fixture.detectChanges();
    }

    /**
     * Returns the elements of all files in the file browser.
     *
     * @returns
     *     The elements of all files in the file browser.
     */
    function fileElements(): HTMLElement[] {
        return Array.from(fixture.nativeElement.querySelectorAll('guac-file'));
    }

    /**
     * Clicks the file with the given index.
     *
     * @param index
     *     The index of the file to click.
     */
    function click(index: number): void {
        fileElements()[index].querySelector<HTMLElement>('.list-item')!.click();
        fixture.detectChanges();
    }

    beforeEach(() => {

        managedFilesystemService = {
            refresh        : vi.fn(),
            changeDirectory: vi.fn(),
            downloadFile   : vi.fn()
        };

        TestBed.configureTestingModule({
            declarations: [GuacFileBrowserComponent, FileComponent],
            providers   : [{ provide: ManagedFilesystemService, useValue: managedFilesystemService }]
        });

        const root = createFile('', ManagedFilesystem.File.Type.DIRECTORY);
        filesystem = new ManagedFilesystem({
            client: null as never,
            object: null as never,
            name  : 'filesystem',
            root  : root,
            currentDirectory: signal(root)
        });

        fixture = TestBed.createComponent(GuacFileBrowserComponent);
        fixture.componentRef.setInput('filesystem', filesystem);
        reloadDirectory('a.txt', 'b.txt');
    });

    it('should mark the clicked file as focused and move the mark to another file', () => {

        click(0);
        expect(fileElements().map(e => e.classList.contains('focused'))).toEqual([true, false]);

        click(1);
        expect(fileElements().map(e => e.classList.contains('focused'))).toEqual([false, true]);
        expect(managedFilesystemService.downloadFile).not.toHaveBeenCalled();
    });

    it('should download a file exactly once when it is clicked a second time', () => {

        click(0);
        click(0);

        expect(managedFilesystemService.downloadFile).toHaveBeenCalledTimes(1);
        expect(managedFilesystemService.downloadFile).toHaveBeenCalledWith(filesystem, '/a.txt');
        expect(fileElements()[0].classList.contains('focused')).toBe(false);
    });

    it('should focus a file again with the third click', () => {

        click(0);
        click(0);
        click(0);

        expect(managedFilesystemService.downloadFile).toHaveBeenCalledTimes(1);
        expect(fileElements()[0].classList.contains('focused')).toBe(true);
    });

    it('should not download on the first click after the directory was reloaded', () => {

        click(0);
        reloadDirectory('a.txt', 'b.txt');
        reloadDirectory('a.txt', 'b.txt');

        expect(fileElements().some(e => e.classList.contains('focused'))).toBe(false);

        click(0);
        expect(managedFilesystemService.downloadFile).not.toHaveBeenCalled();
        expect(fileElements()[0].classList.contains('focused')).toBe(true);

        click(0);
        expect(managedFilesystemService.downloadFile).toHaveBeenCalledTimes(1);
    });
});
