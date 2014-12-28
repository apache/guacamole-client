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
 * A directive which allows elements to be manually scrolled, and for their
 * scroll state to be observed.
 */
angular.module('element').directive('guacScroll', [function guacScroll() {

    return {
        restrict: 'A',

        link: function linkGuacScroll($scope, $element, $attrs) {

            /**
             * The current scroll state of the element.
             *
             * @type ScrollState
             */
            var guacScroll = $scope.$eval($attrs.guacScroll);

            /**
             * The element which is being scrolled, or monitored for changes
             * in scroll.
             *
             * @type Element
             */
            var element = $element[0];

            /**
             * Returns the current left edge of the scrolling rectangle.
             *
             * @returns {Number}
             *     The current left edge of the scrolling rectangle.
             */
            var getScrollLeft = function getScrollLeft() {
                return guacScroll.left;
            };

            /**
             * Returns the current top edge of the scrolling rectangle.
             *
             * @returns {Number}
             *     The current top edge of the scrolling rectangle.
             */
            var getScrollTop = function getScrollTop() {
                return guacScroll.top;
            };

            // Update underlying scrollLeft property when left changes
            $scope.$watch(getScrollLeft, function scrollLeftChanged(left) {
                element.scrollLeft = left;
                guacScroll.left = element.scrollLeft;
            });

            // Update underlying scrollTop property when top changes
            $scope.$watch(getScrollTop, function scrollTopChanged(top) {
                element.scrollTop = top;
                guacScroll.top = element.scrollTop;
            });

        } // end guacScroll link function

    };

}]);
