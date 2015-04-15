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
 * A directive that allows editing of a field.
 */
angular.module('form').directive('guacFormField', [function formField() {
    
    return {
        // Element only
        restrict: 'E',
        replace: true,
        scope: {

            /**
             * The field to display.
             *
             * @type Field
             */
            field : '=',

            /**
             * The property which contains this fields current value. When this
             * field changes, the property will be updated accordingly.
             *
             * @type String
             */
            model : '='

        },
        templateUrl: 'app/form/templates/formField.html',
        controller: ['$scope', '$injector', function formFieldController($scope, $injector) {

            /**
             * The type to use for password input fields. By default, password
             * input fields have type 'password', and are thus masked.
             * 
             * @type String
             * @default 'password'
             */
            $scope.passwordInputType = 'password';

            /**
             * Returns a string which describes the action the next call to
             * togglePassword() will have.
             *
             * @return {String}
             *     A string which describes the action the next call to
             *     togglePassword() will have.
             */
            $scope.getTogglePasswordHelpText = function getTogglePasswordHelpText() {

                // If password is hidden, togglePassword() will show the password
                if ($scope.passwordInputType === 'password')
                    return 'MANAGE.HELP_SHOW_PASSWORD';

                // If password is shown, togglePassword() will hide the password
                return 'MANAGE.HELP_HIDE_PASSWORD';

            };

            /**
             * Toggles visibility of the field contents, if this field is a 
             * password field. Initially, password contents are masked
             * (invisible).
             */
            $scope.togglePassword = function togglePassword() {

                // If password is hidden, show the password
                if ($scope.passwordInputType === 'password')
                    $scope.passwordInputType = 'text';

                // If password is shown, hide the password
                else
                    $scope.passwordInputType = 'password';

            };

            /**
             * Translates the given string field value into an appropriately-
             * typed value as dictated by the attributes of the field,
             * exposing that typed value within the scope as
             * <code>$scope.typedValue<code>.
             *
             * @param {String} modelValue
             *     The current string value of the field.
             */
            var setTypedValue = function setTypedValue(modelValue) {

                // Don't bother if the modelValue is not yet defined
                if (!$scope.field)
                    return;

                // Coerce numeric strings to numbers
                if ($scope.field.type === 'NUMERIC')
                    $scope.typedValue = (modelValue ? Number($scope.field.value) : null);

                // Coerce boolean strings to boolean values
                else if ($scope.field.type === 'BOOLEAN')
                    $scope.typedValue = (modelValue === $scope.field.value);

                // All other field types are represented internally as strings
                else
                    $scope.typedValue = modelValue || '';

            };

            /**
             * Translates the given typed field value into a string as dictated
             * by the attributes of the field, assigning that string value to
             * the model.
             *
             * @param {String|Number|Boolean} typedValue
             *     The current value of the field, as an appropriate JavaScript
             *     type.
             */
            var setModelValue = function setModelValue(typedValue) {

                // Don't bother if the model is not yet defined
                if (!$scope.field)
                    return;

                // Convert numeric values back into strings
                if ($scope.field.type === 'NUMERIC') {
                    if (!typedValue)
                        $scope.model = '';
                    else
                        $scope.model = typedValue.toString();
                }

                // Convert boolean values back into strings based on protocol description
                else if ($scope.field.type === 'BOOLEAN')
                    $scope.model = (typedValue ? $scope.field.value : '');

                // All other field types are already strings
                else
                    $scope.model = typedValue || '';

            };

            // Update string value and re-assign to model when field is changed
            $scope.$watch('field', function setField(field) {
                setTypedValue($scope.model);
                setModelValue($scope.typedValue);
            });

            // Update typed value when model is changed
            $scope.$watch('model', function setModel(model) {
                setTypedValue(model);
            });

            // Update string value in model when typed value is changed
            $scope.$watch('typedValue', function typedValueChanged(typedValue) {
                setModelValue(typedValue);
            });

        }] // end controller
    };
    
}]);