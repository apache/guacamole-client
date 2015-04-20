/*
 * Copyright (C) 2015 Glyptodon LLC
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
        controller: ['$scope', '$injector', '$element', function guacUserMenuController($scope, $injector, $element) {

            // Get required services
            var $document             = $injector.get('$document');
            var $location             = $injector.get('$location');
            var authenticationService = $injector.get('authenticationService');
            var userPageService       = $injector.get('userPageService');

            /**
             * The outermost element of the user menu directive.
             *
             * @type Element
             */
            var element = $element[0];

            /**
             * The main document object.
             *
             * @type Document
             */
            var document = $document[0];

            /**
             * Whether the contents of the user menu are currently shown.
             *
             * @type Boolean
             */
            $scope.menuShown = false;

            /**
             * The username of the current user.
             *
             * @type String
             */
            $scope.username = authenticationService.getCurrentUserID();
            
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
             * Toggles visibility of the user menu.
             */
            $scope.toggleMenu = function toggleMenu() {
                $scope.menuShown = !$scope.menuShown;
            };

            /**
             * Logs out the current user, redirecting them to back to the login
             * screen after logout completes.
             */
            $scope.logout = function logout() {
                authenticationService.logout()['finally'](function logoutComplete() {
                    $location.path('/login/');
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

            // Close menu when use clicks anywhere else
            document.body.addEventListener('click', function clickOutsideMenu() {
                $scope.$apply(function closeMenu() {
                    $scope.menuShown = false;
                });
            }, false);

            // Prevent click within menu from triggering the outside-menu handler
            element.addEventListener('click', function clickInsideMenu(e) {
                e.stopPropagation();
            }, false);

        }] // end controller

    };
}]);
