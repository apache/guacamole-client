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
             * @type PageDefinition[]
             */
            pages : '='

        },

        templateUrl: 'app/navigation/templates/guacPageList.html',
        controller: ['$scope', '$injector', function guacPageListController($scope, $injector) {

            // Required types
            var PageDefinition = $injector.get('PageDefinition');

            // Required services
            var $location = $injector.get('$location');

            /**
             * The URL of the currently-displayed page.
             *
             * @type String
             */
            var currentURL = $location.url();

            /**
             * The names associated with the current page, if the current page
             * is known. The value of this property corresponds to the value of
             * PageDefinition.name. Though PageDefinition.name may be a String,
             * this will always be an Array.
             *
             * @type String[]
             */
            var currentPageName = [];

            /**
             * Array of each level of the page list, where a level is defined
             * by a mapping of names (translation strings) to the
             * PageDefinitions corresponding to those names.
             *
             * @type Object.<String, PageDefinition>[]
             */
            $scope.levels = [];

            /**
             * Returns the names associated with the given page, in
             * hierarchical order. If the page is only associated with a single
             * name, and that name is not stored as an array, it will be still
             * be returned as an array containing a single item.
             *
             * @param {PageDefinition} page
             *     The page to return the names of.
             *
             * @return {String[]}
             *     An array of all names associated with the given page, in
             *     hierarchical order.
             */
            var getPageNames = function getPageNames(page) {

                // If already an array, simply return the name
                if (angular.isArray(page.name))
                    return page.name;

                // Otherwise, transform into array
                return [page.name];

            };

            /**
             * Adds the given PageDefinition to the overall set of pages
             * displayed by this guacPageList, automatically updating the
             * available levels ($scope.levels) and the contents of those
             * levels.
             *
             * @param {PageDefinition} page
             *     The PageDefinition to add.
             *
             * @param {Number} weight
             *     The sorting weight to use for the page if it does not
             *     already have an associated weight.
             */
            var addPage = function addPage(page, weight) {

                // Pull all names for page
                var names = getPageNames(page);

                // Copy the hierarchy of this page into the displayed levels
                // as far as is relevant for the currently-displayed page
                for (var i = 0; i < names.length; i++) {

                    // Create current level, if it doesn't yet exist
                    var pages = $scope.levels[i];
                    if (!pages)
                        pages = $scope.levels[i] = {};

                    // Get the name at the current level
                    var name = names[i];

                    // Determine whether this page definition is part of the
                    // hierarchy containing the current page
                    var isCurrentPage = (currentPageName[i] === name);

                    // Store new page if it doesn't yet exist at this level
                    if (!pages[name]) {
                        pages[name] = new PageDefinition({
                            name      : name,
                            url       : isCurrentPage ? currentURL : page.url,
                            className : page.className,
                            weight    : page.weight || (weight + i)
                        });
                    }

                    // If the name at this level no longer matches the
                    // hierarchy of the current page, do not go any deeper
                    if (currentPageName[i] !== name)
                        break;

                }

            };

            /**
             * Navigate to the given page.
             * 
             * @param {PageDefinition} page
             *     The page to navigate to.
             */
            $scope.navigateToPage = function navigateToPage(page) {
                $location.path(page.url);
            };
            
            /**
             * Tests whether the given page is the page currently being viewed.
             *
             * @param {PageDefinition} page
             *     The page to test.
             *
             * @returns {Boolean}
             *     true if the given page is the current page, false otherwise.
             */
            $scope.isCurrentPage = function isCurrentPage(page) {
                return currentURL === page.url;
            };

            /**
             * Given an arbitrary map of PageDefinitions, returns an array of
             * those PageDefinitions, sorted by weight.
             *
             * @param {Object.<*, PageDefinition>} level
             *     A map of PageDefinitions with arbitrary keys. The value of
             *     each key is ignored.
             *
             * @returns {PageDefinition[]}
             *     An array of all PageDefinitions in the given map, sorted by
             *     weight.
             */
            $scope.getPages = function getPages(level) {

                var pages = [];

                // Convert contents of level to a flat array of pages
                angular.forEach(level, function addPageFromLevel(page) {
                    pages.push(page);
                });

                // Sort page array by weight
                pages.sort(function comparePages(a, b) {
                    return a.weight - b.weight;
                });

                return pages;

            };

            // Update page levels whenever pages changes
            $scope.$watch('pages', function setPages(pages) {

                // Determine current page name
                currentPageName = [];
                angular.forEach(pages, function findCurrentPageName(page) {

                    // If page is current page, store its names
                    if ($scope.isCurrentPage(page))
                        currentPageName = getPageNames(page);

                });

                // Reset contents of levels
                $scope.levels = [];

                // Add all page definitions
                angular.forEach(pages, addPage);

                // Filter to only relevant levels
                $scope.levels = $scope.levels.filter(function isRelevant(level) {

                    // Determine relevancy by counting the number of pages
                    var pageCount = 0;
                    for (var name in level) {

                        // Level is relevant if it has two or more pages
                        if (++pageCount === 2)
                            return true;

                    }

                    // Otherwise, the level is not relevant
                    return false;

                });

            });

        }] // end controller

    };
}]);
