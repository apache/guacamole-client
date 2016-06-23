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
 * A service for accessing local clipboard data.
 */
angular.module('clipboard').factory('clipboardService', ['$injector',
        function clipboardService($injector) {

    // Get required services
    var $q = $injector.get('$q');

    var service = {};

    /**
     * A div which is used to hide the clipboard textarea and remove it from
     * document flow.
     *
     * @type Element
     */
    var clipElement = document.createElement('div');

    /**
     * The textarea that will be used to hold the local clipboard contents.
     *
     * @type Element
     */
    var clipboardContent = document.createElement('textarea');

    // Ensure textarea is selectable but not visible
    clipElement.appendChild(clipboardContent);
    clipElement.style.position = 'fixed';
    clipElement.style.width    = '1px';
    clipElement.style.height   = '1px';
    clipElement.style.left     = '-1px';
    clipElement.style.top      = '-1px';
    clipElement.style.overflow = 'hidden';

    // Add textarea to DOM
    document.body.appendChild(clipElement);

    /**
     * Sets the local clipboard, if possible, to the given text.
     *
     * @param {ClipboardData} data
     *     The data to assign to the local clipboard should be set.
     *
     * @return {Promise}
     *     A promise that will resolve if setting the clipboard was successful,
     *     and will reject if it failed.
     */
    service.setLocalClipboard = function setLocalClipboard(data) {

        var deferred = $q.defer();

        // Track the originally-focused element prior to changing focus
        var originalElement = document.activeElement;

        // Copy the given value into the clipboard DOM element
        clipboardContent.value = 'X';
        clipboardContent.select();

        // Override copied contents of clipboard with the provided value
        clipboardContent.oncopy = function overrideContent(e) {

            // Override the contents of the clipboard
            e.preventDefault();

            // Remove anything already present within the clipboard
            var items = e.clipboardData.items;
            items.clear();

            // If the provided data is a string, add it as such
            if (typeof data.data === 'string')
                items.add(data.data, data.type);

            // Otherwise, add as a File
            else
                items.add(new File([data.data], 'data', { type : data.type }));

        };

        // Attempt to copy data from clipboard element into local clipboard
        if (document.execCommand('copy'))
            deferred.resolve();
        else
            deferred.reject();

        // Unfocus the clipboard DOM event to avoid mobile keyboard opening,
        // restoring whichever element was originally focused
        clipboardContent.blur();
        originalElement.focus();

        return deferred.promise;
    };

    /**
     * Get the current value of the local clipboard.
     *
     * @return {Promise.<ClipboardData>}
     *     A promise that will resolve with the contents of the local clipboard
     *     if getting the clipboard was successful, and will reject if it
     *     failed.
     */
    service.getLocalClipboard = function getLocalClipboard() {

        var deferred = $q.defer();

        // Wait for the next event queue run before attempting to read
        // clipboard data (in case the copy/cut has not yet completed)
        window.setTimeout(function deferredClipboardRead() {

            // Track the originally-focused element prior to changing focus
            var originalElement = document.activeElement;

            // Clear and select the clipboard DOM element
            clipboardContent.value = '';
            clipboardContent.focus();
            clipboardContent.select();

            // FIXME: Only handling text data

            // Attempt paste local clipboard into clipboard DOM element
            if (document.activeElement === clipboardContent && document.execCommand('paste'))
                deferred.resolve(new ClipboardData({
                    type : 'text/plain',
                    data : clipboardContent.value
                }));
            else
                deferred.reject();

            // Unfocus the clipboard DOM event to avoid mobile keyboard opening,
            // restoring whichever element was originally focused
            clipboardContent.blur();
            originalElement.focus();

        }, 100);

        return deferred.promise;
    };

    return service;

}]);
