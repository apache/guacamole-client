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
import { RequestService } from './request.service';
import { Error } from '../types/Error';
import { Observable } from 'rxjs';

/**
 * Service which contains all REST API response caches.
 */
@Injectable({
    providedIn: 'root'
})
export class DataSourceService {

    constructor(private requestService: RequestService) {
    }

    /**
     * Invokes the given function once for each of the given data sources,
     * passing that data source as the first argument to each invocation,
     * followed by any additional arguments passed to apply(). The results of
     * each invocation are aggregated into a map by data source identifier,
     * and handled through a single promise which is resolved or rejected
     * depending on the success/failure of each resulting REST call. Any error
     * results in rejection of the entire apply() operation, except 404 ("NOT
     * FOUND") errors, which are ignored.
     *
     * @param fn
     *     The function to call for each of the given data sources. The data
     *     source identifier will be given as the first argument, followed by
     *     the rest of the arguments given to apply(), in order. The function
     *     must return a Promise which is resolved or rejected depending on the
     *     result of the REST call.
     *
     * @param dataSources
     *     The array or data source identifiers against which the given
     *     function should be called.
     *
     * @param args
     *     Any additional arguments to pass to the given function each time it
     *     is called.
     *
     * @returns
     *     A Promise which resolves with a map of data source identifier to
     *     corresponding result. The result will be the exact object or value
     *     provided as the resolution to the Promise returned by calls to the
     *     given function.
     */
    apply<TData>(fn: (dataSource: string, ...args: any[]) => Observable<TData>, dataSources: string[], ...args: any[]): Promise<Record<string, TData>> {

        return new Promise((resolve, reject) => {

            const requests: Promise<void>[] = [];
            const results: Record<string, any> = {};

            // Retrieve the root group from all data sources
            dataSources.forEach(dataSource => {

                // Add promise to list of pending requests
                let deferredRequestResolve: () => void;
                let deferredRequestReject: (reason: any) => void;
                const deferredRequest = new Promise<void>((resolve, reject) => {
                    deferredRequestResolve = resolve;
                    deferredRequestReject = reject;
                });

                requests.push(deferredRequest);

                // Retrieve root group from data source
                fn(dataSource, ...args)

                    // Store result on success
                    .subscribe({
                        next: (data: TData) => {
                            results[dataSource] = data;
                            deferredRequestResolve();
                        },

                        // Fail on any errors (except "NOT FOUND")
                        error: this.requestService.createPromiseErrorCallback(function immediateRequestFailed(error: any) {

                            if (error.type === Error.Type.NOT_FOUND)
                                deferredRequestResolve();

                            // Explicitly abort for all other errors
                            else
                                deferredRequestReject(error);

                        })
                    });

            });

            // Resolve if all requests succeed
            Promise.all(requests).then(() => {
                    resolve(results);
                },

                // Reject if at least one request fails
                this.requestService.createPromiseErrorCallback((error: any) => {
                    reject(error);
                }));

        });

    }
}
