/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import { IPv4Network } from './IPv4Network';
import { IPv6Network } from './IPv6Network';

/**
 * An arbitrary token having an associated type and value.
 */
export class FilterToken {

    /**
     * The input string that was consumed to produce this token.
     */
    consumed: string;

    /**
     * The type of this token. Each legal type name is a property within
     * FilterToken.Types.
     */
    type: string;

    /**
     * The value of this token.
     */
    value: any;

    /**
     * Create a new FilterToken.
     *
     * @param consumed
     *     The input string consumed to produce this token.
     *
     * @param type
     *     The type of this token. Each legal type name is a property within
     *     FilterToken.Types.
     *
     * @param value
     *     The value of this token. The type of this value is determined by
     *     the token type.
     */
    constructor(consumed: string, type: string, value: any) {

        this.consumed = consumed;
        this.type = type;
        this.value = value;

    }

    /**
     * All legal token types, and corresponding functions which match them.
     * Each function returns the parsed token, or null if no such token was
     * found.
     */
    static Types: Record<string, (str: string) => FilterToken | null> = {

        /**
         * An IPv4 address or subnet. The value of an IPV4_NETWORK token is an
         * IPv4Network.
         */
        IPV4_NETWORK: function parseIPv4(str: string): FilterToken | null {

            const pattern = /^\S+/;

            // Read first word via regex
            const matches = pattern.exec(str);
            if (!matches)
                return null;

            // Validate and parse as IPv4 address
            const network = IPv4Network.parse(matches[0]);
            if (!network)
                return null;

            return new FilterToken(matches[0], 'IPV4_NETWORK', network);

        },

        /**
         * An IPv6 address or subnet. The value of an IPV6_NETWORK token is an
         * IPv6Network.
         */
        IPV6_NETWORK: function parseIPv6(str: string): FilterToken | null {

            const pattern = /^\S+/;

            // Read first word via regex
            const matches = pattern.exec(str);
            if (!matches)
                return null;

            // Validate and parse as IPv6 address
            const network = IPv6Network.parse(matches[0]);
            if (!network)
                return null;

            return new FilterToken(matches[0], 'IPV6_NETWORK', network);

        },

        /**
         * A string literal, which may be quoted. The value of a LITERAL token
         * is a String.
         */
        LITERAL: function parseLiteral(str: string): FilterToken | null {

            const pattern = /^"([^"]*)"|^\S+/;

            // Validate against pattern
            const matches = pattern.exec(str);
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
        WHITESPACE: function parseWhitespace(str: string): FilterToken | null {

            const pattern = /^\s+/;

            // Validate against pattern
            const matches = pattern.exec(str);
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
     * @param str
     *     The string to tokenize.
     *
     * @returns
     *     All tokens identified within the given string, in order.
     */
    static tokenize(str: string): FilterToken[] {

        const tokens: FilterToken[] = [];

        /**
         * Returns the first token on the current string, removing the token
         * from that string.
         *
         * @returns
         *     The first token on the string, or null if no tokens match.
         */
        const popToken = (): FilterToken | null => {

            // Attempt to find a matching token
            for (const type in FilterToken.Types) {

                // Get matching function for current type
                const matcher = FilterToken.Types[type];

                // If token matches, return the matching group
                const token = matcher(str);
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
            const token = popToken();
            if (!token)
                break;

            // Add token to tokens array, if not whitespace
            if (token.type !== 'WHITESPACE')
                tokens.push(token);

        }

        return tokens;

    }
}
