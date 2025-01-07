

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

import { Directive, ElementRef, forwardRef, HostListener, Renderer2 } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

/**
 * A directive which modifies the parsing of a form control value when used
 * on a number input field. The behavior of this directive for other input elements
 * is undefined. Converts between human-readable zoom percentage and display scale.
 */
@Directive({
    selector : '[guacZoomCtrl]',
    providers: [
        {
            provide    : NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => GuacZoomCtrlDirective),
            multi      : true
        }
    ]
})
export class GuacZoomCtrlDirective implements ControlValueAccessor {

    /**
     * Callback function that has to be called when the control's value changes in the UI.
     *
     * @param value
     *    The new value of the control.
     */
    private onChange!: (value: any) => void;

    /**
     * Callback function that has to be called when the control's value changes in the UI.
     */
    private onTouched!: () => void;

    constructor(private renderer: Renderer2, private el: ElementRef) {
    }

    registerOnChange(fn: any): void {
        this.onChange = fn;
    }

    registerOnTouched(fn: any): void {
        this.onTouched = fn;
    }

    setDisabledState(isDisabled: boolean): void {
        this.renderer.setProperty(this.el.nativeElement, 'disabled', isDisabled);
    }

    /**
     * Called by the forms API to write to the view when programmatic changes from model to view are requested.
     * Multiplies the value by 100.
     *
     * @param value
     *     The new value for the input element.
     */
    writeValue(value: any): void {
        const newValue = Math.round(value * 100);
        this.renderer.setProperty(this.el.nativeElement, 'value', newValue);
    }


    /**
     * Form control will be updated on input.
     * The value is divided by 100.
     */
    @HostListener('input', ['$event.target.value'])
    onInput(value: string): void {
        const parsedValue = Math.round(Number(value)) / 100;
        this.onChange(parsedValue);
        this.onTouched();
    }

}
