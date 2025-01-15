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
import { User } from '../types/User';
import { UserPasswordUpdate } from '../types/UserPasswordUpdate';

/**
 * Service for operating on users via the REST API.
 */
@Injectable({
    providedIn: 'root'
})
export class UserService {

    /**
     * Inject required services.
     */
    constructor(private http: HttpClient) {
    }

    /**
     * Makes a request to the REST API to get the list of users,
     * returning an Observable that provides an object of User objects if
     * successful.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the users to be
     *     retrieved. This identifier corresponds to an AuthenticationProvider
     *     within the Guacamole web application.
     *
     * @param permissionTypes
     *     The set of permissions to filter with. A user must have one or more
     *     of these permissions for a user to appear in the result.
     *     If null, no filtering will be performed. Valid values are listed
     *     within PermissionSet.ObjectType.
     *
     * @returns An Observable that will emit an object of User objects
     *          where each key is the identifier (username) of the corresponding user.
     */
    getUsers(dataSource: string, permissionTypes?: string[]): Observable<Record<string, User>> {
        const httpParameters = new HttpParams();

        // Add permission filter if specified
        if (permissionTypes) {
            httpParameters.appendAll({ permission: permissionTypes });
        }

        // Retrieve users
        // TODO:  cache   : cacheService.users,
        return this.http.get<Record<string, User>>(`api/session/data/${encodeURIComponent(dataSource)}/users`, { params: httpParameters });
    }


    /**
     * Makes a request to the REST API to get the user having the given
     * username, returning an observable that provides the corresponding
     * @link{User} if successful.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the user to be
     *     retrieved. This identifier corresponds to an AuthenticationProvider
     *     within the Guacamole web application.
     *
     * @param username
     *     The username of the user to retrieve.
     *
     * @returns
     *     An observable which will emit a @link{User} upon success.
     */
    getUser(dataSource: string, username: string): Observable<User> {

        // Retrieve user
        // TODO: cache: cacheService.users,
        return this.http.get<User>('api/session/data/' + encodeURIComponent(dataSource) + '/users/' + encodeURIComponent(username));

    }

    /**
     * Makes a request to the REST API to delete a user, returning an observable
     * that can be used for processing the results of the call.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the user to be
     *     deleted. This identifier corresponds to an AuthenticationProvider
     *     within the Guacamole web application.
     *
     * @param user
     *     The user to delete.
     *
     * @returns
     *     An observable for the HTTP call which will succeed if and only if the
     *     delete operation is successful.
     */
    deleteUser(dataSource: string, user: User): Observable<void> {

        // Delete user
        return this.http.delete<void>('api/session/data/' + encodeURIComponent(dataSource) + '/users/' + encodeURIComponent(user.username));

    }

    /**
     * Makes a request to the REST API to create a user, returning an observable
     * that can be used for processing the results of the call.
     *
     * @param dataSource
     *     The unique identifier of the data source in which the user should be
     *     created. This identifier corresponds to an AuthenticationProvider
     *     within the Guacamole web application.
     *
     * @param user
     *     The user to create.
     *
     * @returns
     *     An observable for the HTTP call which will succeed if and only if the
     *     create operation is successful.
     */
    createUser(dataSource: string, user: User): Observable<User> {

        // Create user
        return this.http.post<User>('api/session/data/' + encodeURIComponent(dataSource) + '/users', user)

            // Clear the cache
            .pipe(tap(() => {
                // TODO: cacheService.users.removeAll();
            }));

    }

    /**
     * Makes a request to the REST API to save a user, returning an observable that
     * can be used for processing the results of the call.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the user to be
     *     updated. This identifier corresponds to an AuthenticationProvider
     *     within the Guacamole web application.
     *
     * @param user
     *     The user to update.
     *
     * @returns
     *     An observable for the HTTP call which will succeed if and only if the
     *     save operation is successful.
     */
    saveUser(dataSource: string, user: User): Observable<User> {

        // Update user
        return this.http.put<User>('api/session/data/' + encodeURIComponent(dataSource) + '/users/' + encodeURIComponent(user.username), user)

            // Clear the cache
            .pipe(tap(() => {
                // TODO: cacheService.users.removeAll();
            }));

    }

    /**
     * Makes a request to the REST API to update the password for a user,
     * returning an observable that can be used for processing the results of the call.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the user to be
     *     updated. This identifier corresponds to an AuthenticationProvider
     *     within the Guacamole web application.
     *
     * @param username
     *     The username of the user to update.
     *
     * @param oldPassword
     *     The exiting password of the user to update.
     *
     * @param newPassword
     *     The new password of the user to update.
     *
     * @returns
     *     An observable for the HTTP call which will succeed if and only if the
     *     password update operation is successful.
     */
    updateUserPassword(dataSource: string, username: string, oldPassword: string, newPassword: string): Observable<void> {

        // Update user password
        return this.http.put<void>(
            'api/session/data/' + encodeURIComponent(dataSource) + '/users/' + encodeURIComponent(username) + '/password',
            new UserPasswordUpdate({
                oldPassword: oldPassword,
                newPassword: newPassword
            })
        )

            // Clear the cache
            .pipe(tap(() => {
                // TODO: cacheService.users.removeAll();
            }));

    }


    /**
     * Makes a request to the REST API to apply a supplied list of user patches,
     * returning an observable that can be used for processing the results of the
     * call.
     *
     * This operation is atomic - if any errors are encountered during the
     * connection patching process, the entire request will fail, and no
     * changes will be persisted.
     *
     * @param dataSource
     *     The identifier of the data source associated with the users to be
     *     patched.
     *
     * @param patches
     *     An array of patches to apply.
     *
     * @returns
     *     An observable for the HTTP call which will succeed if and only if the
     *     patch operation is successful.
     */
    patchUsers(dataSource: string, patches: DirectoryPatch<User>[]): Observable<DirectoryPatchResponse> {

        // Make the PATCH request
        return this.http.patch<DirectoryPatchResponse>('api/session/data/' + encodeURIComponent(dataSource) + '/users', patches)

            // Clear the cache
            .pipe(tap(patchResponse => {
                // TODO: cacheService.users.removeAll();
            }));

    }
}
