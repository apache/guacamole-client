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

import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormControl } from '@angular/forms';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { Field } from '../../../rest/types/Field';
import { FormModule } from '../../form.module';

/**
 * Displays two text fields side by side.
 */
@Component({
    template: `
        <guac-form-field namespace="TEST" [field]="first" [control]="firstControl"></guac-form-field>
        <guac-form-field namespace="TEST" [field]="second" [control]="secondControl"></guac-form-field>
    `,
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
class TwoFieldsComponent {

    /**
     * The field displayed by the first guac-form-field.
     */
    first = new Field({ name: 'first' });

    /**
     * The field displayed by the second guac-form-field.
     */
    second = new Field({ name: 'second' });

    /**
     * The value of the first field.
     */
    firstControl = new FormControl('');

    /**
     * The value of the second field.
     */
    secondControl = new FormControl('');

}

describe('FormFieldComponent', () => {

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [TwoFieldsComponent],
            imports: [FormModule, TranslocoTestingModule.forRoot({ langs: { en: {} } })]
        });
    });

    it('should leave the content of other fields untouched when its field changes', async () => {
        const fixture = TestBed.createComponent(TwoFieldsComponent);
        fixture.detectChanges();
        await fixture.whenStable();

        fixture.componentInstance.first = new Field({ name: 'replaced' });
        fixture.detectChanges();
        await fixture.whenStable();

        const containers: HTMLElement[] = Array.from(fixture.nativeElement.querySelectorAll('.form-field'));
        expect(containers.map(container => container.querySelector('input')?.name))
            .toEqual(['replaced', 'second']);
    });

});
