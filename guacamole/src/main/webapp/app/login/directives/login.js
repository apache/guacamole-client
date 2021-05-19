/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
         * An optional instructional message to display within the login
         * dialog.
         *
         * @type TranslatableMessage
         */
        helpText : '=',

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
        var $rootScope            = $injector.get('$rootScope');
        var $route                = $injector.get('$route');
        var authenticationService = $injector.get('authenticationService');
        var requestService        = $injector.get('requestService');

        /**
         * A description of the error that occurred during login, if any.
         *
         * @type TranslatableMessage
         */
        $scope.loginError = null;

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

        /**
         * Whether an authentication attempt has been submitted. This will be
         * set to true once credentials have been submitted and will only be
         * reset to false once the attempt has been fully processed, including
         * rerouting the user to the requested page if the attempt succeeded.
         *
         * @type Boolean
         */
        $scope.submitted = false;

        /**
         * The field that is most relevant to the user.
         *
         * @type Field
         */
        $scope.relevantField = null;

        /**
         * Returns whether a previous login attempt is continuing.
         *
         * @return {Boolean}
         *     true if a previous login attempt is continuing, false otherwise.
         */
        $scope.isContinuation = function isContinuation() {

            // The login is continuing if any parameter values are provided
            for (var name in $scope.values)
                return true;

            return false;

        };

        // Ensure provided values are included within entered values, even if
        // they have no corresponding input fields
        $scope.$watch('values', function resetEnteredValues(values) {
            angular.extend($scope.enteredValues, values || {});
        });

        // Update field information when form is changed
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

            // Set default values for all unset fields
            angular.forEach($scope.remainingFields, function setDefault(field) {
                if (!$scope.enteredValues[field.name])
                    $scope.enteredValues[field.name] = '';
            });

            $scope.relevantField = getRelevantField();

        });

        /**
         * Submits the currently-specified username and password to the
         * authentication service, redirecting to the main view if successful.
         */
        $scope.login = function login() {

            // Authentication is now in progress
            $scope.submitted = true;

            // Start with cleared status
            $scope.loginError = null;

            // Attempt login once existing session is destroyed
            authenticationService.authenticate($scope.enteredValues)

            // Retry route upon success (entered values will be cleared only
            // after route change has succeeded as this can take time)
            .then(function loginSuccessful() {
                $route.reload();
            })

            // Reset upon failure
            ['catch'](requestService.createErrorCallback(function loginFailed(error) {

                // Initial submission is complete and has failed
                $scope.submitted = false;

                // Clear out passwords if the credentials were rejected for any reason
                if (error.type !== Error.Type.INSUFFICIENT_CREDENTIALS) {

                    // Flag generic error for invalid login
                    if (error.type === Error.Type.INVALID_CREDENTIALS)
                        $scope.loginError = {
                            'key' : 'LOGIN.ERROR_INVALID_LOGIN'
                        };

                    // Display error if anything else goes wrong
                    else
                        $scope.loginError = error.translatableMessage;

                    // Clear all remaining fields that are not username fields
                    angular.forEach($scope.remainingFields, function clearEnteredValueIfPassword(field) {

                        // If field is not username field, delete it.
                        if (field.type !== Field.Type.USERNAME && field.name in $scope.enteredValues)
                            delete $scope.enteredValues[field.name];

                    });
                }

            }));

        };

        /**
         * Returns the field most relevant to the user given the current state
         * of the login process. This will normally be the first empty field.
         *
         * @return {Field}
         *     The field most relevant, null if there is no single most relevant
         *     field.
         */
        var getRelevantField = function getRelevantField() {

            for (var i = 0; i < $scope.remainingFields.length; i++) {
                var field = $scope.remainingFields[i];
                if (!$scope.enteredValues[field.name])
                    return field;
            }

            return null;

        };

        // Reset state after authentication and routing have succeeded
        $rootScope.$on('$routeChangeSuccess', function routeChanged() {
            $scope.enteredValues = {};
            $scope.submitted = false;
        });

    }];

    return directive;

}]);
