

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

import {
    Directive,
    ElementRef,
    EventEmitter,
    HostListener,
    Input,
    OnChanges,
    OnInit,
    Output,
    Renderer2,
    SimpleChanges
} from '@angular/core';
import { SortOrder } from '../types/SortOrder';

/**
 * Updates the priority of the sorting property given by "guacSortProperty"
 * within the SortOrder object given by "guacSortOrder". The CSS classes
 * "sort-primary" and "sort-descending" will be applied to the associated
 * element depending on the priority and sort direction of the given property.
 *
 * The associated element will automatically be assigned the "sortable" CSS
 * class.
 */
@Directive({
    selector: '[guacSortOrder]'
})
export class GuacSortOrderDirective implements OnInit, OnChanges {

    /**
     * The object defining the sorting order.
     */
    @Input({ required: true }) guacSortOrder: SortOrder | null = null;

    /**
     * Event raised when the sort order changes due to user interaction.
     */
    @Output() sortOrderChange = new EventEmitter<SortOrder>();

    /**
     * The name of the property whose priority within the sort order
     * is controlled by this directive.
     */
    @Input() sortProperty!: string;

    /**
     * Update sort order when clicked.
     */
    @HostListener('click') onClick() {

        if (!this.guacSortOrder) return;

        this.guacSortOrder.togglePrimary(this.sortProperty);
        this.onSortOrderChange();

        // Emit the new sort order
        this.sortOrderChange.emit(new SortOrder(this.guacSortOrder.predicate));
    }

    /**
     * The element associated with this directive.
     */
    private readonly element: Element

    /**
     * Inject required services and references.
     */
    constructor(elementRef: ElementRef, private renderer: Renderer2) {
        this.element = elementRef.nativeElement;
    }

    /**
     * Assign "sortable" class to associated element
     */
    ngOnInit(): void {
        this.renderer.addClass(this.element, 'sortable');
    }

    /**
     * Returns whether the sort property defined via the
     * "guacSortProperty" attribute is the primary sort property of
     * the associated sort order.
     *
     * @returns
     *     true if the sort property defined via the
     *     "guacSortProperty" attribute is the primary sort property,
     *     false otherwise.
     */
    private isPrimary(): boolean {
        return this.guacSortOrder?.primary === this.sortProperty;
    }

    /**
     * Returns whether the primary property of the sort order is
     * sorted in descending order.
     *
     * @returns
     *     true if the primary property of the sort order is sorted in
     *     descending order, false otherwise.
     */
    private isDescending(): boolean {
        return !!this.guacSortOrder?.descending;
    }

    ngOnChanges(changes: SimpleChanges): void {

        if (changes['guacSortOrder']) {
            this.onSortOrderChange();
        }

    }

    onSortOrderChange(): void {
        // Add/remove "sort-primary" class depending on sort order
        const primary = this.isPrimary();

        if (primary)
            this.renderer.addClass(this.element, 'sort-primary');
        else
            this.renderer.removeClass(this.element, 'sort-primary');

        // Add/remove "sort-descending" class depending on sort order
        const descending = this.isDescending();

        if (descending)
            this.renderer.addClass(this.element, 'sort-descending');
        else
            this.renderer.removeClass(this.element, 'sort-descending');

    }


}
