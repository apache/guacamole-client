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
 * The controller for the root of the application.
 */
angular.module('index').controller('indexController', ['$scope', '$injector',
        function indexController($scope, $injector) {

    // Required services
    const $document         = $injector.get('$document');
    const $route            = $injector.get('$route');
    const $window           = $injector.get('$window');
    const clipboardService  = $injector.get('clipboardService');
    const guacNotification  = $injector.get('guacNotification');
    const guacClientManager = $injector.get('guacClientManager');

    /**
     * The error that prevents the current page from rendering at all. If no
     * such error has occurred, this will be null.
     *
     * @type Error
     */
    $scope.fatalError = null;

    /**
     * The notification service.
     */
    $scope.guacNotification = guacNotification;

    /**
     * All currently-active connections, grouped into their corresponding
     * tiled views.
     *
     * @type ManagedClientGroup[]
     */
    $scope.getManagedClientGroups = guacClientManager.getManagedClientGroups;

    /**
     * The message to display to the user as instructions for the login
     * process.
     *
     * @type TranslatableMessage
     */
    $scope.loginHelpText = null;

    /**
     * Whether the user has selected to log back in after having logged out.
     *
     * @type boolean
     */
    $scope.reAuthenticating = false;

    /**
     * The credentials that the authentication service is has already accepted,
     * pending additional credentials, if any. If the user is logged in, or no
     * credentials have been accepted, this will be null. If credentials have
     * been accepted, this will be a map of name/value pairs corresponding to
     * the parameters submitted in a previous authentication attempt.
     *
     * @type Object.<String, String>
     */
    $scope.acceptedCredentials = null;

    /**
     * The credentials that the authentication service is currently expecting,
     * if any. If the user is logged in, this will be null.
     *
     * @type Field[]
     */
    $scope.expectedCredentials = null;

    /**
     * Possible overall states of the client side of the web application.
     *
     * @enum {string}
     */
    var ApplicationState = {

        /**
         * The application has fully loaded but is awaiting credentials from
         * the user before proceeding.
         */
        AWAITING_CREDENTIALS : 'awaitingCredentials',

        /**
         * A fatal error has occurred that will prevent the client side of the
         * application from functioning properly.
         */
        FATAL_ERROR : 'fatalError',

        /**
         * The application has just started within the user's browser and has
         * not yet settled into any specific state.
         */
        LOADING : 'loading',

        /**
         * The user has manually logged out.
         */
        LOGGED_OUT : 'loggedOut',

        /**
         * The application has fully loaded and the user has logged in
         */
        READY : 'ready'

    };

    /**
     * The current overall state of the client side of the application.
     * Possible values are defined by {@link ApplicationState}.
     *
     * @type string
     */
    $scope.applicationState = ApplicationState.LOADING;

    /**
     * Basic page-level information.
     */
    $scope.page = {

        /**
         * The title of the page.
         * 
         * @type String
         */
        title: '',

        /**
         * The name of the CSS class to apply to the page body, if any.
         *
         * @type String
         */
        bodyClassName: ''

    };

    // Add default destination for input events
    var sink = new Guacamole.InputSink();
    $document[0].body.appendChild(sink.getElement());

    // Create event listeners at the global level
    var keyboard = new Guacamole.Keyboard($document[0]);
    keyboard.listenTo(sink.getElement());

    // Broadcast keydown events
    keyboard.onkeydown = function onkeydown(keysym) {

        // Do not handle key events if not logged in
        if ($scope.applicationState !== ApplicationState.READY)
            return true;

        // Warn of pending keydown
        var guacBeforeKeydownEvent = $scope.$broadcast('guacBeforeKeydown', keysym, keyboard);
        if (guacBeforeKeydownEvent.defaultPrevented)
            return true;

        // If not prevented via guacBeforeKeydown, fire corresponding keydown event
        var guacKeydownEvent = $scope.$broadcast('guacKeydown', keysym, keyboard);
        return !guacKeydownEvent.defaultPrevented;

    };
    
    // Broadcast keyup events
    keyboard.onkeyup = function onkeyup(keysym) {

        // Do not handle key events if not logged in or if a notification is
        // shown
        if ($scope.applicationState !== ApplicationState.READY)
            return;

        // Warn of pending keyup
        var guacBeforeKeydownEvent = $scope.$broadcast('guacBeforeKeyup', keysym, keyboard);
        if (guacBeforeKeydownEvent.defaultPrevented)
            return;

        // If not prevented via guacBeforeKeyup, fire corresponding keydown event
        $scope.$broadcast('guacKeyup', keysym, keyboard);

    };

    // Release all keys when window loses focus
    $window.onblur = function () {
        keyboard.reset();
    };

    // Release all keys upon form submission (there may not be corresponding
    // keyup events for key presses involved in submitting a form)
    $document.on('submit', function formSubmitted() {
        keyboard.reset();
    });

    // Attempt to read the clipboard if it may have changed
    $window.addEventListener('load',  clipboardService.resyncClipboard, true);
    $window.addEventListener('copy',  clipboardService.resyncClipboard);
    $window.addEventListener('cut',   clipboardService.resyncClipboard);
    $window.addEventListener('focus', function focusGained(e) {

        // Only recheck clipboard if it's the window itself that gained focus
        if (e.target === $window)
            clipboardService.resyncClipboard();

    }, true);

    /**
     * Reloads the current route and controller, effectively forcing
     * reauthentication. If the user is not logged in, this will result in
     * the login screen appearing.
     */
    $scope.reAuthenticate = function reAuthenticate() {
        $scope.reAuthenticating = true;
        $route.reload();
    };

    // Display login screen if a whole new set of credentials is needed
    $scope.$on('guacInvalidCredentials', function loginInvalid(event, parameters, error) {

        $scope.applicationState = ApplicationState.AWAITING_CREDENTIALS;
        $scope.page.title = 'APP.NAME';
        $scope.page.bodyClassName = '';

        $scope.loginHelpText = null;
        $scope.acceptedCredentials = {};
        $scope.expectedCredentials = error.expected;

    });

    // Prompt for remaining credentials if provided credentials were not enough
    $scope.$on('guacInsufficientCredentials', function loginInsufficient(event, parameters, error) {

        $scope.applicationState = ApplicationState.AWAITING_CREDENTIALS;
        $scope.page.title = 'APP.NAME';
        $scope.page.bodyClassName = '';

        $scope.loginHelpText = error.translatableMessage;
        $scope.acceptedCredentials = parameters;
        $scope.expectedCredentials = error.expected;

    });

    // Replace absolutely all content with an error message if the page itself
    // cannot be displayed due to an error
    $scope.$on('guacFatalPageError', function fatalPageError(error) {

        $scope.applicationState = ApplicationState.FATAL_ERROR;
        $scope.page.title = 'APP.NAME';
        $scope.page.bodyClassName = '';

        $scope.fatalError = error;

    });

    // Replace the overall user interface with an informational message if the
    // user has manually logged out
    $scope.$on('guacLogout', function loggedOut() {
        $scope.applicationState = ApplicationState.LOGGED_OUT;
        $scope.reAuthenticating = false;
    });

    // Ensure new pages always start with clear keyboard state
    $scope.$on('$routeChangeStart', function routeChanging() {
        keyboard.reset();
    });

    // Update title and CSS class upon navigation
    $scope.$on('$routeChangeSuccess', function(event, current, previous) {
       
        // If the current route is available
        if (current.$$route) {

            // Clear login screen if route change was successful (and thus
            // login was either successful or not required)
            $scope.applicationState = ApplicationState.READY;

            // Set title
            var title = current.$$route.title;
            if (title)
                $scope.page.title = title;

            // Set body CSS class
            $scope.page.bodyClassName = current.$$route.bodyClassName || '';
        }

    });

}]);
