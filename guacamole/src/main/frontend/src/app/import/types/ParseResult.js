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
     * The result of parsing a connection import file - containing a list of
     * API patches ready to be submitted to the PATCH REST API for batch
     * connection creation, a set of users and user groups to grant access to
     * each connection, and any errors that may have occured while parsing
     * each connection.
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
         * An array of patches, ready to be submitted to the PATCH REST API for
         * batch connection creation.
         *
         * @type {DirectoryPatch[]}
         */
        this.patches = template.patches || [];

        /**
         * A list of user identifiers that should be granted read access to the
         * the corresponding connection (at the same array index).
         *
         * @type {String[]}
         */
        this.users = template.users || [];

        /**
         * A list of user group identifiers that should be granted read access
         * to the corresponding connection (at the same array index).
         *
         * @type {String[]}
         */
        this.groups = template.groups || [];

        /**
         * An object whose keys are the user identifiers of every user specified
         * in the batch import. i.e. a set of all user identifiers.
         *
         * @type {Object.<String, Boolean>}
         */
        this.allUsers = template.allUsers || {};

        /**
         * An object whose keys are the user group identifiers of every user
         * group specified in the batch import. i.e. a set of all user group
         * identifiers.
         *
         * @type {Object.<String, Boolean>}
         */
        this.allGroups = template.allGroups || {};

        /**
         * An array of errors encountered while parsing the corresponding
         * connection (at the same array index). Each connection should have a
         * an array of errors. If empty, no errors occured for this connection.
         *
         * @type {ParseError[][]}
         */
        this.errors = template.errors || [];

        /**
         * True if any errors were encountered while parsing the connections
         * represented by this ParseResult. This should always be true if there
         * are a non-zero number of elements in the errors list for any
         * connection, or false otherwise.
         */
        this.hasErrors = template.hasErrors || false;

    };

    return ParseResult;
    
}]);
