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
 * A directive which exposes the current clipboard contents, if possible,
 * allowing the user to edit those contents. If the current clipboard contents
 * cannot be directly accessed, the user can at least directly copy/paste data
 * within the field provided by this directive. The contents of this clipboard
 * directive, whether retrieved from the local or manipulated manually by the
 * user, are exposed via the "data" attribute.
 */
angular.module('client').directive('guacClipboard', [function guacClipboard() {

    /**
     * Configuration object for the guacClipboard directive.
     *
     * @type Object.<String, Object>
     */
    var config = {
        restrict    : 'E',
        replace     : true,
        templateUrl : 'app/client/templates/guacClipboard.html'
    };

    // Scope properties exposed by the guacClipboard directive
    config.scope = {

        /**
         * The data to display within the field provided by this directive. If
         * the local clipboard can be accessed by JavaScript, this will be set
         * automatically as the local clipboard changes. Failing that, this
         * will be set when the user manually modifies the contents of the
         * field. Changes to this value will be rendered within the field and,
         * if possible, will be pushed to the local clipboard.
         *
         * @type String
         */
        data : '='

    };

    // guacClipboard directive controller
    config.controller = ['$scope', '$injector', '$element',
            function guacClipboardController($scope, $injector, $element) {

            // STUB

    }];

    return config;

}]);
