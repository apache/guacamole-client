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

import { Component, DestroyRef, Input, OnChanges, OnInit, SimpleChanges, ViewEncapsulation } from '@angular/core';
import { FormFieldBaseComponent } from '../form-field-base/form-field-base.component';
import { FormControl } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

/**
 * Component to display number fields.
 */
@Component({
    selector: 'guac-number-field',
    templateUrl: './number-field.component.html',
    encapsulation: ViewEncapsulation.None
})
export class NumberFieldComponent extends FormFieldBaseComponent implements OnInit, OnChanges {

    /**
     * The ID value that should be used to associate
     * the relevant input element with the label provided by the
     * guacFormField component, if there is such an input element.
     */
    @Input() fieldId?: string;

    /**
     * Internal form control that holds the typed value
     * of the field.
     */
    typedControl: FormControl<number | null> = new FormControl<number | null>(null);


    constructor(private destroyRef: DestroyRef) {
        super();
    }

    /**
     * Initializes the component by setting up value change listeners for the `control` and `typedControl`
     * form controls.
     */
    ngOnInit(): void {
        // Set initial value of typed control
        const initialValue: number | null = this.control?.value ? Number(this.control.value) : null;
        this.typedControl.setValue(initialValue, {emitEvent: false});

        // Update typed value when model is changed
        this.control?.valueChanges
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(value => {
                this.typedControl.setValue((value ? Number(value) : null), {emitEvent: false});
            });

        // Update string value in model when typed value is changed
        this.typedControl.valueChanges
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(typedValue => {
                this.control?.setValue(((typedValue || typedValue === 0) ? typedValue.toString() : ''));
            });
    }

    /**
     * Apply disabled state to typed form control.
     */
    ngOnChanges(changes: SimpleChanges): void {

        if (changes['disabled']) {

            const disabled: boolean = changes['disabled'].currentValue;
            this.setDisabledState(this.typedControl, disabled);

        }
    }

}
