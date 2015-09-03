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
 * Service which contains all REST API response caches.
 */
angular.module('rest').factory('dataSourceService', ['$injector',
        function dataSourceService($injector) {

    // Required services
    var $q = $injector.get('$q');

    // Service containing all caches
    var service = {};

    /**
     * Invokes the given function once for each of the given data sources,
     * passing that data source as the first argument to each invocation,
     * followed by any additional arguments passed to apply(). The results of
     * each invocation are aggregated into a map by data source identifier,
     * and handled through a single promise which is resolved or rejected
     * depending on the success/failure of each resulting REST call. Any error
     * results in rejection of the entire apply() operation, except 404 ("NOT
     * FOUND") errors, which are ignored.
     *
     * @param {Function} fn
     *     The function to call for each of the given data sources. The data
     *     source identifier will be given as the first argument, followed by
     *     the rest of the arguments given to apply(), in order. The function
     *     must return a Promise which is resolved or rejected depending on the
     *     result of the REST call.
     *
     * @param {String[]} dataSources
     *     The array or data source identifiers against which the given
     *     function should be called.
     *
     * @param {...*} args
     *     Any additional arguments to pass to the given function each time it
     *     is called.
     *
     * @returns {Promise.<Object.<String, *>>}
     *     A Promise which resolves with a map of data source identifier to
     *     corresponding result. The result will be the exact object or value
     *     provided as the resolution to the Promise returned by calls to the
     *     given function.
     */
    service.apply = function apply(fn, dataSources) {

        var deferred = $q.defer();

        var requests = [];
        var results = {};

        // Build array of arguments to pass to the given function
        var args = [];
        for (var i = 2; i < arguments.length; i++)
            args.push(arguments[i]);

        // Retrieve the root group from all data sources
        angular.forEach(dataSources, function invokeAgainstDataSource(dataSource) {

            // Add promise to list of pending requests
            var deferredRequest = $q.defer();
            requests.push(deferredRequest.promise);

            // Retrieve root group from data source
            fn.apply(this, [dataSource].concat(args))

            // Store result on success
            .then(function immediateRequestSucceeded(response) {
                results[dataSource] = response.data;
                deferredRequest.resolve();
            },

            // Fail on any errors (except "NOT FOUND")
            function immediateRequestFailed(response) {

                // Ignore "NOT FOUND" errors
                if (response.status === 404)
                    deferredRequest.resolve();

                // Explicitly abort for all other errors
                else
                    deferredRequest.reject(response);

            });

        });

        // Resolve if all requests succeed
        $q.all(requests).then(function requestsSucceeded() {
            deferred.resolve(results);
        },

        // Reject if at least one request fails
        function requestFailed(response) {
            deferred.reject(response);
        });

        return deferred.promise;

    };

    return service;

}]);
