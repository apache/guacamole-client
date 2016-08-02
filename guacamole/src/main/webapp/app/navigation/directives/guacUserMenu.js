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
 * A directive which provides a user-oriented menu containing options for
 * navigation and configuration.
 */
angular.module('navigation').directive('guacUserMenu', [function guacUserMenu() {

    return {
        restrict: 'E',
        replace: true,
        scope: {

            /**
             * Optional array of actions which are specific to this particular
             * location, as these actions may not be appropriate for other
             * locations which contain the user menu.
             *
             * @type MenuAction[]
             */
            localActions : '='

        },

        templateUrl: 'app/navigation/templates/guacUserMenu.html',
        controller: ['$scope', '$injector',
            function guacUserMenuController($scope, $injector) {

            // Get required services
            var $location             = $injector.get('$location');
            var $route                = $injector.get('$route');
            var authenticationService = $injector.get('authenticationService');
            var userPageService       = $injector.get('userPageService');

            /**
             * The username of the current user.
             *
             * @type String
             */
            $scope.username = authenticationService.getCurrentUsername();
            
            /**
             * The available main pages for the current user.
             * 
             * @type Page[]
             */
            $scope.pages = null;

            // Retrieve the main pages from the user page service
            userPageService.getMainPages()
            .then(function retrievedMainPages(pages) {
                $scope.pages = pages;
            });
            
            /**
             * Logs out the current user, redirecting them to back to the root
             * after logout completes.
             */
            $scope.logout = function logout() {
                authenticationService.logout()['finally'](function logoutComplete() {
                    if ($location.path() !== '/')
                        $location.url('/');
                    else
                        $route.reload();
                });
            };

            /**
             * Action which logs out the current user, redirecting them to back
             * to the login screen after logout completes.
             */
            var LOGOUT_ACTION = {
                name      : 'USER_MENU.ACTION_LOGOUT',
                className : 'logout',
                callback  : $scope.logout
            };

            /**
             * All available actions for the current user.
             */
            $scope.actions = [ LOGOUT_ACTION ];

        }] // end controller

    };
}]);
