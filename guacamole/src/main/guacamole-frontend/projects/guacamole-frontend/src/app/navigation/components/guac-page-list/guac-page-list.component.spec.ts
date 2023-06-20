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

import { GuacPageListComponent } from './guac-page-list.component';
import { By } from "@angular/platform-browser";
import { SimpleChange } from "@angular/core";
import { LocaleModule } from "../../../locale/locale.module";

describe('PageListComponent', () => {
    let component: GuacPageListComponent;
    let fixture: ComponentFixture<GuacPageListComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [GuacPageListComponent],
            imports: [LocaleModule]
        });
        fixture = TestBed.createComponent(GuacPageListComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should display a list of links to pages', () => {

        const selector = By.css('a');

        component.pages = [
            {name: 'Page 1', url: '/page1'},
            {name: 'Page 2', url: '/page2'},
            {name: 'Page 3', url: '/page3'}
        ];

        component.ngOnChanges({pages: new SimpleChange([], component.pages, false)});
        fixture.detectChanges();

        const elements = fixture.debugElement.queryAll(selector);
        expect(elements.length).toBe(3);
    });

    it('should add a "current" class to the link of the current page', () => {

        const selector = By.css('a');

        component.pages = [
            {name: 'Page 0', url: '/'},
            {name: 'Page 1', url: '/page1'},
        ];

        component.ngOnChanges({pages: new SimpleChange([], component.pages, false)});
        fixture.detectChanges();

        const elements = fixture.debugElement.queryAll(selector);

        const page0 = elements[0].nativeElement;
        const page1 = elements[1].nativeElement;

        expect(page0).toHaveClass('current');
        expect(page1).not.toHaveClass('current');

        component.currentURL = '/page1';
        fixture.detectChanges();

        expect(page0).not.toHaveClass('current');
        expect(page1).toHaveClass('current');
    });

});
