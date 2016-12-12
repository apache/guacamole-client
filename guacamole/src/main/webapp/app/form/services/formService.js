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
 * A service for maintaining form-related metadata and linking that data to
 * corresponding controllers and templates.
 */
angular.module('form').provider('formService', function formServiceProvider() {

    /**
     * Reference to the provider itself.
     *
     * @type formServiceProvider
     */
    var provider = this;

    /**
     * Map of all registered field type definitions by name.
     *
     * @type Object.<String, FieldType>
     */
    this.fieldTypes = {

        /**
         * Text field type.
         *
         * @see {@link Field.Type.TEXT}
         * @type FieldType
         */
        'TEXT' : {
            templateUrl : 'app/form/templates/textField.html'
        },

        /**
         * Numeric field type.
         *
         * @see {@link Field.Type.NUMERIC}
         * @type FieldType
         */
        'NUMERIC' : {
            module      : 'form',
            controller  : 'numberFieldController',
            templateUrl : 'app/form/templates/numberField.html'
        },

        /**
         * Boolean field type.
         *
         * @see {@link Field.Type.BOOLEAN}
         * @type FieldType
         */
        'BOOLEAN' : {
            module      : 'form',
            controller  : 'checkboxFieldController',
            templateUrl : 'app/form/templates/checkboxField.html'
        },

        /**
         * Username field type. Identical in principle to a text field, but may
         * have different semantics.
         *
         * @see {@link Field.Type.USERNAME}
         * @type FieldType
         */
        'USERNAME' : {
            templateUrl : 'app/form/templates/textField.html'
        },

        /**
         * Password field type. Similar to a text field, but the contents of
         * the field are masked.
         *
         * @see {@link Field.Type.PASSWORD}
         * @type FieldType
         */
        'PASSWORD' : {
            module      : 'form',
            controller  : 'passwordFieldController',
            templateUrl : 'app/form/templates/passwordField.html'
        },

        /**
         * Enumerated field type. The user is presented a finite list of values
         * to choose from.
         *
         * @see {@link Field.Type.ENUM}
         * @type FieldType
         */
        'ENUM' : {
            module      : 'form',
            controller  : 'selectFieldController',
            templateUrl : 'app/form/templates/selectField.html'
        },

        /**
         * Multiline field type. The user may enter multiple lines of text.
         *
         * @see {@link Field.Type.MULTILINE}
         * @type FieldType
         */
        'MULTILINE' : {
            templateUrl : 'app/form/templates/textAreaField.html'
        },

        /**
         * Field type which allows selection of time zones.
         *
         * @see {@link Field.Type.TIMEZONE}
         * @type FieldType
         */
        'TIMEZONE' : {
            module      : 'form',
            controller  : 'timeZoneFieldController',
            templateUrl : 'app/form/templates/timeZoneField.html'
        },

        /**
         * Field type which allows selection of individual dates.
         *
         * @see {@link Field.Type.DATE}
         * @type FieldType
         */
        'DATE' : {
            module      : 'form',
            controller  : 'dateFieldController',
            templateUrl : 'app/form/templates/dateField.html'
        },

        /**
         * Field type which allows selection of times of day.
         *
         * @see {@link Field.Type.TIME}
         * @type FieldType
         */
        'TIME' : {
            module      : 'form',
            controller  : 'timeFieldController',
            templateUrl : 'app/form/templates/timeField.html'
        }

    };

    /**
     * Registers a new field type under the given name.
     *
     * @param {String} fieldTypeName
     *     The name which uniquely identifies the field type being registered.
     *
     * @param {FieldType} fieldType
     *     The field type definition to associate with the given name.
     */
    this.registerFieldType = function registerFieldType(fieldTypeName, fieldType) {

        // Store field type
        provider.fieldTypes[fieldTypeName] = fieldType;

    };

    // Factory method required by provider
    this.$get = ['$injector', function formServiceFactory($injector) {

        // Required services
        var $compile         = $injector.get('$compile');
        var $q               = $injector.get('$q');
        var $templateRequest = $injector.get('$templateRequest');

        var service = {};

        service.fieldTypes = provider.fieldTypes;

        /**
         * Compiles and links the field associated with the given name to the given
         * scope, producing a distinct and independent DOM Element which functions
         * as an instance of that field. The scope object provided must include at
         * least the following properties:
         *
         * namespace:
         *     A String which defines the unique namespace associated the
         *     translation strings used by the form using a field of this type.
         *
         * field:
         *     The Field object that is being rendered, representing a field of
         *     this type.
         *
         * model:
         *     The current String value of the field, if any.
         *
         * @param {Element} fieldContainer
         *     The DOM Element whose contents should be replaced with the
         *     compiled field template.
         *
         * @param {String} fieldTypeName
         *     The name of the field type defining the nature of the element to be
         *     created.
         *
         * @param {Object} scope
         *     The scope to which the new element will be linked.
         *
         * @return {Promise.<Element>}
         *     A Promise which resolves to the compiled Element. If an error occurs
         *     while retrieving the field type, this Promise will be rejected.
         */
        service.insertFieldElement = function insertFieldElement(fieldContainer,
            fieldTypeName, scope) {

            // Ensure field type is defined
            var fieldType = provider.fieldTypes[fieldTypeName];
            if (!fieldType)
                return $q.reject();

            var templateRequest;

            // Use raw HTML template if provided
            if (fieldType.template) {
                var deferredTemplate = $q.defer();
                deferredTemplate.resolve(fieldType.template);
                templateRequest = deferredTemplate.promise;
            }

            // If no raw HTML template is provided, retrieve template from URL
            else
                templateRequest = $templateRequest(fieldType.templateUrl);

            // Defer compilation of template pending successful retrieval
            var compiledTemplate = $q.defer();

            // Resolve with compiled HTML upon success
            templateRequest.then(function templateRetrieved(html) {

                // Insert template into DOM
                fieldContainer.innerHTML = html;

                // Populate scope using defined controller
                if (fieldType.module && fieldType.controller) {
                    var $controller = angular.injector(['ng', fieldType.module]).get('$controller');
                    $controller(fieldType.controller, {
                        '$scope'   : scope,
                        '$element' : angular.element(fieldContainer.childNodes)
                    });
                }

                // Compile DOM with populated scope
                compiledTemplate.resolve($compile(fieldContainer.childNodes)(scope));

            })

            // Reject on failure
            ['catch'](function templateError() {
                compiledTemplate.reject();
            });

            // Return promise which resolves to the compiled template
            return compiledTemplate.promise;

        };

        return service;

    }];

});
