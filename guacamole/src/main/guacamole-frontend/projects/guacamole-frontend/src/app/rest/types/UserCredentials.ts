

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

import { Field } from './Field';

/**
 * Returned by REST API calls to define a full set of valid
 * credentials, including field definitions and corresponding expected
 * values.
 */
export class UserCredentials {

    /**
     * Any parameters which should be provided when these credentials are
     * submitted. If no such information is available, this will be undefined.
     */
    expected: Field[];

    /**
     * A map of all field values by field name. The fields having the names
     * used within this map should be defined within the @link{Field} array
     * stored under the @link{expected} property.
     */
    values: Record<string, string>;

    /**
     * Creates a new UserCredentials object.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     UserCredentials.
     */
    constructor(template: UserCredentials) {
        this.expected = template.expected;
        this.values = template.values;
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
    static getQueryParameters(userCredentials: UserCredentials): string {

        // Build list of parameter name/value pairs
        const parameters: string[] = [];
        userCredentials.expected?.forEach(field => {

            // Only add query parameters
            if (field.type !== Field.Type.QUERY_PARAMETER)
                return;

            // Pull parameter name and value
            const name = field.name;
            const value = userCredentials.values[name];

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
    static getLink(userCredentials: UserCredentials): string {

        const linkOrigin = window.location.origin;

        // Build base link
        let link = linkOrigin
            + window.location.pathname
            + '#/';

        // Add any required parameters
        const params = UserCredentials.getQueryParameters(userCredentials);
        if (params)
            link += '?' + params;

        return link;

    }
}
