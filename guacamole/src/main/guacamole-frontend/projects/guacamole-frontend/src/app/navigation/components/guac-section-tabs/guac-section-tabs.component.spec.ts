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

import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { TranslocoTestingModule } from '@ngneat/transloco';
import { LocaleModule } from '../../../locale/locale.module';

import { GuacSectionTabsComponent } from './guac-section-tabs.component';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('SectionTabsComponent', () => {
    let component: GuacSectionTabsComponent;
    let fixture: ComponentFixture<GuacSectionTabsComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
    declarations: [GuacSectionTabsComponent],
    imports: [LocaleModule,
        TranslocoTestingModule.forRoot({
            langs: {
                en: {
                    'TAB_1': 'TAB_1',
                    'TAB_2': 'TAB_2',
                    'TAB_3': 'TAB_3'
                }
            }
        })],
    providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
});
        fixture = TestBed.createComponent(GuacSectionTabsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should produce the correct section header translation string', () => {
        component.namespace = 'namespace';
        const sectionHeader = component.getSectionHeader('Tab 1');
        expect(sectionHeader).toBe('NAMESPACE.SECTION_HEADER_TAB_1');
    });

    it('should emit the current tab name when a tab is selected', () => {
        component.tabs = ['Tab 1', 'Tab 2', 'Tab 3'];
        component.current = 'Tab 1';
        spyOn(component.currentChange, 'emit');
        component.selectTab('Tab 2');
        expect(component.current).toBe('Tab 2');
        expect(component.currentChange.emit).toHaveBeenCalledWith('Tab 2');
    });

    it('should render the tabs', () => {
        component.tabs = ['Tab 1', 'Tab 2', 'Tab 3'];
        fixture.detectChanges();
        const tabElements = fixture.debugElement.queryAll(By.css('a'));
        expect(tabElements.length).toBe(3);
        expect(tabElements[0].nativeElement.textContent).toContain('TAB_1');
        expect(tabElements[1].nativeElement.textContent).toContain('TAB_2');
        expect(tabElements[2].nativeElement.textContent).toContain('TAB_3');
    });

    it('should apply the "current" CSS class to the current tab', () => {
        component.tabs = ['Tab 1', 'Tab 2', 'Tab 3'];
        component.current = 'Tab 2';
        fixture.detectChanges();
        const tabElements = fixture.debugElement.queryAll(By.css('a'));
        expect(tabElements.length).toBe(3);
        expect(tabElements[0].nativeElement).not.toHaveClass('current');
        expect(tabElements[1].nativeElement).toHaveClass('current');
        expect(tabElements[2].nativeElement).not.toHaveClass('current');
    });
});
