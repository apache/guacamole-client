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
 * A directive which displays the Guacamole on-screen keyboard.
 */
angular.module('osk').directive('guacOsk', [function guacOsk() {

    return {
        restrict: 'E',
        replace: true,
        scope: {

            /**
             * The URL for the Guacamole on-screen keyboard layout to use.
             *
             * @type String
             */
            layout : '='

        },

        templateUrl: 'app/osk/templates/guacOsk.html',
        controller: ['$scope', '$rootScope', '$window', '$element',
            function guacOsk($scope, $rootScope, $window, $element) {

            /**
             * The current on-screen keyboard, if any.
             *
             * @type Guacamole.OnScreenKeyboard
             */
            var keyboard = null;

            /**
             * The main containing element for the entire directive.
             * 
             * @type Element
             */
            var main = $element[0];

            /**
             * Event listener which resizes the current keyboard, if any, such
             * that it fits within available space.
             */
            var resizeListener = function resizeListener() {

                // Resize keyboard, if defined
                if (keyboard)
                    keyboard.resize(main.offsetWidth);

            };

            // Set layout whenever URL changes
            $scope.$watch("layout", function setLayout(layout) {

                // Remove current keyboard
                keyboard = null;
                main.innerHTML = "";

                // Load new keyboard
                if (layout) {

                    // Add OSK element
                    keyboard = new Guacamole.OnScreenKeyboard(layout);
                    main.appendChild(keyboard.getElement());

                    // Init size
                    keyboard.resize(main.offsetWidth);

                    // Broadcast keydown for each key pressed
                    keyboard.onkeydown = function(keysym) {
                        $rootScope.$broadcast('guacSyntheticKeydown', keysym);
                    };
                    
                    // Broadcast keydown for each key released 
                    keyboard.onkeyup = function(keysym) {
                        $rootScope.$broadcast('guacSyntheticKeyup', keysym);
                    };

                    // Resize keyboard whenever window changes size
                    $window.addEventListener('resize', resizeListener);

                }

            }); // end layout scope watch

            // Clean up event listeners upon destroy
            $scope.$on('$destroy', function destroyKeyboard() {
                $window.removeEventListener('resize', resizeListener);
            });

        }]

    };
}]);
