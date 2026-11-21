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
import { NotificationProgress } from '../../types/NotificationProgress';
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

    it('should write form values only into the notification currently displayed', () => {
        const forms = [new Form({ fields: [new Field({ name: 'password', type: Field.Type.PASSWORD })] })];

        const first = new Notification({ forms, formModel: { password: '' } });
        fixture.componentRef.setInput('notification', first);
        fixture.detectChanges();
        const firstFormGroup = fixture.componentInstance.notificationFormGroup;

        const second = new Notification({ forms, formModel: { password: '' } });
        fixture.componentRef.setInput('notification', second);
        fixture.detectChanges();

        firstFormGroup.setValue({ password: 'outdated' });
        fixture.componentInstance.notificationFormGroup.setValue({ password: 'secret' });

        expect(second.formModel).toEqual({ password: 'secret' });

        // Changes to the form of the first notification no longer have an effect
        firstFormGroup.setValue({ password: 'outdated again' });
        expect(second.formModel).toEqual({ password: 'secret' });
    });
});
