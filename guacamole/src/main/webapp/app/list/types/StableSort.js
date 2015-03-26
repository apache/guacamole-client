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
 * A service for defining the StableSort class.
 */
angular.module('list').factory('StableSort', [
    function defineStableSort() {

    /**
     * Maintains a sorting predicate as required by the Angular orderBy filter.
     * The order of properties sorted by the predicate can be altered while
     * otherwise maintaining the sort order.
     *
     * @constructor
     * @param {String[]} predicate
     *     The properties to sort by, in order of precidence.
     */
    var StableSort = function StableSort(predicate) {

        /**
         * Reference to this instance.
         *
         * @type StableSort
         */
        var stableSort = this;

        /**
         * The current sorting predicate.
         *
         * @type String[]
         */
        this.predicate = predicate;

        /**
         * The name of the highest-precedence sorting property.
         *
         * @type String
         */
        this.primary = predicate[0];

        /**
         * Whether the highest-precedence sorting property is sorted in
         * descending order.
         *
         * @type Boolean
         */
        this.descending = false;

        // Handle initially-descending primary properties
        if (this.primary.charAt(0) === '-') {
            this.primary = this.primary.substring(1);
            this.descending = true;
        }

        /**
         * Reorders the currently-defined predicate such that the named
         * property takes precidence over all others. The property will be
         * sorted in ascending order unless otherwise specified.
         *
         * @param {String} name
         *     The name of the property to reorder by.
         *
         * @param {Boolean} [descending=false]
         *     Whether the property should be sorted in descending order. By
         *     default, all properties are sorted in ascending order.
         */
        this.reorder = function reorder(name, descending) {

            // Build ascending and descending predicate components
            var ascendingName  = name;
            var descendingName = '-' + name;

            // Remove requested property from current predicate
            stableSort.predicate = stableSort.predicate.filter(function notRequestedProperty(current) {
                return current !== ascendingName
                    && current !== descendingName;
            });

            // Add property to beginning of predicate
            if (descending)
                stableSort.predicate.unshift(descendingName);
            else
                stableSort.predicate.unshift(ascendingName);

            // Update sorted state
            stableSort.primary    = name;
            stableSort.descending = !!descending;

        };

        /**
         * Returns whether the sort order is primarily determined by the given
         * property.
         *
         * @param {String} property
         *     The name of the property to check.
         *
         * @returns {Boolean}
         *     true if the sort order is primarily determined by the given
         *     property, false otherwise.
         */
        this.isSortedBy = function isSortedBy(property) {
            return stableSort.primary === property;
        };

        /**
         * Sets the primary sorting property to the given property, if not already
         * set. If already set, the ascending/descending sort order is toggled.
         *
         * @param {String} property
         *     The name of the property to assign as the primary sorting property.
         */
        this.togglePrimary = function togglePrimary(property) {

            // Sort in ascending order by new property, if different
            if (!stableSort.isSortedBy(property))
                stableSort.reorder(property, false);

            // Otherwise, toggle sort order
            else
                stableSort.reorder(property, !stableSort.descending);

        };

    };

    return StableSort;

}]);