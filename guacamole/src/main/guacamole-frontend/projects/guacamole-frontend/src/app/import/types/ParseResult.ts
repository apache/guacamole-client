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

import { ImportConnection } from './ImportConnection';
import { ParseError } from './ParseError';

/**
 * The result of parsing a connection import file: parsed connection entries
 * (ImportConnection objects that the import component turns into REST
 * patches), maps of users and user groups to grant access (keyed by patch
 * index), a group path per connection index, per-connection parse errors,
 * and aggregate flags.
 */
export class ParseResult {

    /**
     * An array of all connection data successfully parsed from the user-provided
     * import file. Each entry is an ImportConnection object that has been
     * processed by the field and tree transformers, but has not yet been
     * converted into a DirectoryPatch.
     * These objects act as the "blueprints" used by the import component to
     * determine which groups need to be created before the final
     * connection import occurs.
     */
    connectionObjects: ImportConnection[];

    /**
     * An object whose keys are the user identifiers of users specified
     * in the batch import, and whose values are an array of indices of
     * connections within the patches array to which those users should be
     * granted access.
     */
    users: Record<string, number[]>;

    /**
     * An object whose keys are the user group identifiers of every user
     * group specified in the batch import, and whose values are arrays of
     * connection indices (within the patch array) to grant that group access.
     */
    groups: Record<string, number[]>;

    /**
     * A map of connection index within the patch array, to connection group
     * path for that connection, of the form "ROOT/Parent/Child".
     */
    groupPaths: Record<string, string>;

    /**
     * An array of errors encountered while parsing the corresponding
     * connection (at the same array index in the patches array). Each
     * connection should have an array of errors. If empty, no errors
     * occurred for this connection.
     */
    errors: ParseError[][];

    /**
     * True if any errors were encountered while parsing the connections
     * represented by this ParseResult. This should always be true if there
     * are a non-zero number of elements in the errors list for any
     * connection, or false otherwise.
     */
    hasErrors: boolean;

    /**
     * The number of connections in this parse result (equal to
     * connectionObjects.length). The number of DirectoryPatch operations
     * built from this result may be larger when some connections are
     * replaced with permissions reset (REMOVE and ADD per such row).
     */
    connectionCount: number;

    /**
     * Creates a new ParseResult.
     */
    constructor(template: Partial<ParseResult> = {}) {

        this.connectionObjects = template.connectionObjects || [];
        this.users = template.users || {};
        this.groups = template.groups || {};
        this.groupPaths = template.groupPaths || {};
        this.errors = template.errors || [];
        this.hasErrors = template.hasErrors || false;
        this.connectionCount = template.connectionCount || 0;

    }
}
