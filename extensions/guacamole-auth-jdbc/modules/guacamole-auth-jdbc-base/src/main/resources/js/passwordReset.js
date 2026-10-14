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
 * The page parameter carrying a password reset token. Reset links put it in
 * the query of the URL fragment, where the web application already forwards
 * it with every authentication request.
 */
var JDBC_RESET_TOKEN_PARAMETER = 'password-reset-token';

/**
 * The key of the message the server returns for a reset link that is
 * invalid, expired or already used.
 */
var JDBC_RESET_LINK_INVALID = 'PASSWORD_RESET.ERROR_LINK_INVALID';

/**
 * Whether the most recent failed authentication was a rejected reset link.
 */
angular.module('guacJdbcPasswordReset').value('guacJdbcResetLinkState', { invalid : false, keep : false });

/**
 * A reset link always wins over an existing session. Without this, the
 * stored auth token is sent along with the reset token, the server merely
 * re-validates the existing session, and the reset token is never looked at.
 */
angular.module('guacJdbcPasswordReset').config(['$provide', function resetLinkConfig($provide) {

    $provide.decorator('authenticationService', ['$delegate', '$injector',
            function decorateAuthenticationService($delegate, $injector) {

        var updateCurrentToken = $delegate.updateCurrentToken;

        $delegate.updateCurrentToken = function updateCurrentTokenForResetLink(parameters) {

            if (!parameters || !parameters[JDBC_RESET_TOKEN_PARAMETER])
                return updateCurrentToken.apply(this, arguments);

            // Log out of any existing session, then authenticate afresh with
            // the reset token alone. The failure or success of that attempt
            // replaces the cached authentication result either way.
            var token = $delegate.getCurrentToken();
            if (!token)
                return $delegate.authenticate(parameters);

            // Data cached for the previous user must not be shown to the next.
            // guacLogout is not broadcast, as SSO modules redirect on it.
            $injector.get('cacheService').clearCaches();
            return $delegate.revokeToken(token)['catch'](angular.noop)
            .then(function sessionDropped() {
                return $delegate.authenticate(parameters);
            });

        };

        return $delegate;

    }]);

}]);

/**
 * Tracks rejected reset links, and removes the reset token from the URL once
 * it has been used or rejected, so that a refresh or back navigation does not
 * resend it.
 */
angular.module('guacJdbcPasswordReset').run(['$injector', function resetLinkRun($injector) {

    var $location              = $injector.get('$location');
    var $rootScope             = $injector.get('$rootScope');
    var guacJdbcResetLinkState = $injector.get('guacJdbcResetLinkState');

    /**
     * Removes the reset token from the URL, if present.
     *
     * @returns {Boolean}
     *     true if the token was present.
     */
    var stripToken = function stripToken() {
        if (!$location.search()[JDBC_RESET_TOKEN_PARAMETER])
            return false;
        $location.search(JDBC_RESET_TOKEN_PARAMETER, null).replace();
        return true;
    };

    $rootScope.$on('guacLoginFailed', function resetLinkLoginFailed(event, parameters, error) {

        // Stripping the token re-authenticates without it; that follow-up
        // failure must keep the rejected link message
        if (error && error.translatableMessage
                && error.translatableMessage.key === JDBC_RESET_LINK_INVALID) {
            guacJdbcResetLinkState.invalid = true;
            guacJdbcResetLinkState.keep = stripToken();
        }
        else if (guacJdbcResetLinkState.keep)
            guacJdbcResetLinkState.keep = false;
        else
            guacJdbcResetLinkState.invalid = false;

    });

    $rootScope.$on('guacLogin', function resetLinkLogin() {
        guacJdbcResetLinkState.invalid = false;
        guacJdbcResetLinkState.keep = false;
        stripToken();
    });

}]);

/**
 * Shows the rejected reset link message on the login page in place of the
 * generic invalid login message. Shares the scope of the login directive, so
 * it sets that directive's own error message.
 */
angular.module('guacJdbcPasswordReset').directive('guacJdbcResetLinkError', [
        function guacJdbcResetLinkError() {

    return {
        restrict : 'E',
        controller : ['$scope', '$injector', function resetLinkErrorController($scope, $injector) {

            var $rootScope             = $injector.get('$rootScope');
            var guacJdbcResetLinkState = $injector.get('guacJdbcResetLinkState');

            var showError = function showError() {
                if (guacJdbcResetLinkState.invalid)
                    $scope.loginError = { key : JDBC_RESET_LINK_INVALID };
            };

            // The link may have been rejected before the login page existed
            showError();

            // Registered after the login directive's own handler, so this
            // replaces its generic message
            $scope.$on('$destroy', $rootScope.$on('guacLoginFailed', showError));

        }]
    };

}]);

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

/**
 * "Forgot password?" link on the login page. Shown only if a JDBC data source
 * has self-service password reset enabled, which is the case only when its
 * unauthenticated /api/ext/{dataSource}/password-reset resource exists.
 */
angular.module('guacJdbcPasswordReset').directive('guacJdbcForgotPassword', [
        function guacJdbcForgotPassword() {

    return {
        restrict : 'E',
        scope : {},
        template : '<div class="jdbc-forgot-password" ng-show="dataSource">'
                 +   '<a href="" ng-hide="formShown || accepted" ng-click="showForm()">'
                 +     '{{\'PASSWORD_RESET.ACTION_FORGOT_PASSWORD\' | translate}}'
                 +   '</a>'
                 +   '<form ng-show="formShown" ng-submit="submit()">'
                 +     '<p>{{\'PASSWORD_RESET.HELP_FORGOT_PASSWORD\' | translate}}</p>'
                 +     '<input type="text" name="reset-username" autocomplete="username" ng-model="values.username"'
                 +       ' placeholder="{{\'LOGIN.FIELD_HEADER_USERNAME\' | translate}}">'
                 +     '<input type="submit" ng-disabled="submitting || !values.username"'
                 +       ' value="{{\'PASSWORD_RESET.ACTION_SEND_REQUEST\' | translate}}">'
                 +   '</form>'
                 +   '<p class="jdbc-forgot-password-accepted" ng-show="accepted">'
                 +     '{{\'PASSWORD_RESET.INFO_REQUEST_ACCEPTED\' | translate}}'
                 +   '</p>'
                 + '</div>',
        controller : ['$scope', '$injector', function forgotPasswordController($scope, $injector) {

            var $q             = $injector.get('$q');
            var requestService = $injector.get('requestService');

            /**
             * The data sources served by the JDBC extension, in the order
             * they are probed.
             */
            var JDBC_DATA_SOURCES = ['postgresql', 'mysql', 'sqlserver'];

            /**
             * The JDBC data source accepting self-service requests, or null
             * if there is none.
             */
            $scope.dataSource = null;

            $scope.values = { username : '' };
            $scope.formShown = false;
            $scope.submitting = false;
            $scope.accepted = false;

            /**
             * Probes the given data source and the ones after it, stopping
             * at the first with self-service reset enabled.
             *
             * @param {Number} index
             *     The index within JDBC_DATA_SOURCES to probe.
             */
            var probe = function probe(index) {
                if (index >= JDBC_DATA_SOURCES.length)
                    return;
                requestService({
                    method : 'GET',
                    url    : 'api/ext/' + JDBC_DATA_SOURCES[index] + '/password-reset'
                })
                .then(function enabled() {
                    $scope.dataSource = JDBC_DATA_SOURCES[index];
                }, function notEnabled() {
                    probe(index + 1);
                });
            };

            probe(0);

            /**
             * Shows the username form.
             */
            $scope.showForm = function showForm() {
                $scope.formShown = true;
            };

            /**
             * Submits the request. The server answers identically whether or
             * not the account exists, and so does this page.
             */
            $scope.submit = function submit() {
                $scope.submitting = true;
                requestService({
                    method  : 'POST',
                    url     : 'api/ext/' + $scope.dataSource + '/password-reset',
                    headers : { 'Content-Type' : 'application/x-www-form-urlencoded' },
                    data    : $.param({ username : $scope.values.username })
                })
                ['catch'](function ignored() {
                    return $q.resolve();
                })
                ['finally'](function requestDone() {
                    $scope.submitting = false;
                    $scope.formShown = false;
                    $scope.accepted = true;
                });
            };

        }]
    };

}]);
