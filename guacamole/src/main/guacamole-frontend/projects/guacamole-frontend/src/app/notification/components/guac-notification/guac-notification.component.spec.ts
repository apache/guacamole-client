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

import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { Field } from '../../../rest/types/Field';
import { Form } from '../../../rest/types/Form';
import { Notification } from '../../types/Notification';
import { NotificationCountdown } from '../../types/NotificationCountdown';
import { NotificationProgress } from '../../types/NotificationProgress';
import { NotificationModule } from '../../notification.module';
import { GuacNotificationComponent } from './guac-notification.component';

describe('GuacNotificationComponent', () => {
    let fixture: ComponentFixture<GuacNotificationComponent>;

    /**
     * Shows a notification with the given progress in the component under test.
     *
     * @param progress
     *     The progress of the notification to show.
     */
    function showProgress(progress?: NotificationProgress): void {
        fixture.componentRef.setInput('notification', new Notification({ progress }));
        fixture.detectChanges();
    }

    /**
     * Returns the progress bar element of the component under test.
     *
     * @returns
     *     The progress bar element.
     */
    function getBar(): HTMLElement {
        return fixture.nativeElement.querySelector('.progress .bar');
    }

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [GuacNotificationComponent],
            imports: [
                TranslocoTestingModule.forRoot({
                    langs: { en: {} },
                    translocoConfig: {
                        availableLangs: ['en'],
                        defaultLang: 'en'
                    }
                })
            ],
            schemas: [NO_ERRORS_SCHEMA]
        });
        fixture = TestBed.createComponent(GuacNotificationComponent);
    });

    it('should set the width of the progress bar to the progress ratio in percent', () => {
        showProgress(new NotificationProgress('TEXT', 5, 'MB', 0.5));

        expect(getBar().style.width).toBe('50%');
    });

    it('should update the width of the progress bar when the progress ratio changes', () => {
        const notification = new Notification({ progress: new NotificationProgress('TEXT', 2, 'MB', 0.25) });
        fixture.componentRef.setInput('notification', notification);
        fixture.detectChanges();
        expect(getBar().style.width).toBe('25%');

        notification.progress!.ratio = 0.75;
        fixture.detectChanges();

        expect(getBar().style.width).toBe('75%');
    });

    it('should hide the progress bar if the progress ratio is unknown', () => {
        showProgress(new NotificationProgress('TEXT', 5, 'MB'));

        expect(getBar().hidden).toBe(true);
    });

    it('should reset the width of the progress bar when the progress ratio drops to zero', () => {
        const notification = new Notification({ progress: new NotificationProgress('TEXT', 5, 'MB', 0.5) });
        fixture.componentRef.setInput('notification', notification);
        fixture.detectChanges();
        expect(getBar().style.width).toBe('50%');

        notification.progress!.ratio = 0;
        fixture.detectChanges();

        expect(getBar().style.width).toBe('0%');
        expect(getBar().hidden).toBe(true);
    });

    it('should hide the progress bar when the progress ratio becomes unknown', () => {
        const notification = new Notification({ progress: new NotificationProgress('TEXT', 5, 'MB', 0.5) });
        fixture.componentRef.setInput('notification', notification);
        fixture.detectChanges();
        expect(getBar().hidden).toBe(false);

        notification.progress!.ratio = undefined;
        fixture.detectChanges();

        expect(getBar().hidden).toBe(true);
    });

    it('should show the progress bar again when the progress ratio is known again', () => {
        const notification = new Notification({ progress: new NotificationProgress('TEXT', 5, 'MB', 0.5) });
        fixture.componentRef.setInput('notification', notification);
        fixture.detectChanges();

        notification.progress!.ratio = 0;
        fixture.detectChanges();
        notification.progress!.ratio = undefined;
        fixture.detectChanges();
        notification.progress!.ratio = 0.25;
        fixture.detectChanges();

        expect(getBar().hidden).toBe(false);
        expect(getBar().style.width).toBe('25%');
    });

    describe('countdown', () => {

        /**
         * Returns the time remaining in the countdown of the component under test.
         *
         * @returns
         *     The time remaining in seconds.
         */
        function getTimeRemaining(): number | null {
            return fixture.componentInstance.timeRemaining;
        }

        /**
         * Shows a notification with a countdown in the component under test.
         *
         * @param countdown
         *     The countdown of the notification to show.
         */
        function showCountdown(countdown: NotificationCountdown): void {
            fixture.componentRef.setInput('notification', new Notification({ countdown }));
            fixture.detectChanges();
        }

        beforeEach(() => {
            vi.useFakeTimers();
        });

        afterEach(() => {
            vi.useRealTimers();
        });

        it('should count down once per second', () => {
            showCountdown(new NotificationCountdown('TEXT', 3));
            expect(getTimeRemaining()).toBe(3);

            vi.advanceTimersByTime(1000);
            expect(getTimeRemaining()).toBe(2);

            vi.advanceTimersByTime(2000);
            expect(getTimeRemaining()).toBe(0);
        });

        it('should call the callback once when the countdown reaches zero', () => {
            const callback = vi.fn();
            showCountdown(new NotificationCountdown('TEXT', 2, callback));

            vi.advanceTimersByTime(1000);
            expect(callback).not.toHaveBeenCalled();

            vi.advanceTimersByTime(1000);
            expect(callback).toHaveBeenCalledTimes(1);

            vi.advanceTimersByTime(5000);
            expect(callback).toHaveBeenCalledTimes(1);
        });

        it('should stop at zero instead of counting below zero', () => {
            showCountdown(new NotificationCountdown('TEXT', 2));

            vi.advanceTimersByTime(10000);

            expect(getTimeRemaining()).toBe(0);
        });

        it('should restart the countdown when the notification changes', () => {
            const outdated = vi.fn();
            const current = vi.fn();

            showCountdown(new NotificationCountdown('TEXT', 5, outdated));
            vi.advanceTimersByTime(2000);
            expect(getTimeRemaining()).toBe(3);

            showCountdown(new NotificationCountdown('TEXT', 4, current));
            expect(getTimeRemaining()).toBe(4);

            vi.advanceTimersByTime(4000);
            expect(getTimeRemaining()).toBe(0);
            expect(current).toHaveBeenCalledTimes(1);
            expect(outdated).not.toHaveBeenCalled();
        });
    });
});

describe('GuacNotificationComponent form', () => {
    let fixture: ComponentFixture<GuacNotificationComponent>;

    /**
     * The forms of a connection whose parameters may be requested by the
     * server, like the forms of a protocol.
     */
    const forms = [new Form({
        name: 'authentication',
        fields: [
            new Field({ name: 'username', type: Field.Type.USERNAME }),
            new Field({ name: 'password', type: Field.Type.PASSWORD }),
            new Field({ name: 'domain', type: Field.Type.TEXT })
        ]
    })];

    /**
     * Shows the given notification in the component under test and waits
     * until its fields are rendered.
     *
     * @param notification
     *     The notification to show.
     */
    async function show(notification: Notification): Promise<void> {
        fixture.componentRef.setInput('notification', notification);
        fixture.detectChanges();
        await fixture.whenStable();
        fixture.detectChanges();
    }

    /**
     * Returns the names of all input fields displayed by the component under
     * test.
     *
     * @returns
     *     The names of all displayed input fields, in order.
     */
    function getFieldNames(): string[] {
        return Array.from(fixture.nativeElement.querySelectorAll('input'))
            .map(input => (input as HTMLInputElement).name);
    }

    /**
     * Enters the given value into the displayed input field having the given
     * name, as the user would.
     *
     * @param name
     *     The name of the input field.
     *
     * @param value
     *     The value to enter.
     */
    function enter(name: string, value: string): void {
        const input: HTMLInputElement = fixture.nativeElement.querySelector(`input[name="${name}"]`);
        input.value = value;
        input.dispatchEvent(new Event('input'));
    }

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                NotificationModule,
                TranslocoTestingModule.forRoot({ langs: { en: {} } })
            ]
        });
        fixture = TestBed.createComponent(GuacNotificationComponent);
    });

    it('should show only the fields present within the model of the notification', async () => {
        await show(new Notification({ forms, formModel: { password: '' } }));

        expect(getFieldNames()).toEqual(['password']);
        expect(Object.keys(fixture.componentInstance.notificationFormGroup.controls)).toEqual(['password']);
    });

    it('should write the values entered into the model of the current notification when the notification changes', async () => {
        const first = new Notification({ forms, formModel: { password: '' } });
        await show(first);

        const second = new Notification({ forms, formModel: { password: '' } });
        await show(second);

        enter('password', 'secret');

        expect(second.formModel).toEqual({ password: 'secret' });
        expect(first.formModel).toEqual({ password: '' });
    });
});

describe('GuacNotificationComponent text', () => {
    let fixture: ComponentFixture<GuacNotificationComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [GuacNotificationComponent],
            imports: [
                TranslocoTestingModule.forRoot({
                    langs: { en: { IMPORT: { ERROR_PARSE_FAILURE_YAML: 'Parsing failed with error "{{ERROR}}".' } } },
                    translocoConfig: { availableLangs: ['en'], defaultLang: 'en' }
                })
            ],
            schemas: [NO_ERRORS_SCHEMA]
        });
        fixture = TestBed.createComponent(GuacNotificationComponent);
    });

    it('should show the translated text as one paragraph of plain text, even if a variable contains line breaks', async () => {

        // GIVEN an error whose message spans several lines, like the
        // message of a YAML parser
        const error = 'bad indentation at line 3, column 1:\n    protocol: [ssh\n    ^';

        // WHEN the notification shows it
        fixture.componentRef.setInput('notification', new Notification({
            title: 'IMPORT.DIALOG_HEADER_ERROR',
            text: { key: 'IMPORT.ERROR_PARSE_FAILURE_YAML', variables: { ERROR: error } }
        }));
        fixture.detectChanges();
        await fixture.whenStable();
        fixture.detectChanges();

        // THEN the paragraph holds the text without line break elements, so
        // the line breaks render as spaces like in the AngularJS dialog
        const text: HTMLElement = fixture.nativeElement.querySelector('p.text');
        expect(text.querySelector('br')).toBeNull();
        expect(text.textContent!.trim()).toBe(`Parsing failed with error "${error}".`);

    });
});
