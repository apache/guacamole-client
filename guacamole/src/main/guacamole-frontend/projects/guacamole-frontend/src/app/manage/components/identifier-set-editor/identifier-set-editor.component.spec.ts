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

import { ChangeDetectionStrategy, Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { GuacPagerComponent } from '../../../list/components/guac-pager/guac-pager.component';
import { IdentifierSetEditorComponent } from './identifier-set-editor.component';

/**
 * Hosts an identifier set editor for user groups, with the available groups
 * in the order in which the REST API lists them.
 */
@Component({
    template: `
        <identifier-set-editor
            header="MANAGE_USER.SECTION_HEADER_USER_GROUPS"
            emptyPlaceholder="MANAGE_USER.HELP_NO_USER_GROUPS"
            unavailablePlaceholder="MANAGE_USER.INFO_NO_USER_GROUPS_AVAILABLE"
            [identifiersAvailable]="available"
            [identifiers]="identifiers"
            [identifiersAdded]="added"
            [identifiersRemoved]="removed">
        </identifier-set-editor>`,
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
class HostComponent {

    /**
     * The identifiers of all available user groups, unsorted.
     */
    available: string[] = ['staff', 'Admins', 'guests', 'auditors'];

    /**
     * The identifiers of the user groups which are currently selected.
     */
    identifiers: string[] = ['staff'];

    /**
     * The identifiers added by the editor.
     */
    added: string[] = [];

    /**
     * The identifiers removed by the editor.
     */
    removed: string[] = [];

}

/**
 * Renders the host and expands the list of available identifiers.
 *
 * @returns
 *     The fixture of the host component.
 */
async function openExpanded(): Promise<ComponentFixture<HostComponent>> {
    TestBed.configureTestingModule({
        declarations: [HostComponent, IdentifierSetEditorComponent, GuacPagerComponent],
        schemas: [NO_ERRORS_SCHEMA],
        imports: [
            FormsModule,
            TranslocoTestingModule.forRoot({
                langs: { en: {} },
                translocoConfig: { availableLangs: ['en'], defaultLang: 'en' }
            })
        ]
    });
    const fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    (fixture.nativeElement.querySelector('img.expand') as HTMLElement).click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
}

/**
 * Returns the identifiers listed by the given part of the editor.
 *
 * @param fixture
 *     The fixture of the host component.
 *
 * @param selector
 *     The CSS selector of the list, either the abbreviated list of
 *     selected identifiers or the exhaustive list of available identifiers.
 *
 * @returns
 *     The listed identifiers, in the order shown.
 */
function listed(fixture: ComponentFixture<HostComponent>, selector: string): string[] {
    return Array.from(fixture.nativeElement.querySelectorAll(`${selector} .identifier`) as NodeListOf<HTMLElement>)
        .map(element => element.textContent!.trim());
}

describe('IdentifierSetEditorComponent', () => {

    it('should list the available identifiers in alphabetical order, ignoring case', async () => {

        // GIVEN available user groups in the order of the REST API
        // WHEN the list of available groups is expanded
        const fixture = await openExpanded();

        // THEN it lists them like the orderBy filter of the AngularJS editor
        expect(listed(fixture, '.all-related-objects')).toEqual(['Admins', 'auditors', 'guests', 'staff']);

    });

    it('should list a newly selected identifier among the selected identifiers', async () => {

        // GIVEN the expanded list of available user groups
        const fixture = await openExpanded();

        // WHEN a group is selected
        const guests = Array.from(fixture.nativeElement.querySelectorAll('.all-related-objects li') as NodeListOf<HTMLElement>)
            .find(item => item.textContent!.includes('guests'))!;
        guests.querySelector('input')!.click();
        fixture.detectChanges();
        await fixture.whenStable();
        fixture.detectChanges();

        // THEN it joins the selected groups in sorted order and is reported
        // as added, while the available groups keep their order
        expect(listed(fixture, '.abbreviated-related-objects')).toEqual(['guests', 'staff']);
        expect(fixture.componentInstance.added).toEqual(['guests']);
        expect(listed(fixture, '.all-related-objects')).toEqual(['Admins', 'auditors', 'guests', 'staff']);

    });

});
