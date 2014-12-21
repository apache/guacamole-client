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
 * A directive which displays a button that controls the pressed state of a
 * single keyboard key.
 */
angular.module('textInput').directive('guacKey', [function guacKey() {

    return {
        restrict: 'E',
        replace: true,
        scope: {

            /**
             * The text to display within the key. This will be run through the
             * translation filter prior to display.
             * 
             * @type String
             */
            text    : '=',

            /**
             * The keysym to send within keyup and keydown events when this key
             * is pressed or released.
             * 
             * @type Number
             */
            keysym  : '=',

            /**
             * Whether this key is sticky. Sticky keys toggle their pressed
             * state with each click.
             * 
             * @type Boolean
             * @default false
             */
            sticky  : '=?',

            /**
             * Whether this key is currently pressed.
             * 
             * @type Boolean
             * @default false
             */
            pressed : '=?'

        },

        templateUrl: 'app/textInput/templates/guacKey.html',
        controller: ['$scope', '$rootScope',
            function guacKey($scope, $rootScope) {

            // Not sticky by default
            $scope.sticky = $scope.sticky || false;

            // Unpressed by default
            $scope.pressed = $scope.pressed || false;

            /**
             * Presses and releases this key, sending the corresponding keydown
             * and keyup events. In the case of sticky keys, the pressed state
             * is toggled, and only a single keydown/keyup event will be sent,
             * depending on the current state.
             */
            $scope.updateKey = function updateKey() {

                // If sticky, toggle pressed state
                if ($scope.sticky)
                    $scope.pressed = !$scope.pressed;

                // For all non-sticky keys, press and release key immediately
                else {
                    $rootScope.$broadcast('guacSyntheticKeydown', $scope.keysym);
                    $rootScope.$broadcast('guacSyntheticKeyup', $scope.keysym);
                }

            };

            // Send keyup/keydown when pressed state is altered
            $scope.$watch('pressed', function updatePressedState(isPressed, wasPressed) {

                // If the key is pressed now, send keydown
                if (isPressed)
                    $rootScope.$broadcast('guacSyntheticKeydown', $scope.keysym);

                // If the key was pressed, but is not pressed any longer, send keyup
                else if (wasPressed)
                    $rootScope.$broadcast('guacSyntheticKeyup', $scope.keysym);

            });

        }]

    };
}]);
