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

import { provideHttpClient, withInterceptorsFromDi, withXhr } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import { GroupListModule } from '../../../group-list/group-list.module';
import { ConnectionGroup } from '../../../rest/types/ConnectionGroup';
import { LocationChooserConnectionGroupComponent } from '../location-chooser-connection-group/location-chooser-connection-group.component';
import { LocationChooserComponent } from './location-chooser.component';

/**
 * Hosts a location chooser bound to the parent identifier of an object,
 * like the editors of connections and connection groups.
 */
@Component({
    template: `
        <guac-location-chooser dataSource="postgresql" [rootGroup]="rootGroup"
                               [(value)]="parentIdentifier"></guac-location-chooser>`,
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
class HostComponent {

    /**
     * The root group, containing "Servers", which contains "Linux".
     */
    rootGroup = new ConnectionGroup({
        identifier: 'ROOT',
        name: 'ROOT',
        type: ConnectionGroup.Type.ORGANIZATIONAL,
        activeConnections: 0,
        childConnectionGroups: [new ConnectionGroup({
            identifier: '1',
            parentIdentifier: 'ROOT',
            name: 'Servers',
            type: ConnectionGroup.Type.ORGANIZATIONAL,
            activeConnections: 0,
            childConnectionGroups: [new ConnectionGroup({
                identifier: '2',
                parentIdentifier: '1',
                name: 'Linux',
                type: ConnectionGroup.Type.ORGANIZATIONAL,
                activeConnections: 0
            })]
        })]
    });

    /**
     * The identifier of the parent group of the edited object.
     */
    parentIdentifier = 'ROOT';

}

describe('LocationChooserComponent', () => {

    let fixture: ComponentFixture<HostComponent>;

    /**
     * Renders the host, so that the chooser shows the current location.
     */
    async function render(): Promise<void> {
        fixture.detectChanges();
        await fixture.whenStable();
        fixture.detectChanges();
    }

    /**
     * Returns the entry of the group having the given name within the menu
     * of the chooser, if displayed.
     *
     * @param name
     *     The name of the group.
     *
     * @returns
     *     The entry of the group, or undefined if it is not displayed.
     */
    function entry(name: string): HTMLElement | undefined {
        return Array.from(fixture.nativeElement.querySelectorAll('.dropdown .location-chooser-connection-group') as NodeListOf<HTMLElement>)
            .find(group => group.textContent!.trim() === name);
    }

    /**
     * Opens the menu of the chooser, expands groups until the group having
     * the given name is displayed and clicks it, as the user would.
     *
     * @param name
     *     The name of the group to choose.
     */
    async function choose(name: string): Promise<void> {
        (fixture.nativeElement.querySelector('.location') as HTMLElement).click();
        await render();
        let expander: HTMLElement | null;
        while (!entry(name) && (expander = fixture.nativeElement.querySelector('.dropdown .expandable:not(.expanded) > .caption .icon.expand'))) {
            expander.click();
            await render();
        }
        entry(name)!.click();
        await render();
    }

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [HostComponent, LocationChooserComponent, LocationChooserConnectionGroupComponent],
            imports: [GroupListModule],
            providers: [
                provideHttpClient(withXhr(), withInterceptorsFromDi()),
                provideHttpClientTesting(),
                { provide: AuthenticationService, useValue: { getCurrentUsername: () => 'alice' } }
            ]
        });
        fixture = TestBed.createComponent(HostComponent);
    });

    it('should show the name of the current parent group', async () => {
        fixture.componentInstance.parentIdentifier = '1';
        await render();

        expect(fixture.nativeElement.querySelector('.location').textContent.trim()).toBe('Servers');
    });

    it('should write the identifier of the chosen group to the bound value', async () => {
        await render();

        await choose('Servers');

        expect(fixture.componentInstance.parentIdentifier).toBe('1');
        expect(fixture.nativeElement.querySelector('.location').textContent.trim()).toBe('Servers');
    });

    it('should write the identifier of the root group when chosen after another group', async () => {
        await render();

        await choose('Servers');
        await choose('ROOT');

        expect(fixture.componentInstance.parentIdentifier).toBe('ROOT');
        expect(fixture.nativeElement.querySelector('.location').textContent.trim()).toBe('ROOT');
    });

});
