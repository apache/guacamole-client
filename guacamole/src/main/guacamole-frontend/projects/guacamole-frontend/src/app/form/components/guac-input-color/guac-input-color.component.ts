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

import { Component, ElementRef, Input, ViewChild, ViewEncapsulation } from '@angular/core';
import { FormControl } from '@angular/forms';
import { ColorPickerService } from '../../service/color-picker.service';

/**
 * A component which implements a color input field. If the underlying color
 * picker implementation cannot be used due to a lack of browser support, this
 * directive will become read-only, functioning essentially as a color preview.
 *
 * @see colorPickerService
 */
@Component({
    selector: 'guac-input-color',
    templateUrl: './guac-input-color.component.html',
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class GuacInputColorComponent {

    /**
     * The current selected color value, in standard 6-digit hexadecimal
     * RGB notation. When the user selects a different color using this
     * directive, this value will be updated accordingly.
     */
    @Input({ required: true }) control!: FormControl<string | null>;

    /**
     * An optional array of colors to include within the color picker as a
     * convenient selection of pre-defined colors. The colors within the
     * array must be in standard 6-digit hexadecimal RGB notation.
     */
    @Input() palette?: string[];

    /**
     * Reference to div element that should be used as a button to open the color picker.
     */
    @ViewChild('colorPicker', { static: true }) colorInput!: ElementRef<HTMLDivElement>;

    /**
     * Inject required services.
     */
    constructor(private colorPickerService: ColorPickerService) {
    }

    /**
     * Returns whether the underlying color picker can be used.
     *
     * @returns
     *     true if the underlying color picker can be used, false otherwise.
     */
    isColorPickerAvailable(): boolean {
        return this.colorPickerService.isAvailable();
    }

    /**
     * Returns whether the color currently selected is "dark" in the sense
     * that the color white will have higher contrast against it than the
     * color black.
     *
     * @returns
     *     true if the currently selected color is relatively dark (white
     *     text would provide better contrast than black), false otherwise.
     */
    isDark(): boolean {

        // Assume not dark if color is invalid or undefined
        const rgb = this.control.value && /^#([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})$/.exec(this.control.value);
        if (!rgb)
            return false;

        // Parse color component values as hexadecimal
        const red = parseInt(rgb[1], 16);
        const green = parseInt(rgb[2], 16);
        const blue = parseInt(rgb[3], 16);

        // Convert RGB to luminance in HSL space (as defined by the
        // relative luminance formula given by the W3C for accessibility)
        const luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue;

        // Consider the background to be dark if white text over that
        // background would provide better contrast than black
        return luminance <= 153; // 153 is the component value 0.6 converted from 0-1 to the 0-255 range

    }

    /**
     * Prompts the user to choose a color by displaying a color selection
     * dialog. If the user chooses a color, this directive's model is
     * automatically updated. If the user cancels the dialog, the model is
     * left untouched.
     */
    selectColor(): void {
        this.colorPickerService.selectColor(this.colorInput.nativeElement, this.control.value, this.palette)
            .then((color: string) => {
                this.control.setValue(color);
            }).catch(() => {
            // Do nothing if the user cancels the dialog
        });
    }

}
