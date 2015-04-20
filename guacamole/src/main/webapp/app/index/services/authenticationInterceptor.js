/*
 * Copyright (C) 2014 Glyptodon LLC
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

angular.module('index').factory('authenticationInterceptor', ['$injector',
        function authenticationInterceptor($injector) {

    // Required services
    var $location = $injector.get('$location');
    var $q        = $injector.get('$q');

    var service = {};

    /**
     * Redirect users to login if authorization fails. This is called
     * automatically when this service is registered as an interceptor, as
     * documented at:
     * 
     * https://docs.angularjs.org/api/ng/service/$http#interceptors
     *
     * @param {HttpPromise} rejection
     *     The promise associated with the HTTP request that failed.
     *
     * @returns {Promise}
     *     A rejected promise containing the originally-rejected HttpPromise.
     */
    service.responseError = function responseError(rejection) {

        // Only redirect failed authentication requests
        if ((rejection.status === 401 || rejection.status === 403)
                && rejection.config.url  === 'api/tokens') {

            // Only redirect if not already on login page
            if ($location.path() !== '/login/')
                $location.path('/login/');

        }

        return $q.reject(rejection);

    };

    return service;

}]);
