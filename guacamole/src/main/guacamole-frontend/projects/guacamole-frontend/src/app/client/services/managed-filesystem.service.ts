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

import { Injectable } from '@angular/core';
import { TunnelService } from '../../rest/service/tunnel.service';
import { ManagedClient } from '../types/ManagedClient';
import { ManagedFilesystem } from '../types/ManagedFilesystem';

/**
 * A service for working with ManagedFilesystem objects.
 */
@Injectable({
    providedIn: 'root'
})
export class ManagedFilesystemService {

    /**
     * Inject required services.
     */
    constructor(private tunnelService: TunnelService) {
    }

    /**
     * Refreshes the contents of the given file, if that file is a directory.
     * Only the immediate children of the file are refreshed. Files further
     * down the directory tree are not refreshed.
     *
     * @param filesystem
     *     The filesystem associated with the file being refreshed.
     *
     * @param file
     *     The file being refreshed.
     */
    refresh(filesystem: ManagedFilesystem, file: ManagedFilesystem.File): void {

        // @ts-ignore
        // Do not attempt to refresh the contents of directories
        if (file.mimetype !== Guacamole.Object.STREAM_INDEX_MIMETYPE)
            return;

        // Request contents of given file
        filesystem.object.requestInputStream(file.streamName, (stream: Guacamole.InputStream, mimetype: string) => {

            // @ts-ignore
            // Ignore stream if mimetype is wrong
            if (mimetype !== Guacamole.Object.STREAM_INDEX_MIMETYPE) {
                stream.sendAck('Unexpected mimetype', Guacamole.Status.Code.UNSUPPORTED);
                return;
            }

            // Signal server that data is ready to be received
            stream.sendAck('Ready', Guacamole.Status.Code.SUCCESS);

            // Read stream as JSON
            const reader = new Guacamole.JSONReader(stream);

            // Acknowledge received JSON blobs
            reader.onprogress = function onprogress() {
                stream.sendAck('Received', Guacamole.Status.Code.SUCCESS);
            };

            // Reset contents of directory
            reader.onend = function jsonReady() {

                // Empty contents
                file.files.set({});

                // Determine the expected filename prefix of each stream
                let expectedPrefix = file.streamName;
                if (expectedPrefix.charAt(expectedPrefix.length - 1) !== '/')
                    expectedPrefix += '/';

                // For each received stream name
                const mimetypes = reader.getJSON();
                for (const name in mimetypes) {

                    // Assert prefix is correct
                    if (name.substring(0, expectedPrefix.length) !== expectedPrefix)
                        continue;

                    // Extract filename from stream name
                    const filename = name.substring(expectedPrefix.length);

                    // Deduce type from mimetype
                    let type = ManagedFilesystem.File.Type.NORMAL;
                    // @ts-ignore
                    if (mimetypes[name] === Guacamole.Object.STREAM_INDEX_MIMETYPE)
                        type = ManagedFilesystem.File.Type.DIRECTORY;

                    // Add file entry
                    file.files.update(files => {
                        files[filename] = new ManagedFilesystem.File({
                            // @ts-ignore
                            mimetype  : mimetypes[name],
                            streamName: name,
                            type      : type,
                            parent    : file,
                            name      : filename
                        });
                        return files;
                    });

                }

            };

        });

    }

    /**
     * Creates a new ManagedFilesystem instance from the given Guacamole.Object
     * and human-readable name. Upon creation, a request to populate the
     * contents of the root directory will be automatically dispatched.
     *
     * @param client
     *     The client that originally received the "filesystem" instruction
     *     that resulted in the creation of this ManagedFilesystem.
     *
     * @param object
     *     The Guacamole.Object defining the filesystem.
     *
     * @param name
     *     A human-readable name for the filesystem.
     *
     * @returns
     *     The newly-created ManagedFilesystem.
     */
    getInstance(client: ManagedClient, object: Guacamole.Object, name: string): ManagedFilesystem {

        // Init new filesystem object
        const managedFilesystem = new ManagedFilesystem({
            client: client,
            object: object,
            name  : name,
            root  : new ManagedFilesystem.File({
                // @ts-ignore
                mimetype: Guacamole.Object.STREAM_INDEX_MIMETYPE,
                // @ts-ignore
                streamName: Guacamole.Object.ROOT_STREAM,
                type      : ManagedFilesystem.File.Type.DIRECTORY
            })
        });

        // Retrieve contents of root
        this.refresh(managedFilesystem, managedFilesystem.root);

        return managedFilesystem;

    }

    /**
     * Downloads the given file from the server using the given Guacamole
     * client and filesystem. The browser will automatically start the
     * download upon completion of this function.
     *
     * @param managedFilesystem
     *     The ManagedFilesystem from which the file is to be downloaded. Any
     *     path information provided must be relative to this filesystem.
     *
     * @param path
     *     The full, absolute path of the file to download.
     */
    downloadFile(managedFilesystem: ManagedFilesystem, path: string): void {

        // Request download
        managedFilesystem.object.requestInputStream(path, (stream: Guacamole.InputStream, mimetype: string) => {

            // Parse filename from string
            const filename = path.match(/(.*[\\/])?(.*)/)![2];

            // Start download
            this.tunnelService.downloadStream(managedFilesystem.client.tunnel.uuid!, stream, mimetype, filename);

        });

    }

    /**
     * Changes the current directory of the given filesystem, automatically
     * refreshing the contents of that directory.
     *
     * @param filesystem
     *     The filesystem whose current directory should be changed.
     *
     * @param file
     *     The directory to change to.
     */
    changeDirectory(filesystem: ManagedFilesystem, file: ManagedFilesystem.File): void {

        // Refresh contents
        this.refresh(filesystem, file);

        // Set current directory
        filesystem.currentDirectory.set(file);

    }

}
