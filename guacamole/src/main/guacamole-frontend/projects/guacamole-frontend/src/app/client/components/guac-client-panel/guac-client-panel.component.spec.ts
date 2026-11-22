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

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ManagedClientGroup } from '../../types/ManagedClientGroup';
import { GuacClientPanelComponent } from './guac-client-panel.component';

describe('GuacClientPanelComponent', () => {
    let component: GuacClientPanelComponent;
    let fixture: ComponentFixture<GuacClientPanelComponent>;

    /**
     * Creates a client group with the given attached state.
     *
     * @param attached
     *     Whether the group is attached to the client interface.
     *
     * @returns
     *     A new client group with the given attached state.
     */
    const createGroup = (attached: boolean): ManagedClientGroup => new ManagedClientGroup({ attached });

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [GuacClientPanelComponent]
        });

        // Only the visibility logic of the component is under test, not the
        // thumbnails of its template
        TestBed.overrideTemplate(GuacClientPanelComponent, '');
        fixture = TestBed.createComponent(GuacClientPanelComponent);
        component = fixture.componentInstance;
    });

    describe('hasClientGroups', () => {

        it('should be false if there are no client groups', () => {
            component.clientGroups = [];

            expect(component.hasClientGroups()).toBe(false);
        });

        it('should be false if all client groups are attached', () => {
            component.clientGroups = [createGroup(true), createGroup(true)];

            expect(component.hasClientGroups()).toBe(false);
        });

        it('should be true if at least one client group is not attached', () => {
            component.clientGroups = [createGroup(false)];

            expect(component.hasClientGroups()).toBe(true);
        });

        it('should be true if only some client groups are attached', () => {
            component.clientGroups = [createGroup(true), createGroup(false)];

            expect(component.hasClientGroups()).toBe(true);
        });

    });

});

describe('GuacClientPanelComponent wheel handling', () => {
    let component: GuacClientPanelComponent;
    let fixture: ComponentFixture<GuacClientPanelComponent>;

    /**
     * The scrollable list of the panel, whose wheel events are redirected
     * to horizontal scrolling.
     */
    let scrollableArea: HTMLUListElement;

    /**
     * Dispatches a cancelable wheel event with the given vertical delta on
     * the scrollable area of the panel.
     *
     * @param deltaY
     *     The vertical delta of the wheel event.
     *
     * @param deltaMode
     *     The unit of the delta, as defined by the DOM (0x00 for pixels,
     *     0x01 for lines, 0x02 for pages).
     *
     * @returns
     *     The dispatched event.
     */
    const wheel = (deltaY: number, deltaMode: number): WheelEvent => {
        const event = new WheelEvent('wheel', { deltaY, deltaMode, cancelable: true });
        scrollableArea.dispatchEvent(event);
        return event;
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [GuacClientPanelComponent]
        });

        // Only the wheel handling of the scrollable list is under test
        TestBed.overrideTemplate(GuacClientPanelComponent, '<ul #clientPanelConnectionList></ul>');
        fixture = TestBed.createComponent(GuacClientPanelComponent);
        component = fixture.componentInstance;
        component.clientGroups = [];
        fixture.detectChanges();

        scrollableArea = component.scrollableArea!;
        scrollableArea.scrollLeft = 0;
    });

    it('should scroll horizontally by the number of pixels for pixel deltas', () => {
        const event = wheel(10, 0x00);

        expect(scrollableArea.scrollLeft).toBe(10);
        expect(event.defaultPrevented).toBe(true);
    });

    it('should scroll horizontally by 15 pixels per line for line deltas', () => {
        wheel(2, 0x01);

        expect(scrollableArea.scrollLeft).toBe(30);
    });

    it('should scroll horizontally by the width of the panel for page deltas', () => {
        vi.spyOn(scrollableArea, 'offsetWidth', 'get').mockReturnValue(200);

        wheel(1, 0x02);

        expect(scrollableArea.scrollLeft).toBe(200);
    });

    it('should scroll by the line multiplier for page deltas if the panel has no width', () => {
        // jsdom reports an offsetWidth of 0, as an unlaid-out panel would
        expect(scrollableArea.offsetWidth).toBe(0);

        const event = wheel(2, 0x02);

        expect(scrollableArea.scrollLeft).toBe(30);
        expect(event.defaultPrevented).toBe(true);
    });

    it('should scroll by the line multiplier for an unknown delta mode', () => {
        const event = wheel(2, 0x03);

        expect(scrollableArea.scrollLeft).toBe(30);
        expect(event.defaultPrevented).toBe(true);
    });

    it('should ignore wheel events without a vertical delta', () => {
        const event = wheel(0, 0x00);

        expect(scrollableArea.scrollLeft).toBe(0);
        expect(event.defaultPrevented).toBe(false);
    });

});
