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
 * Service that maintains the TCP reachability state of Guacamole connections
 * and refreshes it every 10 seconds by polling the backend.
 *
 * Typical usage (from guacGroupList.js):
 *   connectionReachabilityService.setTargets('mysql', ['1', '2', '14', '15']);
 *
 * In connection.html template:
 *   ng-class="{'vm-running': reachabilityService.reachable[item.identifier] === true,
 *              'vm-stopped': reachabilityService.reachable[item.identifier] === false}"
 */
angular.module('rest').factory('connectionReachabilityService', ['$injector',
        function connectionReachabilityServiceFactory($injector) {

    // Servicios necesarios obtenidos via $injector (patron estandar de este proyecto)
    var authenticationService = $injector.get('authenticationService');
    var $interval             = $injector.get('$interval');

    var service = {};

    /**
     * Map of connection ID (String) to Boolean reachability state.
     *   true  = host responds on its TCP port (machine is running).
     *   false = host is unreachable (machine is off or port is closed).
     *   No entry = state not yet known (first poll pending).
     *
     * IMPORTANT: this object is NEVER replaced; it is updated in-place via
     * {@code angular.extend()} so that AngularJS bindings in child scopes
     * continue to reference the same object.
     */
    service.reachable = {};

    /** Identifier of the current data source (e.g. "mysql"). */
    var currentDataSource = null;

    /** List of connection IDs to probe on each polling cycle. */
    var currentIds = [];

    /** Handle to the active $interval, used to cancel it when targets change. */
    var pollHandle = null;

    /**
     * Sends a GET request to the /reachable endpoint and merges the result
     * into service.reachable. If the request fails (network error, expired
     * session, etc.), it is silently ignored and the last known state is kept.
     */
    var pollReachability = function pollReachability() {
        if (!currentDataSource || !currentIds.length)
            return;

        authenticationService.request({
            method : 'GET',
            url    : 'api/session/data/' + encodeURIComponent(currentDataSource)
                     + '/connections/reachable',
            params : { ids : currentIds }
        }).then(function reachabilityReceived(result) {
            // result is the {id: bool} map returned by the Java endpoint.
            // angular.extend updates service.reachable without replacing the reference,
            // preserving bindings in child scopes.
            angular.extend(service.reachable, result);
        });
        // Errors are not propagated: if the backend fails transiently,
        // the last known state is displayed until the next poll.
    };

    /**
     * Registers the list of connections to monitor and (re)starts the polling loop.
     * Must be called whenever the connection list changes.
     *
     * @param {String}   dataSource  Data source identifier (e.g. "mysql").
     * @param {String[]} ids         Array of Guacamole connection IDs to probe.
     */
    service.setTargets = function setTargets(dataSource, ids) {
        currentDataSource = dataSource;
        currentIds = ids;

        // Cancel the previous polling interval if one is active
        if (pollHandle) {
            $interval.cancel(pollHandle);
            pollHandle = null;
        }

        // Probe immediately, then again every 10 seconds
        pollReachability();
        pollHandle = $interval(pollReachability, 10000);
    };

    return service;

}]);
