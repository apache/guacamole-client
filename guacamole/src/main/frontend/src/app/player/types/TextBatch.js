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
 * Service which defines the TextBatch class.
 */
angular.module('player').factory('TextBatch', [function defineTextBatch() {

    /**
     * A batch of text associated with a recording. The batch consists of a
     * string representation of the text that would be typed based on the key
     * events in the recording, as well as a timestamp when the batch started.
     *
     * @constructor
     * @param {TextBatch|Object} [template={}]
     *     The object whose properties should be copied within the new TextBatch.
     */
    const TextBatch = function TextBatch(template) {

        // Use empty object by default
        template = template || {};

        /**
         * The text that was typed in this batch.
         *
         * @type String
         */
        this.text = template.text;

        /**
         * The timestamp at which the batch of text was typed.
         *
         * @type Number
         */
        this.timestamp = template.timestamp;

    };

    return TextBatch;

}]);
