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
 * A directive which allows handling of panning gestures on a particular
 * element.
 */
angular.module('touch').directive('guacPan', [function guacPan() {

    return {
        restrict: 'A',
        scope: {

            /**
             * Called as during a panning gesture as the user's finger is
             * placed upon the element, moves, and is lifted from the element.
             *
             * @event
             * @param {Boolean} inProgress
             *     Whether the gesture is currently in progress. This will
             *     always be true except when the gesture has ended, at which
             *     point one final call will occur with this parameter set to
             *     false.
             *
             * @param {Number} startX
             *     The X location at which the panning gesture began.
             *     
             * @param {Number} startY
             *     The Y location at which the panning gesture began.
             *     
             * @param {Number} currentX
             *     The current X location of the user's finger.
             *     
             * @param {Number} currentY
             *     The current Y location of the user's finger.
             *     
             * @param {Number} deltaX
             *     The difference in X location relative to the start of the
             *     gesture.
             * 
             * @param {Number} deltaY
             *     The difference in Y location relative to the start of the
             *     gesture.
             */
            guacPan : '='

        },

        link: function guacPan($scope, $element) {

            /**
             * The element which will register the panning gesture.
             *
             * @type Element
             */
            var element = $element[0];

            /**
             * Whether a drag gesture is in progress.
             * 
             * @type Boolean
             */
            var inProgress = false;
            
            /**
             * The starting X location of the drag gesture.
             * 
             * @type Number
             */
            var startX = null;

            /**
             * The starting Y location of the drag gesture.
             * 
             * @type Number
             */
            var startY = null;

            /**
             * The current X location of the drag gesture.
             * 
             * @type Number
             */
            var currentX = null;

            /**
             * The current Y location of the drag gesture.
             * 
             * @type Number
             */
            var currentY = null;

            /**
             * The change in X relative to drag start.
             * 
             * @type Number
             */
            var deltaX = 0;

            /**
             * The change in X relative to drag start.
             * 
             * @type Number
             */
            var deltaY = 0;

            // When there is exactly one touch, monitor the change in location
            element.addEventListener("touchmove", function(e) {
                if (e.touches.length === 1) {

                    e.preventDefault();
                    e.stopPropagation();

                    // Get touch location
                    var x = e.touches[0].clientX;
                    var y = e.touches[0].clientY;

                    // Init start location and deltas if gesture is starting
                    if (!startX || !startY) {
                        startX = currentX = x;
                        startY = currentY = y;
                        deltaX = 0;
                        deltaY = 0;
                        inProgress = true;
                    }

                    // Update deltas if gesture is in progress
                    else if (inProgress) {
                        deltaX = x - currentX;
                        deltaY = y - currentY;
                        currentX = x;
                        currentY = y;
                    }

                    // Signal start/change in panning gesture
                    if (inProgress && $scope.guacPan) {
                        $scope.$apply(function panChanged() {
                            $scope.guacPan(true, startX, startY, currentX, currentY, deltaX, deltaY);
                        });
                    }

                }
            }, false);

            // Reset monitoring and fire end event when done
            element.addEventListener("touchend", function(e) {

                if (startX && startY && e.touches.length === 0) {

                    e.preventDefault();
                    e.stopPropagation();

                    // Signal end of panning gesture
                    if (inProgress && $scope.guacPan) {
                        $scope.$apply(function panComplete() {
                            $scope.guacPan(true, startX, startY, currentX, currentY, deltaX, deltaY);
                        });
                    }

                    startX = currentX = null;
                    startY = currentY = null;
                    deltaX = 0;
                    deltaY = 0;
                    inProgress = false;

                }

            }, false);

        }

    };
}]);
