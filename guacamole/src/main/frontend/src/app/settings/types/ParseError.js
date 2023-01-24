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
 * Service which defines the ParseError class.
 */
angular.module('settings').factory('ParseError', [function defineParseError() {

    /**
     * An error representing a parsing failure when attempting to convert
     * user-provided data into a list of Connection objects.
     *
     * @constructor
     * @param {ParseError|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     ParseError.
     */
    var ParseError = function ParseError(template) {

        // Use empty object by default
        template = template || {};

        /**
         * A human-readable message describing the error that occurred.
         *
         * @type String
         */
        this.message = template.message;

        /**
         * A message which can be translated using the translation service,
         * consisting of a translation key and optional set of substitution
         * variables.
         *
         * @type TranslatableMessage
         */
        this.translatableMessage = template.translatableMessage;

    };

    return ParseError;

}]);
