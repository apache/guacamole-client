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
 * Service which defines the ProtocolParameter class.
 */
angular.module('rest').factory('ProtocolParameter', [function defineProtocolParameter() {
            
    /**
     * The object returned by REST API calls when representing the data
     * associated with a configuration parameter of a remote desktop protocol.
     * 
     * @constructor
     * @param {ProtocolParameter|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     ProtocolParameter.
     */
    var ProtocolParameter = function ProtocolParameter(template) {

        // Use empty object by default
        template = template || {};

        /**
         * The name which uniquely identifies this parameter.
         *
         * @type String
         */
        this.name = template.name;

        /**
         * A human-readable name for this parameter.
         *
         * @type String
         */
        this.title = template.title;

        /**
         * The type string defining which values this parameter may contain,
         * as well as what properties are applicable. Valid types are listed
         * within ProtocolParameter.Type.
         *
         * @type String
         * @default ProtocolParameter.Type.TEXT
         */
        this.type = template.type || ProtocolParameter.Type.TEXT;

        /**
         * The value to set the parameter to, in the case of a BOOLEAN
         * parameter, to enable that parameter's effect.
         *
         * @type String
         */
        this.value = template.value;

        /**
         * All possible legal values for this parameter. This property is only
         * applicable to ENUM type parameters.
         *
         * @type ProtocolParameterOption[]
         */
        this.options = template.options;

    };

    /**
     * All valid protocol parameter types.
     */
    ProtocolParameter.Type = {

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
         * effect.
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
        MULTILINE : "MULTILINE"

    };

    return ProtocolParameter;

}]);