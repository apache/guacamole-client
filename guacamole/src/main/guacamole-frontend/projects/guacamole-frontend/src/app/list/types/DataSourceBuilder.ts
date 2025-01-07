

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

import { Observable } from 'rxjs';
import { PagerEvent } from '../components/guac-pager/guac-pager.component';
import { FilterService } from '../services/filter.service';
import { PaginationService } from '../services/pagination.service';
import { SortService } from '../services/sort.service';

import { DataSource } from './DataSource';
import { SortOrder } from './SortOrder';

/**
 * A builder that allows to create a DataSource instance. A data source can be
 * configured with a filter, a sort order and pagination.
 *
 * @template T
 *     The type of the elements in the data source.
 */
export class DataSourceBuilder<T> {

    private _source?: T[];
    private _searchString: Observable<string> | null = null;
    private _filterProperties: string[] | null = null;
    private _sortOrder: Observable<SortOrder> | null = null;
    private _pagerEvent: Observable<PagerEvent> | null = null;

    /**
     * Creates a new DataSourceBuilder.
     */
    constructor(private sortService: SortService,
                private filterService: FilterService,
                private paginationService: PaginationService) {
    }

    /**
     * Set the source on which the data operations should be applied.
     *
     * @param source
     *     The source to be used.
     *
     * @returns
     *     The current builder instance for further chaining.
     */
    source(source: T[]): DataSourceBuilder<T> {
        this._source = source;
        return this;
    }

    /**
     * An Observable of TODO
     *
     * @param searchString
     *     The filter search string to use to restrict the displayed items.
     *
     * @param filterProperties
     *     An array of expressions to filter against for each object in the
     *     items array. These expressions must be Angular expressions
     *     which resolve to properties on the objects in the items array.
     *
     * @returns
     *     The current builder instance for further chaining.
     */
    filter(searchString: Observable<string>, filterProperties: string[]): DataSourceBuilder<T> {
        this._searchString = searchString;
        this._filterProperties = filterProperties;
        return this;
    }

    /**
     * An Observable of the sort order that should be applied to the source. Each
     * time the sort order changes, the source will be sorted accordingly.
     *
     * @param sortOrder
     *     The sort order that should be applied.
     *
     * @returns
     *     The current builder instance for further chaining.
     */
    sort(sortOrder: Observable<SortOrder>): DataSourceBuilder<T> {
        this._sortOrder = sortOrder;
        return this;
    }

    /**
     * An Observable of pagination events that should be used extract a page of the source. Each
     * time a page event is emitted, the source will be updated accordingly.
     *
     * @param pagerEvent
     *     The sort order that should be applied.
     *
     * @returns
     *     The current builder instance for further chaining.
     */
    paginate(pagerEvent: Observable<PagerEvent>): DataSourceBuilder<T> {
        this._pagerEvent = pagerEvent;
        return this;
    }

    /**
     * Build the data source with the current configuration.
     *
     * @returns
     *     The data source with the current configuration.
     */
    build(): DataSource<T> {

        if (!this._source)
            throw new Error('No source provided');

        return new DataSource<T>(
            this.sortService,
            this.filterService,
            this.paginationService,
            this._source,
            this._searchString,
            this._filterProperties,
            this._sortOrder,
            this._pagerEvent
        );

    }
}
