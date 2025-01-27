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
import { ActiveConnection } from '../types/ActiveConnection';
import { UserCredentials } from '../types/UserCredentials';

/**
 * Service for operating on active connections via the REST API.
 */
@Injectable({
    providedIn: 'root'
})
export class ActiveConnectionService {

    /**
     * Required services
     */
    constructor(private http: HttpClient) {
    }

    /**
     * Makes a request to the REST API to get a single active connection,
     * returning an Observable that provides the corresponding
     * {@link ActiveConnection} if successful.
     *
     * @param dataSource
     *     The identifier of the data source to retrieve the active connection
     *     from.
     *
     * @param id
     *     The identifier of the active connection.
     *
     * @returns
     *     An Observable which will emit a @link{ActiveConnection} upon
     *     success.
     */
    getActiveConnection(dataSource: string, id: string): Observable<ActiveConnection> {

        // Retrieve active connection
        return this.http.get(
            'api/session/data/' + encodeURIComponent(dataSource) + '/activeConnections/' + encodeURIComponent(id)
        );
    }

    /**
     * Makes a request to the REST API to get the list of active tunnels,
     * returning an Observable that provides a map of @link{ActiveConnection}
     * objects if successful.
     *
     * @param dataSource
     *    The identifier of the data source to retrieve the active connections
     *    from.
     *
     * @param permissionTypes
     *     The set of permissions to filter with. A user must have one or more
     *     of these permissions for an active connection to appear in the
     *     result.  If null, no filtering will be performed. Valid values are
     *     listed within PermissionSet.ObjectType.
     *
     * @returns
     *     An Observable which will emit a map of {@link ActiveConnection}
     *     objects, where each key is the identifier of the corresponding
     *     active connection.
     */
    getActiveConnections(dataSource: string, permissionTypes?: string[]): Observable<Record<string, ActiveConnection>> {

        // Add permission filter if specified
        let httpParameters = new HttpParams();
        if (permissionTypes)
            httpParameters = httpParameters.appendAll({ permission: permissionTypes });

        // Retrieve tunnels
        return this.http.get<Record<string, ActiveConnection>>(
            'api/session/data/' + encodeURIComponent(dataSource) + '/activeConnections',
            { params: httpParameters }
        );

    }

    /**
     * Makes a request to the REST API to delete the active connections having
     * the given identifiers, effectively disconnecting them, returning an
     * Observable that can be used for processing the results of the call.
     *
     * @param dataSource
     *    The identifier of the data source to delete the active connections
     *    from.
     *
     * @param identifiers
     *     The identifiers of the active connections to delete.
     *
     * @returns
     *     An Observable for the HTTP call which will succeed if and only if the
     *     delete operation is successful.
     */
    deleteActiveConnections(dataSource: string, identifiers: string[]): Observable<void> {

        // Convert provided array of identifiers to a patch
        const activeConnectionPatch: any[] = [];
        identifiers.forEach(function addActiveConnectionPatch(identifier) {
            activeConnectionPatch.push({
                op  : 'remove',
                path: '/' + identifier
            });
        });

        // Perform active connection deletion via PATCH
        return this.http.patch<void>(
            'api/session/data/' + encodeURIComponent(dataSource) + '/activeConnections',
            activeConnectionPatch
        );

    }

    /**
     * Makes a request to the REST API to generate credentials which have
     * access strictly to the given active connection, using the restrictions
     * defined by the given sharing profile, returning an Observable that provides
     * the resulting @link{UserCredentials} object if successful.
     *
     * @param dataSource
     *    The identifier of the data source to retrieve the active connection
     *    from.
     *
     * @param id
     *     The identifier of the active connection being shared.
     *
     * @param sharingProfile
     *     The identifier of the sharing profile dictating the
     *     semantics/restrictions which apply to the shared session.
     *
     * @returns
     *     An Observable which will emit a {@link UserCredentials} object
     *     upon success.
     */
    getSharingCredentials(dataSource: string, id: string, sharingProfile: string): Observable<UserCredentials> {

        // Generate sharing credentials
        return this.http.get<UserCredentials>(
            'api/session/data/' + encodeURIComponent(dataSource)
            + '/activeConnections/' + encodeURIComponent(id)
            + '/sharingCredentials/' + encodeURIComponent(sharingProfile)
        );

    }
}
