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

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ConnectionHistoryEntry } from '../types/ConnectionHistoryEntry';

/**
 * Service for operating on history records via the REST API.
 */
@Injectable({
    providedIn: 'root'
})
export class HistoryService {

    /**
     * Inject required services.
     */
    constructor(private http: HttpClient) {
    }

    /**
     * Makes a request to the REST API to get the usage history of all
     * accessible connections, returning an Observable that provides the
     * corresponding array of @link{ConnectionHistoryEntry} objects if
     * successful.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the connection
     *     history records to be retrieved. This identifier corresponds to an
     *     AuthenticationProvider within the Guacamole web application.
     *
     * @param requiredContents
     *     The set of arbitrary strings to filter with. A ConnectionHistoryEntry
     *     must contain each of these values within the associated username,
     *     connection name, start date, or end date to appear in the result. If
     *     null, no filtering will be performed.
     *
     * @param sortPredicates
     *     The set of predicates to sort against. The resulting array of
     *     ConnectionHistoryEntry objects will be sorted according to the
     *     properties and sort orders defined by each predicate. If null, the
     *     order of the resulting entries is undefined. Valid values are listed
     *     within ConnectionHistoryEntry.SortPredicate.
     *
     * @returns
     *     An Observable which will emit an array of
     *     @link{ConnectionHistoryEntry} objects upon success.
     */
    getConnectionHistory(dataSource: string, requiredContents?: string[], sortPredicates?: string[]): Observable<ConnectionHistoryEntry[]> {

        let httpParameters = new HttpParams();

        // Filter according to contents if restrictions are specified
        if (requiredContents)
            httpParameters = httpParameters.appendAll({contains: requiredContents});

        // Sort according to provided predicates, if any
        if (sortPredicates)
            httpParameters = httpParameters.appendAll({sort: sortPredicates});

        // Retrieve connection history
        return this.http.get<ConnectionHistoryEntry[]>(
            'api/session/data/' + encodeURIComponent(dataSource) + '/history/connections',
            {params: httpParameters}
        );

    }
}
