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
