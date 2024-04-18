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
import { Observable, tap } from 'rxjs';
import { DirectoryPatch } from '../types/DirectoryPatch';
import { DirectoryPatchResponse } from '../types/DirectoryPatchResponse';
import { UserGroup } from '../types/UserGroup';

/**
 * Service for operating on user groups via the REST API.
 */
@Injectable({
    providedIn: 'root'
})
export class UserGroupService {

    /**
     * Inject required services.
     */
    constructor(private http: HttpClient) {
    }

    /**
     * Makes a request to the REST API to get the list of user groups,
     * returning an observable that provides an array of @link{UserGroup} objects if
     * successful.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the user groups
     *     to be retrieved. This identifier corresponds to an
     *     AuthenticationProvider within the Guacamole web application.
     *
     * @param permissionTypes
     *     The set of permissions to filter with. A user group must have one or
     *     more of these permissions for a user group to appear in the result.
     *     If null, no filtering will be performed. Valid values are listed
     *     within PermissionSet.ObjectType.
     *
     * @returns
     *     An observable which will emit a map of @link{UserGroup} objects
     *     where each key is the identifier of the corresponding user group.
     */
    getUserGroups(dataSource: string, permissionTypes?: string[]): Observable<Record<string, UserGroup>> {

        // Add permission filter if specified
        let httpParameters = new HttpParams();
        if (permissionTypes)
            httpParameters = httpParameters.appendAll({permission: permissionTypes});

        // Retrieve user groups
        // TODO: cache: cacheService.users,
        return this.http.get<Record<string, UserGroup>>(
            'api/session/data/' + encodeURIComponent(dataSource) + '/userGroups',
            {params: httpParameters}
        );

    }

    /**
     * Makes a request to the REST API to get the user group having the given
     * identifier, returning an observable that provides the corresponding
     * @link{UserGroup} if successful.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the user group to
     *     be retrieved. This identifier corresponds to an
     *     AuthenticationProvider within the Guacamole web application.
     *
     * @param identifier
     *     The identifier of the user group to retrieve.
     *
     * @returns
     *     An observable which will emit a @link{UserGroup} upon success.
     */
    getUserGroup(dataSource: string, identifier: string): Observable<UserGroup> {

        // Retrieve user group
        // TODO  cache   : cacheService.users,
        return this.http.get<UserGroup>(
            'api/session/data/' + encodeURIComponent(dataSource) + '/userGroups/' + encodeURIComponent(identifier)
        );

    }

    /**
     * Makes a request to the REST API to delete a user group, returning an observable
     * that can be used for processing the results of the call.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the user group to
     *     be deleted. This identifier corresponds to an AuthenticationProvider
     *     within the Guacamole web application.
     *
     * @param userGroup
     *     The user group to delete.
     *
     * @returns
     *     An observable for the HTTP call which will succeed if and only if the
     *     delete operation is successful.
     */
    deleteUserGroup(dataSource: string, userGroup: UserGroup): Observable<void> {

        // Delete user group
        return this.http.delete<void>(
            'api/session/data/' + encodeURIComponent(dataSource) + '/userGroups/' + encodeURIComponent(userGroup.identifier!)
        )

            // Clear the cache
            .pipe(tap(() => {
                // TODO: cacheService.users.removeAll();
            }));


    }

    /**
     * Makes a request to the REST API to create a user group, returning an observable
     * that can be used for processing the results of the call.
     *
     * @param dataSource
     *     The unique identifier of the data source in which the user group
     *     should be created. This identifier corresponds to an
     *     AuthenticationProvider within the Guacamole web application.
     *
     * @param userGroup
     *     The user group to create.
     *
     * @returns
     *     An observable for the HTTP call which will succeed if and only if the
     *     create operation is successful.
     */
    createUserGroup(dataSource: string, userGroup: UserGroup): Observable<void> {

        // Create user group
        return this.http.post<void>(
            'api/session/data/' + encodeURIComponent(dataSource) + '/userGroups',
            userGroup
        )

            // Clear the cache
            .pipe(tap(() => {
                // TODO: cacheService.users.removeAll();
            }));

    }

    /**
     * Makes a request to the REST API to save a user group, returning an observable
     * that can be used for processing the results of the call.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the user group to
     *     be updated. This identifier corresponds to an AuthenticationProvider
     *     within the Guacamole web application.
     *
     * @param userGroup
     *     The user group to update.
     *
     * @returns
     *     An observable for the HTTP call which will succeed if and only if the
     *     save operation is successful.
     */
    saveUserGroup(dataSource: string, userGroup: UserGroup): Observable<void> {

        // Update user group
        return this.http.put<void>(
            'api/session/data/' + encodeURIComponent(dataSource) + '/userGroups/' + encodeURIComponent(userGroup.identifier!),
            userGroup
        )

            // Clear the cache
            .pipe(tap(() => {
                // TODO: cacheService.users.removeAll();
            }));

    }

    /**
     * Makes a request to the REST API to apply a supplied list of user group
     * patches, returning an observable that can be used for processing the results
     * of the call.
     *
     * This operation is atomic - if any errors are encountered during the
     * connection patching process, the entire request will fail, and no
     * changes will be persisted.
     *
     * @param dataSource
     *     The identifier of the data source associated with the user groups to
     *     be patched.
     *
     * @param patches
     *     An array of patches to apply.
     *
     * @returns
     *     An observable for the HTTP call which will succeed if and only if the
     *     patch operation is successful.
     */
    patchUserGroups(dataSource: string, patches: DirectoryPatch<UserGroup>[]): Observable<DirectoryPatchResponse> {

        // Make the PATCH request
        return this.http.patch<DirectoryPatchResponse>(
            'api/session/data/' + encodeURIComponent(dataSource) + '/userGroups',
            patches
        )

            // Clear the cache
            .pipe(tap(patchResponse => {
                // TODO: cacheService.users.removeAll();
                return patchResponse;
            }));

    }
}
