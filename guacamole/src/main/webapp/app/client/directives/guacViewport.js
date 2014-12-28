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
 * A directive which provides a fullscreen environment for its content.
 */
angular.module('client').directive('guacViewport', [function guacViewport() {

    return {
        // Element only
        restrict: 'E',
        scope: false,
        transclude: true,
        templateUrl: 'app/client/templates/guacViewport.html',
        controller: ['$window', '$document', '$element',
            function guacViewportController($window, $document, $element) {

            /**
             * The fullscreen container element.
             *
             * @type Element
             */
            var element = $element.find('.viewport')[0];

            /**
             * The main document object.
             *
             * @type Document
             */
            var document = $document[0];

            /**
             * The current adjusted height of the viewport element, if any.
             *
             * @type Number
             */
            var currentAdjustedHeight = null;

            // Fit container within visible region when window scrolls
            $window.onscroll = function fitScrollArea() {

                // Pull scroll properties
                var scrollLeft   = document.body.scrollLeft;
                var scrollTop    = document.body.scrollTop;
                var scrollWidth  = document.body.scrollWidth;
                var scrollHeight = document.body.scrollHeight;

                // Calculate new height
                var adjustedHeight = $window.innerHeight - scrollTop;

                // Only update if not in response to our own call to scrollTo()
                if (scrollLeft !== scrollWidth && scrollTop !== scrollHeight
                        && currentAdjustedHeight !== adjustedHeight) {

                    // Adjust element to fit exactly within visible area
                    element.style.height = adjustedHeight + 'px';
                    currentAdjustedHeight = adjustedHeight;

                    // Scroll to bottom
                    $window.scrollTo(scrollWidth, scrollHeight);

                }

            };

        }]
    };
}]);