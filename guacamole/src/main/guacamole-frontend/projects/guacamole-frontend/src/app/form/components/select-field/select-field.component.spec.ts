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
import { FormControl } from '@angular/forms';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { Field } from '../../../rest/types/Field';
import { FormModule } from '../../form.module';
import { SelectFieldComponent } from './select-field.component';

describe('SelectFieldComponent', () => {
    let component: SelectFieldComponent;
    let fixture: ComponentFixture<SelectFieldComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [SelectFieldComponent]
        });
        fixture = TestBed.createComponent(SelectFieldComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});

describe('SelectFieldComponent options', () => {

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [FormModule, TranslocoTestingModule.forRoot({ langs: { en: {} } })]
        });
    });

    it('should list the options in the order of the field definition', async () => {
        const fixture = TestBed.createComponent(SelectFieldComponent);
        fixture.componentRef.setInput('namespace', 'TEST');
        fixture.componentRef.setInput('field', new Field({ name: 'order', options: ['zebra', 'apple', 'mango'] }));
        fixture.componentRef.setInput('control', new FormControl('apple'));
        fixture.detectChanges();
        await fixture.whenStable();

        const options: HTMLOptionElement[] = Array.from(fixture.nativeElement.querySelectorAll('option'));
        expect(options.map(option => option.value)).toEqual(['zebra', 'apple', 'mango']);
    });

});
