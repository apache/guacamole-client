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

import { Directive, ElementRef, EventEmitter, Inject, Output } from '@angular/core';
import { DOCUMENT } from '@angular/common';

/**
 * A directive which calls a given callback when its associated element is
 * resized. This will modify the internal DOM tree of the associated element,
 * and the associated element MUST have position (for example,
 * "position: relative").
 */
@Directive({
    selector: '[guacResize]'
})
export class GuacResizeDirective {

    /**
     * Will emit an event including the width and height of the element, in pixels whenever the associated
     * element is resized.
     */
    @Output() guacResize = new EventEmitter<{ width: number, height: number }>();

    /**
     * The element which will monitored for size changes.
     */
    element: any;

    /**
     * The resize sensor - an HTML object element.
     */
    resizeSensor: HTMLObjectElement;

    /**
     * The width of the associated element, in pixels.
     */
    lastWidth: number;

    /**
     * The height of the associated element, in pixels.
     */
    lastHeight: number;

    constructor(private el: ElementRef, @Inject(DOCUMENT) private document: Document) {
        this.element = el.nativeElement;
        this.lastWidth = this.element.offsetWidth;
        this.lastHeight = this.element.offsetHeight;

        this.resizeSensor = this.document.createElement('object');

        // Register event listener once window object exists
        this.resizeSensor.onload = () => {
            this.resizeSensor.contentDocument?.defaultView?.addEventListener('resize', () => this.checkSize());
            this.checkSize();
        };

        // Load blank contents
        this.resizeSensor.style.cssText = 'height: 100%; width: 100%; position: absolute; left: 0; top: 0; overflow: hidden; border: none; opacity: 0; z-index: -1;';
        this.resizeSensor.type = 'text/html';
        this.resizeSensor.innerHTML = `
        <!DOCTYPE html>
        <html>
            <head>
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
                <title>_</title>
            </head>
            <body></body>
        </html>
        `;

        // Add resize sensor to associated element
        this.element.insertBefore(this.resizeSensor, this.element.firstChild);
    }

    /**
     * Checks whether the size of the associated element has changed
     * and, if so, calls the resize callback with the new width and
     * height as parameters.
     */
    checkSize(): void {
        // Call callback only if size actually changed
        if (this.element.offsetWidth !== this.lastWidth
            || this.element.offsetHeight !== this.lastHeight) {

            // Call resize callback, if defined
            this.guacResize.emit({width: this.element.offsetWidth, height: this.element.offsetHeight});

            // Update stored size
            this.lastWidth = this.element.offsetWidth;
            this.lastHeight = this.element.offsetHeight;

        }

    }

}
