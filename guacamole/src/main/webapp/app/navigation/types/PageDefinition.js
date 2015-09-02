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
     * @param {PageDefinition|Object} template
     *     The object whose properties should be copied within the new
     *     PageDefinition.
     */
    var PageDefinition = function PageDefinition(template) {

        /**
         * The the name of the page, which should be a translation table key.
         * Alternatively, this may also be a list of names, where the final
         * name represents the page and earlier names represent categorization.
         * Those categorical names may be rendered hierarchically as a system
         * of menus, tabs, etc.
         *
         * @type String|String[]
         */
        this.name = template.name;

        /**
         * The URL of the page.
         *
         * @type String
         */
        this.url = template.url;

        /**
         * The CSS class name to associate with this page, if any. This will be
         * an empty string by default.
         *
         * @type String
         */
        this.className = template.className || '';

        /**
         * A numeric value denoting the relative sort order when compared to
         * other sibling PageDefinitions. If unspecified, sort order is
         * determined by the system using the PageDefinition.
         *
         * @type Number
         */
        this.weight = template.weight;

    };

    return PageDefinition;

}]);
