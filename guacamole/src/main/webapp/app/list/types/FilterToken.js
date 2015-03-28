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
     * An arbitrary token having an associated type and value.
     *
     * @constructor
     * @param {String} consumed
     *     The input string consumed to produce this token.
     *
     * @param {String} type
     *     The type of this token. Each legal type name is a property within
     *     FilterToken.Types.
     *
     * @param {Object} value
     *     The value of this token. The type of this value is determined by
     *     the token type.
     */
    var FilterToken = function FilterToken(consumed, type, value) {

        /**
         * The input string that was consumed to produce this token.
         *
         * @type String
         */
        this.consumed = consumed;

        /**
         * The type of this token. Each legal type name is a property within
         * FilterToken.Types.
         *
         * @type String
         */
        this.type = type;

        /**
         * The value of this token.
         *
         * @type Object
         */
        this.value = value;

    };

    /**
     * All legal token types, and corresponding functions which match them.
     * Each function returns the parsed token, or null if no such token was
     * found.
     *
     * @type Object.<String, Function>
     */
    FilterToken.Types = {

        /**
         * A string literal, which may be quoted. The value of a LITERAL token
         * is a String.
         */
        LITERAL: function parseLiteral(str) {

            var pattern = /^"([^"]*)"|^\S+/;

            // Validate against pattern
            var matches = pattern.exec(str);
            if (!matches)
                return null;

            // If literal is quoted, parse within the quotes
            if (matches[1])
                return new FilterToken(matches[0], 'LITERAL', matches[1]);

            //  Otherwise, literal is unquoted
            return new FilterToken(matches[0], 'LITERAL', matches[0]);

        },

        /**
         * Arbitrary contiguous whitespace. The value of a WHITESPACE token is
         * a String.
         */
        WHITESPACE: function parseWhitespace(str) {

            var pattern = /^\s+/;

            // Validate against pattern
            var matches = pattern.exec(str);
            if (!matches)
                return null;

            //  Generate token from matching whitespace
            return new FilterToken(matches[0], 'WHITESPACE', matches[0]);

        }

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

                // Get matching function for current type
                var matcher = FilterToken.Types[type];

                // If token matches, return the matching group
                var token = matcher(str);
                if (token) {
                    str = str.substring(token.consumed.length);
                    return token;
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