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
 * A directive which provides an arbitrary menu-style container. The contents
 * of the directive are displayed only when the menu is open.
 */
angular.module('navigation').directive('guacMenu', [function guacMenu() {

    return {
        restrict: 'E',
        transclude: true,
        replace: true,
        scope: {

            /**
             * The string which should be rendered as the menu title.
             *
             * @type String
             */
            menuTitle : '='

        },

        templateUrl: 'app/navigation/templates/guacMenu.html',
        controller: ['$scope', '$injector', '$element',
            function guacMenuController($scope, $injector, $element) {

            // Get required services
            var $document = $injector.get('$document');

            /**
             * The outermost element of the guacMenu directive.
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
             * Whether the contents of the menu are currently shown.
             *
             * @type Boolean
             */
            $scope.menuShown = false;

            /**
             * Toggles visibility of the menu contents.
             */
            $scope.toggleMenu = function toggleMenu() {
                $scope.menuShown = !$scope.menuShown;
            };

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
