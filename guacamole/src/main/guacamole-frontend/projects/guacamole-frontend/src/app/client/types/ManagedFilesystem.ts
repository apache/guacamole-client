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

import { ManagedClient } from './ManagedClient';
import { Optional } from '../../util/utility-types';

/**
 * Provides the ManagedFilesystem class used by ManagedClient to represent
 * available remote filesystems.
 *
 * Serves as a surrogate interface, encapsulating a Guacamole
 * filesystem object while it is active, allowing it to be detached and
 * reattached from different client views.
 */
export class ManagedFilesystem {

    /**
     * The client that originally received the "filesystem" instruction
     * that resulted in the creation of this ManagedFilesystem.
     */
    client: ManagedClient;

    /**
     * The Guacamole filesystem object, as received via a "filesystem"
     * instruction.
     */
    object: Guacamole.Object;

    /**
     * The declared, human-readable name of the filesystem
     */
    name: string;

    /**
     * The root directory of the filesystem.
     */
    root: ManagedFilesystem.File;

    /**
     * The current directory being viewed or manipulated within the
     * filesystem.
     */
    currentDirectory: ManagedFilesystem.File;

    /**
     * Creates a new ManagedFilesystem.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     ManagedFilesystem.
     */
    constructor(template: Optional<ManagedFilesystem, 'currentDirectory'>) {
        this.client = template.client;
        this.object = template.object;
        this.name = template.name;
        this.root = template.root;
        this.currentDirectory = template.currentDirectory || template.root;
    }

}

export namespace ManagedFilesystem {

    /**
     * A file within a ManagedFilesystem. Each ManagedFilesystem.File provides
     * sufficient information for retrieval or replacement of the file's
     * contents, as well as the file's name and type.
     */
    export class File {

        /**
         * The mimetype of the data contained within this file.
         */
        mimetype: string;

        /**
         * The name of the stream representing this files contents within its
         * associated filesystem object.
         */
        streamName: string;

        /**
         * The type of this file. All legal file type strings are defined
         * within ManagedFilesystem.File.Type.
         */
        type: string;

        /**
         * The name of this file.
         */
        name?: string;

        /**
         * The parent directory of this file. In the case of the root
         * directory, this will be null.
         */
        parent?: ManagedFilesystem.File;

        /**
         * Map of all known files contained within this file by name. This is
         * only applicable to directories.
         */
        files: Record<string, ManagedFilesystem.File>;

        /**
         * Creates a new ManagedFilesystem.File object.
         *
         * @param template
         *     The object whose properties should be copied within the new
         *     ManagedFilesystem.File.
         */
        constructor(template: Optional<File, | 'files' | 'name' | 'parent'>) {
            this.mimetype = template.mimetype;
            this.streamName = template.streamName;
            this.type = template.type;
            this.name = template.name;
            this.parent = template.parent;
            this.files = template.files || {};
        }
    }

}

export namespace ManagedFilesystem.File {

    /**
     * All legal type strings for a ManagedFilesystem.File.
     */
    export enum Type {
        /**
         * A normal file. As ManagedFilesystem does not currently represent any
         * other non-directory types of files, like symbolic links, this type
         * string may be used for any non-directory file.
         */
        NORMAL = 'NORMAL',

        /**
         * A directory.
         */
        DIRECTORY = 'DIRECTORY'
    }

}
