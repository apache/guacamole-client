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

import { Directive, ElementRef, Input } from '@angular/core';

/**
 * A directive which allows multiple files to be uploaded. Dragging files onto
 * the associated element will call the provided callback function with any
 * dragged files.
 */
@Directive({
    selector: '[guacDrop]',
    standalone: false
})
export class GuacDropDirective {

    /**
     * The function to call whenever files are dragged. The callback is
     * provided a single parameter: the FileList containing all dragged
     * files.
     */
    @Input({ required: true }) guacDrop!: (files: FileList) => void;

    /**
     * Any number of space-seperated classes to be applied to the
     * element a drop is pending: when the user has dragged something
     * over the element, but not yet dropped. These classes will be
     * removed when a drop is not pending.
     */
    @Input() guacDraggedClass: string | undefined;


    /**
     * Whether upload of multiple files should be allowed. If false,
     * guacDropError callback will be called with a message explaining
     * the restriction, otherwise any number of files may be dragged.
     * Defaults to true if not set.
     */
    @Input() guacMultiple: boolean = true;

    /**
     * The function to call whenever an error occurs during the drop
     * operation. The callback is provided a single parameter: the translation
     * key of the error message to display.
     */
    @Input() guacDropError: (errorTextKey: string) => void = () => {
    };

    /**
     * The element which will register drag event.
     */
    private element: HTMLElement;

    constructor(el: ElementRef) {
        this.element = el.nativeElement;

        // Add listeners to the drop target to ensure that the visual state
        // stays up to date
        this.element.addEventListener('dragenter', this.notifyDragStart.bind(this));
        this.element.addEventListener('dragover', this.notifyDragStart.bind(this));
        this.element.addEventListener('dragleave', this.notifyDragEnd.bind(this));

        /**
         * Event listener that will be invoked if the user drops anything
         * onto the event. If a valid file is provided, the onFile callback
         * provided to this directive will be called; otherwise the guacDropError
         * callback will be called, if appropriate.
         *
         * @param e
         *     The drop event that triggered this handler.
         */
        this.element.addEventListener('drop', (e: DragEvent) => {

            this.notifyDragEnd(e);

            const files: FileList = e.dataTransfer?.files ?? new FileList();

            // Ignore any non-files that are dragged into the drop area
            if (files.length < 1)
                return;

            // If multi-file upload is disabled, If more than one file was
            // provided, print an error explaining the problem
            if (!this.guacMultiple && files.length >= 2) {

                this.guacDropError('APP.ERROR_SINGLE_FILE_ONLY');

                return;

            }

            // Invoke the callback with the files. Note that if guacMultiple
            // is set to false, this will always be a single file.
            this.guacDrop(files);

        });

    }

    /**
     * Applies any classes provided in the guacDraggedClass attribute.
     * Further propagation and default behavior of the given event is
     * automatically prevented.
     *
     * @param e
     *     The event related to the in-progress drag/drop operation.
     */
    private notifyDragStart(e: Event): void {

        e.preventDefault();
        e.stopPropagation();

        // Skip further processing if no classes were provided
        if (!this.guacDraggedClass)
            return;

        // Add each provided class
        this.guacDraggedClass.split(' ').forEach(classToApply =>
            this.element.classList.add(classToApply));

    }

    /**
     * Removes any classes provided in the guacDraggedClass attribute.
     * Further propagation and default behavior of the given event is
     * automatically prevented.
     *
     * @param e
     *     The event related to the end of the drag/drop operation.
     */
    private notifyDragEnd(e: Event): void {

        e.preventDefault();
        e.stopPropagation();

        // Skip further processing if no classes were provided
        if (!this.guacDraggedClass)
            return;

        // Remove each provided class
        this.guacDraggedClass.split(' ').forEach(classToRemove =>
            this.element.classList.remove(classToRemove));

    }

}
