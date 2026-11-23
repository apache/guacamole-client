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
import { ViewportComponent } from './viewport.component';

describe('ViewportComponent', () => {

    let fixture: ComponentFixture<ViewportComponent>;
    let component: ViewportComponent;

    /**
     * The inner width of the window before the test, restored afterwards.
     */
    const originalInnerWidth = window.innerWidth;

    beforeEach(() => {
        TestBed.configureTestingModule({ declarations: [ViewportComponent] });
        fixture = TestBed.createComponent(ViewportComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    afterEach(() => {
        fixture.destroy();
        window.innerWidth = originalInnerWidth;
    });

    it('should fit the element to the visible area when the window scrolls', () => {
        window.innerWidth = 1234;

        window.dispatchEvent(new Event('scroll'));

        expect(component.element.style.width).toBe('1234px');
    });

    it('should stop fitting the element on scroll once it is destroyed', () => {
        fixture.destroy();
        window.innerWidth = 1234;

        window.dispatchEvent(new Event('scroll'));

        expect(component.element.style.width).not.toBe('1234px');
    });
});
