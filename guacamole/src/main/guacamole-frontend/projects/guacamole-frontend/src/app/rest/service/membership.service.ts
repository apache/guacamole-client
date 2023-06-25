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
import { HttpClient } from '@angular/common/http';
import { RelatedObjectPatch } from '../types/RelatedObjectPatch';
import { AuthenticationService } from '../../auth/service/authentication.service';
import { Observable, tap } from 'rxjs';

/**
 * Service for operating on user group memberships via the REST API.
 */
@Injectable({
    providedIn: 'root'
})
export class MembershipService {

    /**
     * Inject required services.
     */
    constructor(private http: HttpClient, private authenticationService: AuthenticationService) {
    }

    /**
     * Creates a new array of patches which represents the given changes to an
     * arbitrary set of objects sharing some common relation.
     *
     * @param identifiersToAdd
     *     The identifiers of all objects which should be added to the
     *     relation, if any.
     *
     * @param identifiersToRemove
     *     The identifiers of all objects which should be removed from the
     *     relation, if any.
     *
     * @returns
     *     A new array of patches which represents the given changes.
     */
    private getRelatedObjectPatch(identifiersToAdd?: string[], identifiersToRemove?: string[]): RelatedObjectPatch[] {

        const patch: RelatedObjectPatch[] = [];

        identifiersToAdd?.forEach(identifier => {
            patch.push(new RelatedObjectPatch({
                op: RelatedObjectPatch.Operation.ADD,
                value: identifier
            }));
        });

        identifiersToRemove?.forEach(identifier => {
            patch.push(new RelatedObjectPatch({
                op: RelatedObjectPatch.Operation.REMOVE,
                value: identifier
            }));
        });

        return patch;

    }

    /**
     * Returns the URL for the REST resource most appropriate for accessing
     * the parent user groups of the user or group having the given identifier.
     *
     * It is important to note that a particular data source can authenticate
     * and provide user groups for a user, even if that user does not exist
     * within that data source (and thus cannot be found beneath
     * "api/session/data/{dataSource}/users")
     *
     * @param dataSource
     *     The unique identifier of the data source containing the user or
     *     group whose parent user groups should be retrieved. This identifier
     *     corresponds to an AuthenticationProvider within the Guacamole web
     *     application.
     *
     * @param identifier
     *     The identifier of the user or group for which the URL of the proper
     *     REST resource should be derived.
     *
     * @param group
     *     Whether the provided identifier refers to a user group. If false or
     *     omitted, the identifier given is assumed to refer to a user.
     *
     * @returns
     *     The URL for the REST resource representing the parent user groups of
     *     the user or group having the given identifier.
     */
    private getUserGroupsResourceURL(dataSource: string, identifier: string, group?: boolean): string {

        // Create base URL for data source
        const base = 'api/session/data/' + encodeURIComponent(dataSource);

        // Access parent groups directly (there is no "self" for user groups
        // as there is for users)
        if (group)
            return base + '/userGroups/' + encodeURIComponent(identifier) + '/userGroups';

        // If the username is that of the current user, do not rely on the
        // user actually existing (they may not). Access their parent groups via
        // "self" rather than the collection of defined users.
        if (identifier === this.authenticationService.getCurrentUsername())
            return base + '/self/userGroups';

        // Otherwise, the user must exist for their parent groups to be
        // accessible. Use the collection of defined users.
        return base + '/users/' + encodeURIComponent(identifier) + '/userGroups';

    }

    /**
     * Makes a request to the REST API to retrieve the identifiers of all
     * parent user groups of which a given user or group is a member, returning
     * an observable that can be used for processing the results of the call.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the user or
     *     group whose parent user groups should be retrieved. This identifier
     *     corresponds to an AuthenticationProvider within the Guacamole web
     *     application.
     *
     * @param identifier
     *     The identifier of the user or group to retrieve the parent user
     *     groups of.
     *
     * @param group
     *     Whether the provided identifier refers to a user group. If false or
     *     omitted, the identifier given is assumed to refer to a user.
     *
     * @returns
     *     An observable for the HTTP call which will emit an array
     *     containing the requested identifiers upon success.
     */
    getUserGroups(dataSource: string, identifier: string, group?: boolean): Observable<string[]> {

        // Retrieve parent groups
        // TODO: cache   : cacheService.users,
        return this.http.get<string[]>(this.getUserGroupsResourceURL(dataSource, identifier, group));

    }

    /**
     * Makes a request to the REST API to modify the parent user groups of
     * which a given user or group is a member, returning an observable that can be
     * used for processing the results of the call.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the user or
     *     group whose parent user groups should be modified. This identifier
     *     corresponds to an AuthenticationProvider within the Guacamole web
     *     application.
     *
     * @param identifier
     *     The identifier of the user or group to modify the parent user
     *     groups of.
     *
     * @param addToUserGroups
     *     The identifier of all parent user groups to which the given user or
     *     group should be added as a member, if any.
     *
     * @param removeFromUserGroups
     *     The identifier of all parent user groups from which the given member
     *     user or group should be removed, if any.
     *
     * @param group
     *     Whether the provided identifier refers to a user group. If false or
     *     omitted, the identifier given is assumed to refer to a user.
     *
     * @returns
     *     An observable for the HTTP call which will succeed if and only if the
     *     patch operation is successful.
     */
    patchUserGroups(dataSource: string, identifier: string, addToUserGroups?: string[], removeFromUserGroups?: string[], group?: boolean): Observable<void> {

        // Update parent user groups
        return this.http.patch<void>(
            this.getUserGroupsResourceURL(dataSource, identifier, group),
            this.getRelatedObjectPatch(addToUserGroups, removeFromUserGroups)
        )

            // Clear the cache
            .pipe(tap(() => {
                // TODO: cacheService.users.removeAll();
            }));

    }

    /**
     * Makes a request to the REST API to retrieve the identifiers of all
     * users which are members of the given user group, returning an observable
     * that can be used for processing the results of the call.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the user group
     *     whose member users should be retrieved. This identifier corresponds
     *     to an AuthenticationProvider within the Guacamole web application.
     *
     * @param identifier
     *     The identifier of the user group to retrieve the member users of.
     *
     * @returns
     *     An observable for the HTTP call which will emit an array
     *     containing the requested identifiers upon success.
     */
    getMemberUsers(dataSource: string, identifier: string): Observable<string[]> {

        // Retrieve member users
        // TODO: cache   : cacheService.users,
        return this.http.get<string[]>(
            'api/session/data/' + encodeURIComponent(dataSource) + '/userGroups/' + encodeURIComponent(identifier) + '/memberUsers'
        );

    }

    /**
     * Makes a request to the REST API to modify the member users of a given
     * user group, returning an observable that can be used for processing the
     * results of the call.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the user group
     *     whose member users should be modified. This identifier corresponds
     *     to an AuthenticationProvider within the Guacamole web application.
     *
     * @param identifier
     *     The identifier of the user group to modify the member users of.
     *
     * @param usersToAdd
     *     The identifier of all users to add as members of the given user
     *     group, if any.
     *
     * @param usersToRemove
     *     The identifier of all users to remove from the given user group,
     *     if any.
     *
     * @returns
     *     An observable for the HTTP call which will succeed if and only if the
     *     patch operation is successful.
     */
    patchMemberUsers(dataSource: string, identifier: string, usersToAdd?: string[], usersToRemove?: string[]): Observable<void> {

        // Update member users
        return this.http.patch<void>(
            'api/session/data/' + encodeURIComponent(dataSource) + '/userGroups/' + encodeURIComponent(identifier) + '/memberUsers',
            this.getRelatedObjectPatch(usersToAdd, usersToRemove)
        )

            // Clear the cache
            .pipe(tap(() => {
                // TODO:  cacheService.users.removeAll();
            }));

    }

    /**
     * Makes a request to the REST API to retrieve the identifiers of all
     * user groups which are members of the given user group, returning an
     * observable that can be used for processing the results of the call.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the user group
     *     whose member user groups should be retrieved. This identifier
     *     corresponds to an AuthenticationProvider within the Guacamole web
     *     application.
     *
     * @param identifier
     *     The identifier of the user group to retrieve the member user
     *     groups of.
     *
     * @returns
     *     An observable for the HTTP call which will emit an array
     *     containing the requested identifiers upon success.
     */
    getMemberUserGroups(dataSource: string, identifier: string): Observable<string[]> {

        // Retrieve member user groups
        // TODO: cache: cacheService.users,
        return this.http.get<string[]>(
            'api/session/data/' + encodeURIComponent(dataSource) + '/userGroups/' + encodeURIComponent(identifier) + '/memberUserGroups'
        );

    }

    /**
     * Makes a request to the REST API to modify the member user groups of a
     * given user group, returning an observable that can be used for processing
     * the results of the call.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the user group
     *     whose member user groups should be modified. This identifier
     *     corresponds to an AuthenticationProvider within the Guacamole web
     *     application.
     *
     * @param identifier
     *     The identifier of the user group to modify the member user groups of.
     *
     * @param userGroupsToAdd
     *     The identifier of all user groups to add as members of the given
     *     user group, if any.
     *
     * @param userGroupsToRemove
     *     The identifier of all member user groups to remove from the given
     *     user group, if any.
     *
     * @returns
     *     An observable for the HTTP call which will succeed if and only if the
     *     patch operation is successful.
     */
    patchMemberUserGroups(dataSource: string, identifier: string, userGroupsToAdd?: string[], userGroupsToRemove?: string[]): Observable<void> {

        // Update member user groups
        return this.http.patch<void>(
            'api/session/data/' + encodeURIComponent(dataSource) + '/userGroups/' + encodeURIComponent(identifier) + '/memberUserGroups',
            this.getRelatedObjectPatch(userGroupsToAdd, userGroupsToRemove)
        )

            // Clear the cache
            .pipe(tap(() => {
                // TODO: cacheService.users.removeAll();
            }));

    }


}
