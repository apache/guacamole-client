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
 * A directive which provides a filtering text input field which automatically
 * produces a filtered subset of the elements of some given array.
 */
angular.module('list').directive('guacFilter', [function guacFilter() {

    return {
        restrict: 'E',
        replace: true,
        scope: {

            /**
             * The property to which a subset of the provided array will be
             * assigned.
             *
             * @type Array
             */
            filteredItems : '=',

            /**
             * The placeholder text to display within the filter input field
             * when no filter has been provided.
             * 
             * @type String
             */
            placeholder : '&',

            /**
             * An array objects to filter. A subset of this array will be
             * exposed as filteredItems.
             *
             * @type Array
             */
            items : '&'

        },

        templateUrl: 'app/list/templates/guacFilter.html',
        controller: ['$scope', '$injector', function guacFilterController($scope, $injector) {

            // Required types
            var FilterPattern = $injector.get('FilterPattern');

            /**
             * The pattern object to use when filtering items.
             *
             * @type FilterPattern
             */
            var filterPattern = new FilterPattern();

            /**
             * The filter search string to use to restrict the displayed items.
             *
             * @type String
             */
            $scope.searchString = null;

            /**
             * Applies the current filter predicate, filtering all provided
             * items and storing the result in filteredItems.
             */
            var updateFilteredItems = function updateFilteredItems() {

                var items = $scope.items();
                if (items)
                    $scope.filteredItems = items.filter(filterPattern.predicate);
                else
                    $scope.filteredItems = [];

            };

            // Recompile and refilter when pattern is changed
            $scope.$watch('searchString', function searchStringChanged(searchString) {
                filterPattern.compile(searchString);
                updateFilteredItems();
            });

            // Refilter when items change
            $scope.$watchCollection($scope.items, function itemsChanged() {
                updateFilteredItems();
            });

        }]

    };
}]);
