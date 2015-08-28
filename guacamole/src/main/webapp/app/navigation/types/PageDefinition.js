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
 * Provides the PageDefinition class definition.
 */
angular.module('navigation').factory('PageDefinition', [function definePageDefinition() {

    /**
     * Creates a new PageDefinition object which pairs the URL of a page with
     * an arbitrary, human-readable name.
     *
     * @constructor
     * @param {String} name
     *     The the name of the page, which should be a translation table key.
     *
     * @param {String} url
     *     The URL of the page.
     */
    var PageDefinition = function PageDefinition(name, url) {

        /**
         * The the name of the page, which should be a translation table key.
         *
         * @type String
         */
        this.name = name;

        /**
         * The URL of the page.
         *
         * @type String
         */
        this.url = url;

    };

    return PageDefinition;

}]);
