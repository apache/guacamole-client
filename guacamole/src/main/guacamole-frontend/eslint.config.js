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

// @ts-check
const eslint = require('@eslint/js');
const tseslint = require('typescript-eslint');
const angular = require('angular-eslint');

module.exports = tseslint.config(
    {
        files: ['**/*.ts'],
        extends: [
            eslint.configs.recommended,
            ...tseslint.configs.recommended,
            ...tseslint.configs.stylistic,
            ...angular.configs.tsRecommended,
        ],
        processor: angular.processInlineTemplates,
        rules: {
            '@angular-eslint/directive-selector': [
                'error',
                {
                    type: 'attribute',
                    prefix: 'guac',
                    style: 'camelCase',
                },
            ],
            '@angular-eslint/component-selector': [
                'error',
                {
                    type: 'element',
                    prefix: 'guac',
                    style: 'kebab-case',
                },
            ],
            // Inputs bound to the route parameters id and dataSource and the
            // client input of guac-client keep the names of the AngularJS
            // frontend, while their fields keep the names of its controllers
            '@angular-eslint/no-input-rename': ['error', { allowedNames: ['id', 'dataSource', 'client'] }],
            // NgModules group the declarations like the modules of the AngularJS
            // frontend; moving to standalone components is follow-up work
            '@angular-eslint/prefer-standalone': 'off',
            // Constructor injection is kept; moving to inject() is follow-up work
            '@angular-eslint/prefer-inject': 'off',
            // ChangeDetectionStrategy.Eager keeps the pre-v22 behavior; moving to OnPush is follow-up work
            '@angular-eslint/prefer-on-push-component-change-detection': 'off',
            // Namespaces merged with a class or interface of the same name hold
            // its nested types, such as PermissionSet.ObjectPermissionType, under
            // the names of the AngularJS frontend
            '@typescript-eslint/no-namespace': 'off'
        }
    },
    {
        files: ['**/*.html'],
        extends: [
            ...angular.configs.templateRecommended,
            ...angular.configs.templateAccessibility,
        ],
        'rules': {
            // The transloco directive sets the text of headings and buttons
            '@angular-eslint/template/elements-content': ['error', { allowList: ['transloco'] }],
            // Negated async pipes stand for ng-show and ng-hide of the AngularJS
            // templates: a value that has not arrived yet counts as false, as an
            // undefined value does in AngularJS
            '@angular-eslint/template/no-negated-async': 'off',

            // The following accessibility rules conflict with markup taken over
            // from the AngularJS templates. Changing that markup concerns both
            // frontends and is not part of the migration.
            // Clickable elements such as div, span and img have no key handlers
            '@angular-eslint/template/click-events-have-key-events': 'off',
            // Clickable elements such as div, span and img are not focusable
            '@angular-eslint/template/interactive-supports-focus': 'off',
            // Some images, such as the share indicator of the client tiles, have no alt text
            '@angular-eslint/template/alt-text': 'off',
            // Some labels wrap an image and text instead of a form control
            '@angular-eslint/template/label-has-associated-control': 'off',
            // The text input focuses its textarea with autofocus
            '@angular-eslint/template/no-autofocus': 'off'
        }
    }
);
