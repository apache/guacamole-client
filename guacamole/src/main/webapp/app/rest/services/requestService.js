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
angular.module('rest').factory('requestService', ['$injector',
        function requestService($injector) {

    // Required services
    var $http = $injector.get('$http');

    // Required types
    var Error = $injector.get('Error');

    /**
     * Given a configuration object formatted for the $http service, returns
     * a promise that will resolve or reject with the data from the HTTP
     * response. If the promise is rejected due to the HTTP response indicating
     * failure, the promise will be rejected strictly with an instance of an
     * @link{Error} object.
     *
     * @param {Object} object
     *   Configuration object for $http service call.
     *
     * @returns {Promise.<Object>}
     *   A promise that will resolve with the data from the HTTP response for
     *   the underlying $http call if successful, or reject with an @link{Error}
     *   describing the failure.
     */
    var service = function wrapHttpServiceCall(object) {
        return $http(object).then(
            function success(response) { return response.data; },
            function failure(response) {

                // Wrap true error responses from $http within REST Error objects
                if (response.data)
                    throw new Error(response.data);

                // The value provided is not actually a response object from
                // the $http service
                throw response;

            }
        );
    };

    return service;

}]);
