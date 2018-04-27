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
 * Service for converting $http promises that pass the entire response into
 * promises that pass only the data from that response.
 */
angular.module('rest').factory('requestService', ['$q', '$http', 'Error',
        function requestService($q, $http, Error) {

    /**
     * Given a configuration object formatted for the $http service, returns
     * a promise that will resolve or reject with only the data from the $http
     * response.
     *
     * @param {Object} object
     *   Configuration object for $http service call.
     *
     * @returns {Promise}
     *   A promise that will resolve or reject with the data from the response
     *   to the $http call.
     */
    var wrappedHttpCall = function wrappedHttpCall(object) {
        return $http(object).then(
            function success(request) { return request.data; },
            function failure(request) { throw new Error(request.data); }
        );
    };

    return wrappedHttpCall;
}]);
