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
         * @type Field[]
         */
        form : '=',

        /**
         * A map of all field name/value pairs that have already been provided.
         * If not null, the user will be prompted to continue their login
         * attempt using only the fields which remain.
         */
        values : '='

    };

    // Controller for login directive
    directive.controller = ['$scope', '$injector',
        function loginController($scope, $injector) {
        
        // Required types
        var Error = $injector.get('Error');
        var Field = $injector.get('Field');

        // Required services
        var $route                = $injector.get('$route');
        var authenticationService = $injector.get('authenticationService');

        /**
         * An action to be provided along with the object assigned to
         * $scope.loginStatus which closes the currently-shown status dialog.
         */
        var ACKNOWLEDGE_ACTION = {
            name        : "LOGIN.ACTION_ACKNOWLEDGE",
            // Handle action
            callback    : function acknowledgeCallback() {
                $scope.loginStatus = false;
            }
        };

        /**
         * The currently-visible notification describing login status, or false
         * if no notification should be shown.
         *
         * @type Notification|Boolean|Object
         */
        $scope.loginStatus = false;

        /**
         * Whether an error occurred during login.
         * 
         * @type Boolean
         */
        $scope.loginError = false;

        /**
         * All form values entered by the user, as parameter name/value pairs.
         *
         * @type Object.<String, String>
         */
        $scope.enteredValues = {};

        /**
         * All form fields which have not yet been filled by the user.
         *
         * @type Field[]
         */
        $scope.remainingFields = [];

        $scope.$watch('values', function resetEnteredValues(values) {
            angular.extend($scope.enteredValues, values || {});
        });

        $scope.$watch('form', function resetRemainingFields(fields) {

            // If no fields are provided, then no fields remain
            if (!fields) {
                $scope.remainingFields = [];
                return;
            }

            // Filter provided fields against provided values
            $scope.remainingFields = fields.filter(function isRemaining(field) {
                return !(field.name in $scope.values);
            });

        });

        /**
         * Submits the currently-specified username and password to the
         * authentication service, redirecting to the main view if successful.
         */
        $scope.login = function login() {

            // Start with cleared status
            $scope.loginError  = false;
            $scope.loginStatus = false;

            // Attempt login once existing session is destroyed
            authenticationService.authenticate($scope.enteredValues)

            // Clear and reload upon success
            .then(function loginSuccessful() {
                $scope.enteredValues = {};
                $route.reload();
            })

            // Reset upon failure
            ['catch'](function loginFailed(error) {

                // Clear out passwords if the credentials were rejected for any reason
                if (error.type !== Error.Type.INSUFFICIENT_CREDENTIALS) {

                    // Flag generic error for invalid login
                    if (error.type === Error.Type.INVALID_CREDENTIALS)
                        $scope.loginError = true;

                    // Display error if anything else goes wrong
                    else
                        $scope.loginStatus = {
                            'className' : 'error',
                            'title'     : 'LOGIN.DIALOG_HEADER_ERROR',
                            'text'      : error.message,
                            'actions'   : [ ACKNOWLEDGE_ACTION ]
                        };

                    // Clear all visible password fields
                    angular.forEach($scope.remainingFields, function clearEnteredValueIfPassword(field) {

                        // Remove entered value only if field is a password field
                        if (field.type === Field.Type.PASSWORD)
                            delete $scope.enteredValues[field.name];

                    });
                }

            });

        };

    }];

    return directive;

}]);
