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

/**
 * Reformats the URL of the current page such that normal query parameters will
 * be received by the application, whose router reads only the query parameters
 * within the fragment identifier. If possible, this reformatting operation
 * will be performed using the HTML5 History API, thus avoiding reloading the
 * page.
 *
 * For example, if a user visits the following URL:
 *
 *     http://example.org/some/application/?foo=bar
 *
 * this function will reformat the URL as:
 *
 *     http://example.org/some/application/#/?foo=bar
 *
 * If the URL does not contain query parameters, or the query parameters are
 * already in a format which the router can read, then the URL is left
 * untouched.
 *
 * If query parameters are present both in the normal format AND within the
 * URL fragment identifier, the query parameters are merged such that the
 * router can read all parameters.
 *
 * @param location
 *     The Location object representing the URL of the current page.
 *
 * @param history
 *     The History object of the current page, used to replace the URL.
 *
 * @param title
 *     The title of the current page.
 */
export function relocateParameters(
    location: Pick<Location, 'search' | 'origin' | 'pathname' | 'hash'>,
    history: Pick<History, 'replaceState'>,
    title: string
): void {

    /**
     * The default path, including leading '#' character, which should be used
     * if the URL of the current page has no fragment identifier.
     */
    const DEFAULT_ROUTER_PATH = '#/';

    /**
     * The query parameters within the URL of the current page, including the
     * leading '?' character.
     */
    const parameters = location.search;

    // Nothing to do if there are no query parameters outside the fragment
    if (!parameters)
        return;

    /**
     * The base URL of the current page, containing only the protocol, hostname,
     * and path. Query parameters and the fragment, if any, are excluded.
     */
    const baseUrl = location.origin + location.pathname;

    /**
     * The router-specific path within the fragment identifier of the URL of
     * the current page, including the leading '#' character of the fragment
     * identifier. If no fragment identifier is present, the default path will
     * be used.
     */
    const routerUrl = location.hash || DEFAULT_ROUTER_PATH;

    // If the fragment already contains parameters, replace the leading '?' of
    // the parameters to add with an '&' prior to appending them
    const appendedParameters = routerUrl.includes('?') ? '&' + parameters.substring(1) : parameters;

    // Reformat the URL such that query parameters are after the router's path
    history.replaceState(null, title, baseUrl + routerUrl + appendedParameters);

}
