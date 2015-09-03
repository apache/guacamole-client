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
 * The controller for the general settings page.
 */
angular.module('manage').controller('settingsController', ['$scope', '$injector', 
        function settingsController($scope, $injector) {

    // Required services
    var $routeParams    = $injector.get('$routeParams');
    var userPageService = $injector.get('userPageService');

    /**
     * The array of settings pages available to the current user, or null if
     * not yet known.
     *
     * @type Page[]
     */
    $scope.settingsPages = null;

    /**
     * The currently-selected settings tab. This may be 'users', 'connections',
     * or 'sessions'.
     *
     * @type String
     */
    $scope.activeTab = $routeParams.tab;

    // Retrieve settings pages
    userPageService.getSettingsPages()
    .then(function settingsPagesRetrieved(pages) {
        $scope.settingsPages = pages;
    });

}]);
