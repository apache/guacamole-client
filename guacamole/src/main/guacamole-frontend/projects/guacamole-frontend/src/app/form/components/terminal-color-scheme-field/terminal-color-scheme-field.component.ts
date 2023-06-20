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
import { ColorScheme } from '../../types/ColorScheme';
import { FormFieldBaseComponent, getFieldOption } from '../form-field-base/form-field-base.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';
import includes from 'lodash/includes';

/**
 * The string value which is assigned to selectedColorScheme if a custom
 * color scheme is selected.
 */
const CUSTOM_COLOR_SCHEME: string = 'custom';

/**
 * The palette indices of all colors which are considered low-intensity.
 */
const lowIntensity = [0, 1, 2, 3, 4, 5, 6, 7] as const;

/**
 * The palette indices of all colors which are considered high-intensity.
 */
const highIntensity = [8, 9, 10, 11, 12, 13, 14, 15] as const;

/**
 * The palette indices of all colors.
 */
const terminalColorIndices = [...lowIntensity, ...highIntensity] as const;

/**
 * Type definition for all possible values of a terminal color palette index.
 */
type TerminalColorIndex = typeof terminalColorIndices[number];

/**
 * Component for terminal color scheme fields.
 */
@Component({
    selector: 'guac-terminal-color-scheme-field',
    templateUrl: './terminal-color-scheme-field.component.html',
    encapsulation: ViewEncapsulation.None
})
export class TerminalColorSchemeFieldComponent extends FormFieldBaseComponent implements OnInit, OnChanges {


    /**
     * The ID value that should be used to associate
     * the relevant input element with the label provided by the
     * guacFormField component, if there is such an input element.
     */
    @Input() fieldId?: string;

    /**
     * The currently selected color scheme. If a pre-defined color scheme is
     * selected, this will be the connection parameter value associated with
     * that color scheme. If a custom color scheme is selected, this will be
     * the string "custom".
     */
    selectedColorScheme: FormControl<string | null> = new FormControl<string>('');

    /**
     * The array of colors to include within the color picker as pre-defined
     * options for convenience.
     */
    defaultPalette: string[] = new ColorScheme().colors;

    /**
     * Whether the raw details of the custom color scheme should be shown. By
     * default, such details are hidden.
     *
     * @default false
     */
    detailsShown: boolean = false;

    /**
     * Form group which contains the form controls for the custom color scheme.
     */
    customColorSchemeFormGroup!: FormGroup<{
        background: FormControl<string | null>,
        foreground: FormControl<string | null>,
        colors: FormGroup<Record<TerminalColorIndex, FormControl<string | null>>>
    }>;

    /**
     * The form control for the background color of the custom color scheme.
     */
    get customColorSchemeBackground(): FormControl<string> {
        return this.customColorSchemeFormGroup.get('background') as FormControl<string>;
    }

    /**
     * The form control for the foreground color of the custom color scheme.
     */
    get customColorSchemeForeground(): FormControl<string> {
        return this.customColorSchemeFormGroup.get('foreground') as FormControl<string>;
    }

    /**
     * The form control for the color at the given palette index of the custom
     * color scheme.
     * @param index
     *     The palette index of the color to retrieve.
     *
     * @returns
     *     The form control for the color at the given palette index.
     */
    customColorSchemeColorFormControl(index: TerminalColorIndex): FormControl<string> {
        return this.customColorSchemeFormGroup.get('colors')?.get(index.toString()) as FormControl<string>;
    }

    constructor(private destroyRef: DestroyRef, private fb: FormBuilder) {
        super();

        // Initialize empty form group for custom color scheme colors
        const colorFormControls: Record<TerminalColorIndex, FormControl<string | null>> = {} as any;

        // Create form controls for each color
        terminalColorIndices.forEach((index: TerminalColorIndex) => {
            colorFormControls[index] = this.fb.control('');
        });

        // Create form group for custom color scheme
        this.customColorSchemeFormGroup = this.fb.group({
            background: this.fb.control(''),
            foreground: this.fb.control(''),
            colors: this.fb.group(colorFormControls)
        });

    }

    /**
     * Sets the values of the form controls to the given color scheme.
     *
     * @param customColorScheme
     *     The custom color scheme to set.
     */
    private setCustomColorScheme(customColorScheme: ColorScheme): void {

        this.customColorSchemeFormGroup.setValue({
            background: customColorScheme.background,
            foreground: customColorScheme.foreground,
            // Transform color array into object with indices as keys
            colors: terminalColorIndices.reduce((acc, index: TerminalColorIndex) => {
                acc[index] = customColorScheme.colors[index];
                return acc;
            }, {} as Record<TerminalColorIndex, string>)
        }, {onlySelf: true, emitEvent: false});
    }

    /**
     * Constructs a {@link ColorScheme} object from the values of the form controls.
     *
     * @returns
     *     Color scheme object representing the current color scheme.
     */
    private getCustomColorScheme(): ColorScheme {
        return new ColorScheme({
            background: this.customColorSchemeBackground.value,
            foreground: this.customColorSchemeForeground.value,
            colors: terminalColorIndices.map(index => this.customColorSchemeColorFormControl(index).value)
        });
    }

    /**
     * Initializes the component by setting up value change listeners for the `control` and `selectedColorScheme`
     * form controls.
     */
    ngOnInit(): void {

        /**
         * Updates the component data based on the given string value from the form control input.
         */
        const updateComponentData = (value: string | null): void => {
            if (this.selectedColorScheme.value === CUSTOM_COLOR_SCHEME || (value && !includes(this.field.options, value))) {
                this.setCustomColorScheme(ColorScheme.fromString(value as string));
                this.selectedColorScheme.setValue(CUSTOM_COLOR_SCHEME, {emitEvent: false});
            } else
                this.selectedColorScheme.setValue(value || '', {emitEvent: false});
        }

        // Set initial value of the selected color scheme control
        const initialValue: string | null = this.control?.value === undefined ? null : this.control.value;
        updateComponentData(initialValue);

        // Keep selected color scheme and custom color scheme in sync with changes
        // to model
        this.control?.valueChanges
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(value => {
                updateComponentData(value);
            });

        // Keep model in sync with changes to selected color scheme
        this.selectedColorScheme.valueChanges
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(selectedColorScheme => {
                if (!selectedColorScheme)
                    this.control?.setValue('');
                else if (selectedColorScheme === CUSTOM_COLOR_SCHEME)
                    this.control?.setValue(ColorScheme.toString(this.getCustomColorScheme()));
                else
                    this.control?.setValue(selectedColorScheme);
            });

        // Keep model in sync with changes to custom color scheme
        this.customColorSchemeFormGroup?.valueChanges
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(() => {
                if (this.selectedColorScheme.value === CUSTOM_COLOR_SCHEME)
                    this.control?.setValue(ColorScheme.toString(this.getCustomColorScheme()));
            });
    }

    /**
     * Apply disabled state to selected color scheme controls.
     */
    ngOnChanges(changes: SimpleChanges): void {
        if (changes['disabled']) {

            const disabled: boolean = changes['disabled'].currentValue;
            this.setDisabledState(this.selectedColorScheme, disabled);
            this.setDisabledState(this.control, disabled);

        }

    }

    /**
     * Returns whether a custom color scheme has been selected.
     *
     * @returns
     *     true if a custom color scheme has been selected, false otherwise.
     */
    isCustom(): boolean {
        return this.selectedColorScheme.value === CUSTOM_COLOR_SCHEME;
    }

    /**
     * Shows the raw details of the custom color scheme. If the details are
     * already shown, this function has no effect.
     */
    showDetails(): void {
        this.detailsShown = true;
    }

    /**
     * Hides the raw details of the custom color scheme. If the details are
     * already hidden, this function has no effect.
     */
    hideDetails(): void {
        this.detailsShown = false;
    }

    /**
     * Make getFieldOption available in template.
     */
    readonly getFieldOption = getFieldOption;

    /**
     * Make lowIntensity available in template.
     */
    readonly lowIntensity = lowIntensity;

    /**
     * Make highIntensity available in template.
     */
    readonly highIntensity = highIntensity;
}
