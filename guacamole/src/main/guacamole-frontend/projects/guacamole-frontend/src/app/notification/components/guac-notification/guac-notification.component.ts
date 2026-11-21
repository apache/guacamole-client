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
import { FormGroup } from '@angular/forms';
import { Subscription } from 'rxjs';
import { FormService } from '../../../form/service/form.service';
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
     * Form group holding the values of the form fields of the notification.
     * Any change is written back to the formModel of the notification.
     */
    notificationFormGroup: FormGroup = new FormGroup({});

    /**
     * The subscription which writes the values of notificationFormGroup back
     * into the current notification, if any.
     */
    private formValueSubscription: Subscription | null = null;

    /**
     * Inject required services.
     */
    constructor(private formService: FormService,
                private destroyRef: DestroyRef) {
    }

    /**
     * Custom change detection logic for the component.
     */
    ngDoCheck(): void {

        // Update progress bar if end known
        if (this.notification?.progress?.ratio) {
            this.progressPercent = this.notification.progress.ratio * 100;
        }

    }

    /**
     * Build the form group for the fields of the notification and restart
     * the countdown, if any, whenever the notification input changes.
     */
    ngOnChanges(changes: SimpleChanges): void {
        if (changes['notification']) {
            const forms = this.formService.asFormArray(this.notification.forms);
            this.notificationFormGroup = this.formService.getFormGroup(forms);
            this.notificationFormGroup.patchValue(this.notification.formModel || {});

            // Stop writing the values of the previous form group
            this.formValueSubscription?.unsubscribe();
            this.formValueSubscription = this.notificationFormGroup.valueChanges
                .pipe(takeUntilDestroyed(this.destroyRef))
                .subscribe((value) => {
                    this.notification.formModel = value;
                });

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

                    // Call countdown callback when time remaining expires
                    if (this.timeRemaining === 0 && countdown.callback)
                        countdown.callback();

                }, 1000, this.timeRemaining);

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
