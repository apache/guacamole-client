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

import { DOCUMENT } from '@angular/common';
import { Component, ElementRef, Inject, Input, OnInit, ViewEncapsulation } from '@angular/core';

@Component({
    selector: 'guac-menu',
    templateUrl: './guac-menu.component.html',
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class GuacMenuComponent implements OnInit {

    /**
     * The string which should be rendered as the menu title.
     */
    @Input() menuTitle = '';

    /**
     * Whether the menu should remain open while the user interacts
     * with the contents of the menu. By default, the menu will close
     * if the user clicks within the menu contents.
     */
    @Input() interactive = false;

    /**
     * The outermost element of the guacMenu directive.
     */
    element?: Element;

    /**
     * Whether the contents of the menu are currently shown.
     */
    menuShown = false;

    constructor(@Inject(DOCUMENT) private document: Document, private elementRef: ElementRef) {
        this.element = elementRef.nativeElement;
    }

    ngOnInit(): void {

        // Close menu when user clicks anywhere outside this specific menu
        this.document.body.addEventListener('click', (e: MouseEvent) => {

            if (e.target !== this.element && !this.element?.contains(e.target as HTMLElement))
                this.menuShown = false;

        }, false);

    }


    /**
     * Toggles visibility of the menu contents.
     */
    toggleMenu(): void {
        this.menuShown = !this.menuShown;
    }

    /**
     *  Prevent clicks within menu contents from toggling menu visibility
     *  if the menu contents are intended to be interactive.
     */
    clickInsideMenuContents(e: Event): void {
        if (this.interactive)
            e.stopPropagation();
    }

}
