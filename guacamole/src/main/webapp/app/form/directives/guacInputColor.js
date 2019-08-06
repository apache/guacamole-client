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
 * A directive which implements a color input field, leveraging the "Pickr"
 * color picker.
 */
angular.module('form').directive('guacInputColor', [function guacInputColor() {

    var config = {
        restrict: 'E',
        replace: true,
        templateUrl: 'app/form/templates/guacInputColor.html',
    };

    config.scope = {

        /**
         * The current selected color value, in standard 6-digit hexadecimal
         * RGB notation. When the user selects a different color using this
         * directive, this value will updated accordingly.
         *
         * @type String
         */
        model: '=',

        /**
         * An optional array of colors to include within the color picker as a
         * convenient selection of pre-defined colors. The colors within the
         * array must be in standard 6-digit hexadecimal RGB notation.
         *
         * @type String[]
         */
        palette: '='

    };

    config.controller = ['$scope', '$element', '$injector',
        function guacInputColorController($scope, $element, $injector) {

        // Required services
        var $q         = $injector.get('$q');
        var $translate = $injector.get('$translate');

        // Init color picker after required translation strings are available
        $q.all({
            'save'   : $translate('APP.ACTION_SAVE'),
            'cancel' : $translate('APP.ACTION_CANCEL')
        }).then(function stringsRetrieved(strings) {

            /**
             * An instance of the "Pickr" color picker, bound to the underlying
             * element of this directive.
             *
             * @type Pickr
             */
            var pickr = Pickr.create({

                // Bind color picker to the underlying element of this directive
                el : $element[0],

                // Wrap color picker dialog in Guacamole-specific class for
                // sake of additional styling
                appClass : 'guac-input-color-picker',

                // Display color details as hex
                defaultRepresentation : 'HEX',

                // Use "monolith" theme, as a nice balance between "nano" (does
                // not work in Internet Explorer) and "classic" (too big)
                theme : 'monolith',

                // Leverage the container element as the button which shows the
                // picker, relying on our own styling for that button
                useAsButton  : true,
                appendToBody : true,

                // Do not include opacity controls
                lockOpacity : true,

                // Include a selection of palette entries for convenience and
                // reference
                swatches : $scope.palette || [],

                components: {

                    // Include hue and color preview controls
                    preview : true,
                    hue     : true,

                    // Display only a text color input field and the save and
                    // cancel buttons (no clear button)
                    interaction: {
                        input  : true,
                        save   : true,
                        cancel : true
                    }

                },

                // Use translation strings for buttons
                strings : strings

            });

            // Hide color picker after user clicks "cancel"
            pickr.on('cancel', function colorChangeCanceled() {
                pickr.hide();
            });

            // Keep model in sync with changes to the color picker
            pickr.on('save', function colorChanged(color) {
                $scope.$evalAsync(function updateModel() {
                    $scope.model = color.toHEXA().toString();
                });
            });

            // Keep color picker in sync with changes to the model
            pickr.on('init', function pickrReady(color) {
                $scope.$watch('model', function modelChanged(model) {
                    pickr.setColor(model);
                });
            });

        }, angular.noop);

    }];

    return config;

}]);
