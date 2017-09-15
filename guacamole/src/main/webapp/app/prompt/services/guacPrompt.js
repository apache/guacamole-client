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
 * Service for displaying prompts and modal status dialogs.
 */
angular.module('prompt').factory('guacPrompt', ['$injector',
        function guacPrompt($injector) {

    // Required services
    var $location             = $injector.get('$location');
    var $log                  = $injector.get('$log');
    var $q                    = $injector.get('$q');
    var $rootScope            = $injector.get('$rootScope');
    var $window               = $injector.get('$window');
    var sessionStorageFactory = $injector.get('sessionStorageFactory');

    var service = {};

    /**
     * Getter/setter which retrieves or sets the current status prompt,
     * which may simply be false if no status is currently shown.
     * 
     * @type Function
     */
    var storedPrompt = sessionStorageFactory.create(false);

    /**
     * Retrieves the current status prompt, which may simply be false if
     * no status is currently shown.
     * 
     * @type Prompt|Boolean
     */
    service.getPrompt = function getPrompt() {
        return storedPrompt();
    };

    /**
     * Shows or hides the given prompt as a modal status. If a status
     * prompt is currently shown, no further statuses will be shown
     * until the current status is hidden.
     *
     * @param {Prompt|Boolean|Object} status
     *     The status prompt to show.
     *
     * @example
     * 
     * // To show a status message with actions
     * guacPrompt.showPrompt({
     *     'title'      : 'Disconnected',
     *     'text'       : {
     *         'key' : 'NAMESPACE.SOME_TRANSLATION_KEY'
     *     },
     *     'actions'    : {
     *         'name'       : 'reconnect',
     *         'callback'   : function () {
     *             // Reconnection code goes here
     *         }
     *     }
     * });
     * 
     * // To hide the status message
     * guacPrompt.showPrompt(false);
     */
    service.showPrompt = function showPrompt(status) {
        if (!storedPrompt() || !status)
            storedPrompt(status);
    };

    /**
     * Prompt for fields.
     */
    service.getUserInput = function getUserInput(prompts,connection) {

        $log.debug('Received prompt object: ' + JSON.stringify(prompts));
        $log.debug('Received connection object: ' + JSON.stringify(connection));

        var deferred = $q.defer();
        var responses = {};
        var homeUrl = '/';

        service.showPrompt({
            'title'     : 'Connection Parameters for ' + connection.name,
            'connection' : connection,
            'text'      : {
                key     : 'Please provide the following parameters to complete the connection:'
            },
            'prompts'   : prompts,
            'actions'   : [{
                'name'  : 'Connect',
                'callback' : function() {
                    $log.debug('Received responses: ' + responses);
                    deferred.resolve(responses);
                    service.showPrompt(false);
                },
            },
            {
                'name'  : 'Cancel',
                'callback' : function() {
                    $log.debug('Cancelling connection and going home.');
                    deferred.reject();
                    service.showPrompt(false);
                    $location.url(homeUrl);
                },
            }],
            'responses' : responses
        });

        $log.debug('One day, we will return those prompts, we promise!');
        return deferred.promise;

    };

    var preventEvent = function(event) {
        if (service.getPrompt())
            event.stopPropagation();
    };

    $window.addEventListener('input', preventEvent, true);
    $window.addEventListener('keydown', preventEvent, true);
    $window.addEventListener('keypress', preventEvent, true);
    $window.addEventListener('keyup', preventEvent, true);
    $window.addEventListener('click', preventEvent, false);


    // Hide status upon navigation
    $rootScope.$on('$routeChangeSuccess', function() {
        service.showPrompt(false);
    });

    return service;

}]);
