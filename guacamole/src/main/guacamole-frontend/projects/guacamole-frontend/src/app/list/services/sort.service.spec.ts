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

import { TestBed } from '@angular/core/testing';
import { SortService } from './sort.service';

describe('orderByPredicate', () => {

    let service: SortService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [SortService]
        });

        service = TestBed.get(SortService);
    });


    it('should sort the collection in ascending order based on a single predicate', () => {
        const collection = [
            { name: 'John', age: 30 },
            { name: 'Jane', age: 25 },
            { name: 'Bob', age: 35 },
        ];
        const predicates = ['age'];

        const result = service.orderByPredicate(collection, predicates);

        expect(result).toEqual([
            { name: 'Jane', age: 25 },
            { name: 'John', age: 30 },
            { name: 'Bob', age: 35 },
        ]);
    });

    it('should sort the collection in descending order based on a single predicate', () => {
        const collection = [
            { name: 'John', age: 30 },
            { name: 'Jane', age: 25 },
            { name: 'Bob', age: 35 },
        ];
        const predicates = ['-age'];

        const result = service.orderByPredicate(collection, predicates);

        expect(result).toEqual([
            { name: 'Bob', age: 35 },
            { name: 'John', age: 30 },
            { name: 'Jane', age: 25 },
        ]);
    });

    it('should sort the collection based on multiple predicates', () => {
        const collection = [
            { name: 'John', age: 30, salary: 50000 },
            { name: 'Jane', age: 25, salary: 60000 },
            { name: 'Bob', age: 35, salary: 40000 },
            { name: 'Alice', age: 25, salary: 50000 },
        ];
        const predicates = ['age', '-salary'];

        const result = service.orderByPredicate(collection, predicates);

        expect(result).toEqual([
            { name: 'Jane', age: 25, salary: 60000 },
            { name: 'Alice', age: 25, salary: 50000 },
            { name: 'John', age: 30, salary: 50000 },
            { name: 'Bob', age: 35, salary: 40000 },
        ]);
    });

    it('should return the same collection if no predicates are provided', () => {
        const collection = [
            { name: 'John', age: 30 },
            { name: 'Jane', age: 25 },
            { name: 'Bob', age: 35 },
        ];
        const predicates: string[] = [];

        const result = service.orderByPredicate(collection, predicates);

        expect(result).toEqual(collection);
    });

    it('should return an empty collection if the input collection is empty', () => {
        const collection: any[] = [];
        const predicates = ['age'];

        const result = service.orderByPredicate(collection, predicates);

        expect(result).toEqual([]);
    });
});
