/*
 * Copyright (C) 2014 Glyptodon LLC
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
 * Service which defines the Field class.
 */
angular.module('rest').factory('Field', [function defineField() {
            
    /**
     * The object returned by REST API calls when representing the data
     * associated with a field or configuration parameter.
     * 
     * @constructor
     * @param {Field|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     Field.
     */
    var Field = function Field(template) {

        // Use empty object by default
        template = template || {};

        /**
         * The name which uniquely identifies this parameter.
         *
         * @type String
         */
        this.name = template.name;

        /**
         * The type string defining which values this parameter may contain,
         * as well as what properties are applicable. Valid types are listed
         * within Field.Type.
         *
         * @type String
         * @default Field.Type.TEXT
         */
        this.type = template.type || Field.Type.TEXT;

        /**
         * All possible legal values for this parameter.
         *
         * @type String[]
         */
        this.options = template.options;

    };

    /**
     * All valid field types.
     */
    Field.Type = {

        /**
         * The type string associated with parameters that may contain a single
         * line of arbitrary text.
         *
         * @type String
         */
        TEXT : "TEXT",

        /**
         * The type string associated with parameters that may contain an
         * arbitrary string, where that string represents the username of the
         * user authenticating with the remote desktop service.
         * 
         * @type String
         */
        USERNAME : "USERNAME",

        /**
         * The type string associated with parameters that may contain an
         * arbitrary string, where that string represents the password of the
         * user authenticating with the remote desktop service.
         * 
         * @type String
         */
        PASSWORD : "PASSWORD",

        /**
         * The type string associated with parameters that may contain only
         * numeric values.
         * 
         * @type String
         */
        NUMERIC : "NUMERIC",

        /**
         * The type string associated with parameters that may contain only a
         * single possible value, where that value enables the parameter's
         * effect. It is assumed that each BOOLEAN field will provide exactly
         * one possible value (option), which will be the value if that field
         * is true.
         * 
         * @type String
         */
        BOOLEAN : "BOOLEAN",

        /**
         * The type string associated with parameters that may contain a
         * strictly-defined set of possible values.
         * 
         * @type String
         */
        ENUM : "ENUM",

        /**
         * The type string associated with parameters that may contain any
         * number of lines of arbitrary text.
         *
         * @type String
         */
        MULTILINE : "MULTILINE",

        /**
         * The type string associated with parameters that may contain timezone
         * IDs. Valid timezone IDs are dictated by Java:
         * http://docs.oracle.com/javase/7/docs/api/java/util/TimeZone.html#getAvailableIDs%28%29
         *
         * @type String
         */
        TIMEZONE : "TIMEZONE",

        /**
         * The type string associated with parameters that may contain dates.
         * The format of the date is standardized as YYYY-MM-DD, zero-padded.
         *
         * @type String
         */
        DATE : "DATE",

        /**
         * The type string associated with parameters that may contain times.
         * The format of the time is stnadardized as HH:MM:DD, zero-padded,
         * 24-hour.
         *
         * @type String
         */
        TIME : "TIME"

    };

    return Field;

}]);