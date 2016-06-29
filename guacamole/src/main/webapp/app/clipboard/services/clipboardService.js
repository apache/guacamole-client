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
    var $q      = $injector.get('$q');
    var $window = $injector.get('$window');

    // Required types
    var ClipboardData = $injector.get('ClipboardData');

    var service = {};

    /**
     * The amount of time to wait before actually serving a request to read
     * clipboard data, in milliseconds. Providing a reasonable delay between
     * request and read attempt allows the cut/copy operation to settle, in
     * case the data we are anticipating to be present is not actually present
     * in the clipboard yet.
     *
     * @constant
     * @type Number
     */
    var CLIPBOARD_READ_DELAY = 100;

    /**
     * Reference to the window.document object.
     *
     * @private
     * @type HTMLDocument
     */
    var document = $window.document;

    /**
     * The textarea that will be used to hold the local clipboard contents.
     *
     * @type Element
     */
    var clipboardContent = document.createElement('div');

    // Ensure clipboard target is selectable but not visible
    clipboardContent.setAttribute('contenteditable', 'true');
    clipboardContent.className = 'clipboard-service-target';

    // Add clipboard target to DOM
    document.body.appendChild(clipboardContent);

    /**
     * Stops the propogation of the given event through the DOM tree. This is
     * identical to invoking stopPropogation() on the event directly, except
     * that this function is usable as an event handler itself.
     *
     * @param {Event} e
     *     The event whose propogation through the DOM tree should be stopped.
     */
    var stopEventPropagation = function stopEventPropagation(e) {
        e.stopPropagation();
    };

    // Prevent events generated due to execCommand() from disturbing external things
    clipboardContent.addEventListener('copy',  stopEventPropagation);
    clipboardContent.addEventListener('paste', stopEventPropagation);

    /**
     * A stack of past node selection ranges. A range convering the nodes
     * currently selected within the document can be pushed onto this stack
     * with pushSelection(), and the most recently pushed selection can be
     * popped off the stack (and thus re-selected) with popSelection().
     *
     * @type Range[]
     */
    var selectionStack = [];

    /**
     * Pushes the current selection range to the selection stack such that it
     * can later be restored with popSelection().
     */
    var pushSelection = function pushSelection() {

        // Add a range representing the current selection to the stack
        var selection = $window.getSelection();
        if (selection.getRangeAt && selection.rangeCount)
            selectionStack.push(selection.getRangeAt(0));

    };

    /**
     * Pops a selection range off the selection stack restoring the document's
     * previous selection state. The selection range will be the most recent
     * selection range pushed by pushSelection(). If there are no selection
     * ranges currently on the stack, this function has no effect.
     */
    var popSelection = function popSelection() {

        // Pull one selection range from the stack
        var range = selectionStack.pop();
        if (!range)
            return;

        // Replace any current selection with the retrieved selection
        var selection = $window.getSelection();
        selection.removeAllRanges();
        selection.addRange(range);

    };

    /**
     * Selects all nodes within the given element. This will replace the
     * current selection with a new selection range that covers the element's
     * contents. If the original selection should be preserved, use
     * pushSelection() and popSelection().
     *
     * @param {Element} element
     *     The element whose contents should be selected.
     */
    var selectAll = function selectAll(element) {

        // Generate a range which selects all nodes within the given element
        var range = document.createRange();
        range.selectNodeContents(element);

        // Replace any current selection with the generated range
        var selection = $window.getSelection();
        selection.removeAllRanges();
        selection.addRange(range);

    };

    /**
     * Modifies the contents of the given element such that it contains only
     * plain text. All non-text child elements will be stripped and replaced
     * with their text equivalents. As this function performs the conversion
     * through incremental changes only, cursor position within the given
     * element is preserved.
     *
     * @param {Element} element
     *     The elements whose contents should be converted to plain text.
     */
    var convertToText = function convertToText(element) {

        // For each child of the given element
        var current = element.firstChild;
        while (current) {

            // Preserve the next child in the list, in case the current
            // node is replaced
            var next = current.nextSibling;

            // If the child is not already a text node, replace it with its
            // own text contents
            if (current.nodeType !== Node.TEXT_NODE) {
                var textNode = document.createTextNode(current.textContent);
                current.parentElement.replaceChild(textNode, current);
            }

            // Advance to next child
            current = next;

        }

    };

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
        pushSelection();

        // Copy the given value into the clipboard DOM element
        if (typeof data.data === 'string')
            clipboardContent.textContent = data.data;
        else {
            clipboardContent.innerHTML = '';
            var img = document.createElement('img');
            img.src = URL.createObjectURL(data.data);
            clipboardContent.appendChild(img);
        }

        // Select all data within the clipboard target
        selectAll(clipboardContent);

        // Attempt to copy data from clipboard element into local clipboard
        if (document.execCommand('copy'))
            deferred.resolve();
        else
            deferred.reject();

        // Unfocus the clipboard DOM event to avoid mobile keyboard opening,
        // restoring whichever element was originally focused
        clipboardContent.blur();
        originalElement.focus();
        popSelection();

        return deferred.promise;
    };

    /**
     * Parses the given data URL, returning its decoded contents as a new Blob.
     * If the URL is not a valid data URL, null will be returned instead.
     *
     * @param {String} url
     *     The data URL to parse.
     *
     * @returns {Blob}
     *     A new Blob containing the decoded contents of the data URL, or null
     *     if the URL is not a valid data URL.
     */
    service.parseDataURL = function parseDataURL(url) {

        // Parse given string as a data URL
        var result = /^data:([^;]*);base64,([a-zA-Z0-9+/]*[=]*)$/.exec(url);
        if (!result)
            return null;

        // Pull the mimetype and base64 contents of the data URL
        var type = result[1];
        var data = $window.atob(result[2]);

        // Convert the decoded binary string into a typed array
        var buffer = new Uint8Array(data.length);
        for (var i = 0; i < data.length; i++)
            buffer[i] = data.charCodeAt(i);

        // Produce a proper blob containing the data and type provided in
        // the data URL
        return new Blob([buffer], { type : type });

    };

    /**
     * Replaces the current text content of the given element with the given
     * text. To avoid affecting the position of the cursor within an editable
     * element, or firing unnecessary DOM modification events, the underlying
     * <code>textContent</code> property of the element is only touched if
     * doing so would actually change the text.
     *
     * @param {Element} element
     *     The element whose text content should be changed.
     *
     * @param {String} text
     *     The text content to assign to the given element.
     */
    service.setTextContent = function setTextContent(element, text) {

        // Strip out any non-text content while preserving cursor position
        convertToText(element);

        // Reset text content only if doing so will actually change the content
        if (element.textContent !== text)
            element.textContent = text;

    };

    /**
     * Returns the URL of the single image within the given element, if the
     * element truly contains only one child and that child is an image. If the
     * content of the element is mixed or not an image, null is returned.
     *
     * @param {Element} element
     *     The element whose image content should be retrieved.
     *
     * @returns {String}
     *     The URL of the image contained within the given element, if that
     *     element contains only a single child element which happens to be an
     *     image, or null if the content of the element is not purely an image.
     */
    service.getImageContent = function getImageContent(element) {

        // Return the source of the single child element, if it is an image
        var firstChild = element.firstChild;
        if (firstChild && firstChild.nodeName === 'IMG' && !firstChild.nextSibling)
            return firstChild.getAttribute('src');

        // Otherwise, the content of this element is not simply an image
        return null;

    };

    /**
     * Replaces the current contents of the given element with a single image
     * having the given URL. To avoid affecting the position of the cursor
     * within an editable element, or firing unnecessary DOM modification
     * events, the content of the element is only touched if doing so would
     * actually change content.
     *
     * @param {Element} element
     *     The element whose image content should be changed.
     *
     * @param {String} url
     *     The URL of the image which should be assigned as the contents of the
     *     given element.
     */
    service.setImageContent = function setImageContent(element, url) {

        // Retrieve the URL of the current image contents, if any
        var currentImage = service.getImageContent(element);

        // If the current contents are not the given image (or not an image
        // at all), reassign the contents
        if (currentImage !== url) {

            // Clear current contents
            element.innerHTML = '';

            // Add a new image as the sole contents of the element
            var img = document.createElement('img');
            img.src = url;
            element.appendChild(img);

        }

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
        $window.setTimeout(function deferredClipboardRead() {

            // Track the originally-focused element prior to changing focus
            var originalElement = document.activeElement;
            pushSelection();

            // Clear and select the clipboard DOM element
            clipboardContent.innerHTML = '';
            clipboardContent.focus();
            selectAll(clipboardContent);

            // Attempt paste local clipboard into clipboard DOM element
            if (document.activeElement === clipboardContent && document.execCommand('paste')) {

                // If the pasted data is a single image, resolve with a blob
                // containing that image
                var currentImage = service.getImageContent(clipboardContent);
                if (currentImage) {

                    // Convert the image's data URL into a blob
                    var blob = service.parseDataURL(currentImage);
                    if (blob) {
                        deferred.resolve(new ClipboardData({
                            type : blob.type,
                            data : blob
                        }));
                    }

                    // Reject if conversion fails
                    else
                        deferred.reject();

                } // end if clipboard is an image

                // Otherwise, assume the clipboard contains plain text
                else
                    deferred.resolve(new ClipboardData({
                        type : 'text/plain',
                        data : clipboardContent.textContent
                    }));

            }

            // Otherwise, reading from the clipboard has failed
            else
                deferred.reject();

            // Unfocus the clipboard DOM event to avoid mobile keyboard opening,
            // restoring whichever element was originally focused
            clipboardContent.blur();
            originalElement.focus();
            popSelection();

        }, CLIPBOARD_READ_DELAY);

        return deferred.promise;
    };

    return service;

}]);
