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
 * Service for operating on HTML patches via the REST API.
 */
@Injectable({
    providedIn: 'root'
})
export class PatchService {

    /**
     * Inject required services.
     */
    constructor(private http: HttpClient) {
    }

    /**
     * Makes a request to the REST API to get the list of patches, returning
     * an observable that provides the array of all applicable patches if
     * successful. Each patch is a string of raw HTML with meta information
     * describing the patch operation stored within meta tags.
     *
     * @returns
     *     An observable which will emit an array of HTML patches upon
     *     success.
     */
    getPatches(): Observable<string[]> {

        // Retrieve all applicable HTML patches
        return this.http.get<string[]>('api/patches');

    }

}
