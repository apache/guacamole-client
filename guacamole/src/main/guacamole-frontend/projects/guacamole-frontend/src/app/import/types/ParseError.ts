

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
 * An error representing a parsing failure when attempting to convert
 * user-provided data into a list of Connection objects.
 */
export class ParseError {

    /**
     * A human-readable message describing the error that occurred.
     */
    message: string;

    /**
     * The key associated with the translation string that used when
     * displaying this message.
     */
    key: string;

    /**
     * The object which should be passed through to the translation service
     * for the sake of variable substitution. Each property of the provided
     * object will be substituted for the variable of the same name within
     * the translation string.
     */
    variables: any;

    /**
     * Creates a new ParseError.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     ParseError.
     */
    constructor(template: ParseError) {

        this.message = template.message;
        this.key = template.key;
        this.variables = template.variables;

        // If no translation key is available, fall back to the untranslated
        // key, passing the raw message directly through the translation system
        if (!this.key) {
            this.key = 'APP.TEXT_UNTRANSLATED';
            this.variables = { MESSAGE: this.message };
        }

    }

}
