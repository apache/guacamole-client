

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
 * Service for the pagination of an arbitrary array.
 */
@Injectable({
    providedIn: 'root'
})
export class PaginationService {

    /**
     * Extracts a page from the given source.
     *
     * @template T
     *     The type of the elements in the source array.
     *
     * @param source
     *     The source from which the page should be extracted.
     *
     * @param pageIndex
     *     The index of the page that should be returned.
     *
     * @param pageSize
     *     The size of each page.
     *
     * @returns
     *     The page with the given index.
     */
    paginate<T>(source: T[] | null, pageIndex: number, pageSize: number): T[] {

        if (!source)
            return [];

        const startIndex = pageIndex * pageSize;
        const endIndex = startIndex + pageSize;

        return source.slice(startIndex, endIndex);
    }

}
