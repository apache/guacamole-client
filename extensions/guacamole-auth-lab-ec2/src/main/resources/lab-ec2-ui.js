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
 * UI module that shows a friendly overlay during long-running API requests.
 *
 * This is primarily intended for the lab-ec2 extension, where login can block
 * while EC2 instances are started or created.
 */
(function () {
    'use strict';

    var MODULE_NAME = 'labEc2Ui';
    var OVERLAY_ID = 'lab-ec2-wait-overlay';
    var SHOW_DELAY_MS = 600;

    var ensureOverlay = function ensureOverlay() {

        var overlay = document.getElementById(OVERLAY_ID);
        if (overlay)
            return overlay;

        overlay = document.createElement('div');
        overlay.id = OVERLAY_ID;
        overlay.setAttribute('aria-hidden', 'true');

        overlay.innerHTML =
            '<div class="lab-ec2-wait-overlay__content" role="status" aria-live="polite">' +
            '  <div class="lab-ec2-wait-overlay__coffee" aria-hidden="true">' +
            '    <div class="lab-ec2-wait-overlay__steam lab-ec2-wait-overlay__steam--1"></div>' +
            '    <div class="lab-ec2-wait-overlay__steam lab-ec2-wait-overlay__steam--2"></div>' +
            '    <div class="lab-ec2-wait-overlay__steam lab-ec2-wait-overlay__steam--3"></div>' +
            '    <div class="lab-ec2-wait-overlay__cup"></div>' +
            '  </div>' +
            '  <div class="lab-ec2-wait-overlay__title">Brewing your coffee…</div>' +
            '  <div class="lab-ec2-wait-overlay__subtitle">Starting your lab VM. This can take a minute.</div>' +
            '</div>';

        document.body.appendChild(overlay);
        return overlay;
    };

    angular.module(MODULE_NAME, [])

    .config(['$httpProvider', function labEc2UiConfig($httpProvider) {

        var pendingRequests = 0;
        var showTimer = null;

        var show = function show() {
            var overlay = ensureOverlay();
            overlay.classList.add('lab-ec2-wait-overlay--visible');
            overlay.setAttribute('aria-hidden', 'false');
        };

        var hide = function hide() {
            var overlay = document.getElementById(OVERLAY_ID);
            if (!overlay)
                return;
            overlay.classList.remove('lab-ec2-wait-overlay--visible');
            overlay.setAttribute('aria-hidden', 'true');
        };

        var scheduleShow = function scheduleShow() {
            if (showTimer !== null)
                return;
            showTimer = window.setTimeout(function () {
                showTimer = null;
                if (pendingRequests > 0)
                    show();
            }, SHOW_DELAY_MS);
        };

        var cancelShow = function cancelShow() {
            if (showTimer === null)
                return;
            window.clearTimeout(showTimer);
            showTimer = null;
        };

        var requestStarted = function requestStarted() {
            pendingRequests++;
            if (pendingRequests === 1)
                scheduleShow();
        };

        var requestFinished = function requestFinished() {
            pendingRequests = Math.max(0, pendingRequests - 1);
            if (pendingRequests === 0) {
                cancelShow();
                hide();
            }
        };

        var isApiRequest = function isApiRequest(url) {
            return typeof url === 'string' && url.indexOf('/api/') !== -1;
        };

        $httpProvider.interceptors.push(['$q', function ($q) {
            return {
                request: function (config) {
                    if (isApiRequest(config.url))
                        requestStarted();
                    return config;
                },
                response: function (response) {
                    if (isApiRequest(response.config && response.config.url))
                        requestFinished();
                    return response;
                },
                responseError: function (rejection) {
                    if (isApiRequest(rejection.config && rejection.config.url))
                        requestFinished();
                    return $q.reject(rejection);
                }
            };
        }]);

    }]);

    // Ensure module is loaded in both login and post-login apps
    try { angular.module('login').requires.push(MODULE_NAME); } catch (e) {}
    try { angular.module('index').requires.push(MODULE_NAME); } catch (e) {}

})();
