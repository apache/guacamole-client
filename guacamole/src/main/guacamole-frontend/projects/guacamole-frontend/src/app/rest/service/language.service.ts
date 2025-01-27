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

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/**
 * Service for operating on language metadata via the REST API.
 */
@Injectable({
    providedIn: 'root'
})
export class LanguageService {

    /**
     * Inject required services.
     */
    constructor(private http: HttpClient) {
    }

    /**
     * Makes a request to the REST API to get the list of languages, returning
     * an observable that provides a map of language names by language key if
     * successful.
     *
     * @returns
     *     An observable which will emit a map of language names by
     *     language key upon success.
     */
    getLanguages(): Observable<Record<string, string>> {

        // Retrieve available languages
        // TODO     cache   : cacheService.languages,
        return this.http.get<Record<string, string>>('api/languages');

    }
}
