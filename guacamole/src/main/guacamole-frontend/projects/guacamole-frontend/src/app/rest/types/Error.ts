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

import { HttpErrorResponse } from '@angular/common/http';
import { DirectoryPatchOutcome } from './DirectoryPatchOutcome';
import { Field } from './Field';
import { TranslatableMessage } from './TranslatableMessage';

/**
 * Returned by REST API calls when an error occurs.
 */
export class Error {

    /**
     * A human-readable message describing the error that occurred.
     */
    message?: string;

    /**
     * A message which can be translated using the translation service,
     * consisting of a translation guac-key and optional set of substitution
     * variables.
     */
    translatableMessage?: TranslatableMessage;

    /**
     * The Guacamole protocol status code associated with the error that
     * occurred. This is only valid for errors of type STREAM_ERROR.
     */
    statusCode?: number | null;

    /**
     * The type string defining which values this parameter may contain,
     * as well as what properties are applicable. Valid types are listed
     * within Error.Type.
     *
     * @default Error.Type.INTERNAL_ERROR
     */
    type: Error.Type;

    /**
     * Any parameters which were expected in the original request, or are
     * now expected as a result of the original request, if any. If no
     * such information is available, this will be undefined.
     */
    expected?: Field[];

    /**
     * The outcome for each patch that was submitted as part of the request
     * that generated this error, if the request was a directory PATCH
     * request. In all other cases, this will be undefined.
     */
    patches?: DirectoryPatchOutcome[];

    /**
     * Creates a new Error.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     Error.
     */
    constructor(template: Partial<Error> | HttpErrorResponse = {}) {

        if (template instanceof HttpErrorResponse) {
            this.message = template.message;
            this.statusCode = template.status;
            this.type = Error.Type.INTERNAL_ERROR;
            return;
        }

        this.message = template.message;
        this.translatableMessage = template.translatableMessage;
        this.statusCode = template.statusCode;
        this.type = template.type || Error.Type.INTERNAL_ERROR;
        this.expected = template.expected;
        this.patches = template.patches;
    }
}

export namespace Error {

    /**
     * All valid error types.
     */
    export enum Type {
        /**
         * The requested operation could not be performed because the request
         * itself was malformed.
         */
        BAD_REQUEST = 'BAD_REQUEST',

        /**
         * The credentials provided were invalid.
         */
        INVALID_CREDENTIALS = 'INVALID_CREDENTIALS',

        /**
         * The credentials provided were not necessarily invalid, but were not
         * sufficient to determine validity.
         */
        INSUFFICIENT_CREDENTIALS = 'INSUFFICIENT_CREDENTIALS',

        /**
         * An internal server error has occurred.
         */
        INTERNAL_ERROR = 'INTERNAL_ERROR',

        /**
         * An object related to the request does not exist.
         */
        NOT_FOUND = 'NOT_FOUND',

        /**
         * Permission was denied to perform the requested operation.
         */
        PERMISSION_DENIED = 'PERMISSION_DENIED',

        /**
         * An error occurred within an intercepted stream, terminating that
         * stream. The Guacamole protocol status code of that error will be
         * stored within statusCode.
         */
        STREAM_ERROR = 'STREAM_ERROR'
    }
}
