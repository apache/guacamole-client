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
import { map, Observable, tap } from 'rxjs';
import { ConnectionGroup } from '../types/ConnectionGroup';

/**
 * Service for operating on connection groups via the REST API.
 */
@Injectable({
    providedIn: 'root'
})
export class ConnectionGroupService {

    /**
     * Inject required services.
     */
    constructor(private http: HttpClient) {
    }

    /**
     * Makes a request to the REST API to get an individual connection group
     * and all descendants, returning an observable that provides the corresponding
     * {@link ConnectionGroup} if successful. Descendant groups and connections
     * will be stored as children of that connection group. If a permission
     * type is specified, the result will be filtering by that permission.
     *
     * @param connectionGroupID
     *     The ID of the connection group to retrieve. If not provided, the
     *     root connection group will be retrieved by default.
     *
     * @param permissionTypes
     *     The set of permissions to filter with. A user must have one or more
     *     of these permissions for a connection to appear in the result.
     *     If null, no filtering will be performed. Valid values are listed
     *     within PermissionSet.ObjectType.
     *
     * @returns
     *     An observable which will emit a {@link ConnectionGroup} upon
     *     success.
     */
    getConnectionGroupTree(dataSource: string, connectionGroupID: string = ConnectionGroup.ROOT_IDENTIFIER, permissionTypes?: string[]): Observable<ConnectionGroup> {

        // Add permission filter if specified
        let httpParameters = new HttpParams();
        if (permissionTypes)
            httpParameters = httpParameters.appendAll({ 'permission': permissionTypes });

        // TODO: cache: cacheService.connections,
        // Retrieve connection group
        return this.http.get<ConnectionGroup>('api/session/data/' + encodeURIComponent(dataSource) + '/connectionGroups/' + encodeURIComponent(connectionGroupID) + '/tree',
            {
                params: httpParameters
            });

    }

    /**
     * Makes a request to the REST API to get an individual connection group,
     * returning an observable that provides the corresponding
     * {@link ConnectionGroup} if successful.
     *
     * @param [connectionGroupID=ConnectionGroup.ROOT_IDENTIFIER]
     *     The ID of the connection group to retrieve. If not provided, the
     *     root connection group will be retrieved by default.
     *
     * @returns
     *     An observable which will emit a {@link ConnectionGroup} upon
     *     success.
     */
    getConnectionGroup(dataSource: string, connectionGroupID: string = ConnectionGroup.ROOT_IDENTIFIER): Observable<ConnectionGroup> {

        // TODO cache: cacheService.connections,
        // Retrieve connection group
        return this.http.get<ConnectionGroup>('api/session/data/' + encodeURIComponent(dataSource) + '/connectionGroups/' + encodeURIComponent(connectionGroupID));

    }

    /**
     * Makes a request to the REST API to save a connection group, returning a
     * promise that can be used for processing the results of the call. If the
     * connection group is new, and thus does not yet have an associated
     * identifier, the identifier will be automatically set in the provided
     * connection group upon success.
     *
     * @param connectionGroup
     *     The connection group to update.
     *
     * @returns
     *     An observable for the HTTP call which will succeed if and only if the
     *     save operation is successful.
     */
    saveConnectionGroup(dataSource: string, connectionGroup: ConnectionGroup): Observable<void> {

        // If connection group is new, add it and set the identifier automatically
        if (!connectionGroup.identifier) {
            return this.http.post<ConnectionGroup>(
                'api/session/data/' + encodeURIComponent(dataSource) + '/connectionGroups',
                connectionGroup
            )

                // Set the identifier on the new connection group and clear the cache
                .pipe(
                    map((newConnectionGroup: ConnectionGroup) => {
                        connectionGroup.identifier = newConnectionGroup.identifier;
                        // TODO: cacheService.connections.removeAll();

                        // Clear users cache to force reload of permissions for this
                        // newly created connection group
                        // TODO cacheService.users.removeAll();
                    })
                );
        }

        // Otherwise, update the existing connection group
        else {
            return this.http.put<void>(
                'api/session/data/' + encodeURIComponent(dataSource) + '/connectionGroups/' + encodeURIComponent(connectionGroup.identifier),
                connectionGroup
            )

                // Clear the cache
                .pipe(
                    tap(() => {
                        // TODO: cacheService.connections.removeAll();

                        // Clear users cache to force reload of permissions for this
                        // newly updated connection group
                        // TODO: cacheService.users.removeAll();
                    })
                );
        }

    }

    /**
     * Makes a request to the REST API to delete a connection group, returning
     * an observable that can be used for processing the results of the call.
     *
     * @param connectionGroup
     *     The connection group to delete.
     *
     * @returns
     *     An observable for the HTTP call which will succeed if and only if the
     *     delete operation is successful.
     */
    deleteConnectionGroup(dataSource: string, connectionGroup: ConnectionGroup): Observable<void> {

        // Delete connection group
        return this.http.delete<void>('api/session/data/' + encodeURIComponent(dataSource) + '/connectionGroups/' + encodeURIComponent(connectionGroup.identifier || ''))

            // Clear the cache
            .pipe(
                tap(() => {
                    // TODO: cacheService.connections.removeAll();
                })
            );

    }


}
