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
 * A directive for the guacamole client.
 */
angular.module('notification').directive('guacNotification', [function guacNotification() {

    return {
        restrict: 'E',
        replace: true,
        scope: {

            /**
             * The CSS class to apply to the notification.
             *
             * @type String
             */
            className : '=',

            /**
             * The title of the notification.
             *
             * @type String
             */
            title : '=',

            /**
             * The body text of the notification.
             *
             * @type String
             */
            text : '=',

            /**
             * The text to use for displaying the countdown. For the sake of
             * i18n, the variable REMAINING will be applied within the
             * translation string for formatting plurals, etc.
             *
             * @type String
             * @example
             * "Only {REMAINING} {REMAINING, plural, one{second} other{seconds}} remain."
             */
            countdownText : '=',

            /**
             * The number of seconds to wait before automatically calling the
             * default callback.
             *
             * @type Number
             */
            countdown : '=',

            /**
             * The function to call when timeRemaining expires. If timeRemaining
             * is not set, this does not apply.
             *
             * @type Function
             */
            defaultCallback : '=',

            /**
             * The text to use for displaying the progress. For the sake of
             * i18n, the variable PROGRESS will be applied within the
             * translation string for formatting plurals, etc., while the
             * variable UNIT will be applied, if needed, for whatever units
             * are applicable to the progress display.
             *
             * @type String
             * @example
             * "{PROGRESS} {UNIT, select, b{B} kb{KB}} uploaded."
             */
            progressText : '=',

            /**
             * The unit which applies to the progress indicator, if any. This
             * will be substituted in the progressText string with the UNIT
             * variable.
             *
             * @type String
             */
            progressUnit : '=',

            /**
             * Arbitrary value denoting how much progress has been made
             * in some ongoing task that this notification represents.
             *
             * @type Number
             */
            progress : '=',

            /**
             * Array of name/callback pairs for each action the user is allowed
             * to take once the notification is shown.
             *
             * @type Array
             * @example
             * [
             *     {
             *         name     : "Action 1 name",
             *         callback : actionCallback1
             *     },
             *     {
             *         name     : "Action 2 text",
             *         callback : actionCallback2
             *     }
             * ]
             */
            actions : '='

        },

        templateUrl: 'app/notification/templates/guacNotification.html',
        controller: ['$scope', '$interval', function guacNotificationController($scope, $interval) {

            // Set countdown interval when associated property is set
            $scope.$watch("countdown", function resetTimeRemaining(countdown) {

                $scope.timeRemaining = countdown;

                // Clean up any existing interval
                if ($scope.interval)
                    $interval.cancel($scope.interval);

                // Update and handle countdown, if provided
                if ($scope.timeRemaining) {

                    $scope.interval = $interval(function updateTimeRemaining() {

                        // Update time remaining
                        $scope.timeRemaining--;

                        // Call countdown callback when time remaining expires
                        if ($scope.timeRemaining === 0 && $scope.defaultCallback)
                            $scope.defaultCallback();

                    }, 1000, $scope.timeRemaining);

                }

            });

            // Clean up interval upon destruction
            $scope.$on("$destroy", function destroyNotification() {

                if ($scope.interval)
                    $interval.cancel($scope.interval);

            });

        }]

    };
}]);