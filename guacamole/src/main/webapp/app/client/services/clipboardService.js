/*
 * Copyright (C) 2016 Glyptodon LLC
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
 * A service for accessing local clipboard data.
 */
angular.module('client').factory('clipboardService', ['$injector',
        function clipboardService($injector) {

    // Get required services
    var $q         = $injector.get('$q');
    var $rootScope = $injector.get('$rootScope');

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

    /**
     * The contents of the last clipboard event broadcast by this service when
     * the clipboard contents changed.
     *
     * @type String
     */
    var lastClipboardEvent = '';

    clipElement.appendChild(clipboardContent);
    clipElement.style.position = 'absolute';
    clipElement.style.width = '0px';
    clipElement.style.height = '0px';
    clipElement.style.overflow = 'hidden';

    document.body.appendChild(clipElement);

    /**
     * Sets the local clipboard, if possible, to the given text.
     *
     * @param {String} text
     *     The text to which the local clipboard should be set.
     *
     * @return {Promise}
     *     A promise that will resolve if setting the clipboard was successful,
     *     and will reject if it failed.
     */
    service.setLocalClipboard = function setLocalClipboard(text) {

        var deferred = $q.defer();

        // First, see if allow clipboard extension is installed
        if (window.AllowClipboard) {

            var clipboardClient = new AllowClipboard.Client.ClipboardClient();

            clipboardClient.write(text, function(success) {
                if (success)
                    deferred.resolve();
                else
                    deferred.reject();
            });

        }

        // Otherwise, try execCommand
        else {

            // Copy the given value into the clipboard DOM element
            clipboardContent.value = text;
            clipboardContent.select();

            // Attempt to copy data from clipboard element into local clipboard
            if (document.execCommand('copy'))
                deferred.resolve();
            else
                deferred.reject();

        }

        return deferred.promise;
    };

    /**
     * Get the current value of the local clipboard.
     *
     * @return {Promise}
     *     A promise that will resolve with the contents of the local clipboard
     *     if getting the clipboard was successful, and will reject if it
     *     failed.
     */
    service.getLocalClipboard = function getLocalClipboard() {

        var deferred = $q.defer();

        // First, see if allow clipboard extension is installed
        if (window.AllowClipboard) {

            var clipboardClient = new AllowClipboard.Client.ClipboardClient();

            clipboardClient.read(function(success, data) {
                if (success)
                    deferred.resolve(data);
                else
                    deferred.reject();
            });

        }

        // Otherwise, try execCommand
        else {

            // Clear and select the clipboard DOM element
            clipboardContent.value = '';
            clipboardContent.select();

            // Attempt paste local clipboard into clipboard DOM element
            if (document.execCommand('paste'))
                deferred.resolve(clipboardContent.value);
            else
                deferred.reject();

        }

        return deferred.promise;
    };

    // Periodically attempt to read the clipboard, firing an event if successful
    window.setInterval(function periodicallyReadClipboard() {
        service.getLocalClipboard().then(function clipboardRead(data) {

            // Fire clipboard event if the data has changed
            if (data !== lastClipboardEvent) {
               $rootScope.$broadcast('guacClipboard', 'text/plain', data);
               lastClipboardEvent = data;
            }

        });
    }, 100);

    return service;

}]);
