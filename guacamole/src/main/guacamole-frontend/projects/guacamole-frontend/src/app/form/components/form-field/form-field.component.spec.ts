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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, FormGroup } from '@angular/forms';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { Field } from '../../../rest/types/Field';
import { LogService } from '../../../util/log.service';
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

/**
 * Displays the fields "exec-command" and "terminal-type" of a Kubernetes
 * connection within a form, where the label of "terminal-type" may depend on
 * the value of "exec-command".
 */
@Component({
    template: `
        <guac-form namespace="TEST" [content]="fields" [modelFormGroup]="values"></guac-form>
    `,
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
class KubernetesFormComponent {

    /**
     * The field whose value decides the label of the "terminal-type" field.
     */
    execCommand = new Field({ name: 'exec-command' });

    /**
     * The field whose label may depend on the value of "exec-command".
     */
    terminalType = new Field({
        name: 'terminal-type',
        type: Field.Type.ENUM,
        options: ['', 'xterm', 'vt100']
    });

    /**
     * All fields displayed by the form.
     */
    fields: Field[] = [this.execCommand, this.terminalType];

    /**
     * The values of all fields displayed by the form.
     */
    values = new FormGroup({
        'exec-command': new FormControl(''),
        'terminal-type': new FormControl('')
    });

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

describe('FormFieldComponent conditionalLabel', () => {

    /**
     * The usual label of the "terminal-type" field.
     */
    const TERMINAL_TYPE = 'Terminal type (available as $TERM in command):';

    /**
     * The alternate label of the "terminal-type" field.
     */
    const TERMINAL_TYPE_ATTACH = 'Terminal type (set manually in session):';

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [KubernetesFormComponent],
            imports: [
                FormModule,
                TranslocoTestingModule.forRoot({
                    langs: {
                        en: {
                            TEST: {
                                FIELD_HEADER_EXEC_COMMAND: 'Command:',
                                FIELD_HEADER_TERMINAL_TYPE: TERMINAL_TYPE,
                                FIELD_HEADER_TERMINAL_TYPE_ATTACH: TERMINAL_TYPE_ATTACH
                            }
                        }
                    },
                    translocoConfig: {
                        availableLangs: ['en'],
                        defaultLang: 'en'
                    },
                    preloadLangs: true
                })
            ]
        });
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    /**
     * Renders the Kubernetes form with the given conditional label for the
     * "terminal-type" field.
     *
     * @param conditionalLabel
     *     The conditional label of the "terminal-type" field.
     *
     * @param execCommand
     *     The initial value of the "exec-command" field.
     *
     * @returns
     *     The fixture of the rendered form.
     */
    async function renderForm(conditionalLabel: Field.ConditionalLabel,
                              execCommand: string): Promise<ComponentFixture<KubernetesFormComponent>> {
        const fixture = TestBed.createComponent(KubernetesFormComponent);
        fixture.componentInstance.terminalType.conditionalLabel = conditionalLabel;
        fixture.componentInstance.values.controls['exec-command'].setValue(execCommand);
        fixture.detectChanges();
        await fixture.whenStable();
        fixture.detectChanges();
        return fixture;
    }

    /**
     * Returns the label of the "terminal-type" field.
     *
     * @param fixture
     *     The fixture of the rendered form.
     *
     * @returns
     *     The text of the label of the "terminal-type" field.
     */
    function getTerminalTypeHeader(fixture: ComponentFixture<KubernetesFormComponent>): string | undefined {
        const fields: HTMLElement[] = Array.from(fixture.nativeElement.querySelectorAll('.labeled-field'));
        const field = fields.find(element => element.querySelector('[name="terminal-type"]'));
        return field?.querySelector('.field-header label')?.textContent?.trim();
    }

    /**
     * Enters the given value into the "exec-command" field as a user would.
     *
     * @param fixture
     *     The fixture of the rendered form.
     *
     * @param value
     *     The value to enter.
     */
    function enterExecCommand(fixture: ComponentFixture<KubernetesFormComponent>, value: string): void {
        const input: HTMLInputElement = fixture.nativeElement.querySelector('input[name="exec-command"]');
        input.value = value;
        input.dispatchEvent(new Event('input'));
        fixture.detectChanges();
    }

    it('should use the alternate label if the other field equals the given value', async () => {
        const fixture = await renderForm({
            field: 'exec-command',
            equals: '',
            labelKey: 'FIELD_HEADER_TERMINAL_TYPE_ATTACH'
        }, '');

        expect(getTerminalTypeHeader(fixture)).toBe(TERMINAL_TYPE_ATTACH);
    });

    it('should use the usual label if the other field does not equal the given value', async () => {
        const fixture = await renderForm({
            field: 'exec-command',
            equals: '',
            labelKey: 'FIELD_HEADER_TERMINAL_TYPE_ATTACH'
        }, 'bash');

        expect(getTerminalTypeHeader(fixture)).toBe(TERMINAL_TYPE);
    });

    it('should use the alternate label if the other field does not equal the value given by notEquals', async () => {
        const fixture = await renderForm({
            field: 'exec-command',
            notEquals: '',
            labelKey: 'FIELD_HEADER_TERMINAL_TYPE_ATTACH'
        }, 'bash');

        expect(getTerminalTypeHeader(fixture)).toBe(TERMINAL_TYPE_ATTACH);
    });

    it('should use the usual label if the other field equals the value given by notEquals', async () => {
        const fixture = await renderForm({
            field: 'exec-command',
            notEquals: '',
            labelKey: 'FIELD_HEADER_TERMINAL_TYPE_ATTACH'
        }, '');

        expect(getTerminalTypeHeader(fixture)).toBe(TERMINAL_TYPE);
    });

    it('should evaluate only equals if both equals and notEquals are given', async () => {
        const fixture = await renderForm({
            field: 'exec-command',
            equals: '',
            notEquals: '',
            labelKey: 'FIELD_HEADER_TERMINAL_TYPE_ATTACH'
        }, '');

        expect(getTerminalTypeHeader(fixture)).toBe(TERMINAL_TYPE_ATTACH);
    });

    it('should warn and use the usual label if neither equals nor notEquals is given', async () => {
        const warn = vi.spyOn(TestBed.inject(LogService), 'warn').mockImplementation(() => undefined);

        const fixture = await renderForm({
            field: 'exec-command',
            labelKey: 'FIELD_HEADER_TERMINAL_TYPE_ATTACH'
        }, '');

        expect(getTerminalTypeHeader(fixture)).toBe(TERMINAL_TYPE);
        expect(warn).toHaveBeenCalledWith('conditionalLabel for field "terminal-type" specifies '
            + 'neither "equals" nor "notEquals"; ignoring.');
    });

    it('should switch the label when the value of the other field changes', async () => {
        const fixture = await renderForm({
            field: 'exec-command',
            equals: '',
            labelKey: 'FIELD_HEADER_TERMINAL_TYPE_ATTACH'
        }, '');
        expect(getTerminalTypeHeader(fixture)).toBe(TERMINAL_TYPE_ATTACH);

        enterExecCommand(fixture, 'bash');
        expect(getTerminalTypeHeader(fixture)).toBe(TERMINAL_TYPE);

        enterExecCommand(fixture, '');
        expect(getTerminalTypeHeader(fixture)).toBe(TERMINAL_TYPE_ATTACH);
    });

});
