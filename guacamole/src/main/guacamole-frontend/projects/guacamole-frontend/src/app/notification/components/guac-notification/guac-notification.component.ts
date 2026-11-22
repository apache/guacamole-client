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

import {
    Component,
    DestroyRef,
    DoCheck,
    Input,
    OnChanges,
    OnDestroy,
    SimpleChanges,
    ViewEncapsulation,
    ChangeDetectionStrategy
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup } from '@angular/forms';
import { Notification } from '../../types/Notification';

/**
 * A component for displaying notifications.
 */
@Component({
    selector: 'guac-notification',
    templateUrl: './guac-notification.component.html',
    encapsulation: ViewEncapsulation.None,
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class GuacNotificationComponent implements OnChanges, DoCheck, OnDestroy {

    /**
     * The notification to display.
     */
    // eslint-disable-next-line @typescript-eslint/no-explicit-any -- guac-client-notification also passes false and plain objects as its status
    @Input({ required: true }) notification!: Notification | any;

    /**
     * The percentage of the operation that has been completed, if known.
     */
    progressPercent: number | null = null;

    /**
     * The time remaining in the countdown, if any.
     */
    timeRemaining: number | null = null;

    /**
     * The interval used to update the countdown, if any.
     */
    private interval: number | null = null;

    /**
     * Form group holding the values of the form fields of the notification,
     * with a control for exactly each property of the formModel of the
     * notification. Any change is written back into that same formModel.
     * The form group and its controls remain the same instances when the
     * notification changes, as each field receives its control only once,
     * when the field is created.
     */
    readonly notificationFormGroup: FormGroup = new FormGroup({});

    /**
     * Inject required services, and write the values of the form fields
     * into the formModel of the current notification as they change.
     */
    constructor(private destroyRef: DestroyRef) {
        this.notificationFormGroup.valueChanges
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(() => Object.assign(this.notification.formModel, this.notificationFormGroup.getRawValue()));
    }

    /**
     * Custom change detection logic for the component.
     */
    ngDoCheck(): void {

        // Update progress bar if end known
        this.progressPercent = this.notification?.progress?.ratio * 100;

    }

    /**
     * Update the form group to the formModel of the notification and restart
     * the countdown, if any, whenever the notification input changes.
     */
    ngOnChanges(changes: SimpleChanges): void {
        if (changes['notification']) {

            // Provide a form control for exactly the properties of the model,
            // such that only the corresponding fields are shown, without
            // writing these values back into the model
            const formModel: Record<string, string> = this.notification.formModel || {};
            for (const name of Object.keys(this.notificationFormGroup.controls)) {
                if (!(name in formModel))
                    this.notificationFormGroup.removeControl(name, { emitEvent: false });
            }

            for (const [name, value] of Object.entries(formModel)) {
                const control = this.notificationFormGroup.get(name);
                if (control)
                    control.setValue(value, { emitEvent: false });
                else
                    this.notificationFormGroup.addControl(name, new FormControl(value), { emitEvent: false });
            }

            const countdown = this.notification.countdown;

            // Clean up any existing interval
            if (this.interval)
                window.clearInterval(this.interval);

            // Update and handle countdown, if provided
            if (countdown) {

                this.timeRemaining = countdown.remaining;

                this.interval = window.setInterval(() => {

                    // Update time remaining
                    this.timeRemaining!--;

                    // Stop counting and call countdown callback when time
                    // remaining expires
                    if (this.timeRemaining === 0) {
                        window.clearInterval(this.interval!);
                        this.interval = null;
                        countdown.callback?.();
                    }

                }, 1000);

            }

        }

    }

    /**
     * Clean up interval upon destruction.
     */
    ngOnDestroy(): void {
        if (this.interval)
            window.clearInterval(this.interval);
    }


}
