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

import { Directive, ElementRef, HostListener, Input } from '@angular/core';

/**
 * A directive which allows handling of drag gestures on a particular element.
 */
@Directive({
    selector: '[guacTouchDrag]',
    standalone: false
})
export class GuacTouchDragDirective {

    /**
     * Called during a drag gesture as the user's finger is placed upon
     * the element, moves, and is lifted from the element.
     *
     * @param inProgress
     *     Whether the gesture is currently in progress. This will
     *     always be true except when the gesture has ended, at which
     *     point one final call will occur with this parameter set to
     *     false.
     *
     * @param startX
     *     The X location at which the drag gesture began.
     *
     * @param startY
     *     The Y location at which the drag gesture began.
     *
     * @param currentX
     *     The current X location of the user's finger.
     *
     * @param  currentY
     *     The current Y location of the user's finger.
     *
     * @param deltaX
     *     The difference in X location relative to the start of the
     *     gesture.
     *
     * @param deltaY
     *     The difference in Y location relative to the start of the
     *     gesture.
     *
     * @return {boolean}
     *     false if the default action of the touch event should be
     *     prevented, any other value otherwise.
     */
    @Input({ required: true }) guacTouchDrag!: (inProgress: boolean,
                                                startX: number,
                                                startY: number,
                                                currentX: number,
                                                currentY: number,
                                                deltaX: number,
                                                deltaY: number) => boolean;

    /**
     * Whether a drag gesture is in progress.
     */
    inProgress = false;

    /**
     * The starting X location of the drag gesture.
     */
    startX?: number = undefined;

    /**
     * The starting Y location of the drag gesture.
     */
    startY?: number = undefined;

    /**
     * The current X location of the drag gesture.
     */
    currentX?: number = undefined;

    /**
     * The current Y location of the drag gesture.
     */
    currentY?: number = undefined;

    /**
     * The change in X relative to drag start.
     */
    deltaX = 0;

    /**
     * The change in X relative to drag start.
     */
    deltaY = 0;

    /**
     * The element which will register the drag gesture.
     */
    element: Element;

    constructor(private el: ElementRef) {
        this.element = el.nativeElement;
    }

    // When there is exactly one touch, monitor the change in location
    @HostListener('touchmove', ['$event']) dragTouchMove(e: TouchEvent): void {
        if (e.touches.length === 1) {

            // Get touch location
            const x = e.touches[0].clientX;
            const y = e.touches[0].clientY;

            // Init start location and deltas if gesture is starting
            if (!this.startX || !this.startY) {
                this.startX = this.currentX = x;
                this.startY = this.currentY = y;
                this.deltaX = 0;
                this.deltaY = 0;
                this.inProgress = true;
            }

            // Update deltas if gesture is in progress
            else if (this.inProgress) {
                this.deltaX = x - (this.currentX as number);
                this.deltaY = y - (this.currentY as number);
                this.currentX = x;
                this.currentY = y;
            }

            // Signal start/change in drag gesture
            if (this.inProgress) {

                if (this.guacTouchDrag(true, this.startX, this.startY, (this.currentX as number),
                    (this.currentY as number), this.deltaX, this.deltaY) === false) {
                    e.preventDefault();
                }

            }

        }
    }

    @HostListener('touchend', ['$event']) dragTouchEnd(e: TouchEvent): void {
        if (this.startX && this.startY && e.touches.length === 0) {

            // Signal end of drag gesture
            if (this.inProgress && this.guacTouchDrag) {

                if (this.guacTouchDrag(true, this.startX, this.startY, (this.currentX as number),
                    (this.currentY as number), this.deltaX, this.deltaY) === false) {
                    e.preventDefault();
                }

            }

            this.startX = this.currentX = undefined;
            this.startY = this.currentY = undefined;
            this.deltaX = 0;
            this.deltaY = 0;
            this.inProgress = false;

        }
    }

}
