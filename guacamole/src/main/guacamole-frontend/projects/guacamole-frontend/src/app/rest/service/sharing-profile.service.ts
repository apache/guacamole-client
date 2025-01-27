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
import { map, Observable, tap } from 'rxjs';
import { SharingProfile } from '../types/SharingProfile';

/**
 * Service for operating on sharing profiles via the REST API.
 */
@Injectable({
    providedIn: 'root'
})
export class SharingProfileService {

    /**
     * Inject required services.
     */
    constructor(private http: HttpClient) {

    }

    /**
     * Makes a request to the REST API to get a single sharing profile,
     * returning an observable that provides the corresponding @link{SharingProfile}
     * if successful.
     *
     * @param id
     *     The ID of the sharing profile.
     *
     * @returns
     *     An observable which will emit a @link{SharingProfile} upon
     *     success.
     *
     * @example
     *
     * sharingProfileService.getSharingProfile('mySharingProfile').subscribe(sharingProfile => {
     *     // Do something with the sharing profile
     * });
     */
    getSharingProfile(dataSource: string, id: string): Observable<SharingProfile> {

        // Retrieve sharing profile
        // TODO: cache   : cacheService.connections,
        return this.http.get<SharingProfile>(
            'api/session/data/' + encodeURIComponent(dataSource) + '/sharingProfiles/' + encodeURIComponent(id)
        );

    }

    /**
     * Makes a request to the REST API to get the parameters of a single
     * sharing profile, returning an observable that provides the corresponding
     * map of parameter name/value pairs if successful.
     *
     * @param  id
     *     The identifier of the sharing profile.
     *
     * @returns
     *     An observable which will emit a map of parameter name/value
     *     pairs upon success.
     */
    getSharingProfileParameters(dataSource: string, id: string): Observable<Record<string, string>> {

        // Retrieve sharing profile parameters
        // TODO: cache: cacheService.connections,
        return this.http.get<Record<string, string>>(
            'api/session/data/' + encodeURIComponent(dataSource) + '/sharingProfiles/' + encodeURIComponent(id) + '/parameters'
        );

    }

    /**
     * Makes a request to the REST API to save a sharing profile, returning an
     * observable that can be used for processing the results of the call. If the
     * sharing profile is new, and thus does not yet have an associate
     * identifier, the identifier will be automatically set in the provided
     * sharing profile upon success.
     *
     * @param sharingProfile
     *     The sharing profile to update.
     *
     * @returns
     *     An observable for the HTTP call which will succeed if and only if the
     *     save operation is successful.
     */
    saveSharingProfile(dataSource: string, sharingProfile: SharingProfile): Observable<void> {

        // If sharing profile is new, add it and set the identifier automatically
        if (!sharingProfile.identifier) {
            return this.http.post<SharingProfile>(
                'api/session/data/' + encodeURIComponent(dataSource) + '/sharingProfiles',
                sharingProfile
            ).pipe(map((newSharingProfile) => {

                // Set the identifier on the new sharing profile and clear the cache
                sharingProfile.identifier = newSharingProfile.identifier;
                // TODO: cacheService.connections.removeAll();

                // Clear users cache to force reload of permissions for this
                // newly created sharing profile
                // TODO: cacheService.users.removeAll();
            }));
        }

        // Otherwise, update the existing sharing profile
        else {
            return this.http.put<void>(
                'api/session/data/' + encodeURIComponent(dataSource) + '/sharingProfiles/' + encodeURIComponent(sharingProfile.identifier),
                sharingProfile
            )

                // Clear the cache
                .pipe(tap(() => {
                    // TODO: cacheService.connections.removeAll();

                    // Clear users cache to force reload of permissions for this
                    // newly updated sharing profile
                    // TODO: cacheService.users.removeAll();
                }));
        }

    }

    /**
     * Makes a request to the REST API to delete a sharing profile,
     * returning an observable that can be used for processing the results of the call.
     *
     * @param sharingProfile
     *     The sharing profile to delete.
     *
     * @returns
     *     An observable for the HTTP call which will succeed if and only if the
     *     delete operation is successful.
     */
    deleteSharingProfile(dataSource: string, sharingProfile: SharingProfile): Observable<void> {

        // Delete sharing profile
        return this.http.delete<void>(
            'api/session/data/' + encodeURIComponent(dataSource) + '/sharingProfiles/' + encodeURIComponent(sharingProfile.identifier!)
        )

            // Clear the cache
            .pipe(tap(() => {
                // TODO: cacheService.connections.removeAll();
            }));

    }

}
