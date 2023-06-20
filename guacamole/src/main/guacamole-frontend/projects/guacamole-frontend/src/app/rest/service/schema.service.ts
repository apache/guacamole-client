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
import { Observable } from 'rxjs';
import { Form } from '../types/Form';
import { Protocol } from '../types/Protocol';

/**
 * Service for operating on metadata via the REST API.
 */
@Injectable({
    providedIn: 'root'
})
export class SchemaService {

    /**
     * Inject required services.
     */
    constructor(private http: HttpClient) {
    }

    /**
     * Makes a request to the REST API to get the list of available attributes
     * for user objects, returning an observable that provides an array of
     * @link{Form} objects if successful. Each element of the array describes
     * a logical grouping of possible attributes.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the users whose
     *     available attributes are to be retrieved. This identifier
     *     corresponds to an AuthenticationProvider within the Guacamole web
     *     application.
     *
     * @returns
     *     An observable which will emit an array of @link{Form}
     *     objects, where each @link{Form} describes a logical grouping of
     *     possible attributes.
     */
    getUserAttributes(dataSource: string): Observable<Form[]> {

        // Retrieve available user attributes
        // TODO: cache   : cacheService.schema,
        return this.http.get<Form[]>('api/session/data/' + encodeURIComponent(dataSource) + '/schema/userAttributes');

    }

    /**
     * Makes a request to the REST API to get the list of available user preference
     * attributes, returning an observable that provides an array of @link{Form} objects
     * if successful. Each element of the array describes a logical grouping of
     * possible user preference attributes.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the users whose
     *     available user preference attributes are to be retrieved. This
     *     identifier corresponds to an AuthenticationProvider within the
     *     Guacamole web application.
     *
     * @returns
     *     An observable which will emit an array of @link{Form}
     *     objects, where each @link{Form} describes a logical grouping of
     *     possible attributes.
     */
    getUserPreferenceAttributes(dataSource: string): Observable<Form[]> {

        // Retrieve available user attributes
        // TODO: cache   : cacheService.schema,
        return this.http.get<Form[]>(
            'api/session/data/' + encodeURIComponent(dataSource) + '/schema/userPreferenceAttributes'
        );

    }

    /**
     * Makes a request to the REST API to get the list of available attributes
     * for user group objects, returning an observable that provides an array of
     * @link{Form} objects if successful. Each element of the array describes
     * a logical grouping of possible attributes.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the user groups
     *     whose available attributes are to be retrieved. This identifier
     *     corresponds to an AuthenticationProvider within the Guacamole web
     *     application.
     *
     * @returns
     *     An observable which will emit an array of @link{Form}
     *     objects, where each @link{Form} describes a logical grouping of
     *     possible attributes.
     */
    getUserGroupAttributes(dataSource: string): Observable<Form[]> {

        // Retrieve available user group attributes
        // TODO: cache   : cacheService.schema,
        return this.http.get<Form[]>(
            'api/session/data/' + encodeURIComponent(dataSource) + '/schema/userGroupAttributes'
        );

    }

    /**
     * Makes a request to the REST API to get the list of available attributes
     * for connection objects, returning an observable that provides an array of
     * @link{Form} objects if successful. Each element of the array describes
     * a logical grouping of possible attributes.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the connections
     *     whose available attributes are to be retrieved. This identifier
     *     corresponds to an AuthenticationProvider within the Guacamole web
     *     application.
     *
     * @returns
     *     An observable which will emit an array of @link{Form}
     *     objects, where each @link{Form} describes a logical grouping of
     *     possible attributes.
     */
    getConnectionAttributes(dataSource: string): Observable<Form[]> {

        // Retrieve available connection attributes
        // TODO: cache   : cacheService.schema,
        return this.http.get<Form[]>(
            'api/session/data/' + encodeURIComponent(dataSource) + '/schema/connectionAttributes'
        );

    }

    /**
     * Makes a request to the REST API to get the list of available attributes
     * for sharing profile objects, returning an observable that provides an array
     * of @link{Form} objects if successful. Each element of the array describes
     * a logical grouping of possible attributes.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the sharing
     *     profiles whose available attributes are to be retrieved. This
     *     identifier corresponds to an AuthenticationProvider within the
     *     Guacamole web application.
     *
     * @returns
     *     An observable which will emit an array of @link{Form}
     *     objects, where each @link{Form} describes a logical grouping of
     *     possible attributes.
     */
    getSharingProfileAttributes(dataSource: string): Observable<Form[]> {

        // Retrieve available sharing profile attributes
        // TODO: cache   : cacheService.schema,
        return this.http.get<Form[]>(
            'api/session/data/' + encodeURIComponent(dataSource) + '/schema/sharingProfileAttributes'
        );

    }

    /**
     * Makes a request to the REST API to get the list of available attributes
     * for connection group objects, returning an observable that provides an array
     * of @link{Form} objects if successful. Each element of the array
     * a logical grouping of possible attributes.
     *
     * @param dataSource
     *     The unique identifier of the data source containing the connection
     *     groups whose available attributes are to be retrieved. This
     *     identifier corresponds to an AuthenticationProvider within the
     *     Guacamole web application.
     *
     * @returns
     *     An observable which will emit an array of @link{Form}
     *     objects, where each @link{Form} describes a logical grouping of
     *     possible attributes.
     */
    getConnectionGroupAttributes(dataSource: string): Observable<Form[]> {

        // Retrieve available connection group attributes
        // TODO: cache   : cacheService.schema,
        return this.http.get<Form[]>(
            'api/session/data/' + encodeURIComponent(dataSource) + '/schema/connectionGroupAttributes'
        );

    }

    /**
     * Makes a request to the REST API to get the list of protocols, returning
     * an observable that provides a map of @link{Protocol} objects by protocol
     * name if successful.
     *
     * @param dataSource
     *     The unique identifier of the data source defining available
     *     protocols. This identifier corresponds to an AuthenticationProvider
     *     within the Guacamole web application.
     *
     * @returns
     *     An observable which will emit a map of @link{Protocol}
     *     objects by protocol name upon success.
     */
    getProtocols(dataSource: string): Observable<Record<string, Protocol>> {

        // Retrieve available protocols
        // TODO: cache   : cacheService.schema,
        return this.http.get<Record<string, Protocol>>(
            'api/session/data/' + encodeURIComponent(dataSource) + '/schema/protocols'
        );

    }

}
