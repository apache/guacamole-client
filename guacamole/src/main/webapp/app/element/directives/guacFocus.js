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
 * A directive which allows elements to be manually focused / blurred.
 */
angular.module('element').directive('guacFocus', ['$parse', function guacFocus($parse) {

    return {
        restrict: 'A',

        link: function linkGuacFocus($scope, $element, $attrs) {

            /**
             * Whether the element associated with this directive should be
             * focussed.
             *
             * @type Boolean
             */
            var guacFocus = $parse($attrs.guacFocus);

            /**
             * The element which will register the drag gesture.
             *
             * @type Element
             */
            var element = $element[0];

            // Set/unset focus depending on value of guacFocus
            $scope.$watch(guacFocus, function updateFocus(value) {
                $scope.$evalAsync(function updateFocusAsync() {
                    if (value)
                        element.focus();
                    else
                        element.blur();
                });
            });

            // Set focus flag when focus is received
            element.addEventListener('focus', function focusReceived() {
                $scope.$evalAsync(function setGuacFocusAsync() {
                    guacFocus.assign($scope, true);
                });
            });

            // Unset focus flag when focus is lost
            element.addEventListener('blur', function focusLost() {
                $scope.$evalAsync(function unsetGuacFocusAsync() {
                    guacFocus.assign($scope, false);
                });
            });

        } // end guacFocus link function

    };

}]);
