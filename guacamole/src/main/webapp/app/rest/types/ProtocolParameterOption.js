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
 * Service which defines the ProtocolParameterOption class.
 */
angular.module('rest').factory('ProtocolParameterOption', [function defineProtocolParameterOption() {
            
    /**
     * The object returned by REST API calls when representing a single possible
     * legal value of a configuration parameter of a remote desktop protocol.
     * 
     * @constructor
     * @param {ProtocolParameterOption|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     ProtocolParameterOption.
     */
    var ProtocolParameterOption = function ProtocolParameterOption(template) {

        // Use empty object by default
        template = template || {};

        /**
         * A human-readable name for this parameter value.
         *
         * @type String
         */
        this.title = template.title;

        /**
         * The actual value to set the parameter to, if this option is
         * selected.
         *
         * @type String
         */
        this.value = template.value;

    };

    return ProtocolParameterOption;

}]);