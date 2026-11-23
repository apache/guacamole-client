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
import { FormControl, FormGroup } from '@angular/forms';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { Field } from '../../../rest/types/Field';
import { FormModule } from '../../form.module';
import { TimeZoneFieldComponent } from './time-zone-field.component';

describe('TimeZoneFieldComponent', () => {

    /**
     * Displays a time zone field for the given time zone within a form group
     * holding a second, enabled field, like a form of attributes. A form group
     * whose controls are all disabled would report the values of all of them.
     *
     * @param timeZone
     *     The time zone the model holds initially.
     *
     * @returns
     *     The fixture of the field and the form group containing its control.
     */
    const display = async (timeZone: string | null): Promise<{ fixture: ComponentFixture<TimeZoneFieldComponent>, form: FormGroup }> => {
        const form = new FormGroup({
            timezone: new FormControl<string | null>(timeZone),
            other: new FormControl<string | null>('')
        });
        const fixture = TestBed.createComponent(TimeZoneFieldComponent);
        fixture.componentRef.setInput('namespace', 'TEST');
        fixture.componentRef.setInput('field', new Field({ name: 'timezone', type: Field.Type.TIMEZONE }));
        fixture.componentRef.setInput('control', form.get('timezone'));
        fixture.detectChanges();
        await fixture.whenStable();
        return { fixture, form };
    };

    /**
     * Selects the option with the given text in the list of the field having
     * the given class, as a user would.
     *
     * @param fixture
     *     The fixture of the field.
     *
     * @param list
     *     The class of the list: "time-zone-region" or "time-zone".
     *
     * @param text
     *     The text of the option to select.
     */
    const choose = async (fixture: ComponentFixture<TimeZoneFieldComponent>, list: string, text: string): Promise<void> => {
        const select: HTMLSelectElement = fixture.nativeElement.querySelector(`select.${list}`);
        select.selectedIndex = Array.from(select.options).findIndex(option => option.textContent!.trim() === text);
        select.dispatchEvent(new Event('change'));
        fixture.detectChanges();
        await fixture.whenStable();
    };

    /**
     * Returns the text of the selected option in the list of the field having
     * the given class.
     *
     * @param fixture
     *     The fixture of the field.
     *
     * @param list
     *     The class of the list: "time-zone-region" or "time-zone".
     *
     * @returns
     *     The text of the selected option, or null if no option is selected.
     */
    const selected = (fixture: ComponentFixture<TimeZoneFieldComponent>, list: string): string | null => {
        const select: HTMLSelectElement = fixture.nativeElement.querySelector(`select.${list}`);
        return select.selectedIndex >= 0 ? select.options[select.selectedIndex].textContent!.trim() : null;
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [FormModule, TranslocoTestingModule.forRoot({ langs: { en: {} } })]
        });
    });

    it('should keep an unset time zone in the value of the form', async () => {
        const { form } = await display(null);
        expect(form.value).toEqual({ timezone: null, other: '' });
    });

    it('should keep a time zone outside the list in the value of the form', async () => {
        const { fixture, form } = await display('UTC');
        expect(form.value).toEqual({ timezone: 'UTC', other: '' });
        expect(selected(fixture, 'time-zone-region')).toBe('');
    });

    it('should report the first time zone of a chosen region to the form', async () => {
        const { fixture, form } = await display(null);
        const values: unknown[] = [];
        form.valueChanges.subscribe(value => values.push(value));

        await choose(fixture, 'time-zone-region', 'Europe');

        expect(values).toEqual([{ timezone: 'Europe/Amsterdam', other: '' }]);
    });

    it('should report the first time zone of another region to the form', async () => {
        const { fixture, form } = await display('Europe/Berlin');
        const values: unknown[] = [];
        form.valueChanges.subscribe(value => values.push(value));

        await choose(fixture, 'time-zone-region', 'America');

        expect(values).toEqual([{ timezone: 'America/Adak', other: '' }]);
        expect(selected(fixture, 'time-zone')).toBe('Adak');
    });

    it('should clear the time zone when the blank region is chosen', async () => {
        const { fixture, form } = await display('Europe/Berlin');
        const values: unknown[] = [];
        form.valueChanges.subscribe(value => values.push(value));

        await choose(fixture, 'time-zone-region', '');

        expect(values).toEqual([{ timezone: null, other: '' }]);
    });

    it('should report a chosen time zone to the form', async () => {
        const { fixture, form } = await display(null);
        await choose(fixture, 'time-zone-region', 'Europe');
        const values: unknown[] = [];
        form.valueChanges.subscribe(value => values.push(value));

        await choose(fixture, 'time-zone', 'Berlin');

        expect(values).toEqual([{ timezone: 'Europe/Berlin', other: '' }]);
    });

    it('should disable both lists of a disabled field, keeping the time zone in the value of the form', async () => {
        const form = new FormGroup({
            timezone: new FormControl<string | null>('Europe/Berlin'),
            other: new FormControl<string | null>('')
        });
        const fixture = TestBed.createComponent(TimeZoneFieldComponent);
        fixture.componentRef.setInput('field', new Field({ name: 'timezone', type: Field.Type.TIMEZONE }));
        fixture.componentRef.setInput('control', form.get('timezone'));
        fixture.componentRef.setInput('disabled', true);
        fixture.detectChanges();
        await fixture.whenStable();

        expect(fixture.nativeElement.querySelector('select.time-zone-region').disabled).toBe(true);
        expect(fixture.nativeElement.querySelector('select.time-zone').disabled).toBe(true);
        expect(form.value).toEqual({ timezone: 'Europe/Berlin', other: '' });
    });

    it('should disable the list of time zones while no region is chosen', async () => {
        const { fixture } = await display(null);
        const timeZones: HTMLSelectElement = fixture.nativeElement.querySelector('select.time-zone');
        expect(timeZones.disabled).toBe(true);

        await choose(fixture, 'time-zone-region', 'Europe');
        expect(timeZones.disabled).toBe(false);

        await choose(fixture, 'time-zone-region', '');
        expect(timeZones.disabled).toBe(true);
    });

});
