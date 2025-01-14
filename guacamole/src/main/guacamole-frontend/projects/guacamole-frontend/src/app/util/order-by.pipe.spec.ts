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
import { SortService } from '../list/services/sort.service';
import { OrderByPipe } from './order-by.pipe';

describe('OrderByPipe', () => {
    let pipe: OrderByPipe;
    let sortService: SortService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [SortService]
        });

        sortService = TestBed.inject(SortService);
        pipe = new OrderByPipe(sortService);
    });

    it('create an instance', () => {
        expect(pipe).toBeTruthy();
    });

    it('orders by property', () => {
        const result = pipe.transform([{name: 'b'}, {name: 'a'}], ['name']);
        expect(result).toEqual([{name: 'a'}, {name: 'b'}]);
    });
});
