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
 * A directive that allows editing of a collection of fields.
 */
angular.module('form').directive('guacForm', [function form() {

    return {
        // Element only
        restrict: 'E',
        replace: true,
        scope: {

            /**
             * The fields to display.
             *
             * @type Field[]
             */
            fields : '=',

            /**
             * The object which will receive all field values. Each field value
             * will be assigned to the property of this object having the same
             * name.
             *
             * @type Object.<String, String>
             */
            model : '='

        },
        templateUrl: 'app/form/templates/form.html',
        controller: ['$scope', function formController($scope) {

            /**
             * The object which will receive all field values. Normally, this
             * will be the object provided within the "model" attribute. If
             * no such object has been provided, a blank model will be used
             * instead as a placeholder, such that the fields of this form
             * will have something to bind to.
             *
             * @type Object.<String, String>
             */
            $scope.values = {};

            // Update string value and re-assign to model when field is changed
            $scope.$watch('model', function setModel(model) {

                // Assign new model only if provided
                if (model)
                    $scope.values = model;

                // Otherwise, use blank model
                else
                    $scope.values = {};

            });

        }] // end controller
    };

}]);
