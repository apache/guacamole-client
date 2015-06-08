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
 * A directive that allows editing of a field.
 */
angular.module('form').directive('guacFormField', [function formField() {
    
    return {
        // Element only
        restrict: 'E',
        replace: true,
        scope: {

            /**
             * The translation namespace of the translation strings that will
             * be generated for this field. This namespace is absolutely
             * required. If this namespace is omitted, all generated
             * translation strings will be placed within the MISSING_NAMESPACE
             * namespace, as a warning.
             *
             * @type String
             */
            namespace : '=',

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
        controller: ['$scope', '$injector', '$element', function formFieldController($scope, $injector, $element) {

            // Required services
            var formService              = $injector.get('formService');
            var translationStringService = $injector.get('translationStringService');

            /**
             * The element which should contain any compiled field content. The
             * actual content of a field is dynamically determined by its type.
             *
             * @type Element[]
             */
            var fieldContent = $element.find('.form-field');

            /**
             * Produces the translation string for the header of the current
             * field. The translation string will be of the form:
             *
             * <code>NAMESPACE.FIELD_HEADER_NAME<code>
             *
             * where <code>NAMESPACE</code> is the namespace provided to the
             * directive and <code>NAME</code> is the field name transformed
             * via translationStringService.canonicalize().
             *
             * @returns {String}
             *     The translation string which produces the translated header
             *     of the field.
             */
            $scope.getFieldHeader = function getFieldHeader() {

                // If no field, or no name, then no header
                if (!$scope.field || !$scope.field.name)
                    return '';

                return translationStringService.canonicalize($scope.namespace || 'MISSING_NAMESPACE')
                        + '.FIELD_HEADER_' + translationStringService.canonicalize($scope.field.name);

            };

            // Update field contents when field definition is changed
            $scope.$watch('field', function setField(field) {

                // Reset contents
                fieldContent.innerHTML = '';

                // Append field content
                if (field) {
                    formService.createFieldElement(field.type, $scope)
                    .then(function fieldElementCreated(element) {
                        fieldContent.append(element);
                    });
                }

            });

        }] // end controller
    };
    
}]);