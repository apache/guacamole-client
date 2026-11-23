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

import AngularExpression from 'angular-expressions';
import _ from 'lodash';
import { FilterToken } from './FilterToken';
import { IPv4Network } from './IPv4Network';
import { IPv6Network } from './IPv6Network';

/**
 * Handles compilation of filtering predicates as used by
 * the {@link FilterService}. Predicates are compiled from a user-
 * specified search string.
 */
export class FilterPattern {

    /**
     * Array of getters corresponding to the Angular expressions provided
     * to the constructor of this class. The functions returned are those
     * produced by AngularExpression.compile().
     */
    private readonly getters: ((object: unknown) => unknown)[];

    /**
     * The current filtering predicate.
     */
    predicate: (object: unknown) => boolean;

    /**
     * Creates a new FilterPattern.
     *
     * @param expressions
     *     The expressions whose values are to be filtered.
     */
    constructor(expressions: string[]) {
        this.getters = expressions.map(expression => AngularExpression.compile(expression));
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
    private matchesToken(object: unknown, token: FilterToken): boolean {

        // Match depending on token type
        switch (token.type) {

            // Simple string literal
            case 'LITERAL':
                return this.matchesString(object, token.value as string);

            // IPv4 network address / subnet
            case 'IPV4_NETWORK':
                return this.matchesIPv4(object, token.value as IPv4Network);

            // IPv6 network address / subnet
            case 'IPV6_NETWORK':
                return this.matchesIPv6(object, token.value as IPv6Network);

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
    private matchesString(object: unknown, str: string): boolean {

        // For each defined getter
        for (const getter of this.getters) {

            // Retrieve value of current getter
            let value: unknown;
            if (_.isObjectLike(object))
                value = getter(object);
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
    private matchesIPv4(object: unknown, network: IPv4Network): boolean {

        // For each defined getter
        for (const getter of this.getters) {

            // Test each possible IPv4 address within the string against
            // the given IPv4 network
            const addresses = String(getter(object)).split(/[^0-9.]+/);
            for (const address of addresses) {
                const value = IPv4Network.parse(address);
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
    matchesIPv6(object: unknown, network: IPv6Network): boolean {

        // For each defined getter
        for (const getter of this.getters) {

            // Test each possible IPv6 address within the string against
            // the given IPv6 network
            const addresses = String(getter(object)).split(/[^0-9A-Fa-f:]+/);
            for (const address of addresses) {
                const value = IPv6Network.parse(address);
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
        this.predicate = (object: unknown) => {

            // False if any token does not match
            for (const token of tokens) {
                if (!this.matchesToken(object, token))
                    return false;
            }

            // True if all tokens matched
            return true;

        };

    }

}
