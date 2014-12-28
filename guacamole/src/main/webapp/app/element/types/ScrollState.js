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
 * Provides the ScrollState class definition.
 */
angular.module('element').factory('ScrollState', [function defineScrollState() {

    /**
     * Creates a new ScrollState, representing the current scroll position of
     * an arbitrary element. This constructor initializes the properties of the
     * new ScrollState with the corresponding properties of the given template.
     *
     * @constructor
     * @param {ScrollState|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     ScrollState.
     */
    var ScrollState = function ScrollState(template) {

        // Use empty object by default
        template = template || {};

        /**
         * The left edge of the view rectangle within the scrollable area. This
         * value naturally increases as the user scrolls right.
         *
         * @type Number
         */
        this.left = template.left || 0;

        /**
         * The top edge of the view rectangle within the scrollable area. This
         * value naturally increases as the user scrolls down.
         *
         * @type Number
         */
        this.top = template.top || 0;

    };

    return ScrollState;

}]);
