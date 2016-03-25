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
 * Controller for select fields.
 */
angular.module('form').controller('selectFieldController', ['$scope', '$injector',
    function selectFieldController($scope, $injector) {

    // Required services
    var translationStringService = $injector.get('translationStringService');

    // Interpret undefined/null as empty string
    $scope.$watch('model', function setModel(model) {
        if (!model && model !== '')
            $scope.model = '';
    });

    /**
     * Produces the translation string for the given field option
     * value. The translation string will be of the form:
     *
     * <code>NAMESPACE.FIELD_OPTION_NAME_VALUE<code>
     *
     * where <code>NAMESPACE</code> is the namespace provided to the
     * directive, <code>NAME</code> is the field name transformed
     * via translationStringService.canonicalize(), and
     * <code>VALUE</code> is the option value transformed via
     * translationStringService.canonicalize()
     *
     * @param {String} value
     *     The name of the option value.
     *
     * @returns {String}
     *     The translation string which produces the translated name of the
     *     value specified.
     */
    $scope.getFieldOption = function getFieldOption(value) {

        // If no field, or no value, then no corresponding translation string
        if (!$scope.field || !$scope.field.name || !value)
            return '';

        return translationStringService.canonicalize($scope.namespace || 'MISSING_NAMESPACE')
                + '.FIELD_OPTION_' + translationStringService.canonicalize($scope.field.name)
                + '_'              + translationStringService.canonicalize(value || 'EMPTY');

    };

}]);
