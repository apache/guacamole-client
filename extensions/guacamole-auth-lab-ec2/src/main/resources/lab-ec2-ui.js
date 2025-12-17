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
    var SHOW_DELAY_MS = 150;
    var DEBUG = true;
    var overlayElement = null;

    var log = function log() {
        if (!DEBUG || !window || !window.console)
            return;
        try { window.console.log.apply(window.console, arguments); }
        catch (e) {}
    };

    var ensureOverlay = function ensureOverlay() {

        var overlay = document.getElementById(OVERLAY_ID);
        if (overlay)
            return overlay;

        if (overlayElement)
            return overlayElement;

        log('[lab-ec2-ui] creating overlay');

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
            '  <div class="lab-ec2-wait-overlay__title">Brewing your coffee...</div>' +
            '  <div class="lab-ec2-wait-overlay__subtitle">Starting your lab VM. This can take a minute.</div>' +
            '</div>';

        overlayElement = overlay;

        var parent = document.body || document.documentElement;
        if (!parent) {
            log('[lab-ec2-ui] overlay parent not ready yet (document.readyState=' + document.readyState + ')');

            try {
                document.addEventListener('DOMContentLoaded', function () {
                    try {
                        var readyParent = document.body || document.documentElement;
                        if (readyParent && !document.getElementById(OVERLAY_ID)) {
                            readyParent.appendChild(overlay);
                            log('[lab-ec2-ui] overlay appended after DOMContentLoaded');
                        }
                    }
                    catch (e) {}
                }, { once: true });
            }
            catch (e) {}

            return overlay;
        }

        parent.appendChild(overlay);
        log('[lab-ec2-ui] overlay appended to', parent === document.body ? 'body' : 'documentElement');
        return overlay;
    };

    log('[lab-ec2-ui] script loaded (readyState=' + document.readyState + ')');
    if (!window.angular)
        log('[lab-ec2-ui] WARNING: angular not found on window yet');

    angular.module(MODULE_NAME, [])

    .config(['$httpProvider', function labEc2UiConfig($httpProvider) {

        log('[lab-ec2-ui] module config start');

        var pendingRequests = 0;
        var showTimer = null;

        var show = function show() {
            var overlay = ensureOverlay();
            overlay.classList.add('lab-ec2-wait-overlay--visible');
            overlay.setAttribute('aria-hidden', 'false');
            log('[lab-ec2-ui] show (pendingRequests=' + pendingRequests + ')');
        };

        var hide = function hide() {
            var overlay = document.getElementById(OVERLAY_ID);
            if (!overlay)
                return;
            overlay.classList.remove('lab-ec2-wait-overlay--visible');
            overlay.setAttribute('aria-hidden', 'true');
            log('[lab-ec2-ui] hide');
        };

        var scheduleShow = function scheduleShow() {
            if (showTimer !== null)
                return;
            showTimer = window.setTimeout(function () {
                showTimer = null;
                if (pendingRequests > 0)
                    show();
            }, SHOW_DELAY_MS);
            log('[lab-ec2-ui] schedule show in ' + SHOW_DELAY_MS + 'ms');
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
            if (typeof url !== 'string')
                return false;

            // Guacamole uses relative URLs like "api/tokens" and
            // "api/session/data/...". lab-ec2 can block these while waiting
            // for EC2 instances to start.
            return url.indexOf('api/') === 0 || url.indexOf('/api/') !== -1;
        };

        $httpProvider.interceptors.push(['$q', function ($q) {
            return {
                request: function (config) {
                    if (isApiRequest(config.url)) {
                        log('[lab-ec2-ui] intercept request:', config.method, config.url);
                        requestStarted();
                    }
                    return config;
                },
                response: function (response) {
                    if (isApiRequest(response.config && response.config.url)) {
                        log('[lab-ec2-ui] intercept response:', response.status, response.config.url);
                        requestFinished();
                    }
                    return response;
                },
                responseError: function (rejection) {
                    if (isApiRequest(rejection.config && rejection.config.url)) {
                        log('[lab-ec2-ui] intercept responseError:', rejection.status, rejection.config.url);
                        requestFinished();
                    }
                    return $q.reject(rejection);
                }
            };
        }]);

        log('[lab-ec2-ui] interceptor registered');

    }]);

    // Ensure module is loaded in both login and post-login apps
    try { angular.module('login').requires.push(MODULE_NAME); } catch (e) {}
    try { angular.module('index').requires.push(MODULE_NAME); } catch (e) {}

    log('[lab-ec2-ui] module injected into angular apps (if present)');

})();
