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

import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable, tap } from 'rxjs';
import { Connection } from '../types/Connection';
import { ConnectionHistoryEntry } from '../types/ConnectionHistoryEntry';
import { DirectoryPatch } from '../types/DirectoryPatch';
import { DirectoryPatchResponse } from '../types/DirectoryPatchResponse';
import { CACHE, CacheService } from './cache.service';

/**
 * Service for operating on connections via the REST API.
 */
@Injectable({
    providedIn: 'root'
})
export class ConnectionService {

    /**
     * Inject required services.
     */
    constructor(private http: HttpClient, private cacheService: CacheService) {
    }

    /**
     * Makes a request to the REST API to get a single connection, returning an
     * observable that provides the corresponding @link{Connection} if successful.
     *
     * @param id
     *     The ID of the connection.
     *
     * @returns
     *     An observable which will emit a @link{Connection} upon success.
     *
     * @example
     *
     * connectionService.getConnection('myConnection').subscribe(connection => {
     *     // Do something with the connection
     * });
     */
    getConnection(dataSource: string, id: string): Observable<Connection> {

        // Retrieve connection
        return this.http.get<Connection>(
            'api/session/data/' + encodeURIComponent(dataSource) + '/connections/' + encodeURIComponent(id),
            { context: new HttpContext().set(CACHE, 'connections') }
        );

    }

    /**
     * Makes a request to the REST API to get the usage history of a single
     * connection, returning an observable that provides the corresponding
     * array of @link{ConnectionHistoryEntry} objects if successful.
     *
     * @param id
     *     The identifier of the connection.
     *
     * @returns
     *     An observable which will emit an array of
     *     @link{ConnectionHistoryEntry} objects upon success.
     */
    getConnectionHistory(dataSource: string, id: string): Observable<ConnectionHistoryEntry[]> {

        // Retrieve connection history
        return this.http.get<ConnectionHistoryEntry[]>(
            'api/session/data/' + encodeURIComponent(dataSource) + '/connections/' + encodeURIComponent(id) + '/history'
        );

    }

    /**
     * Makes a request to the REST API to get the parameters of a single
     * connection, returning an observable that provides the corresponding
     * map of parameter name/value pairs if successful.
     *
     * @param id
     *     The identifier of the connection.
     *
     * @returns
     *     An observable which will emit an map of parameter name/value
     *     pairs upon success.
     */
    getConnectionParameters(dataSource: string, id: string): Observable<Record<string, string>> {

        // Retrieve connection parameters
        return this.http.get<Record<string, string>>(
            'api/session/data/' + encodeURIComponent(dataSource) + '/connections/' + encodeURIComponent(id) + '/parameters',
            { context: new HttpContext().set(CACHE, 'connections') }
        );

    }

    /**
     * Makes a request to the REST API to save a connection, returning an
     * observable that can be used for processing the results of the call. If the
     * connection is new, and thus does not yet have an associated identifier,
     * the identifier will be automatically set in the provided connection
     * upon success.
     *
     * @param connection
     *     The connection to update.
     *
     * @returns
     *     An observable for the HTTP call which will succeed if and only if the
     *     save operation is successful.
     */
    saveConnection(dataSource: string, connection: Connection): Observable<void> {

        // If connection is new, add it and set the identifier automatically
        if (!connection.identifier) {
            return this.http.post<Connection>(
                'api/session/data/' + encodeURIComponent(dataSource) + '/connections',
                connection
            )

                // Set the identifier on the new connection and clear the cache
                .pipe(
                    map(newConnection => {
                        connection.identifier = newConnection.identifier;
                        this.cacheService.connections.clear();

                        // Clear users cache to force reload of permissions for this
                        // newly created connection
                        this.cacheService.users.clear();
                    })
                );
        }

        // Otherwise, update the existing connection
        else {
            return this.http.put<void>(
                'api/session/data/' + encodeURIComponent(dataSource) + '/connections/' + encodeURIComponent(connection.identifier),
                connection
            )

                // Clear the cache
                .pipe(
                    tap(() => {
                        this.cacheService.connections.clear();

                        // Clear users cache to force reload of permissions for this
                        // newly updated connection
                        this.cacheService.users.clear();
                    })
                );
        }

    }

    /**
     * Makes a request to the REST API to apply a supplied list of connection
     * patches, returning an observable that can be used for processing the results
     * of the call.
     *
     * This operation is atomic - if any errors are encountered during the
     * connection patching process, the entire request will fail, and no
     * changes will be persisted.
     *
     * @param dataSource
     *     The identifier of the data source associated with the connections to
     *     be patched.
     *
     * @param patches
     *     An array of patches to apply.
     *
     * @returns
     *     An observable for the HTTP call which will succeed if and only if the
     *     patch operation is successful.
     */
    patchConnections(dataSource: string, patches: DirectoryPatch<Connection>[]): Observable<DirectoryPatchResponse> {

        // Make the PATCH request
        return this.http.patch<DirectoryPatchResponse>(
            'api/session/data/' + encodeURIComponent(dataSource) + '/connections',
            patches
        )

            // Clear the cache
            .pipe(
                tap(() => {
                    this.cacheService.connections.clear();

                    // Clear users cache to force reload of permissions for any
                    // newly created or replaced connections
                    this.cacheService.users.clear();

                })
            );

    }

    /**
     * Makes a request to the REST API to delete a connection,
     * returning an observable that can be used for processing the results of the call.
     *
     * @param connection
     *     The connection to delete.
     *
     * @returns
     *     An observable for the HTTP call which will succeed if and only if the
     *     delete operation is successful.
     */
    deleteConnection(dataSource: string, connection: Connection): Observable<void> {

        // Delete connection
        return this.http.delete<void>(
            'api/session/data/' + encodeURIComponent(dataSource) + '/connections/' + encodeURIComponent(connection.identifier!)
        )

            // Clear the cache
            .pipe(
                tap(() => {
                    this.cacheService.connections.clear();
                })
            );

    }

}
