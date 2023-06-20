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

import { compile } from 'angular-expressions';
import { IPv4Network } from './IPv4Network';
import { IPv6Network } from './IPv6Network';
import { FilterToken } from './FilterToken';
import isObjectLike from 'lodash/isObjectLike';

/**
 * Handles compilation of filtering predicates as used by
 * the {@link FilterService}. Predicates are compiled from a user-
 * specified search string.
 * TODO: Look at doc
 */
export class FilterPattern {

    /**
     * Array of getters corresponding to the Angular expressions provided
     * to the constructor of this class. The functions returns are those
     * produced by the $parse service.
     */
    private readonly getters: Function[];

    /**
     * The current filtering predicate.
     */
    predicate: Function;

    /**
     * Create a new FilterPattern.
     *
     * @param expressions
     *     The expressions whose values are to be filtered.
     */
    constructor(expressions: string[]) {
        this.getters = expressions.map(expression => compile(expression));
        this.predicate = this.nullPredicate;
    }

    /**
     * Filter predicate which simply matches everything. This function
     * always returns true.
     *
     * @returns
     *     true.
     */
    private nullPredicate(): boolean {
        return true;
    }

    /**
     * Determines whether the given object matches the given filter pattern
     * token.
     *
     * @param object
     *     The object to match the token against.
     *
     * @param token
     *     The token from the tokenized filter pattern to match aginst the
     *     given object.
     *
     * @returns
     *     true if the object matches the token, false otherwise.
     */
    private matchesToken(object: object, token: FilterToken): boolean {

        // Match depending on token type
        switch (token.type) {

            // Simple string literal
            case 'LITERAL':
                return this.matchesString(object, token.value);

            // IPv4 network address / subnet
            case 'IPV4_NETWORK':
                return this.matchesIPv4(object, token.value);

            // IPv6 network address / subnet
            case 'IPV6_NETWORK':
                return this.matchesIPv6(object, token.value);

            // Unsupported token type
            default:
                return false;

        }

    }

    /**
     * Determines whether the given object contains properties that match
     * the given string, according to the provided getters.
     *
     * @param object
     *     The object to match against.
     *
     * @param str
     *     The string to match.
     *
     * @returns
     *     true if the object matches the given string, false otherwise.
     */
    private matchesString(object: object, str: string): boolean {

        // For each defined getter
        for (let i = 0; i < this.getters.length; i++) {

            // Retrieve value of current getter
            let value: any;
            if (isObjectLike(object))
                value = (this.getters)[i](object);
            // If the given object is a primitive value use the value directly
            else
                value = object;

            // If the value matches the pattern, the whole object matches
            if (String(value).toLowerCase().indexOf(str) !== -1)
                return true;

        }

        // No matches found
        return false;

    }

    /**
     * Determines whether the given object contains properties that match
     * the given IPv4 network, according to the provided getters.
     *
     * @param object
     *     The object to match against.
     *
     * @param network
     *     The IPv4 network to match.
     *
     * @returns
     *     true if the object matches the given network, false otherwise.
     */
    private matchesIPv4(object: object, network: IPv4Network): boolean {

        // For each defined getter
        for (let i = 0; i < this.getters.length; i++) {

            // Test each possible IPv4 address within the string against
            // the given IPv4 network
            const addresses = String((this.getters)[i](object)).split(/[^0-9.]+/);
            for (let j = 0; j < addresses.length; j++) {
                const value = IPv4Network.parse(addresses[j]);
                if (value && network.contains(value))
                    return true;
            }

        }

        // No matches found
        return false;

    }

    /**
     * Determines whether the given object contains properties that match
     * the given IPv6 network, according to the provided getters.
     *
     * @param object
     *     The object to match against.
     *
     * @param network
     *     The IPv6 network to match.
     *
     * @returns
     *     true if the object matches the given network, false otherwise.
     */
    matchesIPv6(object: object, network: IPv6Network): boolean {

        // For each defined getter
        for (let i = 0; i < this.getters.length; i++) {

            // Test each possible IPv6 address within the string against
            // the given IPv6 network
            const addresses = String((this.getters)[i](object)).split(/[^0-9A-Fa-f:]+/);
            for (let j = 0; j < addresses.length; j++) {
                const value = IPv6Network.parse(addresses[j]);
                if (value && network.contains(value))
                    return true;
            }

        }

        // No matches found
        return false;

    }

    /**
     * Compiles the given pattern string, assigning the resulting filter
     * predicate. The resulting predicate will accept only objects that
     * match the given pattern.
     *
     * @param pattern
     *     The pattern to compile.
     */
    compile(pattern: string): void {

        // If no pattern provided, everything matches
        if (!pattern) {
            this.predicate = this.nullPredicate;
            return;
        }

        // Tokenize pattern, converting to lower case for case-insensitive matching
        const tokens = FilterToken.tokenize(pattern.toLowerCase());

        // Return predicate which matches against the value of any getter in the getters array
        this.predicate = (object: object) => {

            // False if any token does not match
            for (let i = 0; i < tokens.length; i++) {
                if (!this.matchesToken(object, tokens[i]))
                    return false;
            }

            // True if all tokens matched
            return true;

        };

    }

}
