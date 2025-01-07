

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

import { Component, EventEmitter, Input, Output, ViewEncapsulation } from '@angular/core';
import { canonicalize } from '../../../locale/service/translation.service';

/**
 * Component which displays a set of tabs dividing a section of a page into
 * logical subsections or views. The currently selected tab is communicated
 * through assignment to the variable bound to the <code>current</code>
 * attribute. No navigation occurs as a result of selecting a tab.
 */
@Component({
    selector     : 'guac-section-tabs',
    templateUrl  : './guac-section-tabs.component.html',
    encapsulation: ViewEncapsulation.None
})
export class GuacSectionTabsComponent {

    /**
     * The translation namespace to use when producing translation
     * strings for each tab. Tab translation strings will be of the
     * form:
     *
     * <code>NAMESPACE.SECTION_HEADER_NAME<code>
     *
     * where <code>NAMESPACE</code> is the namespace provided to this
     * attribute and <code>NAME</code> is one of the names within the
     * array provided to the <code>tabs</code> attribute and
     * transformed via canonicalize().
     */
    @Input() namespace?: string;

    /**
     * The name of the currently selected tab. This name MUST be one of
     * the names present in the array given via the <code>tabs</code>
     * attribute. This directive will not automatically choose an
     * initially selected tab, and a default value should be manually
     * assigned to <code>current</code> to ensure a tab is initially
     * selected.
     */
    @Input() current?: string;

    /**
     * The name of the currently selected tab. This name MUST be one of
     * the names present in the array given via the <code>tabs</code>
     * attribute. This directive will not automatically choose an
     * initially selected tab, and a default value should be manually
     * assigned to <code>current</code> to ensure a tab is initially
     * selected.
     *
     * When the current tab changes, this output property emits the new
     * tab name as a string.
     */
    @Output() currentChange: EventEmitter<string> = new EventEmitter<string>();

    /**
     * The unique names of all tabs which should be made available, in
     * display order. These names will be assigned to the variable
     * bound to the <code>current</code> attribute when the current
     * tab changes.
     */
    @Input() tabs?: string[];

    /**
     * Produces the translation string for the section header representing
     * the tab having the given name. The translation string will be of the
     * form:
     *
     * <code>NAMESPACE.SECTION_HEADER_NAME<code>
     *
     * where <code>NAMESPACE</code> is the namespace provided to the
     * directive and <code>NAME</code> is the given name transformed
     * via canonicalize().
     *
     * @param name
     *     The name of the tab.
     *
     * @returns
     *     The translation string which produces the translated header
     *     of the tab having the given name.
     */
    getSectionHeader(name: string): string {

        // If no name, then no header
        if (!name)
            return '';

        return canonicalize(this.namespace || 'MISSING_NAMESPACE')
            + '.SECTION_HEADER_' + canonicalize(name);

    }

    /**
     * Selects the tab having the given name. The name of the currently
     * selected tab will be communicated outside the directive through
     * this.current.
     *
     * @param name
     *     The name of the tab to select.
     */
    selectTab(name: string): void {
        this.current = name;
        this.currentChange.emit(this.current);
    }

    /**
     * Returns whether the tab having the given name is currently
     * selected. A tab is currently selected if its name is stored within
     * this.current, as assigned externally or by selectTab().
     *
     * @param name
     *     The name of the tab to test.
     *
     * @returns
     *     true if the tab having the given name is currently selected,
     *     false otherwise.
     */
    isSelected(name: string): boolean {
        return this.current === name;
    }
}
