/*
 * Copyright (C) 2015 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/**
 * Service for operating on history records via the REST API.
 */
angular.module('rest').factory('historyService', ['$injector',
        function historyService($injector) {

    // Required services
    var $http                 = $injector.get('$http');
    var authenticationService = $injector.get('authenticationService');

    var service = {};

    /**
     * Makes a request to the REST API to get the usage history of all
     * accessible connections, returning a promise that provides the
     * corresponding array of @link{ConnectionHistoryEntry} objects if
     * successful.
     *
     * @param {String} dataSource
     *     The unique identifier of the data source containing the connection
     *     history records to be retrieved. This identifier corresponds to an
     *     AuthenticationProvider within the Guacamole web application.
     *
     * @param {String[]} [requiredContents]
     *     The set of arbitrary strings to filter with. A ConnectionHistoryEntry
     *     must contain each of these values within the associated username,
     *     connection name, start date, or end date to appear in the result. If
     *     null, no filtering will be performed.
     *
     * @param {String[]} [sortPredicates]
     *     The set of predicates to sort against. The resulting array of
     *     ConnectionHistoryEntry objects will be sorted according to the
     *     properties and sort orders defined by each predicate. If null, the
     *     order of the resulting entries is undefined. Valid values are listed
     *     within ConnectionHistoryEntry.SortPredicate.
     *
     * @returns {Promise.<ConnectionHistoryEntry[]>}
     *     A promise which will resolve with an array of
     *     @link{ConnectionHistoryEntry} objects upon success.
     */
    service.getConnectionHistory = function getConnectionHistory(dataSource,
        requiredContents, sortPredicates) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Filter according to contents if restrictions are specified
        if (requiredContents)
            httpParameters.contains = requiredContents;

        // Sort according to provided predicates, if any
        if (sortPredicates)
            httpParameters.order = sortPredicates;

        // Retrieve connection history
        return $http({
            method  : 'GET',
            url     : 'api/data/' + encodeURIComponent(dataSource) + '/history/connections',
            params  : httpParameters
        });

    };

    return service;

}]);
