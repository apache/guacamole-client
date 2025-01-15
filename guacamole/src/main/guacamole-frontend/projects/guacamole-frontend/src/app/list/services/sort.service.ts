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

import { Injectable } from '@angular/core';
import AngularExpression from 'angular-expressions';
import _ from 'lodash';

/**
 * TODO: Document
 */
@Injectable({
    providedIn: 'root'
})
export class SortService {

    /**
     * TODO Document
     * @template T
     *     The type of the elements in the target array.
     *
     * @param target
     * @param iteratees
     */
    orderBy<T>(target: T[], iteratees?: (keyof T)[]): T[] {
        return _.orderBy(target, iteratees);
    }

    /**
     * Sorts the given collection by the given predicate.
     * The predicate is a string (AngularJS expression) that can be compiled into a function.
     * This function will be evaluated against each item and the result will be used for sorting.
     * The predicate can be prefixed with a minus sign to indicate descending order.
     *
     * @template T
     *     The type of the elements in the collection array.
     *
     * @param collection
     *     The collection to sort.
     *
     * @param predicates
     *     A list of predicates to be used for sorting.
     *
     * @returns
     *     The sorted collection.
     */
    orderByPredicate<T>(collection: T[] | null | undefined, predicates: string[]): T[] {
        if (!collection || collection.length === 0) {
            return [];
        }

        // Predicates starting with a minus sign indicate descending order
        const orders = predicates.map(p => p.startsWith('-') ? 'desc' : 'asc');

        const expressions = predicates.map(p => {
            const expression = p.startsWith('-') || p.startsWith('+') ? p.substring(1) : p;
            // Compare strings in a case-insensitive manner
            return (item: T) => {
                const value = AngularExpression.compile(expression)(item);

                // If the value is a number, return it as-is
                if (typeof value === 'number')
                    return value;

                // Try to convert to string and compare in a case-insensitive manner
                if (value?.toString)
                    return value.toString().toLowerCase();

                return null;
            };
        });

        return _.orderBy(collection, expressions, orders);
    }
}
