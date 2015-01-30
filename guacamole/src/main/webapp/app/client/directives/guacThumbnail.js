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
 * A directive for displaying a Guacamole client as a non-interactive
 * thumbnail.
 */
angular.module('client').directive('guacThumbnail', [function guacThumbnail() {

    return {
        // Element only
        restrict: 'E',
        replace: true,
        scope: {

            /**
             * The client to display within this guacThumbnail directive.
             * 
             * @type ManagedClient
             */
            client : '='
            
        },
        templateUrl: 'app/client/templates/guacThumbnail.html',
        controller: ['$scope', '$injector', '$element', function guacThumbnailController($scope, $injector, $element) {
   
            // Required services
            var $window = $injector.get('$window');

            /**
             * The optimal thumbnail width, in pixels.
             *
             * @type Number
             */
            var THUMBNAIL_WIDTH = 320;

            /**
             * The optimal thumbnail height, in pixels.
             *
             * @type Number
             */
            var THUMBNAIL_HEIGHT = 240;
                
            /**
             * The display of the current Guacamole client instance.
             * 
             * @type Guacamole.Display
             */
            var display = null;

            /**
             * The element associated with the display of the current
             * Guacamole client instance.
             *
             * @type Element
             */
            var displayElement = null;

            /**
             * The element which must contain the Guacamole display element.
             *
             * @type Element
             */
            var displayContainer = $element.find('.display')[0];

            /**
             * The main containing element for the entire directive.
             * 
             * @type Element
             */
            var main = $element[0];

            /**
             * The element which functions as a detector for size changes.
             * 
             * @type Element
             */
            var resizeSensor = $element.find('.resize-sensor')[0];

            /**
             * Updates the scale of the attached Guacamole.Client based on current window
             * size and "auto-fit" setting.
             */
            var updateDisplayScale = function updateDisplayScale() {

                if (!display) return;

                // Fit within available area
                display.scale(Math.min(
                    main.offsetWidth  / Math.max(display.getWidth(),  1),
                    main.offsetHeight / Math.max(display.getHeight(), 1)
                ));

            };

            // Attach any given managed client
            $scope.$watch('client', function attachManagedClient(managedClient) {

                // Remove any existing display
                displayContainer.innerHTML = "";

                // Only proceed if a client is given 
                if (!managedClient)
                    return;

                // Get Guacamole client instance
                var client = managedClient.client;

                // Attach possibly new display
                display = client.getDisplay();

                // Add display element
                displayElement = display.getElement();
                displayContainer.appendChild(displayElement);

            });

            // Update scale when display is resized
            $scope.$watch('client.managedDisplay.size', function setDisplaySize(size) {

                var width;
                var height;

                // If no display size yet, assume optimal thumbnail size
                if (!size || size.width === 0 || size.height === 0) {
                    width  = THUMBNAIL_WIDTH;
                    height = THUMBNAIL_HEIGHT;
                }

                // Otherwise, generate size that fits within thumbnail bounds
                else {
                    var scale = Math.min(THUMBNAIL_WIDTH / size.width, THUMBNAIL_HEIGHT / size.height, 1);
                    width  = size.width  * scale;
                    height = size.height * scale;
                }
                
                // Generate dummy background image
                var thumbnail = document.createElement("canvas");
                thumbnail.width  = width;
                thumbnail.height = height;
                $scope.thumbnail = thumbnail.toDataURL("image/png");

                $scope.$evalAsync(updateDisplayScale);

            });

            // If the element is resized, attempt to resize client
            resizeSensor.contentDocument.defaultView.addEventListener('resize', function mainElementResized() {
                $scope.$apply(updateDisplayScale);
            });

        }]
    };
}]);