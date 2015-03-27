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
angular.module('list').factory('FilterPattern', ['$injector',
    function defineFilterPattern($injector) {

    // Required types
    var FilterToken = $injector.get('FilterToken');

    // Required services
    var $parse = $injector.get('$parse');

    /**
     * Object which handles compilation of filtering predicates as used by
     * the Angular "filter" filter. Predicates are compiled from a user-
     * specified search string.
     *
     * @constructor
     * @param {String[]} expressions 
     *     The Angular expressions whose values are to be filtered.
     */
    var FilterPattern = function FilterPattern(expressions) {

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
         * Array of getters corresponding to the Angular expressions provided
         * to the constructor of this class. The functions returns are those
         * produced by the $parse service.
         *
         * @type Function[]
         */
        var getters = [];

        // Parse all expressions
        angular.forEach(expressions, function parseExpression(expression) {
            getters.push($parse(expression));
        });

        /**
         * Determines whether the given object matches the given filter pattern
         * token.
         *
         * @param {Object} object
         *     The object to match the token against.
         * 
         * @param {FilterToken} token
         *     The token from the tokenized filter pattern to match aginst the
         *     given object.
         *
         * @returns {Boolean}
         *     true if the object matches the token, false otherwise.
         */
        var matchesToken = function matchToken(object, token) {

            // Only match against literals
            if (token.type !== 'LITERAL')
                return false;

            // For each defined getter
            for (var i=0; i < getters.length; i++) {

                // Retrieve value of current getter
                var value = getters[i](object);

                // If the value matches the pattern, the whole object matches
                if (String(value).toLowerCase().indexOf(token.value) !== -1) 
                    return true;

            }

            // No matches found
            return false;

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
                
            // Tokenize pattern, converting to lower case for case-insensitive matching
            var tokens = FilterToken.tokenize(pattern.toLowerCase());

            // Return predicate which matches against the value of any getter in the getters array
            filterPattern.predicate = function matchesAllTokens(object) {

                // False if any token does not match
                for (var i=0; i < tokens.length; i++) {
                    if (!matchesToken(object, tokens[i]))
                        return false;
                }

                // True if all tokens matched
                return true;

            };
            
        };

    };

    return FilterPattern;

}]);