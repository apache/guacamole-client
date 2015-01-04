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
             * The notification to display.
             *
             * @type Notification|Object 
             */
            notification : '='

        },

        templateUrl: 'app/notification/templates/guacNotification.html',
        controller: ['$scope', '$interval', function guacNotificationController($scope, $interval) {

            // Update progress bar if end known
            $scope.$watch("notification.progress.ratio", function updateProgress(ratio) {
                $scope.progressPercent = ratio * 100;
            });

            $scope.$watch("notification", function resetTimeRemaining(notification) {

                var countdown = notification.countdown;

                // Clean up any existing interval
                if ($scope.interval)
                    $interval.cancel($scope.interval);

                // Update and handle countdown, if provided
                if (countdown) {

                    $scope.timeRemaining = countdown.remaining;

                    $scope.interval = $interval(function updateTimeRemaining() {

                        // Update time remaining
                        $scope.timeRemaining--;

                        // Call countdown callback when time remaining expires
                        if ($scope.timeRemaining === 0 && countdown.callback)
                            countdown.callback();

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
