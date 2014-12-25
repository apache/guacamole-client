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
 * A directive that allows editing of a connection parameter.
 */
angular.module('manage').directive('guacConnectionParameter', [function connectionParameter() {
    
    return {
        // Element only
        restrict: 'E',
        replace: true,
        scope: {

            /**
             * The protocol this parameter is associated with.
             *
             * @type Protocol
             */
            protocol : '=',

            /**
             * The unique name of this parameter within the protocol
             * definition.
             * 
             * @type String
             */
            name : '=',

            /**
             * The current map of parameter names to their corresponding string
             * values.
             * 
             * @type Object.<String, String>
             */
            parameters : '='

        },
        templateUrl: 'app/manage/templates/connectionParameter.html',
        controller: ['$scope', '$q', function connectionParameterController($scope, $q) {

            /**
             * Deferred load of the parameter definition, pending availability
             * of the protocol definition as a whole.
             *
             * @type Deferred
             */
            var parameterDefinitionAvailable = $q.defer();

            /**
             * Populates the parameter definition on the scope as
             * <code>$scope.parameter</code> if both the parameter name and
             * protocol definition are available. If either are unavailable,
             * this function has no effect.
             */
            var retrieveParameterDefinition = function retrieveParameterDefinition() {

                // Both name and protocol are needed to retrieve the parameter definition
                if (!$scope.name || !$scope.protocol)
                    return;

                // Once protocol definition is available, locate parameter definition by name
                $scope.protocol.parameters.forEach(function findParameter(parameter) {
                    if (parameter.name === $scope.name) {
                        $scope.parameter = parameter;
                        parameterDefinitionAvailable.resolve(parameter);
                    }
                });

            };

            // Load parameter definition once protocol definition is available.
            $scope.$watch('name',     retrieveParameterDefinition);
            $scope.$watch('protocol', retrieveParameterDefinition);

            // Update typed value when parameter set is changed
            $scope.$watch('parameters', function setParameters(parameters) {

                // Don't bother if no parameters were provided
                if (!parameters)
                    return;

                // Wait for parameter definition
                parameterDefinitionAvailable.promise.then(function setTypedValue() {

                    // Pull parameter value
                    var value = parameters[$scope.name];

                    // Coerce numeric strings to numbers
                    if ($scope.parameter.type === 'NUMERIC')
                        $scope.typedValue = (value ? Number(value) : null);
                    
                    // Coerce boolean strings to boolean values
                    else if ($scope.parameter.type === 'BOOLEAN')
                        $scope.typedValue = (value === $scope.parameter.value);
                    
                    // All other parameter types are represented internally as strings
                    else
                        $scope.typedValue = value || '';

                });
            
            });
            
            // Update string value in parameter set when typed value is changed
            $scope.$watch('typedValue', function typedValueChanged(typedValue) {
                
                // Don't bother if there's nothing to set
                if (!$scope.parameters)
                    return;

                // Wait for parameter definition
                parameterDefinitionAvailable.promise.then(function setValue() {

                    // Convert numeric values back into strings
                    if ($scope.parameter.type === 'NUMERIC') {
                        if (!typedValue)
                            $scope.parameters[$scope.name] = '';
                        else
                            $scope.parameters[$scope.name] = typedValue.toString();
                    }
                    
                    // Convert boolean values back into strings based on protocol description
                    else if ($scope.parameter.type === 'BOOLEAN')
                        $scope.parameters[$scope.name] = (typedValue ? $scope.parameter.value : '');
                    
                    // All other parameter types are already strings
                    else
                        $scope.parameters[$scope.name] = typedValue || '';
                
                });

            }); // end watch typedValue

        }] // end controller
    };
    
}]);