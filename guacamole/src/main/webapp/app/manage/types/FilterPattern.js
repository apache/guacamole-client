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
 * A service for defining the FilterPattern class.
 */
angular.module('manage').factory('FilterPattern', [
    function defineFilterPattern() {

    /**
     * Object which handles compilation of filtering predicates as used by
     * the Angular "filter" filter. Predicates are compiled from a user-
     * specified search string.
     *
     * @constructor
     */
    var FilterPattern = function FilterPattern() {

        /**
         * Reference to this instance.
         *
         * @type FilterPattern
         */
        var filterPattern = this;

        /**
         * Filter predicate which simply matches everything. This function
         * always returns true.
         *
         * @returns {Boolean}
         *     true.
         */
        var nullPredicate = function nullPredicate() {
            return true;
        };

        /**
         * The current filtering predicate.
         *
         * @type Function
         */
        this.predicate = nullPredicate;

        /**
         * Compiles the given pattern string, assigning the resulting filter
         * predicate. The resulting predicate will accept only objects that
         * match the given pattern.
         * 
         * @param {String} pattern
         *     The pattern to compile.
         */
        this.compile = function compile(pattern) {

            // If no pattern provided, everything matches
            if (!pattern) {
                filterPattern.predicate = nullPredicate;
                return;
            }
                
            // Convert to lower case for case insensitive matching            
            pattern = pattern.toLowerCase();

            // TODONT: Return predicate specific to a type of object this class should know nothing about
            filterPattern.predicate = function oddlySpecificPredicate(wrapper) {

                // Check to see if the search string matches the connection name
                if (wrapper.name.toLowerCase().indexOf(pattern) !== -1) 
                    return true;

                // Check to see if the search string matches the username
                if (wrapper.activeConnection.username.toLowerCase().indexOf(pattern) !== -1) 
                    return true;

                // Check to see if the search string matches the remote host
                if (wrapper.activeConnection.remoteHost.toLowerCase().indexOf(pattern) !== -1) 
                    return true;

                return false;
            };
            
        };

    };

    return FilterPattern;

}]);