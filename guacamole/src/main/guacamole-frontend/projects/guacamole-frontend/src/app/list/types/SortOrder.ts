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

/**
 * Maintains a sorting predicate as required by the {@link SortService}.
 * The order of properties sorted by the predicate can be altered while
 * otherwise maintaining the sort order.
 * TODO: Look at doc
 */
export class SortOrder {

    /**
     * The current sorting predicate.
     */
    predicate: string[];

    /**
     * The name of the highest-precedence sorting property.
     */
    primary: string;

    /**
     * Whether the highest-precedence sorting property is sorted in
     * descending order.
     */
    descending: boolean;

    /**
     * Creates a new SortOrder.

     * @param predicate
     *     The properties to sort by, in order of precedence.
     */
    constructor(predicate: string[]) {

        this.predicate = predicate;
        this.primary = predicate[0];
        this.descending = false;

        // Handle initially-descending primary properties
        if (this.primary.charAt(0) === '-') {
            this.primary = this.primary.substring(1);
            this.descending = true;
        }
    }

    /**
     * Reorders the currently-defined predicate such that the named
     * property takes precedence over all others. The property will be
     * sorted in ascending order unless otherwise specified.
     *
     * @param name
     *     The name of the property to reorder by.
     *
     * @param descending
     *     Whether the property should be sorted in descending order. By
     *     default, all properties are sorted in ascending order.
     */
    reorder(name: string, descending = false): void {

        // Build ascending and descending predicate components
        const ascendingName = name;
        const descendingName = '-' + name;

        // Remove requested property from current predicate
        this.predicate = this.predicate.filter(function notRequestedProperty(current) {
            return current !== ascendingName
                && current !== descendingName;
        });

        // Add property to beginning of predicate
        if (descending)
            this.predicate.unshift(descendingName);
        else
            this.predicate.unshift(ascendingName);

        // Update sorted state
        this.primary = name;
        this.descending = descending;

    }

    /**
     * Returns whether the sort order is primarily determined by the given
     * property.
     *
     * @param property
     *     The name of the property to check.
     *
     * @returns
     *     true if the sort order is primarily determined by the given
     *     property, false otherwise.
     */
    isSortedBy(property: string): boolean {
        return this.primary === property;
    }

    /**
     * Sets the primary sorting property to the given property, if not already
     * set. If already set, the ascending/descending sort order is toggled.
     *
     * @param property
     *     The name of the property to assign as the primary sorting property.
     */
    togglePrimary(property: string): void {

        // Sort in ascending order by new property, if different
        if (!this.isSortedBy(property))
            this.reorder(property, false);

        // Otherwise, toggle sort order
        else
            this.reorder(property, !this.descending);

    }
}
