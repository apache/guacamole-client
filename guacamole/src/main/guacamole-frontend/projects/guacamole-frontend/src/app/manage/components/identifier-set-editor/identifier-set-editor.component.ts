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
    Component,
    Input,
    OnChanges,
    OnInit,
    SimpleChanges,
    ViewChild,
    ViewEncapsulation
} from '@angular/core';
import isEmpty from 'lodash/isEmpty';
import sortedIndex from 'lodash/sortedIndex';
import sortedIndexOf from 'lodash/sortedIndexOf';
import { FilterService } from '../../../list/services/filter.service';
import { SortService } from '../../../list/services/sort.service';
import { GuacPagerComponent } from '../../../list/components/guac-pager/guac-pager.component';
import { BehaviorSubject } from 'rxjs';
import { DataSourceBuilderService } from '../../../list/services/data-source-builder.service';
import { DataSource } from '../../../list/types/DataSource';

/**
 * A directive for manipulating a set of objects sharing some common relation
 * and represented by an array of their identifiers. The specific objects
 * added or removed are tracked within a separate pair of arrays of
 * identifiers.
 */
@Component({
    selector: 'identifier-set-editor',
    templateUrl: './identifier-set-editor.component.html',
    encapsulation: ViewEncapsulation.None,
})
export class IdentifierSetEditorComponent implements OnInit, OnChanges {

    /**
     * The translation key of the text which should be displayed within
     * the main header of the identifier set editor.
     */
    @Input({required: true}) header!: string;

    /**
     * The translation key of the text which should be displayed if no
     * identifiers are currently present within the set.
     */
    @Input({required: true}) emptyPlaceholder!: string;

    /**
     * The translation key of the text which should be displayed if no
     * identifiers are available to be added within the set.
     */
    @Input({required: true}) unavailablePlaceholder!: string;

    /**
     * All identifiers which are available to be added to or removed
     * from the identifier set being edited.
     */
    @Input() identifiersAvailable: string[] | null = null;

    /**
     * The current state of the identifier set being manipulated. This
     * array will be modified as changes are made through this
     * identifier set editor.
     */
    @Input() identifiers: string[] = [];

    /**
     * The set of identifiers that have been added, relative to the
     * initial state of the identifier set being manipulated.
     */
    @Input({required: true}) identifiersAdded!: string[];

    /**
     * The set of identifiers that have been removed, relative to the
     * initial state of the identifier set being manipulated.
     *
     * @type String[]
     */
    @Input({required: true}) identifiersRemoved!: string[];

    /**
     * Reference to the instance of the pager component.
     */
    @ViewChild(GuacPagerComponent, {static: true}) pager!: GuacPagerComponent;

    /**
     * TODO: document
     */
    identifiersAvailableDataSourceView: DataSource<string> | null = null;

    /**
     * TODO: document
     */
    identifiersDataSourceView: DataSource<string> | null = null;

    /**
     * Whether the full list of available identifiers should be displayed.
     * Initially, only an abbreviated list of identifiers currently present
     * is shown.
     */
    expanded = false;

    /**
     * Map of identifiers to boolean flags indicating whether that
     * identifier is currently present (true) or absent (false). If an
     * identifier is absent, it may also be absent from this map.
     */
    identifierFlags: Record<string, boolean> = {};

    /**
     * Map of identifiers to boolean flags indicating whether that
     * identifier is editable. If an identifier is not editable, it will be
     * absent from this map.
     */
    isEditable: Record<string, boolean> = {};

    /**
     * The string currently being used to filter the list of available
     * identifiers.
     */
    filterString: BehaviorSubject<string> = new BehaviorSubject<string>('');

    /**
     * Inject required services.
     */
    constructor(private filterService: FilterService,
                private sortService: SortService,
                private dataSourceBuilderService: DataSourceBuilderService) {
    }

    ngOnInit(): void {
        // Build the data source for the available identifiers.
        this.identifiersAvailableDataSourceView = this.dataSourceBuilderService.getBuilder<string>()
            .source(this.identifiersAvailable || [])
            .filter(this.filterString, [''])
            .paginate(this.pager.page)
            .build();

        // Build the data source for the current state of the identifier set being manipulated
        this.identifiersDataSourceView = this.dataSourceBuilderService.getBuilder<string>()
            .source(this.identifiers)
            .filter(this.filterString, [''])
            .build();
    }

    ngOnChanges(changes: SimpleChanges): void {

        // Keep identifierFlags up to date when identifiers array is replaced
        // or initially assigned
        if (changes['identifiers']) {

            // Maintain identifiers in sorted order so additions and removals
            // can be made more efficiently
            if (this.identifiers)
                this.identifiers.sort();

            // Convert array of identifiers into set of boolean
            // presence/absence flags
            const identifierFlags: Record<string, boolean> = {};
            this.identifiers.forEach(identifier => {
                identifierFlags[identifier] = true;
            });
            this.identifierFlags = identifierFlags;

            // Update the corresponding data source
            this.identifiersDataSourceView?.updateSource(this.identifiers);
        }


        if (changes['identifiersAvailable']) {

            // An identifier is editable iff it is available to be added or removed
            // from the identifier set being edited (iff it is within the
            // identifiersAvailable array)
            this.isEditable = {};
            this.identifiers.forEach(identifier => {
                this.isEditable[identifier] = true;
            });

            // Update the corresponding data source
            this.identifiersAvailableDataSourceView?.updateSource(this.identifiersAvailable || []);
        }
    }

    /**
     * Adds the given identifier to the given sorted array of identifiers,
     * preserving the sorted order of the array. If the identifier is
     * already present, no change is made to the array. The given array
     * must already be sorted in ascending order.
     *
     * @param arr
     *     The sorted array of identifiers to add the given identifier to.
     *
     * @param identifier
     *     The identifier to add to the given array.
     */
    private addIdentifier(arr: string[], identifier: string): void {

        // Determine location that the identifier should be added to
        // maintain sorted order
        const index = sortedIndex(arr, identifier);

        // Do not add if already present
        if (arr[index] === identifier)
            return;

        // Insert identifier at determined location
        arr.splice(index, 0, identifier);

    }

    /**
     * Removes the given identifier from the given sorted array of
     * identifiers, preserving the sorted order of the array. If the
     * identifier is already absent, no change is made to the array. The
     * given array must already be sorted in ascending order.
     *
     * @param arr
     *     The sorted array of identifiers to remove the given identifier
     *     from.
     *
     * @param identifier
     *     The identifier to remove from the given array.
     *
     * @returns
     *     true if the identifier was present in the given array and has
     *     been removed, false otherwise.
     */
    private removeIdentifier(arr: string[], identifier: string): boolean {

        // Search for identifier in sorted array
        const index = sortedIndexOf(arr, identifier);

        // Nothing to do if already absent
        if (index === -1)
            return false;

        // Remove identifier
        arr.splice(index, 1);
        return true;

    }

    /**
     * Notifies the controller that a change has been made to the flag
     * denoting presence/absence of a particular identifier within the
     * <code>identifierFlags</code> map. The <code>identifiers</code>,
     * <code>identifiersAdded</code>, and <code>identifiersRemoved</code>
     * arrays are updated accordingly.
     *
     * @param identifier
     *     The identifier which has been added or removed through modifying
     *     its boolean flag within <code>identifierFlags</code>.
     */
    identifierChanged(identifier: string): void {

        // Determine status of modified identifier
        const present = this.identifierFlags[identifier];

        // Add/remove identifier from added/removed sets depending on
        // change in flag state
        if (present) {

            this.addIdentifier(this.identifiers, identifier);

            if (!this.removeIdentifier(this.identifiersRemoved, identifier))
                this.addIdentifier(this.identifiersAdded, identifier);

        } else {

            this.removeIdentifier(this.identifiers, identifier);

            if (!this.removeIdentifier(this.identifiersAdded, identifier))
                this.addIdentifier(this.identifiersRemoved, identifier);

        }

        // Update the corresponding signal
        this.identifiersDataSourceView?.updateSource(this.identifiers);
    }

    /**
     * Removes the given identifier, updating <code>identifierFlags</code>,
     * <code>identifiers</code>, <code>identifiersAdded</code>, and
     * <code>identifiersRemoved</code> accordingly.
     *
     * @param {String} identifier
     *     The identifier to remove.
     */
    removeIdentifierFromScope(identifier: string): void {
        this.identifierFlags[identifier] = false;
        this.identifierChanged(identifier);
    }

    /**
     * Shows the full list of available identifiers. If the full list is
     * already shown, this function has no effect.
     */
    expand(): void {
        this.expanded = true;
    }

    /**
     * Hides the full list of available identifiers. If the full list is
     * already hidden, this function has no effect.
     */
    collapse(): void {
        this.expanded = false;
    }

    /**
     * Returns whether there are absolutely no identifiers that can be
     * managed using this editor. If true, the editor is effectively
     * useless, as there is nothing whatsoever to display.
     *
     * @returns}
     *     true if there are no identifiers that can be managed using this
     *     editor, false otherwise.
     */
    isEmpty(): boolean {
        return isEmpty(this.identifiers)
            && isEmpty(this.identifiersAvailable);
    }

}
