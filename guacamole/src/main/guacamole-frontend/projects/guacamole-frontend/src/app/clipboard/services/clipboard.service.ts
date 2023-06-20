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

import { Inject, Injectable } from '@angular/core';
import { GuacEventService } from 'guacamole-frontend-lib';
import {
    GuacFrontendEventArguments
} from '../../events/types/GuacFrontendEventArguments';
import { ClipboardData } from '../types/ClipboardData';
import { DOCUMENT } from '@angular/common';
import { SessionStorageFactory } from '../../storage/session-storage-factory.service';

/**
 * The amount of time to wait before actually serving a request to read
 * clipboard data, in milliseconds. Providing a reasonable delay between
 * request and read attempt allows the cut/copy operation to settle, in
 * case the data we are anticipating to be present is not actually present
 * in the clipboard yet.
 *
 * @constant
 */
const CLIPBOARD_READ_DELAY = 100;

/**
 * A service for maintaining and accessing clipboard data. If possible, this
 * service will leverage the local clipboard. If the local clipboard is not
 * available, an internal in-memory clipboard will be used instead.
 */
@Injectable({
    providedIn: 'root'
})
export class ClipboardService {

    /**
     * Getter/setter which retrieves or sets the current stored clipboard
     * contents. The stored clipboard contents are strictly internal to
     * Guacamole, and may not reflect the local clipboard if local clipboard
     * access is unavailable.
     */
    private readonly storedClipboardData: Function = this.sessionStorageFactory.create(new ClipboardData());

    /**
     * The textarea that will be used to hold the local clipboard contents.
     */
    private readonly clipboardContentElement: HTMLTextAreaElement;

    /**
     * The promise associated with the current pending clipboard read attempt.
     * If no clipboard read is active, this will be null.
     *
     * @type Promise.<ClipboardData>
     */
    private pendingRead: Promise<ClipboardData> | null = null;

    /**
     * A stack of past node selection ranges. A range convering the nodes
     * currently selected within the document can be pushed onto this stack
     * with pushSelection(), and the most recently pushed selection can be
     * popped off the stack (and thus re-selected) with popSelection().
     *
     * @type Range[]
     */
    private selectionStack: Range[] = [];

    /**
     * Reference to the window object.
     */
    private readonly window: Window;

    constructor(@Inject(DOCUMENT) private document: Document,
                private readonly sessionStorageFactory: SessionStorageFactory,
                private guacEventService: GuacEventService<GuacFrontendEventArguments>) {
        this.window = this.document.defaultView as Window;

        this.clipboardContentElement = this.document.createElement('textarea');
        // Ensure clipboard target is selectable but not visible
        this.clipboardContentElement.className = 'clipboard-service-target';
        // Add clipboard target to DOM
        document.body.appendChild(this.clipboardContentElement);


        // Prevent events generated due to execCommand() from disturbing external things
        this.clipboardContentElement.addEventListener('cut', this.stopEventPropagation);
        this.clipboardContentElement.addEventListener('copy', this.stopEventPropagation);
        this.clipboardContentElement.addEventListener('paste', this.stopEventPropagation);
        this.clipboardContentElement.addEventListener('input', this.stopEventPropagation);
    }

    /**
     * Stops the propagation of the given event through the DOM tree. This is
     * identical to invoking stopPropagation() on the event directly, except
     * that this function is usable as an event handler itself.
     *
     * @param e
     *     The event whose propagation through the DOM tree should be stopped.
     */
    private stopEventPropagation(e: Event) {
        e.stopPropagation();
    };

    /**
     * Pushes the current selection range to the selection stack such that it
     * can later be restored with popSelection().
     */
    private pushSelection() {

        if (!this.window)
            return;

        // Add a range representing the current selection to the stack
        const selection = this.window.getSelection();

        if (!selection)
            return;

        if (selection.getRangeAt && selection.rangeCount)
            this.selectionStack.push(selection.getRangeAt(0));

    };

    /**
     * Pops a selection range off the selection stack restoring the document's
     * previous selection state. The selection range will be the most recent
     * selection range pushed by pushSelection(). If there are no selection
     * ranges currently on the stack, this function has no effect.
     */
    private popSelection() {

        // Pull one selection range from the stack
        const range = this.selectionStack.pop();
        if (!range)
            return;

        // Replace any current selection with the retrieved selection
        const selection = this.window.getSelection();
        if (!selection)
            return;
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
    private selectAll(element: Element) {

        // Use the select() function defined for input elements, if available
        if ((element as HTMLInputElement).select) {
            (element as HTMLInputElement).select();
        }

        // Fallback to manual manipulation of the selection
        else {

            // Generate a range which selects all nodes within the given element
            const range = this.document.createRange();
            range.selectNodeContents(element);

            // Replace any current selection with the generated range
            const selection = this.window.getSelection();
            if (!selection)
                return;
            selection.removeAllRanges();
            selection.addRange(range);

        }

    }

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
    private setLocalClipboard(data: ClipboardData): Promise<void> {
        return new Promise((resolve, reject) => {

            try {
                if (navigator.clipboard && navigator.clipboard.writeText) {
                    if (data.type === 'text/plain' && typeof data.data === 'string') {
                        navigator.clipboard.writeText(data.data).then(resolve).catch(reject);
                        return;
                    }
                }
            }

                // Ignore any hard failures to use Asynchronous Clipboard API, falling
                // back to traditional document.execCommand()
            catch (ignore) {
            }

            const originalElement = this.document.activeElement as HTMLElement;
            this.pushSelection();

            // Copy the given value into the clipboard DOM element
            if (typeof data.data === 'string')
                this.clipboardContentElement.value = data.data;
            else {
                this.clipboardContentElement.innerHTML = '';
                const img = document.createElement('img');
                img.src = URL.createObjectURL(data.data);
                this.clipboardContentElement.appendChild(img);
            }

            // Select all data within the clipboard target
            this.clipboardContentElement.focus();
            this.selectAll(this.clipboardContentElement);

            // Attempt to copy data from clipboard element into local clipboard
            if (document.execCommand('copy')) {
                resolve();
            } else {
                reject();
            }

            // Unfocus the clipboard DOM event to avoid mobile keyboard opening,
            // restoring whichever element was originally focused
            this.clipboardContentElement.blur();
            originalElement.focus();
            this.popSelection();
        });
    }

    /**
     * Parses the given data URL, returning its decoded contents as a new Blob.
     * If the URL is not a valid data URL, null will be returned instead.
     *
     * @param url
     *     The data URL to parse.
     *
     * @returns
     *     A new Blob containing the decoded contents of the data URL, or null
     *     if the URL is not a valid data URL.
     */
    parseDataURL(url: string): Blob | null {

        // Parse given string as a data URL
        const result = /^data:([^;]*);base64,([a-zA-Z0-9+/]*[=]*)$/.exec(url);
        if (!result)
            return null;

        // Pull the mimetype and base64 contents of the data URL
        const type = result[1];
        const data = this.window.atob(result[2]);

        // Convert the decoded binary string into a typed array
        const buffer = new Uint8Array(data.length);
        for (let i = 0; i < data.length; i++)
            buffer[i] = data.charCodeAt(i);

        // Produce a proper blob containing the data and type provided in
        // the data URL
        return new Blob([buffer], {type: type});
    }

    /**
     * Returns the content of the given element as plain, unformatted text,
     * preserving only individual characters and newlines. Formatting, images,
     * etc. are not taken into account.
     *
     * @param element
     *     The element whose text content should be returned.
     *
     * @returns
     *     The plain text contents of the given element, including newlines and
     *     spacing but otherwise without any formatting.
     */
    getTextContent(element: Element): string {

        const blocks = [];
        let currentBlock = '';

        // For each child of the given element
        // TODO: Oder doch firstChild wie vorher?
        let current = element.firstElementChild;
        while (current) {

            // Simply append the content of any text nodes
            if (current.nodeType === Node.TEXT_NODE)
                currentBlock += current.nodeValue;

            // Render <br> as a newline character
            else if (current.nodeName === 'BR')
                currentBlock += '\n';

            // Render <img> as alt text, if available
            else if (current.nodeName === 'IMG')
                currentBlock += current.getAttribute('alt') || '';

                // For all other nodes, handling depends on whether they are
            // block-level elements
            else {

                // If we are entering a new block context, start a new block if
                // the current block is non-empty
                if (currentBlock.length && this.window.getComputedStyle(current).display === 'block') {

                    // Trim trailing newline (would otherwise inflate the line count by 1)
                    if (currentBlock.substring(currentBlock.length - 1) === '\n')
                        currentBlock = currentBlock.substring(0, currentBlock.length - 1);

                    // Finish current block and start a new block
                    blocks.push(currentBlock);
                    currentBlock = '';

                }

                // Append the content of the current element to the current block
                currentBlock += this.getTextContent(current);

            }

            // TODO: Oder doch nextSibling wie vorher?
            current = current.nextElementSibling;

        }

        // Add any in-progress block
        if (currentBlock.length)
            blocks.push(currentBlock);

        // Combine all non-empty blocks, separated by newlines
        return blocks.join('\n');

    }

    /**
     * Replaces the current text content of the given element with the given
     * text. To avoid affecting the position of the cursor within an editable
     * element, or firing unnecessary DOM modification events, the underlying
     * <code>textContent</code> property of the element is only touched if
     * doing so would actually change the text.
     *
     * @param element
     *     The element whose text content should be changed.
     *
     * @param text
     *     The text content to assign to the given element.
     */
    setTextContent(element: Element, text: string) {

        // Strip out any images
        element.querySelectorAll('img').forEach(img => {
            img.parentNode?.removeChild(img);
        });


        // Reset text content only if doing so will actually change the content
        if (this.getTextContent(element) !== text)
            element.textContent = text;

    }

    /**
     * Returns the URL of the single image within the given element, if the
     * element truly contains only one child and that child is an image. If the
     * content of the element is mixed or not an image, null is returned.
     *
     * @param element
     *     The element whose image content should be retrieved.
     *
     * @returns
     *     The URL of the image contained within the given element, if that
     *     element contains only a single child element which happens to be an
     *     image, or null if the content of the element is not purely an image.
     */
    getImageContent(element: Element): string | null {

        // Return the source of the single child element, if it is an image
        // TODO: FirstElement?
        const firstChild = element.firstElementChild;
        if (firstChild && firstChild.nodeName === 'IMG' && !firstChild.nextSibling)
            return firstChild.getAttribute('src');

        // Otherwise, the content of this element is not simply an image
        return null;

    }

    /**
     * Replaces the current contents of the given element with a single image
     * having the given URL. To avoid affecting the position of the cursor
     * within an editable element, or firing unnecessary DOM modification
     * events, the content of the element is only touched if doing so would
     * actually change content.
     *
     * @param element
     *     The element whose image content should be changed.
     *
     * @param url
     *     The URL of the image which should be assigned as the contents of the
     *     given element.
     */
    setImageContent(element: Element, url: string) {

        // Retrieve the URL of the current image contents, if any
        const currentImage = this.getImageContent(element);

        // If the current contents are not the given image (or not an image
        // at all), reassign the contents
        if (currentImage !== url) {

            // Clear current contents
            element.innerHTML = '';

            // Add a new image as the sole contents of the element
            const img = document.createElement('img');
            img.src = url;
            element.appendChild(img);

        }

    }

    /**
     * Get the current value of the local clipboard.
     *
     * @return {Promise.<ClipboardData>}
     *     A promise that will resolve with the contents of the local clipboard
     *     if getting the clipboard was successful, and will reject if it
     *     failed.
     */
    private getLocalClipboard(): Promise<ClipboardData> {

        // If the clipboard is already being read, do not overlap the read
        // attempts; instead share the result across all requests
        if (this.pendingRead)
            return this.pendingRead;

        let resolvePendingRead: (data: ClipboardData) => void;
        let rejectPendingRead: (reason?: any) => void;

        this.pendingRead = new Promise<ClipboardData>((resolve, reject) => {
            resolvePendingRead = resolve;
            rejectPendingRead = reject;
        });

        try {

            // Attempt to read the clipboard using the Asynchronous Clipboard
            // API, if it's available
            if (navigator.clipboard && navigator.clipboard.readText) {

                navigator.clipboard.readText().then(text => {
                    resolvePendingRead(new ClipboardData({
                        type: 'text/plain',
                        data: text
                    }));
                }, rejectPendingRead!);

                return this.pendingRead;
            }

        }

            // Ignore any hard failures to use Asynchronous Clipboard API, falling
            // back to traditional document.execCommand()
        catch (ignore) {
        }

        // Track the originally-focused element prior to changing focus
        const originalElement = document.activeElement as HTMLElement | null;

        /**
         * Attempts to paste the clipboard contents into the
         * currently-focused element. The promise related to the current
         * attempt to read the clipboard will be resolved or rejected
         * depending on whether the attempt to paste succeeds.
         */
        const performPaste = () => {

            // Attempt paste local clipboard into clipboard DOM element
            if (document.execCommand('paste')) {

                // If the pasted data is a single image, resolve with a blob
                // containing that image
                const currentImage = this.getImageContent(this.clipboardContentElement);
                if (currentImage) {

                    // Convert the image's data URL into a blob
                    const blob = this.parseDataURL(currentImage);
                    if (blob) {
                        resolvePendingRead(new ClipboardData({
                            type: blob.type,
                            data: blob
                        }));
                    }

                    // Reject if conversion fails
                    else
                        rejectPendingRead();

                } // end if clipboard is an image

                // Otherwise, assume the clipboard contains plain text
                else
                    resolvePendingRead(new ClipboardData({
                        type: 'text/plain',
                        data: this.clipboardContentElement.value
                    }));

            }

            // Otherwise, reading from the clipboard has failed
            else
                rejectPendingRead();

        };

        // Mark read attempt as in progress, cleaning up event listener and
        // selection once the paste attempt has completed
        this.pendingRead = this.pendingRead.finally(() => {

            // Do not use future changes in focus
            this.clipboardContentElement.removeEventListener('focus', performPaste);

            // Unfocus the clipboard DOM event to avoid mobile keyboard opening,
            // restoring whichever element was originally focused
            this.clipboardContentElement.blur();
            originalElement?.focus();
            this.popSelection();

            // No read is pending any longer
            this.pendingRead = null;

        });

        // Wait for the next event queue run before attempting to read
        // clipboard data (in case the copy/cut has not yet completed)
        this.window.setTimeout(() => {

            this.pushSelection();

            // Ensure clipboard element is blurred (and that the "focus" event
            // will fire)
            this.clipboardContentElement.blur();
            this.clipboardContentElement.addEventListener('focus', performPaste);

            // Clear and select the clipboard DOM element
            this.clipboardContentElement.value = '';
            this.clipboardContentElement.focus();
            this.selectAll(this.clipboardContentElement);

            // If focus failed to be set, we cannot read the clipboard
            if (this.document.activeElement !== this.clipboardContentElement)
                rejectPendingRead();

        }, CLIPBOARD_READ_DELAY);

        return this.pendingRead!;
    }

    /**
     * Returns the current value of the internal clipboard shared across all
     * active Guacamole connections running within the current browser tab. If
     * access to the local clipboard is available, the internal clipboard is
     * first synchronized with the current local clipboard contents. If access
     * to the local clipboard is unavailable, only the internal clipboard will
     * be used.
     *
     * @return
     *     A promise that will resolve with the contents of the internal
     *     clipboard, first retrieving those contents from the local clipboard
     *     if permission to do so has been granted. This promise is always
     *     resolved.
     */
    getClipboard(): Promise<ClipboardData> {
        return this.getLocalClipboard()
            .then((data) => this.storedClipboardData(data), () => this.storedClipboardData());
    }

    /**
     * Sets the content of the internal clipboard shared across all active
     * Guacamole connections running within the current browser tab. If
     * access to the local clipboard is available, the local clipboard is
     * first set to the provided clipboard content. If access to the local
     * clipboard is unavailable, only the internal clipboard will be used. A
     * "guacClipboard" event will be broadcast with the assigned data once the
     * operation has completed.
     *
     * @param data
     *     The data to assign to the clipboard.
     *
     * @return
     *     A promise that will resolve after the clipboard content has been
     *     set. This promise is always resolved.
     */
    setClipboard(data: ClipboardData): Promise<void> {
        return this.setLocalClipboard(data).finally(() => {

            // Update internal clipboard and broadcast event notifying of
            // updated contents
            this.storedClipboardData(data);
            this.guacEventService.broadcast('guacClipboard', {data});

        });
    }

    /**
     * Resynchronizes the local and internal clipboards, setting the contents
     * of the internal clipboard to that of the local clipboard (if local
     * clipboard access is granted) and broadcasting a "guacClipboard" event
     * with the current internal clipboard contents for consumption by external
     * components like the "guacClient" directive.
     */
    resyncClipboard = () => {
        this.getLocalClipboard().then(data => this.setClipboard(data));
    }

}
