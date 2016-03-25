/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * A directive which provides a fullscreen environment for its content.
 */
angular.module('client').directive('guacViewport', [function guacViewport() {

    return {
        // Element only
        restrict: 'E',
        scope: {},
        transclude: true,
        templateUrl: 'app/client/templates/guacViewport.html',
        controller: ['$scope', '$injector', '$element',
            function guacViewportController($scope, $injector, $element) {

            // Required services
            var $window   = $injector.get('$window');
            var $document = $injector.get('$document');

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

            /**
             * Resizes the container element inside the guacViewport such that
             * it exactly fits within the visible area, even if the browser has
             * been scrolled.
             */
            var fitVisibleArea = function fitVisibleArea() {

                // Pull scroll properties
                var scrollLeft   = document.body.scrollLeft;
                var scrollTop    = document.body.scrollTop;
                var scrollWidth  = document.body.scrollWidth;
                var scrollHeight = document.body.scrollHeight;

                // Calculate new height
                var adjustedHeight = scrollHeight - scrollTop;

                // Only update if not in response to our own call to scrollTo()
                if (scrollLeft !== scrollWidth && scrollTop !== scrollHeight
                        && currentAdjustedHeight !== adjustedHeight) {

                    // Adjust element to fit exactly within visible area
                    element.style.height = adjustedHeight + 'px';
                    currentAdjustedHeight = adjustedHeight;

                    // Scroll to bottom
                    $window.scrollTo(scrollWidth, scrollHeight);

                }

                // Manually attempt scroll if height has not been adjusted
                else if (adjustedHeight === 0)
                    $window.scrollTo(scrollWidth, scrollHeight);

            };

            // Fit container within visible region when window scrolls
            $window.addEventListener('scroll', fitVisibleArea);

            // Poll every 10ms, in case scroll event does not fire
            var pollArea = $window.setInterval(fitVisibleArea, 10);

            // Clean up on destruction
            $scope.$on('$destroy', function destroyViewport() {
                $window.removeEventListener('scroll', fitVisibleArea);
                $window.clearInterval(pollArea);
            });

        }]
    };
}]);
