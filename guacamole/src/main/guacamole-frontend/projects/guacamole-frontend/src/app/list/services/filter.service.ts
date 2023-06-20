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

/**
 * TODO: Document
 * Replacement for AngularJS filter function https://docs.angularjs.org/api/ng/filter/filter
 */
@Injectable({
    providedIn: 'root'
})
export class FilterService {

    constructor() {
    }

    /**
     * TODO: Document
     * @param target
     * @param filter
     */
    applyFilter<T extends { toString(): string }>(target: T[] | null, filter: string): T[] {

        if (!target)
            return [];

        return target.filter((element) => {
            return element.toString().toLowerCase().includes(filter.toLowerCase());
        });
    }

    /**
     * TODO: Document
     * @param target
     * @param predicate
     */
    filterByPredicate<T>(target: T[] | null, predicate: Function): T[] {
        if (!target)
            return [];

        return target.filter(predicate as any);
    }
}
