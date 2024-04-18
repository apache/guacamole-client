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
import { ChangeDetectorRef, Directive, ElementRef, Inject, Input, OnInit } from '@angular/core';

/**
 * A directive which allows files to be uploaded. Clicking on the associated
 * element will result in a file selector dialog, which then calls the provided
 * callback function with any chosen files.
 */
@Directive({
    selector: '[guacUpload]'
})
export class GuacUploadDirective implements OnInit {

    /**
     * The function to call whenever files are chosen. The callback is
     * provided a single parameter: the FileList containing all chosen
     * files.
     */
    @Input({ required: true }) guacUpload!: (files: FileList) => void;

    /**
     * Whether upload of multiple files should be allowed. If false, the
     * file dialog will only allow a single file to be chosen at once,
     * otherwise any number of files may be chosen. Defaults to true if
     * not set.
     */
    @Input() guacMultiple = true;

    /**
     * The element which will register the click.
     */
    element: Element;

    /**
     * Internal form, containing a single file input element.
     */
    readonly form: HTMLFormElement;

    /**
     * Internal file input element.
     */
    readonly input: HTMLInputElement;

    constructor(private el: ElementRef,
                @Inject(DOCUMENT) private document: Document,
                private cdr: ChangeDetectorRef) {
        this.element = el.nativeElement;
        this.form = this.document.createElement('form');
        this.input = this.document.createElement('input');
    }

    ngOnInit(): void {
        // Init input element
        this.input.type = 'file';
        this.input.multiple = this.guacMultiple;

        // Add input element to internal form
        this.form.appendChild(this.input);

        // Notify of any chosen files
        this.input.addEventListener('change', () => {

            // Only set chosen files selection is not canceled
            if (this.input.files && this.input.files.length > 0)
                this.guacUpload(this.input.files);

            // Reset selection
            this.form.reset();
            this.cdr.detectChanges();
        });

        // Open file chooser when element is clicked
        this.element.addEventListener('click', () => {
            this.input.click();
        });
    }

}
