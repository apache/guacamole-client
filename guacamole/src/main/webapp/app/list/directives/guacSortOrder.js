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
 * Updates the priority of the sorting property given by "guac-sort-property"
 * within the SortOrder object given by "guac-sort-order". The CSS classes
 * "sort-primary" and "sort-descending" will be applied to the associated
 * element depending on the priority and sort direction of the given property.
 * 
 * The associated element will automatically be assigned the "sortable" CSS
 * class.
 */
angular.module('list').directive('guacSortOrder', [function guacFocus() {

    return {
        restrict: 'A',

        link: function linkGuacSortOrder($scope, $element, $attrs) {

            /**
             * The object defining the sorting order.
             *
             * @type SortOrder
             */
            var sortOrder = $scope.$eval($attrs.guacSortOrder);

            /**
             * The name of the property whose priority within the sort order
             * is controlled by this directive.
             *
             * @type String
             */
            var sortProperty = $scope.$eval($attrs.guacSortProperty);

            /**
             * Returns whether the sort property defined via the
             * "guac-sort-property" attribute is the primary sort property of
             * the associated sort order.
             *
             * @returns {Boolean}
             *     true if the sort property defined via the
             *     "guac-sort-property" attribute is the primary sort property,
             *     false otherwise.
             */
            var isPrimary = function isPrimary() {
                return sortOrder.primary === sortProperty;
            };

            /**
             * Returns whether the primary property of the sort order is
             * sorted in descending order.
             *
             * @returns {Boolean}
             *     true if the primary property of the sort order is sorted in
             *     descending order, false otherwise.
             */
            var isDescending = function isDescending() {
                return sortOrder.descending;
            };

            // Assign "sortable" class to associated element
            $element.addClass('sortable');

            // Add/remove "sort-primary" class depending on sort order
            $scope.$watch(isPrimary, function primaryChanged(primary) {
                $element.toggleClass('sort-primary', primary);
            });

            // Add/remove "sort-descending" class depending on sort order
            $scope.$watch(isDescending, function descendingChanged(descending) {
                $element.toggleClass('sort-descending', descending);
            });

            // Update sort order when clicked
            $element[0].addEventListener('click', function clicked() {
                $scope.$evalAsync(function updateSortOrder() {
                    sortOrder.togglePrimary(sortProperty);
                });
            });

        } // end guacSortOrder link function

    };

}]);
