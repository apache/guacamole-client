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
 * A directive which calls a given callback when its associated element is
 * resized. This will modify the internal DOM tree of the associated element,
 * and the associated element MUST have position (for example,
 * "position: relative").
 */
angular.module('element').directive('guacResize', ['$document', function guacResize($document) {

    return {
        restrict: 'A',

        link: function linkGuacResize($scope, $element, $attrs) {

            /**
             * The function to call whenever the associated element is
             * resized. The function will be passed the width and height of
             * the element, in pixels.
             *
             * @type Function 
             */
            var guacResize = $scope.$eval($attrs.guacResize);

            /**
             * The element which will monitored for size changes.
             *
             * @type Element
             */
            var element = $element[0];

            /**
             * The resize sensor - an HTML object element.
             *
             * @type HTMLObjectElement
             */
            var resizeSensor = $document[0].createElement('object');

            /**
             * The width of the associated element, in pixels.
             *
             * @type Number
             */
            var lastWidth = element.offsetWidth;

            /**
             * The height of the associated element, in pixels.
             *
             * @type Number
             */
            var lastHeight = element.offsetHeight;

            /**
             * Checks whether the size of the associated element has changed
             * and, if so, calls the resize callback with the new width and
             * height as parameters.
             */
            var checkSize = function checkSize() {

                // Call callback only if size actually changed
                if (element.offsetWidth !== lastWidth
                 || element.offsetHeight !== lastHeight) {

                    // Call resize callback, if defined
                    if (guacResize) {
                        $scope.$apply(function elementSizeChanged() {
                            guacResize(element.offsetWidth, element.offsetHeight);
                        });
                    }

                    // Update stored size
                    lastWidth  = element.offsetWidth;
                    lastHeight = element.offsetHeight;

                 }

            };

            // Register event listener once window object exists
            resizeSensor.onload = function resizeSensorReady() {
                resizeSensor.contentDocument.defaultView.addEventListener('resize', checkSize);
                checkSize();
            };

            // Load blank contents
            resizeSensor.className = 'resize-sensor';
            resizeSensor.type      = 'text/html';
            resizeSensor.data      = 'app/element/templates/blank.html';

            // Add resize sensor to associated element
            element.insertBefore(resizeSensor, element.firstChild);

        } // end guacResize link function

    };

}]);
