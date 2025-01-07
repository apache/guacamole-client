

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

import { FormField } from 'guacamole-frontend-ext-lib';
import { TranslatableMessage } from './TranslatableMessage';

/**
 * The object returned by REST API calls when representing the data
 * associated with a field or configuration parameter.
 */
export class Field implements FormField {

    /**
     * The name which uniquely identifies this parameter.
     */
    name: string;

    /**
     * The type string defining which values this parameter may contain,
     * as well as what properties are applicable. Valid types are listed
     * within Field.Type.
     *
     * @default Field.Type.TEXT
     */
    type: Field.Type;

    /**
     * All possible legal values for this parameter.
     */
    options?: string[];

    /**
     * A message which can be translated using the translation service,
     * consisting of a translation key and optional set of substitution
     * variables.
     */
    translatableMessage?: TranslatableMessage;

    /**
     * The URL to which the user should be redirected when a field of type
     * REDIRECT is displayed.
     */
    redirectUrl?: string;

    /**
     * Creates a new Field instance.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     Field.
     */
    constructor(template: { name: string, type?: Field.Type, options?: string[] }) {
        this.name = template.name;
        this.type = template.type || Field.Type.TEXT;
        this.options = template.options;
    }

}

export namespace Field {

    /**
     * All valid field types.
     */
    export enum Type {
        /**
         * The type string associated with parameters that may contain a single
         * line of arbitrary text.
         */
        TEXT = 'TEXT',

        /**
         * The type string associated with parameters that may contain an email
         * address.
         */
        EMAIL = 'EMAIL',

        /**
         * The type string associated with parameters that may contain an
         * arbitrary string, where that string represents the username of the
         * user authenticating with the remote desktop service.
         */
        USERNAME = 'USERNAME',

        /**
         * The type string associated with parameters that may contain an
         * arbitrary string, where that string represents the password of the
         * user authenticating with the remote desktop service.
         */
        PASSWORD = 'PASSWORD',

        /**
         * The type string associated with parameters that may contain only
         * numeric values.
         */
        NUMERIC = 'NUMERIC',

        /**
         * The type string associated with parameters that may contain only a
         * single possible value, where that value enables the parameter's
         * effect. It is assumed that each BOOLEAN field will provide exactly
         * one possible value (option), which will be the value if that field
         * is true.
         */
        BOOLEAN = 'BOOLEAN',

        /**
         * The type string associated with parameters that may contain a
         * strictly-defined set of possible values.
         */
        ENUM = 'ENUM',

        /**
         * The type string associated with parameters that may contain any
         * number of lines of arbitrary text.
         */
        MULTILINE = 'MULTILINE',

        /**
         * Field type which allows selection of languages. The languages
         * displayed are the set of languages supported by the Guacamole web
         * application. Legal values are valid language IDs, as dictated by
         * the filenames of Guacamole's available translations.
         */
        LANGUAGE = 'LANGUAGE',

        /**
         * The type string associated with parameters that may contain timezone
         * IDs. Valid timezone IDs are dictated by Java=
         * http=//docs.oracle.com/javase/7/docs/api/java/util/TimeZone.html#getAvailableIDs%28%29
         */
        TIMEZONE = 'TIMEZONE',

        /**
         * The type string associated with parameters that may contain dates.
         * The format of the date is standardized as YYYY-MM-DD, zero-padded.
         */
        DATE = 'DATE',

        /**
         * The type string associated with parameters that may contain times.
         * The format of the time is stnadardized as HH:MM:DD, zero-padded,
         * 24-hour.
         */
        TIME = 'TIME',

        /**
         * An HTTP query parameter which is expected to be embedded in the URL
         * given to a user.
         */
        QUERY_PARAMETER = 'QUERY_PARAMETER',

        /**
         * The type string associated with parameters that may contain color
         * schemes accepted by the Guacamole server terminal emulator and
         * protocols which leverage it.
         */
        TERMINAL_COLOR_SCHEME = 'TERMINAL_COLOR_SCHEME',

        /**
         * Field type that supports redirecting the client browser to another
         * URL.
         */
        REDIRECT = 'REDIRECT'
    }

}
