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
 * Service which defines the FieldType class.
 */
angular.module('form').factory('FieldType', [function defineFieldType() {
            
    /**
     * The object used by the formService for describing field types.
     * 
     * @constructor
     * @param {FieldType|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     FieldType.
     */
    var FieldType = function FieldType(template) {

        // Use empty object by default
        template = template || {};

        /**
         * The URL of the template that should be injected into the DOM of a
         * form using this field type.
         *
         * @type String
         */
        this.templateUrl = template.templateUrl;

        /**
         * The name of the AngularJS module defining the controller for this
         * field type. This is optional, as not all field types will need
         * controllers.
         *
         * @type String
         */
        this.module = template.module;

        /**
         * The name of the controller for this field type. This is optional, as
         * not all field types will need controllers. If a controller is
         * specified, it will receive the following properties on the scope:
         *
         * namespace:
         *     A String which defines the unique namespace associated the
         *     translation strings used by the form using a field of this type.
         *
         * field:
         *     The Field object that is being rendered, representing a field of
         *     this type.
         *
         * model:
         *     The current String value of the field, if any.
         *
         * @type String
         */
        this.controller = template.controller;

    };

    return FieldType;

}]);