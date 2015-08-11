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
 * on an HTML5 time input field, relaxing the otherwise strict parsing and
 * validation behavior. The behavior of this directive for other input elements
 * is undefined.
 */
angular.module('form').directive('guacLenientTime', ['$injector',
    function guacLenientTime($injector) {

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
    config.link = function linkGuacLenientTIme($scope, $element, $attrs, ngModel) {

        // Parse time strings leniently
        ngModel.$parsers = [function parse(viewValue) {

            // If blank, return null
            if (!viewValue)
                return null;

            // Match basic time pattern
            var match = /([0-9]*)(?::([0-9]*)(?::([0-9]*))?)?(?:\s*(a|p))?/.exec(viewValue.toLowerCase());
            if (!match)
                return null;

            // Determine hour, minute, and second based on pattern
            var hour   = parseInt(match[1] || '0');
            var minute = parseInt(match[2] || '0');
            var second = parseInt(match[3] || '0');

            // Handle AM/PM
            if (match[4]) {

                // Interpret 12 AM as 00:00 and 12 PM as 12:00
                if (hour === 12)
                    hour = 0;

                // Increment hour to evening if PM
                if (match[4] === 'p')
                    hour += 12;

            }

            // Wrap seconds and minutes into minutes and hours
            minute += second / 60; second %= 60;
            hour   += minute / 60; minute %= 60;

            // Constrain hours to 0 - 23
            hour %= 24;

            // Convert to Date object
            var parsedDate = new Date(Date.UTC(1970, 0, 1, hour, minute, second));
            if (isNaN(parsedDate.getTime()))
                return null;

            return parsedDate;

        }];

        // Format time strings as "HH:mm:ss"
        ngModel.$formatters = [function format(modelValue) {
            return modelValue ? $filter('date')(modelValue, 'HH:mm:ss', 'UTC') : '';
        }];

    };

    return config;

}]);
