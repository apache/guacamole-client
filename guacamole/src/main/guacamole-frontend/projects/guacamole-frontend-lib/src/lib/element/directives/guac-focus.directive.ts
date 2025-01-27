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

import { Directive, ElementRef, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';

/**
 * A directive which allows elements to be manually focused / blurred.
 */
@Directive({
    selector: '[guacFocus]',
    standalone: false
})
export class GuacFocusDirective implements OnInit, OnChanges {

    /**
     * Whether the element associated with this directive should be
     * focussed.
     */
    @Input() guacFocus?: boolean;

    /**
     * The element which will be focused / blurred.
     */
    element: any;

    constructor(private el: ElementRef) {
        this.element = el.nativeElement;
    }

    ngOnInit() {
        this.updateFocus();
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['guacFocus'])
            this.updateFocus();
    }

    /**
     * Set/unset focus depending on value of guacFocus.
     */
    updateFocus() {
        if (this.guacFocus) {
            this.element.focus();
        } else {
            this.element.blur();
        }
    }

}
