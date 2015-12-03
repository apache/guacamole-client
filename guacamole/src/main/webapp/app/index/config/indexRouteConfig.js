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

/**
 * The config block for setting up all the url routing.
 */
angular.module('index').config(['$routeProvider', '$locationProvider', 
        function indexRouteConfig($routeProvider, $locationProvider) {

    // Disable HTML5 mode (use # for routing)
    $locationProvider.html5Mode(false);

    /**
     * Attempts to re-authenticate with the Guacamole server, sending any
     * query parameters in the URL, along with the current auth token, and
     * updating locally stored token if necessary.
     *
     * @param {Service} $injector
     *     The Angular $injector service.
     * 
     * @returns {Promise}
     *     A promise which resolves successfully only after an attempt to
     *     re-authenticate has been made. If the authentication attempt fails,
     *     the promise will be rejected.
     */
    var updateCurrentToken = ['$injector', function updateCurrentToken($injector) {

        // Required services
        var $location             = $injector.get('$location');
        var authenticationService = $injector.get('authenticationService');

        // Re-authenticate including any parameters in URL
        return authenticationService.updateCurrentToken($location.search());

    }];

    /**
     * Redirects the user to their home page. This necessarily requires
     * attempting to re-authenticate with the Guacamole server, as the user's
     * credentials may have changed, and thus their most-appropriate home page
     * may have changed as well.
     *
     * @param {Service} $injector
     *     The Angular $injector service.
     * 
     * @returns {Promise}
     *     A promise which resolves successfully only after an attempt to
     *     re-authenticate and determine the user's proper home page has been
     *     made.
     */
    var routeToUserHomePage = ['$injector', function routeToUserHomePage($injector) {

        // Required services
        var $location       = $injector.get('$location');
        var $q              = $injector.get('$q');
        var userPageService = $injector.get('userPageService');

        // Promise for routing attempt
        var route = $q.defer();

        // Re-authenticate including any parameters in URL
        $injector.invoke(updateCurrentToken)
        .then(function tokenUpdateComplete() {

            // Redirect to home page
            userPageService.getHomePage()
            .then(function homePageRetrieved(homePage) {

                // If home page is the requested location, allow through
                if ($location.path() === homePage.url)
                    route.resolve();

                // Otherwise, reject and reroute
                else {
                    $location.url(homePage.url);
                    route.reject();
                }

            })

            // If retrieval of home page fails, assume requested page is OK
            ['catch'](function homePageFailed() {
                route.resolve();
            });

        });

        // Return promise that will resolve only if the requested page is the
        // home page
        return route.promise;

    }];

    // Configure each possible route
    $routeProvider

        // Home screen
        .when('/', {
            title         : 'APP.NAME',
            bodyClassName : 'home',
            templateUrl   : 'app/home/templates/home.html',
            controller    : 'homeController',
            resolve       : { routeToUserHomePage: routeToUserHomePage }
        })

        // Management screen
        .when('/settings/:dataSource?/:tab', {
            title         : 'APP.NAME',
            bodyClassName : 'settings',
            templateUrl   : 'app/settings/templates/settings.html',
            controller    : 'settingsController',
            resolve       : { updateCurrentToken: updateCurrentToken }
        })

        // Connection editor
        .when('/manage/:dataSource/connections/:id?', {
            title         : 'APP.NAME',
            bodyClassName : 'manage',
            templateUrl   : 'app/manage/templates/manageConnection.html',
            controller    : 'manageConnectionController',
            resolve       : { updateCurrentToken: updateCurrentToken }
        })

        // Connection group editor
        .when('/manage/:dataSource/connectionGroups/:id?', {
            title         : 'APP.NAME',
            bodyClassName : 'manage',
            templateUrl   : 'app/manage/templates/manageConnectionGroup.html',
            controller    : 'manageConnectionGroupController',
            resolve       : { updateCurrentToken: updateCurrentToken }
        })

        // User editor
        .when('/manage/:dataSource/users/:id?', {
            title         : 'APP.NAME',
            bodyClassName : 'manage',
            templateUrl   : 'app/manage/templates/manageUser.html',
            controller    : 'manageUserController',
            resolve       : { updateCurrentToken: updateCurrentToken }
        })

        // Client view
        .when('/client/:id/:params?', {
            bodyClassName : 'client',
            templateUrl   : 'app/client/templates/client.html',
            controller    : 'clientController',
            resolve       : { updateCurrentToken: updateCurrentToken }
        })

        // Redirect to home screen if page not found
        .otherwise({
            resolve : { routeToUserHomePage: routeToUserHomePage }
        });

}]);
