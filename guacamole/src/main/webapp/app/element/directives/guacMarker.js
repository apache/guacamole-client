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
 * A directive which stores a marker which refers to a specific element,
 * allowing that element to be scrolled into view when desired.
 */
angular.module('element').directive('guacMarker', ['$injector', function guacMarker($injector) {

    // Required types
    var Marker = $injector.get('Marker');

    // Required services
    var $parse = $injector.get('$parse');

    return {
        restrict: 'A',

        link: function linkGuacMarker($scope, $element, $attrs) {

            /**
             * The property in which a new Marker should be stored. The new
             * Marker will refer to the element associated with this directive.
             *
             * @type Marker
             */
            var guacMarker = $parse($attrs.guacMarker);

            /**
             * The element to associate with the new Marker.
             *
             * @type Element
             */
            var element = $element[0];

            // Assign new marker
            guacMarker.assign($scope, new Marker(element));

        }

    };

}]);
