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

import { combineLatest, finalize, map, Observable, of, Subject, takeUntil } from 'rxjs';
import { PagerEvent } from '../components/guac-pager/guac-pager.component';
import { FilterService } from '../services/filter.service';
import { PaginationService } from '../services/pagination.service';
import { SortService } from '../services/sort.service';
import { FilterPattern } from './FilterPattern';
import { SortOrder } from './SortOrder';

/**
 * A data source which wraps a source array and applies filtering, sorting and pagination.
 *
 * @template T
 *     The type of the elements in the data source.
 */
export class DataSource<T> {

    /**
     * An observable which emits the filtered, sorted and paginated data of the source array.
     */
    data!: Observable<T[]>;

    /**
     * An observable which emits the length of the filtered source array.
     */
    totalLength!: Observable<number>;

    /**
     * The pattern object to use when filtering items.
     */
    private readonly filterPattern: FilterPattern | null = null;

    /**
     * A subject which emits when the currently loaded data is replaced by new data.
     */
    private dataComplete!: Subject<void>;

    /**
     * Creates a new DataSource, which wraps the given source array and
     * applies filtering, sorting and pagination.
     */
    constructor(private sortService: SortService,
                private filterService: FilterService,
                private paginationService: PaginationService,
                private source: T[],
                private searchString: Observable<string> | null,
                private filterProperties: string[] | null,
                private sort: Observable<SortOrder> | null,
                private paginate: Observable<PagerEvent> | null) {

        if (filterProperties !== null)
            this.filterPattern = new FilterPattern(filterProperties);

        this.computeData();
    }

    /**
     * (Re)creates the data and totalLength observables from the current
     * source and the configured operations. Called by the constructor and
     * whenever the source is replaced. The totalLength observable completes
     * when the data observable completes.
     */
    private computeData(): void {
        this.dataComplete = new Subject<void>();

        this.totalLength = (this.searchString || of(null as string | null)).pipe(
            // Also complete when the data observable completes.
            takeUntil(this.dataComplete),
            map(filter => {
                return this.applyFilter(this.source, filter).length;
            }),
        );

        // If an operation is null, replace it with an observable which emits null so that combineLatest
        // will emit when the other operations emit.
        const operations = [
            this.searchString || of(null as string | null),
            this.sort || of(null as SortOrder | null),
            this.paginate || of(null as PagerEvent | null),
        ] as const;

        this.data = combineLatest(operations).pipe(
            map(([searchString, sort, pagination]) => {
                    let data: T[] = this.source;

                    // Filter, sort and paginate the data
                    data = this.applyFilter(data, searchString);
                    data = this.orderByPredicate(data, sort);
                    data = this.applyPagination(data, pagination);

                    return data;
                }
            ),

            // Complete the totalLength observable when the data observable completes
            finalize(() => {
                this.dataComplete.next();
                this.dataComplete.complete();
            })
        );
    }

    /**
     * Sets the source array and recomputes the data.
     *
     * @param source
     *     The new source array.
     */
    updateSource(source: T[]): void {
        this.source = source;
        this.computeData();
    }

    /**
     * Applies the filter predicate compiled from the given search string,
     * filtering all provided data. The data is returned unchanged if the
     * search string is null or empty, or if no filter properties were
     * provided to this data source.
     *
     * @param data
     *     The data to filter.
     *
     * @param searchString
     *     The filter search string to use to restrict the data, or null if
     *     the data should not be filtered.
     *
     * @returns
     *     The elements of the data which match the search string.
     */
    private applyFilter(data: T[], searchString: string | null): T[] {
        if (searchString === null || searchString === '' || this.filterPattern === null)
            return data;

        this.filterPattern.compile(searchString);
        return this.filterService.filterByPredicate(data, this.filterPattern.predicate);
    }

    /**
     * Sorts the given data according to the predicate of the given sort
     * order.
     *
     * @param data
     *     The data to sort.
     *
     * @param sortOrder
     *     The sort order to apply, or null if the data should not be sorted.
     *
     * @returns
     *     The sorted data, or the given data itself if there is no sort
     *     order.
     */
    private orderByPredicate(data: T[], sortOrder: SortOrder | null): T[] {
        if (sortOrder === null)
            return data;

        return this.sortService.orderByPredicate(data, sortOrder.predicate);
    }

    /**
     * Extracts the page described by the given pager event from the given
     * data.
     *
     * @param data
     *     The data to paginate.
     *
     * @param pagination
     *     The pager event describing the page to extract, or null if the
     *     data should not be paginated. A page size of zero also disables
     *     pagination.
     *
     * @returns
     *     The elements of the selected page, or the given data itself if
     *     pagination is disabled.
     */
    private applyPagination(data: T[], pagination: PagerEvent | null): T[] {
        if (pagination === null || pagination.pageSize === 0)
            return data;

        return this.paginationService.paginate(data, pagination.pageIndex, pagination.pageSize);
    }

}
