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
 * A directive which allows handling of pinch gestures (pinch-to-zoom, for
 * example) on a particular element.
 */
@Directive({
    selector: '[guacTouchPinch]'
})
export class GuacTouchPinchDirective {

    /**
     * Called when a pinch gesture begins, changes, or ends.
     *
     * @param inProgress
     *     Whether the gesture is currently in progress. This will
     *     always be true except when the gesture has ended, at which
     *     point one final call will occur with this parameter set to
     *     false.
     *
     * @param startLength
     *     The initial distance between the two touches of the
     *     pinch gesture, in pixels.
     *
     * @param currentLength
     *     The current distance between the two touches of the
     *     pinch gesture, in pixels.
     *
     * @param centerX
     *     The current X coordinate of the center of the pinch gesture.
     *
     * @param centerY
     *     The current Y coordinate of the center of the pinch gesture.
     *
     * @return {boolean}
     *     false if the default action of the touch event should be
     *     prevented, any other value otherwise.
     */
    @Input({required: true}) guacTouchPinch!: (inProgress: boolean,
                                               startLength: number,
                                               currentLength: number,
                                               centerX: number,
                                               centerY: number) => boolean;

    /**
     * The starting pinch distance, or null if the gesture has not yet
     * started.
     */
    startLength?: number = undefined;

    /**
     * The current pinch distance, or null if the gesture has not yet
     * started.
     */
    currentLength?: number = undefined;

    /**
     * The X coordinate of the current center of the pinch gesture.
     */
    centerX: number = 0;

    /**
     * The Y coordinate of the current center of the pinch gesture.
     */
    centerY: number = 0;

    /**
     * The element which will register the pinch gesture.
     */
    element: Element;

    constructor(private el: ElementRef) {
        this.element = el.nativeElement;
    }

    // When there are exactly two touches, monitor the distance between
    // them, firing zoom events as appropriate
    @HostListener('touchmove', ['$event']) pinchTouchMove(e: TouchEvent) {
        if (e.touches.length === 2) {

            // Calculate current zoom level
            this.currentLength = this.pinchDistance(e);

            // Calculate center
            this.centerX = this.pinchCenterX(e);
            this.centerY = this.pinchCenterY(e);

            // Init start length if pinch is not in progress
            if (!this.startLength)
                this.startLength = this.currentLength;

            // Notify of pinch status
            if (this.guacTouchPinch) {
                if (this.guacTouchPinch(true, this.startLength, this.currentLength,
                    this.centerX, this.centerY) === false)
                    e.preventDefault();
            }

        }
    }

    // Reset monitoring and fire end event when done
    @HostListener('touchend', ['$event']) pinchTouchEnd(e: TouchEvent) {
        if (this.startLength && e.touches.length < 2) {

            // Notify of pinch end
            if (this.guacTouchPinch) {
                if (this.guacTouchPinch(false, this.startLength, (this.currentLength as number),
                    this.centerX, this.centerY) === false) {
                    e.preventDefault();
                }
            }

            this.startLength = undefined;

        }

    }

    /**
     * Given a touch event, calculates the distance between the first
     * two touches in pixels.
     *
     * @param e
     *     The touch event to use when performing distance calculation.
     *
     * @return
     *     The distance in pixels between the first two touches.
     */
    pinchDistance(e: TouchEvent): number {

        const touchA = e.touches[0];
        const touchB = e.touches[1];

        const deltaX = touchA.clientX - touchB.clientX;
        const deltaY = touchA.clientY - touchB.clientY;

        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);

    }

    /**
     * Given a touch event, calculates the center between the first two
     * touches in pixels, returning the X coordinate of this center.
     *
     * @param e
     *     The touch event to use when performing center calculation.
     *
     * @return
     *     The X coordinate of the center of the first two touches.
     */
    pinchCenterX(e: TouchEvent): number {

        var touchA = e.touches[0];
        var touchB = e.touches[1];

        return (touchA.clientX + touchB.clientX) / 2;

    };

    /**
     * Given a touch event, calculates the center between the first two
     * touches in pixels, returning the Y coordinate of this center.
     *
     * @param e
     *     The touch event to use when performing center calculation.
     *
     * @return
     *     The Y coordinate of the center of the first two touches.
     */
    pinchCenterY(e: TouchEvent): number {

        var touchA = e.touches[0];
        var touchB = e.touches[1];

        return (touchA.clientY + touchB.clientY) / 2;

    };


}
