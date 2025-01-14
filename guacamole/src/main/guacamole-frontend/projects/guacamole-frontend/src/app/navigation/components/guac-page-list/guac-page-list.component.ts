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

import { Component, Input, OnChanges, OnInit, SimpleChanges, ViewEncapsulation } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { isArray } from '../../../util/is-array';
import { PageDefinition } from '../../types/PageDefinition';

/**
 * A directive which provides a list of links to specific pages.
 */
@Component({
    selector     : 'guac-page-list',
    templateUrl  : './guac-page-list.component.html',
    encapsulation: ViewEncapsulation.None
})
export class GuacPageListComponent implements OnInit, OnChanges {

    /**
     * The array of pages to display.
     */
    @Input() pages: PageDefinition[] = [];

    /**
     * The URL of the currently-displayed page.
     */
    currentURL = '';

    /**
     * The names associated with the current page, if the current page
     * is known. The value of this property corresponds to the value of
     * PageDefinition.name. Though PageDefinition.name may be a String,
     * this will always be an Array.
     */
    currentPageName: string[] = [];

    /**
     * Array of each level of the page list, where a level is defined
     * by a mapping of names (translation strings) to the
     * PageDefinitions corresponding to those names.
     */
    levels: Record<string, PageDefinition>[] = [];

    /**
     * Inject required services.
     */
    constructor(private router: Router, private route: ActivatedRoute) {
    }

    ngOnInit(): void {

        // Set the currentURL initially
        this.currentURL = this.router.url;

        // Updates the currentURL when the route changes.
        this.router.events
            .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
            .subscribe((event) => {
                this.currentURL = event.url;
            });
    }


    /**
     * Returns the names associated with the given page, in
     * hierarchical order. If the page is only associated with a single
     * name, and that name is not stored as an array, it will be still
     * be returned as an array containing a single item.
     *
     * @param page
     *     The page to return the names of.
     *
     * @return
     *     An array of all names associated with the given page, in
     *     hierarchical order.
     */
    getPageNames(page: PageDefinition): string[] {

        // If already an array, simply return the name
        if (isArray(page.name))
            return page.name;

        // Otherwise, transform into array
        return [page.name];

    }

    /**
     * Adds the given PageDefinition to the overall set of pages
     * displayed by this guacPageList, automatically updating the
     * available levels and the contents of those levels.
     *
     * @param page
     *     The PageDefinition to add.
     *
     * @param weight
     *     The sorting weight to use for the page if it does not
     *     already have an associated weight.
     */
    addPage(page: PageDefinition, weight = 0): void {

        // Pull all names for page
        const names = this.getPageNames(page);

        // Copy the hierarchy of this page into the displayed levels
        // as far as is relevant for the currently-displayed page
        for (let i = 0; i < names.length; i++) {

            // Create current level, if it doesn't yet exist
            let pages = this.levels[i];
            if (!pages)
                pages = this.levels[i] = {};

            // Get the name at the current level
            const name = names[i];

            // Determine whether this page definition is part of the
            // hierarchy containing the current page
            const isCurrentPage = (this.currentPageName[i] === name);

            // Store new page if it doesn't yet exist at this level
            if (!pages[name]) {
                pages[name] = new PageDefinition({
                    name     : name,
                    url      : isCurrentPage ? this.currentURL : page.url,
                    className: page.className,
                    weight   : page.weight || (weight + i)
                });
            }

            // If the name at this level no longer matches the
            // hierarchy of the current page, do not go any deeper
            if (this.currentPageName[i] !== name)
                break;

        }

    }

    /**
     * Navigate to the given page.
     *
     * @param page
     *     The page to navigate to.
     */
    navigateToPage(page: PageDefinition): void {
        this.router.navigate([page.url]);
    }

    /**
     * Tests whether the given page is the page currently being viewed.
     *
     * @param page
     *     The page to test.
     *
     * @returns
     *     true if the given page is the current page, false otherwise.
     */
    isCurrentPage(page: PageDefinition): boolean {
        return this.currentURL === page.url;
    }

    /**
     * Given an arbitrary map of PageDefinitions, returns an array of
     * those PageDefinitions, sorted by weight.
     *
     * @param level
     *     A map of PageDefinitions with arbitrary keys. The value of
     *     each key is ignored.
     *
     * @returns
     *     An array of all PageDefinitions in the given map, sorted by
     *     weight.
     */
    getPages(level: Record<any, PageDefinition>): PageDefinition[] {

        const pages: PageDefinition[] = [];

        // Convert contents of level to a flat array of pages
        for (const key in level) {
            const page = level[key];
            pages.push(page);

        }

        // Sort page array by weight
        pages.sort(function comparePages(a, b) {
            return (a.weight || 0) - (b.weight || 0);
        });

        return pages;

    }

    ngOnChanges(changes: SimpleChanges): void {
        // Update page levels whenever pages changes
        if (changes['pages']) {
            const pages: PageDefinition[] = changes['pages'].currentValue;

            // Determine current page name
            this.currentPageName = [];
            for (const page of pages) {

                // If page is current page, store its names
                if (this.isCurrentPage(page))
                    this.currentPageName = this.getPageNames(page);
            }


            // Reset contents of levels
            this.levels = [];

            // Add all page definitions
            pages.forEach((page) => this.addPage(page))

            // Filter to only relevant levels
            this.levels = this.levels.filter(function isRelevant(level) {

                // Determine relevancy by counting the number of pages
                let pageCount = 0;
                for (const name in level) {

                    // Level is relevant if it has two or more pages
                    if (++pageCount === 2)
                        return true;

                }

                // Otherwise, the level is not relevant
                return false;

            });
        }
    }

    trackByIndex(index: number): number {
        return index;
    }

    /**
     * Make isArray() available to the template to render page.name accordingly.
     */
    protected readonly isArray = isArray;
}
