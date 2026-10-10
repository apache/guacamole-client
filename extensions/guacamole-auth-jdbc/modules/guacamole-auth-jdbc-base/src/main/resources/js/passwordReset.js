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


/*
 * Plain ES5 only: this file is served as-is, without transpilation.
 */

/**
 * Module which adds password reset links to the JDBC extension.
 */
angular.module('guacJdbcPasswordReset', ['auth', 'notification', 'rest']);

// Ensure the module is loaded along with the rest of the app
angular.module('index').requires.push('guacJdbcPasswordReset');

/**
 * Button on the manage user page which mails the user a password reset link.
 * Shown only for existing users of a JDBC data source.
 */
angular.module('guacJdbcPasswordReset').directive('guacJdbcPasswordResetButton', [
        function guacJdbcPasswordResetButton() {

    return {
        restrict : 'E',
        scope : {
            dataSource : '=',
            username   : '=',
            exists     : '='
        },
        template : '<div class="jdbc-password-reset" ng-show="isApplicable()">'
                 +   '<button type="button" ng-click="send()" ng-disabled="sending">'
                 +     '{{\'PASSWORD_RESET.ACTION_SEND_LINK\' | translate}}'
                 +   '</button>'
                 + '</div>',
        controller : ['$scope', '$injector', function passwordResetButtonController($scope, $injector) {

            var authenticationService = $injector.get('authenticationService');
            var guacNotification      = $injector.get('guacNotification');

            /**
             * The data sources served by the JDBC extension.
             */
            var JDBC_DATA_SOURCES = ['postgresql', 'mysql', 'sqlserver'];

            /**
             * Whether a request is in progress.
             */
            $scope.sending = false;

            /**
             * Returns whether the button applies to the user being edited.
             *
             * @returns {Boolean}
             *     true if the user exists within a JDBC data source.
             */
            $scope.isApplicable = function isApplicable() {
                return !!$scope.exists && JDBC_DATA_SOURCES.indexOf($scope.dataSource) !== -1;
            };

            /**
             * Asks the server to mail a reset link to the user.
             */
            $scope.send = function send() {
                $scope.sending = true;
                authenticationService.request({
                    method  : 'POST',
                    url     : 'api/session/ext/' + encodeURIComponent($scope.dataSource) + '/password-reset',
                    headers : { 'Content-Type' : 'application/x-www-form-urlencoded' },
                    data    : $.param({ username : $scope.username })
                })
                .then(function linkSent() {
                    guacNotification.showStatus({
                        title   : 'PASSWORD_RESET.DIALOG_HEADER_SENT',
                        text    : {
                            key       : 'PASSWORD_RESET.INFO_LINK_SENT',
                            variables : { USERNAME : $scope.username }
                        },
                        actions : [ guacNotification.ACKNOWLEDGE_ACTION ]
                    });
                }, guacNotification.SHOW_REQUEST_ERROR)
                ['finally'](function requestDone() {
                    $scope.sending = false;
                });
            };

        }]
    };

}]);
