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
 * Controller for time fields.
 */
angular.module('form').controller('timeFieldController', ['$scope', '$injector',
    function timeFieldController($scope, $injector) {

    // Required services
    var $filter = $injector.get('$filter');

    /**
     * Options which dictate the behavior of the input field model, as defined
     * by https://docs.angularjs.org/api/ng/directive/ngModelOptions
     *
     * @type Object.<String, String>
     */
    $scope.modelOptions = {

        /**
         * The time zone to use when reading/writing the Date object of the
         * model.
         *
         * @type String
         */
        timezone : 'UTC'

    };

    /**
     * Parses the time components of the given string into a Date with only the
     * time components set. The resulting Date will be in the UTC timezone,
     * with the date left as 1970-01-01. The input string must be in the format
     * HH:MM:SS (zero-padded, 24-hour).
     *
     * @param {String} str
     *     The time string to parse.
     *
     * @returns {Date}
     *     A Date object, in the UTC timezone, with only the time components
     *     set.
     */
    var parseTime = function parseTime(str) {
        return new Date('1970-01-01T' + str + 'Z');
    };

    // Update typed value when model is changed
    $scope.$watch('model', function modelChanged(model) {
        $scope.typedValue = (model ? parseTime(model) : null);
    });

    // Update string value in model when typed value is changed
    $scope.$watch('typedValue', function typedValueChanged(typedValue) {
        $scope.model = (typedValue ? $filter('date')(typedValue, 'HH:mm:ss', 'UTC') : '');
    });

}]);
