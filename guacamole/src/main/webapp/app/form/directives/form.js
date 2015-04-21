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
             * The translation namespace of the translation strings that will
             * be generated for all fields. This namespace is absolutely
             * required. If this namespace is omitted, all generated
             * translation strings will be placed within the MISSING_NAMESPACE
             * namespace, as a warning.
             *
             * @type String
             */
            namespace : '=',

            /**
             * The form content to display. This may be a form, an array of
             * forms, or a simple array of fields.
             *
             * @type Form[]|Form|Field[]|Field
             */
            content : '=',

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
        controller: ['$scope', '$injector', function formController($scope, $injector) {

            // Required services
            var translationStringService = $injector.get('translationStringService');

            /**
             * The array of all forms to display.
             *
             * @type Form[]
             */
            $scope.forms = [];

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

            /**
             * Produces the translation string for the section header of the
             * given form. The translation string will be of the form:
             *
             * <code>NAMESPACE.SECTION_HEADER_NAME<code>
             *
             * where <code>NAMESPACE</code> is the namespace provided to the
             * directive and <code>NAME</code> is the form name transformed
             * via translationStringService.canonicalize().
             *
             * @param {Form} form
             *     The form for which to produce the translation string.
             *
             * @returns {String}
             *     The translation string which produces the translated header
             *     of the form.
             */
            $scope.getSectionHeader = function getSectionHeader(form) {

                // If no form, or no name, then no header
                if (!form || !form.name)
                    return '';

                return translationStringService.canonicalize($scope.namespace || 'MISSING_NAMESPACE')
                        + '.SECTION_HEADER_' + translationStringService.canonicalize(form.name);

            };

            /**
             * Determines whether the given object is a form, under the
             * assumption that the object is either a form or a field.
             *
             * @param {Form|Field} obj
             *     The object to test.
             *
             * @returns {Boolean}
             *     true if the given object appears to be a form, false
             *     otherwise.
             */
            var isForm = function isForm(obj) {
                return !!('name' in obj && 'fields' in obj);
            };

            // Produce set of forms from any given content
            $scope.$watch('content', function setContent(content) {

                // If no content provided, there are no forms
                if (!content) {
                    $scope.forms = [];
                    return;
                }

                // Ensure content is an array
                if (!angular.isArray(content))
                    content = [content];

                // If content is an array of fields, convert to an array of forms
                if (content.length && !isForm(content[0])) {
                    content = [{
                        fields : content
                    }];
                }

                // Content is now an array of forms
                $scope.forms = content;

            });

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
