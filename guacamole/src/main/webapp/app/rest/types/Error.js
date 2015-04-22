/*
 * Copyright (C) 2015 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/**
 * Service which defines the Error class.
 */
angular.module('rest').factory('Error', [function defineError() {

    /**
     * The object returned by REST API calls when an error occurs.
     *
     * @constructor
     * @param {Error|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     Error.
     */
    var Error = function Error(template) {

        // Use empty object by default
        template = template || {};

        /**
         * A human-readable message describing the error that occurred.
         *
         * @type String
         */
        this.message = template.message;

        /**
         * The type string defining which values this parameter may contain,
         * as well as what properties are applicable. Valid types are listed
         * within Error.Type.
         *
         * @type String
         * @default Error.Type.INTERNAL_ERROR
         */
        this.type = template.type || Error.Type.INTERNAL_ERROR;

        /**
         * Any parameters which were expected in the original request, or are
         * now expected as a result of the original request, if any. If no
         * such information is available, this will be null.
         *
         * @type Field[]
         */
        this.expected = template.expected;

    };

    /**
     * All valid field types.
     */
    Error.Type = {

        /**
         * The requested operation could not be performed because the request
         * itself was malformed.
         *
         * @type String
         */
        BAD_REQUEST : 'BAD_REQUEST',

        /**
         * The credentials provided were invalid.
         *
         * @type String
         */
        INVALID_CREDENTIALS : 'INVALID_CREDENTIALS',

        /**
         * The credentials provided were not necessarily invalid, but were not
         * sufficient to determine validity.
         *
         * @type String
         */
        INSUFFICIENT_CREDENTIALS : 'INSUFFICIENT_CREDENTIALS',

        /**
         * An internal server error has occurred.
         *
         * @type String
         */
        INTERNAL_ERROR : 'INTERNAL_ERROR',

        /**
         * An object related to the request does not exist.
         *
         * @type String
         */
        NOT_FOUND : 'NOT_FOUND',

        /**
         * Permission was denied to perform the requested operation.
         *
         * @type String
         */
        PERMISSION_DENIED : 'PERMISSION_DENIED'

    };

    return Error;

}]);