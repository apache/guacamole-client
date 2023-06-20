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

import { AfterViewInit, Component, ElementRef, Inject, OnDestroy, ViewChild, ViewEncapsulation } from '@angular/core';
import { DOCUMENT } from '@angular/common';


/**
 * A component which provides a fullscreen environment for its content.
 */
@Component({
    selector: 'guac-viewport',
    templateUrl: './viewport.component.html',
    encapsulation: ViewEncapsulation.None
})
export class ViewportComponent implements AfterViewInit, OnDestroy {

    private readonly window: Window;

    /**
     * The wrapped fullscreen container element.
     */
    @ViewChild('viewport') elementRef!: ElementRef;

    /**
     * The fullscreen container element.
     */
    element!: HTMLElement;

    /**
     * The width of the browser viewport when fitVisibleArea() was last
     * invoked, in pixels, or null if fitVisibleArea() has not yet been
     * called.
     */
    lastViewportWidth?: number = undefined;

    /**
     * The height of the browser viewport when fitVisibleArea() was
     * last invoked, in pixels, or null if fitVisibleArea() has not yet
     * been called.
     */
    lastViewportHeight?: number = undefined;
    private pollArea?: number;

    constructor(@Inject(DOCUMENT) private document: Document,) {
        this.window = this.document.defaultView as Window;
    }

    ngAfterViewInit(): void {
        this.element = this.elementRef.nativeElement;

        // Fit container within visible region when window scrolls
        this.window.addEventListener('scroll', this.fitVisibleArea);

        // Poll every 10ms, in case scroll event does not fire
        this.pollArea = this.window.setInterval(() => this.fitVisibleArea(), 10);
    }

    ngOnDestroy(): void {
        // Clean up on destruction
        this.window.removeEventListener('scroll', this.fitVisibleArea);
        this.window.clearInterval(this.pollArea);
    }

    /**
     * Resizes the container element inside the guacViewport such that
     * it exactly fits within the visible area, even if the browser has
     * been scrolled.
     */
    fitVisibleArea(): void {

        // Calculate viewport dimensions (this is NOT necessarily the
        // same as 100vw and 100vh, 100%, etc., particularly when the
        // on-screen keyboard of a mobile device pops open)
        const viewportWidth = this.window.innerWidth;
        const viewportHeight = this.window.innerHeight;

        // Adjust element width to fit exactly within visible area
        if (viewportWidth !== this.lastViewportWidth) {
            this.element.style.width = viewportWidth + 'px';
            this.lastViewportWidth = viewportWidth;
        }

        // Adjust this.element height to fit exactly within visible area
        if (viewportHeight !== this.lastViewportHeight) {
            this.element.style.height = viewportHeight + 'px';
            this.lastViewportHeight = viewportHeight;
        }

        // Scroll this.element such that its upper-left corner is exactly
        // within the viewport upper-left corner, if not already there
        if (this.element.scrollLeft || this.element.scrollTop) {
            this.window.scrollTo(
                this.window.scrollX + this.element.scrollLeft,
                this.window.scrollY + this.element.scrollTop
            );
        }

    }
}
