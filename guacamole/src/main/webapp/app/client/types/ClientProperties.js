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
 * A service for generating new guacClient properties objects.
 */
angular.module('client').factory('ClientProperties', [function defineClientProperties() {
            
    /**
     * Object used for interacting with a guacClient directive.
     * 
     * @constructor
     * @param {ClientProperties|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     ClientProperties.
     */
    var ClientProperties = function ClientProperties(template) {

        // Use empty object by default
        template = template || {};

        /**
         * Whether the display should be scaled automatically to fit within the
         * available space.
         * 
         * @type Boolean
         */
        this.autoFit = template.autoFit || true;

        /**
         * The current scale. If autoFit is true, the effect of setting this
         * value is undefined.
         * 
         * @type Number
         */
        this.scale = template.scale || 1;

        /**
         * The minimum scale value.
         * 
         * @type Number
         */
        this.minScale = template.minScale || 1;

        /**
         * The maximum scale value.
         * 
         * @type Number
         */
        this.maxScale = template.maxScale || 3;

        /**
         * Whether or not the client should listen to keyboard events.
         * 
         * @type Boolean
         */
        this.keyboardEnabled = template.keyboardEnabled || true;
        
        /**
         * Whether translation of touch to mouse events should emulate an
         * absolute pointer device, or a relative pointer device.
         * 
         * @type Boolean
         */
        this.emulateAbsoluteMouse = template.emulateAbsoluteMouse || true;

        /**
         * The relative Y coordinate of the scroll offset of the display within
         * the client element.
         * 
         * @type Number
         */
        this.scrollTop = template.scrollTop || 0;

        /**
         * The relative X coordinate of the scroll offset of the display within
         * the client element.
         * 
         * @type Number
         */
        this.scrollLeft = template.scrollLeft || 0;

    };

    return ClientProperties;

}]);