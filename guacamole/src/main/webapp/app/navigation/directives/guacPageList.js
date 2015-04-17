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
 * A directive which provides a list of links to specific pages.
 */
angular.module('navigation').directive('guacPageList', [function guacPageList() {

    return {
        restrict: 'E',
        replace: true,
        scope: {

            /**
             * The array of pages to display.
             *
             * @type Page[]
             */
            pages : '='

        },

        templateUrl: 'app/navigation/templates/guacPageList.html',
        controller: ['$scope', '$injector', function guacPageListController($scope, $injector) {

            // Get required services
            var $location = $injector.get('$location');

            /**
             * Navigate to the given page.
             * 
             * @param {Page} page
             *     The page to navigate to.
             */
            $scope.navigateToPage = function navigateToPage(page) {
                $location.path(page.url);
            };
            
            /**
             * Tests whether the given page is the page currently being viewed.
             *
             * @param {Page} page
             *     The page to test.
             *
             * @returns {Boolean}
             *     true if the given page is the current page, false otherwise.
             */
            $scope.isCurrentPage = function isCurrentPage(page) {
                return $location.url() === page.url;
            };

        }] // end controller

    };
}]);
