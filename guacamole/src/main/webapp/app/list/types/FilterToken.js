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
 * A service for defining the FilterToken class.
 */
angular.module('list').factory('FilterToken', [
    function defineFilterToken() {

    /**
     * An arbitrary token having an associated type and string value.
     *
     * @constructor
     * @param {String} type
     *     The type of this token. Each legal type name is a property within
     *     FilterToken.Types.
     *
     * @param {String} value
     *     The string value of this token.
     */
    var FilterToken = function FilterToken(type, value) {

        /**
         * The type of this token. Each legal type name is a property within
         * FilterToken.Types.
         *
         * @type String
         */
        this.type = type;

        /**
         * The string value of this token.
         *
         * @type String
         */
        this.value = value;

    };

    /**
     * All legal token types, and corresponding regular expressions which match
     * them. If the regular expression contains capturing groups, the last
     * matching group will be used as the value of the token.
     *
     * @type Object.<String, RegExp>
     */
    FilterToken.Types = {

        /**
         * A string literal.
         */
        LITERAL: /^"([^"]*)"|^\S+/,

        /**
         * Arbitrary contiguous whitespace.
         */
        WHITESPACE: /^\s+/

    };

    /**
     * Tokenizes the given string, returning an array of tokens. Whitespace
     * tokens are dropped.
     *
     * @param {String} str
     *     The string to tokenize.
     *
     * @returns {FilterToken[]}
     *     All tokens identified within the given string, in order.
     */
    FilterToken.tokenize = function tokenize(str) {

        var tokens = [];

        /**
         * Returns the first token on the current string, removing the token
         * from that string.
         *
         * @returns FilterToken
         *     The first token on the string, or null if no tokens match.
         */
        var popToken = function popToken() {

            // Attempt to find a matching token
            for (var type in FilterToken.Types) {

                // Get regular expression for current type
                var regex = FilterToken.Types[type];

                // If token matches, return the matching group
                var match = regex.exec(str);
                if (match) {

                    // Advance to next token
                    str = str.substring(match[0].length);

                    // Grab last matching group
                    var matchingGroup = match[0];
                    for (var i=1; i < match.length; i++)
                        matchingGroup = match[i] || matchingGroup;

                    // Return new token
                    return new FilterToken(type, matchingGroup);

                }

            }

            // No match
            return null;

        };

        // Tokenize input until no input remains
        while (str) {

            // Remove first token
            var token = popToken();
            if (!token)
                break;

            // Add token to tokens array, if not whitespace
            if (token.type !== 'WHITESPACE')
                tokens.push(token);

        }

        return tokens;

    };

    return FilterToken;

}]);