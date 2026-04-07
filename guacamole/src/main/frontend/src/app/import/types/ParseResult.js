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

/**
 * Service which defines the ParseResult class.
 */
angular.module('import').factory('ParseResult', [function defineParseResult() {

    /**
     * The result of parsing a connection import file: parsed connection entries
     * (ImportConnection objects that the import controller turns into REST
     * patches), maps of users and user groups to grant access (keyed by patch
     * index), a group path per connection index, per-connection parse errors,
     * and aggregate flags.
     *
     * @constructor
     * @param {ParseResult|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     ParseResult.
     */
    const ParseResult = function ParseResult(template) {

        // Use empty object by default
        template = template || {};

        /**
         * An array of all connection data successfully parsed from the user-provided 
         * import file. Each entry is an ImportConnection object that has been 
         * processed by the field and tree transformers, but has not yet been 
         * converted into a DirectoryPatch. 
         * These objects act as the "blueprints" used by the controller to 
         * determine which groups need to be created before the final 
         * connection import occurs.
         *
         * @type {ImportConnection[]}
         */
        this.connectionObjects = template.connectionObjects || [];

        /**
         * An object whose keys are the user identifiers of users specified
         * in the batch import, and whose values are an array of indices of
         * connections within the patches array to which those users should be
         * granted access.
         *
         * @type {Object.<String, Integer[]>}
         */
        this.users = template.users || {};

        /**
         * An object whose keys are the user group identifiers of every user
         * group specified in the batch import, and whose values are arrays of
         * connection indices (within the patch array) to grant that group access.
         *
         * @type {Object.<String, Integer[]>}
         */
        this.groups = template.groups || {};

        /**
         * A map of connection index within the patch array, to connection group
         * path for that connection, of the form "ROOT/Parent/Child".
         *
         * @type {Object.<String, String>}
         */
        this.groupPaths = template.groupPaths || {};

        /**
         * An array of errors encountered while parsing the corresponding
         * connection (at the same array index in the patches array). Each 
         * connection should have an array of errors. If empty, no errors
         * occurred for this connection.
         *
         * @type {ParseError[][]}
         */
        this.errors = template.errors || [];

        /**
         * True if any errors were encountered while parsing the connections
         * represented by this ParseResult. This should always be true if there
         * are a non-zero number of elements in the errors list for any
         * connection, or false otherwise.
         *
         * @type {Boolean}
         */
        this.hasErrors = template.hasErrors || false;

        /**
         * The number of connections in this parse result (equal to
         * connectionObjects.length). The number of DirectoryPatch operations
         * built from this result may be larger when some connections are
         * replaced with permissions reset (REMOVE and ADD per such row).
         *
         * @type {Number}
         */
        this.connectionCount = template.connectionCount || 0;

    };

    return ParseResult;

}]);
