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
angular.module('client').factory('clientProperties', [function clientProperties() {
            
    /**
     * Object used for interacting with a guacClient directive.
     * 
     * @constructor
     */
    return function() {

        /**
         * Whether the display should be scaled automatically to fit within the
         * available space.
         * 
         * @type Boolean
         */
        this.autoFit = true;

        /**
         * The current scale. If autoFit is true, the effect of setting this
         * value is undefined.
         * 
         * @type Number
         */
        this.scale = 1;

        /**
         * The minimum scale value.
         * 
         * @type Number
         */
        this.minScale = 1;

        /**
         * The maximum scale value.
         * 
         * @type Number
         */
        this.maxScale = 3;

        /**
         * Whether or not the client should listen to keyboard events.
         * 
         * @type Boolean
         */
        this.keyboardEnabled = true;
    };

}]);