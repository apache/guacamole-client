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
import { DataSourceBuilderService } from './data-source-builder.service';
import { testScheduler } from '../../util/test-helper';
import { from, map, of } from 'rxjs';
import { SortOrder } from '../types/SortOrder';
import { PagerEvent } from '../components/guac-pager/guac-pager.component';
import { DataSource } from '../types/DataSource';

interface Person {
    id: number;
    name: string;
}

describe('DataSourceBuilderService', () => {
    let service: DataSourceBuilderService;
    const source: Person[] = [
        {id: 1, name: 'John'},
        {id: 2, name: 'Jane'},
        {id: 3, name: 'Jack'},
        {id: 4, name: 'Jill'},
        {id: 5, name: 'Joe'},
        {id: 6, name: 'Jenny'},
        {id: 7, name: 'Jim'},
        {id: 8, name: 'Jen'},
        {id: 9, name: 'Jesse'},
        {id: 10, name: 'Jasmine'}
    ];

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(DataSourceBuilderService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('source', () => {

        it('should return source and complete if noting else is configured', () => {
            testScheduler.run(({expectObservable}) => {

                const dataSource = service.getBuilder().source(source).build();

                const data = dataSource.data;

                expectObservable(data).toBe('(0|)', [source]);

            });
        });

    });
    describe('totalLength', () => {


        it('should return source length if no filter is configured', () => {
            testScheduler.run(({expectObservable}) => {

                const dataSource = service.getBuilder().source(source).build();
                const length = dataSource.totalLength;

                expectObservable(length).toBe('(a|)', {a: source.length});

            });
        });


        it('should return length filtered source', () => {
            testScheduler.run(({expectObservable}) => {

                const dataSource: DataSource<Person> = service.getBuilder<Person>()
                    .source(source)
                    .filter(of('ji'))
                    .build();

                const length = dataSource.totalLength;

                // TODO: implement correct filtering expectObservable(length).toBe('(a|)', {a: 2});

            });
        });

    });

    describe('sort', () => {

        it('should return source sorted by name ascending', () => {
            testScheduler.run(({expectObservable}) => {

                const dataSource: DataSource<Person> = service.getBuilder<Person>()
                    .source(source)
                    .sort(of(new SortOrder(['name'])))
                    .build();


                const data = dataSource.data
                    // Map to ids
                    .pipe(map(data => data.map(p => p.id)));

                expectObservable(data).toBe('(0|)', [[3, 2, 10, 8, 6, 9, 4, 7, 5, 1]]);

            });
        });

        it('should return source sorted by name descending', () => {
            testScheduler.run(({expectObservable}) => {

                const dataSource: DataSource<Person> = service.getBuilder<Person>()
                    .source(source)
                    .sort(of(new SortOrder(['-name'])))
                    .build();

                const data = dataSource.data
                    // Map to ids
                    .pipe(map(data => data.map(p => p.id)));

                expectObservable(data).toBe('(0|)', [[1, 5, 7, 4, 9, 6, 8, 10, 2, 3]]);

            });
        });

    });

    describe('paginate', () => {


        it('should return source paginated', () => {

            testScheduler.run(({expectObservable}) => {

                const pageEvents: PagerEvent[] = [
                    {pageIndex: 0, pageSize: 3, previousPageIndex: 0},
                    {pageIndex: 1, pageSize: 3, previousPageIndex: 0},
                    {pageIndex: 2, pageSize: 3, previousPageIndex: 1},
                    {pageIndex: 3, pageSize: 3, previousPageIndex: 2},
                ]


                const dataSource: DataSource<Person> = service.getBuilder<Person>()
                    .source(source)
                    .paginate(from(pageEvents))
                    .build();

                const page = dataSource.data
                    // Map to ids
                    .pipe(map(data => data.map(p => p.id)));

                expectObservable(page).toBe('(0 1 2 3|)', [
                    [1, 2, 3],
                    [4, 5, 6],
                    [7, 8, 9],
                    [10]
                ]);

            });

        });

    });


    describe('updateSource', () => {

        it('should update source and emit new data', () => {
            testScheduler.run(({expectObservable, flush}) => {

                const firstSource: Person[] = source.slice(0, 5);
                const secondSource: Person[] = source.slice(5, 10);

                const dataSource: DataSource<Person> = service.getBuilder<Person>()
                    .source(firstSource)
                    .sort(of(new SortOrder(['-name'])))
                    .build();

                let data = dataSource.data
                    // Map to ids
                    .pipe(map(data => data.map(p => p.id)));

                expectObservable(data).toBe('(0|)', [[1, 5, 4, 2, 3]]);

                flush();

                dataSource.updateSource(secondSource);
                data = dataSource.data
                    // Map to ids
                    .pipe(map(data => data.map(p => p.id)));

                expectObservable(data).toBe('(0|)', [[7, 9, 6, 8, 10]]);

            });
        });

    });
});
