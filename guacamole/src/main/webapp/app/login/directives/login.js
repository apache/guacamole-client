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
 * A directive for displaying an arbitrary login form.
 */
angular.module('login').directive('guacLogin', [function guacLogin() {

    // Login directive
    var directive = {
        restrict    : 'E',
        replace     : true,
        templateUrl : 'app/login/templates/login.html'
    };

    // Login directive scope
    directive.scope = {

        /**
         * The login form or set of fields. This will be displayed to the user
         * to capture their credentials.
         *
         * @type Form[]|Form|Field[]|Field 
         */
        form : '='

    };

    // Controller for login directive
    directive.controller = ['$scope', '$injector',
        function loginController($scope, $injector) {
        
        // Required services
        var $route                = $injector.get('$route');
        var authenticationService = $injector.get('authenticationService');

        /**
         * Whether an error occurred during login.
         * 
         * @type Boolean
         */
        $scope.loginError = false;

        /**
         * All form values entered by the user.
         */
        $scope.values = {};

        /**
         * Submits the currently-specified username and password to the
         * authentication service, redirecting to the main view if successful.
         */
        $scope.login = function login() {

            // Attempt login once existing session is destroyed
            authenticationService.authenticate($scope.values)

            // Clear and reload upon success
            .then(function loginSuccessful() {
                $scope.loginError = false;
                $scope.values = {};
                $route.reload();
            })

            // Reset upon failure
            ['catch'](function loginFailed() {
                $scope.loginError = true;
                $scope.values.password = '';
            });

        };

    }];

    return directive;

}]);
