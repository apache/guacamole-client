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

            /**
             * Returns whether the current field should be displayed.
             *
             * @returns {Boolean}
             *     true if the current field should be displayed, false
             *     otherwise.
             */
            $scope.isFieldVisible = function isFieldVisible() {
                return fieldContent[0].hasChildNodes();
            };

            // Update field contents when field definition is changed
            $scope.$watch('field', function setField(field) {

                // Reset contents
                fieldContent.innerHTML = '';

                // Append field content
                if (field) {
                    formService.insertFieldElement(fieldContent[0],
                        field.type, $scope);
                }

            });

        }] // end controller
    };
    
}]);