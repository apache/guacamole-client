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
        var $compile       = $injector.get('$compile');
        var $http          = $injector.get('$http');
        var $q             = $injector.get('$q');
        var $templateCache = $injector.get('$templateCache');

        var service = {};

        service.fieldTypes = provider.fieldTypes;

        /**
         * Returns a Promise which resolves with the HTML contents of the
         * template at the given URL. The template contents will be retrieved from
         * the $templateCache if possible.
         *
         * @param {String} url
         *     The URL of the template to retrieve.
         *
         * @returns {Promise.<String>}
         *     A Promise which resolves with the HTML contents of the template at
         *     the given URL.
         */
        var templateRequest = function templateRequest(url) {

            // Pull template from cache if present
            var template = $templateCache.get(url);
            if (template)
                return $q.when(template);

            // Defer retrieval of template
            var templateContent = $q.defer();

            // Retrieve template manually
            $http({
                method : 'GET',
                url    : url,
                cache  : true
            })

            // Upon success, resolve promise and update template cache
            .success(function templateRetrieved(html) {
                $templateCache.put(url, html);
                templateContent.resolve(html);
            })

            // Fail if template cannot be retrieved
            .error(function templateError() {
                templateContent.reject();
            });

            // Return promise which will resolve with the retrieved template
            return templateContent.promise;

        };

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
        service.createFieldElement = function createFieldElement(fieldTypeName, scope) {

            // Ensure field type is defined
            var fieldType = provider.fieldTypes[fieldTypeName];
            if (!fieldType)
                return $q.reject();

            // Populate scope using defined controller
            if (fieldType.module && fieldType.controller) {
                var $controller = angular.injector(['ng', fieldType.module]).get('$controller');
                $controller(fieldType.controller, {'$scope' : scope});
            }

            // Defer compilation of template pending successful retrieval
            var compiledTemplate = $q.defer();

            // Attempt to retrieve template HTML
            templateRequest(fieldType.templateUrl)

            // Resolve with compiled HTML upon success
            .then(function templateRetrieved(html) {
                compiledTemplate.resolve($compile(html)(scope));
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
