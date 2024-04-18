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

import { Directive, ElementRef, Input, OnChanges, SimpleChanges } from '@angular/core';
import { ScrollState } from '../types/ScrollState';

/**
 * A directive which allows elements to be manually scrolled, and for their
 * scroll state to be observed.
 */
@Directive({
    selector: '[guacScroll]'
})
export class GuacScrollDirective implements OnChanges {

    /**
     * The current scroll state of the element.
     */
    @Input({ required: false }) guacScroll!: ScrollState;

    /**
     * The element which is being scrolled, or monitored for changes
     * in scroll.
     */
    element: Element;

    constructor(el: ElementRef) {
        this.element = el.nativeElement;
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['guacScroll']) {
            const currentValue = changes['guacScroll'].currentValue as ScrollState;
            const previousValue = changes['guacScroll'].previousValue as ScrollState | undefined;

            // Update underlying scrollLeft property when left changes
            if (currentValue.left !== previousValue?.left) {
                this.element.scrollLeft = currentValue.left;
                this.guacScroll.left = this.element.scrollLeft;
            }

            // Update underlying scrollTop property when top changes
            if (currentValue.top !== previousValue?.top) {
                this.element.scrollTop = currentValue.top;
                this.guacScroll.top = this.element.scrollTop;
            }
        }
    }
}
