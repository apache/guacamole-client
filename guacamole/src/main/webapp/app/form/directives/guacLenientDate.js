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
 * A directive which modifies the parsing and formatting of ngModel when used
 * on an HTML5 date input field, relaxing the otherwise strict parsing and
 * validation behavior. The behavior of this directive for other input elements
 * is undefined.
 */
angular.module('form').directive('guacLenientDate', ['$injector',
    function guacLenientDate($injector) {

    // Required services
    var $filter = $injector.get('$filter');

    /**
     * Directive configuration object.
     *
     * @type Object.<String, Object>
     */
    var config = {
        restrict : 'A',
        require  : 'ngModel'
    };

    // Linking function
    config.link = function linkGuacLenientDate($scope, $element, $attrs, ngModel) {

        // Parse date strings leniently
        ngModel.$parsers = [function parse(viewValue) {

            // If blank, return null
            if (!viewValue)
                return null;

            // Match basic date pattern
            var match = /([0-9]*)(?:-([0-9]*)(?:-([0-9]*))?)?/.exec(viewValue);
            if (!match)
                return null;

            // Determine year, month, and day based on pattern
            var year  = parseInt(match[1] || '0') || new Date().getFullYear();
            var month = parseInt(match[2] || '0') || 1;
            var day   = parseInt(match[3] || '0') || 1;

            // Convert to Date object
            var parsedDate = new Date(Date.UTC(year, month - 1, day));
            if (isNaN(parsedDate.getTime()))
                return null;

            return parsedDate;

        }];

        // Format date strings as "yyyy-MM-dd"
        ngModel.$formatters = [function format(modelValue) {
            return modelValue ? $filter('date')(modelValue, 'yyyy-MM-dd', 'UTC') : '';
        }];

    };

    return config;

}]);
