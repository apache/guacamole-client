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
    
    $routeProvider.
        when('/', {
            title: 'application.title',
            templateUrl: 'app/home/templates/home.html',
            controller: 'homeController'
        }).
        when('/manage/', {
            title: 'application.title',
            templateUrl: 'app/manage/templates/manage.html',
            controller: 'manageController'
        }).
        when('/login/', {
            title: 'application.title',
            templateUrl: 'app/login/templates/login.html',
            controller: 'loginController'
        }).
        when('/client/:type/:id/:params?', {
            templateUrl: 'app/client/templates/client.html',
            controller: 'clientController'
        }).
        otherwise({
            redirectTo: '/'
        });
}]);


