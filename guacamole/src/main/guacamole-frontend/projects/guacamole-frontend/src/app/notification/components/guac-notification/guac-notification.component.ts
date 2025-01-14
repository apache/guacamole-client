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

import { DOCUMENT } from '@angular/common';
import {
    Component,
    DestroyRef, DoCheck,
    Inject,
    Input,
    OnChanges,
    OnDestroy,
    SimpleChanges,
    ViewEncapsulation
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormGroup } from '@angular/forms';
import { FormService } from '../../../form/service/form.service';
import { Notification } from '../../types/Notification';

/**
 * A directive for displaying notifications.
 */
@Component({
    selector     : 'guac-notification',
    templateUrl  : './guac-notification.component.html',
    encapsulation: ViewEncapsulation.None
})
export class GuacNotificationComponent implements OnChanges, DoCheck, OnDestroy {

    /**
     * The notification to display.
     */
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
     * TODO
     */
    notificationFormGroup: FormGroup = new FormGroup({});

    constructor(private formService: FormService,
                private destroyRef: DestroyRef) {
    }

    /**
     * Custom change detection logic for the component.
     */
    ngDoCheck(): void {

        // Update progress bar if end known
        if(this.notification?.progress?.ratio) {
            this.progressPercent = this.notification.progress.ratio * 100;
        }

    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['notification']) {
            const forms = this.formService.asFormArray(this.notification.forms);
            this.notificationFormGroup = this.formService.getFormGroup(forms);
            this.notificationFormGroup.patchValue(this.notification.formModel || {});
            this.notificationFormGroup.valueChanges
                .pipe(takeUntilDestroyed(this.destroyRef))
                .subscribe((value) => {
                    this.notification.formModel = value;
                })

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
