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
 * Service for displaying prompts as modal dialogs.
 */
angular.module('prompt').factory('guacPrompt', ['$injector',
        function guacPrompt($injector) {

    // Required services
    var $location             = $injector.get('$location');
    var $q                    = $injector.get('$q');
    var $rootScope            = $injector.get('$rootScope');
    var $window               = $injector.get('$window');
    var sessionStorageFactory = $injector.get('sessionStorageFactory');

    var service = {};

    /**
     * Object which retrieves or sets the current prompts,
     * or false if no prompt is currently shown.
     * 
     * @type Function
     */
    var storedPrompt = sessionStorageFactory.create(false);

    /**
     * Retrieves the current prompt, or false if no prompt
     * is currently shown.
     * 
     * @type Prompt|Boolean
     */
    service.getPrompt = function getPrompt() {
        return storedPrompt();
    };

    /**
     * Shows or hides the given prompt as a modal dialog.  Only one
     * prompt dialog will be shown at a time.
     *
     * @param {Prompt|Boolean|Object} status
     *     The prompt object to show.
     */
    service.showPrompt = function showPrompt(status) {
        if (!storedPrompt() || !status)
            storedPrompt(status);
    };

    /**
     * Taking a list of prompts and a connection, display the prompt
     * dialog for the user to fill out, and return a promise that
     * responses will be provided.
     *
     * @param prompts
     *     The list of prompts to display to the user.
     *
     * @param connection
     *     The connection that the prompts are being used for.
     *
     * @returns
     *     A promise for responses that the user will fill in.
     */
    service.getUserInput = function getUserInput(prompts,connection) {

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
                    deferred.resolve(responses);
                    service.showPrompt(false);
                },
            },
            {
                'name'  : 'Cancel',
                'callback' : function() {
                    deferred.reject();
                    service.showPrompt(false);
                    $location.url(homeUrl);
                },
            }],
            'responses' : responses
        });

        return deferred.promise;

    };

    /**
     * Method to stop propagation of events.
     *
     * @param event
     *     The event to stop.
     */
    var preventEvent = function(event) {
        if (service.getPrompt())
            event.stopPropagation();
    };

    /**
     * When the prompt object is displayed, prevent keypress and
     * mouse clicks from propagating through to the client so that
     * the prompt dialog can be completed.
     */
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
