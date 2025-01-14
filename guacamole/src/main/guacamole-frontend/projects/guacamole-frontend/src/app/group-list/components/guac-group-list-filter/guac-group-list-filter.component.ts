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

import { Component, Input, ViewEncapsulation } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

/**
 * A component which provides a filtering text input field
 * to filter connection groups.
 */
@Component({
    selector     : 'guac-group-list-filter',
    templateUrl  : './guac-group-list-filter.component.html',
    encapsulation: ViewEncapsulation.None
})
export class GuacGroupListFilterComponent {

    /**
     * The filter search string to use to restrict the displayed
     * connection groups. This subject will emit a new value whenever
     * the search string changes.
     */
    searchStringChange: BehaviorSubject<string> = new BehaviorSubject<string>('');

    /**
     * The placeholder text to display within the filter input field
     * when no filter has been provided.
     */
    @Input({ required: true }) placeholder!: string;

    /**
     * The filter search string to use to restrict the displayed items.
     */
    protected searchString: string | null = null;

    /**
     * TODO: Document
     */
    protected searchStringChanged(searchString: string) {
        this.searchStringChange.next(searchString);
    }

}
