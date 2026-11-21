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
