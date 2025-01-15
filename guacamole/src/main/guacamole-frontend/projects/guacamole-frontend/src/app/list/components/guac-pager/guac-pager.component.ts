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

import { Component, Input, OnChanges, SimpleChanges, ViewEncapsulation } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

/**
 * The default size of a page, if not provided via the pageSize
 * attribute.
 */
const DEFAULT_PAGE_SIZE = 10;

/**
 * The default maximum number of page choices to provide, if a
 * value is not provided via the pageCount attribute.
 */
const DEFAULT_PAGE_COUNT = 11;

/**
 * Event data emitted by GuacPagerComponent when the user selects a
 * new page.
 */
export interface PagerEvent {

    /**
     * The index of the page that is currently selected.
     */
    pageIndex: number;

    /**
     * The current page size.
     */
    pageSize: number;

    /**
     * The index of the page that was previously selected.
     */
    previousPageIndex: number;
}

/**
 * A component which provides pagination controls, along with a paginated
 * subset of the elements of some given array.
 */
@Component({
    selector: 'guac-pager',
    templateUrl: './guac-pager.component.html',
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class GuacPagerComponent implements OnChanges {

    /**
     * Behavior subject which emits information about the current page
     * whenever the user selects a new page.
     */
    page: BehaviorSubject<PagerEvent> = new BehaviorSubject<PagerEvent>({
        pageIndex        : 0,
        pageSize         : 0,
        previousPageIndex: 0
    });

    /**
     * The maximum number of items per page.
     *
     * @default {@link DEFAULT_PAGE_SIZE}
     */
    @Input() pageSize: number | null | undefined = DEFAULT_PAGE_SIZE;

    /**
     * The maximum number of page choices to provide, regardless of the
     * total number of pages.
     *
     * @default {@link DEFAULT_PAGE_COUNT}
     */
    @Input() pageCount: number = DEFAULT_PAGE_COUNT;

    /**
     * The length of the array to paginate.
     */
    @Input({ required: true }) length!: number | null;

    /**
     * The number of the first selectable page.
     */
    firstPage = 1;

    /**
     * The number of the page immediately before the currently-selected
     * page.
     */
    previousPage = 1;

    /**
     * The number of the currently-selected page.
     */
    currentPage = 1;

    /**
     * The number of the page immediately after the currently-selected
     * page.
     */
    nextPage = 1;

    /**
     * The number of the last selectable page.
     */
    lastPage = 1;

    /**
     * An array of relevant page numbers that the user may want to jump
     * to directly.
     */
    pageNumbers: number[] = [];

    /**
     * Updates the displayed page number choices.
     */
    private updatePageNumbers(): void {

        // Get page count
        const pageCount = this.pageCount || DEFAULT_PAGE_COUNT;

        // Determine start/end of page window
        let windowStart = this.currentPage - (pageCount - 1) / 2;
        let windowEnd = windowStart + pageCount - 1;

        // Shift window as necessary if it extends beyond the first page
        if (windowStart < this.firstPage) {
            windowEnd = Math.min(this.lastPage, windowEnd - windowStart + this.firstPage);
            windowStart = this.firstPage;
        }

        // Shift window as necessary if it extends beyond the last page
        else if (windowEnd > this.lastPage) {
            windowStart = Math.max(1, windowStart - windowEnd + this.lastPage);
            windowEnd = this.lastPage;
        }

        // Generate list of relevant page numbers
        this.pageNumbers = [];
        for (let pageNumber = windowStart; pageNumber <= windowEnd; pageNumber++)
            this.pageNumbers.push(pageNumber);

    }

    /**
     * Iterates through the bound items array, splitting it into pages
     * based on the current page size.
     */
    private updatePages(): void {

        // Get current items and page size
        const length = this.length || 0;
        const pageSize = this.pageSize || DEFAULT_PAGE_SIZE;

        // Update minimum and maximum values
        this.firstPage = 1;
        this.lastPage = Math.max(1, Math.ceil(length / pageSize));

        // Select an appropriate page
        const adjustedCurrentPage = Math.min(this.lastPage, Math.max(this.firstPage, this.currentPage));
        this.selectPage(adjustedCurrentPage);

    }

    /**
     * Selects the page having the given number, assigning that page to
     * the property bound to the page attribute. If no such page
     * exists, the property will be set to undefined instead. Valid
     * page numbers begin at 1.
     *
     * @param page
     *     The number of the page to select. Valid page numbers begin
     *     at 1.
     */
    selectPage(page: number): void {

        // Select the chosen page
        this.currentPage = page;
        this.page.next({
            pageIndex        : page - 1,
            pageSize         : this.pageSize || DEFAULT_PAGE_SIZE,
            previousPageIndex: this.previousPage
        });

        // Update next/previous page numbers
        this.nextPage = Math.min(this.lastPage, this.currentPage + 1);
        this.previousPage = Math.max(this.firstPage, this.currentPage - 1);

        // Update which page numbers are shown
        this.updatePageNumbers();

    }

    /**
     * Returns whether the given page number can be legally selected
     * via selectPage(), resulting in a different page being shown.
     *
     * @param page
     *     The page number to check.
     *
     * @returns
     *     true if the page having the given number can be selected,
     *     false otherwise.
     */
    canSelectPage(page: number): boolean {
        return page !== this.currentPage
            && page >= this.firstPage
            && page <= this.lastPage;
    }

    /**
     * Returns whether the page having the given number is currently
     * selected.
     *
     * @param page
     *     The page number to check.
     *
     * @returns
     *     true if the page having the given number is currently
     *     selected, false otherwise.
     */
    isSelected(page: number): boolean {
        return page === this.currentPage;
    }

    /**
     * Returns whether pages exist before the first page listed in the
     * pageNumbers array.
     *
     * @returns
     *     true if pages exist before the first page listed in the
     *     pageNumbers array, false otherwise.
     */
    hasMorePagesBefore(): boolean {
        const firstPageNumber = this.pageNumbers[0];
        return firstPageNumber !== this.firstPage;
    }

    /**
     * Returns whether pages exist after the last page listed in the
     * pageNumbers array.
     *
     * @returns
     *     true if pages exist after the last page listed in the
     *     pageNumbers array, false otherwise.
     */
    hasMorePagesAfter(): boolean {
        const lastPageNumber = this.pageNumbers[this.pageNumbers.length - 1];
        return lastPageNumber !== this.lastPage;
    }

    ngOnChanges(changes: SimpleChanges): void {

        // Use default page size if none is specified
        if (changes['pageSize'])
            this.pageSize = this.pageSize || DEFAULT_PAGE_SIZE;


        // Update available pages when available items or page count is changed
        if (changes['length'] || changes['pageSize'])
            this.updatePages();


        // Update available page numbers when page count is changed
        if (changes['length'] || changes['pageCount'])
            this.updatePageNumbers();


    }

}
