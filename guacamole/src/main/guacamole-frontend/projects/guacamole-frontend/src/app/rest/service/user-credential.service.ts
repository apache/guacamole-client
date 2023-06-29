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

import { Inject, Injectable } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { UserCredentials } from '../types/UserCredentials';
import { Field } from '../types/Field';

/**
 * Service which provides functions for working with UserCredentials
 */
@Injectable({
    providedIn: 'root'
})
export class UserCredentialService {

    /**
     * Reference to the window object.
     */
    private window: Window;

    /**
     * Inject a document reference to get a reference to the window object.
     */
    constructor(@Inject(DOCUMENT) private document: Document) {
        this.window = this.document.defaultView as Window;
    }

    /**
     * Generates a query string containing all QUERY_PARAMETER fields from the
     * given UserCredentials object, along with their corresponding values. The
     * parameter names and values will be appropriately URL-encoded and
     * separated by ampersands.
     *
     * @param userCredentials
     *     The UserCredentials to retrieve all query parameters from.
     *
     * @returns
     *     A string containing all QUERY_PARAMETER fields as name/value pairs
     *     separated by ampersands, where each name is separated by the value
     *     by an equals sign.
     */
    getQueryParameters(userCredentials: UserCredentials): string {

        // Build list of parameter name/value pairs
        const parameters: string[] = [];
        userCredentials.expected?.forEach(field => {

            // Only add query parameters
            if (field.type !== Field.Type.QUERY_PARAMETER)
                return;

            // Pull parameter name and value
            const name = field.name;
            const value = userCredentials.values?.[name] || '';

            // Properly encode name/value pair
            parameters.push(encodeURIComponent(name) + '=' + encodeURIComponent(value));

        });

        // Separate each name/value pair by an ampersand
        return parameters.join('&');

    }

    /**
     * Returns a fully-qualified, absolute URL to Guacamole prepopulated with
     * any query parameters dictated by the QUERY_PARAMETER fields defined in
     * the given UserCredentials.
     *
     * @param userCredentials
     *     The UserCredentials to retrieve all query parameters from.
     *
     * @returns
     *     A fully-qualified, absolute URL to Guacamole prepopulated with the
     *     query parameters dictated by the given UserCredentials.
     */
    getLink(userCredentials: UserCredentials): string {

        const linkOrigin = this.window.location.origin;

        // Build base link
        let link = linkOrigin
            + this.window.location.pathname
            + '#/';

        // Add any required parameters
        const params = this.getQueryParameters(userCredentials);
        if (params)
            link += '?' + params;

        return link;

    }
}
