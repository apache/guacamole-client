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
 * Provides the Prompt class definition.
 */
angular.module('prompt').factory('Prompt', [function definePrompt() {

    /**
     * Creates a new Prompt, where the properties of that Prompt are set to
     * the corresponding properties of the given template.
     *
     * @constructor
     * @param {Prompt|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     Prompt.
     */
    var Prompt = function Prompt(template) {

        // Use empty object by default
        template = template || {};

        /**
         * The CSS class to associate with the prompt, if any.
         *
         * @type String
         */
        this.className = template.className;
        
        /**
         * The object that stores response to the prompts.
         * 
         * @type Object{String, String}
         */
        this.responses = template.responses;

        /**
         * An array of all actions available to the user in response to this
         * prompt.
         *
         * @type PromptAction[]
         */
        this.actions = template.actions || [];

    };

    return Prompt;

}]);