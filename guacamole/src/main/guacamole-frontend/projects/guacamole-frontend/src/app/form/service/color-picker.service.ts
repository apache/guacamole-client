

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
import { Inject, Injectable } from '@angular/core';
import { TranslocoService } from '@ngneat/transloco';
import Pickr from '@simonwep/pickr';
import { forkJoin, take } from 'rxjs';
import HSVaColor = Pickr.HSVaColor;

/**
 * A service for prompting the user to choose a color using the "Pickr" color
 * picker. As the Pickr color picker might not be available if the JavaScript
 * features it requires are not supported by the browser (Internet Explorer),
 * the isAvailable() function should be used to test for usability.
 */
@Injectable({
    providedIn: 'root'
})
export class ColorPickerService {

    /**
     * A singleton instance of the "Pickr" color picker, shared by all users of
     * this service. Pickr does not initialize synchronously, nor is it
     * supported by all browsers. If Pickr is not yet initialized, or is
     * unsupported, this will be null.
     */
    pickr?: Pickr = undefined;

    /**
     * Whether Pickr has completed initialization.
     */
    public pickrInitComplete = false;

    /**
     * The HTML element to provide to Pickr as the root element.
     */
    pickerContainer: HTMLDivElement;

    /**
     * A Promise which represents an active request for the
     * user to choose a color. The promise will
     * be resolved with the chosen color once a color is chosen, and rejected
     * if the request is cancelled or Pickr is not available. If no request is
     * active, this will be undefined.
     */
    activeRequest?: Promise<string> = undefined;

    /**
     * Resolve function of the {@link activeRequest} promise.
     */
    private activeRequestResolve?: (value: string) => void = undefined;

    /**
     * Reject function of the {@link activeRequest} promise.
     */
    private activeRequestReject?: (reason?: any) => void = undefined;

    private saveString?: string;
    private cancelString?: string;

    constructor(@Inject(DOCUMENT) private document: Document, private translocoService: TranslocoService) {

        // Create container element for Pickr
        this.pickerContainer = this.document.createElement('div');
        this.pickerContainer.className = 'shared-color-picker';

        // Wait for translation strings
        const saveString = this.translocoService.selectTranslate('APP.ACTION_SAVE').pipe(take(1));
        const cancelString = this.translocoService.selectTranslate('APP.ACTION_CANCEL').pipe(take(1));

        forkJoin({
            saveString,
            cancelString
        }).subscribe(({ saveString, cancelString }) => {
            this.saveString = saveString;
            this.cancelString = cancelString;

            this.createPickr();
            this.createPickrPromise();
        });

    }

    private createPickrPromise(): void {
        this.pickrPromise = new Promise<void>((resolve, reject) => {

            // Resolve promise when Pickr has completed initialization
            if (this.pickrInitComplete)
                resolve();
            else if (this.pickr)
                this.pickr.on('init', resolve);

            // Reject promise if Pickr cannot be used at all
            else
                reject();
        });
    }

    /**
     * Resolves the current active request with the given color value. If no
     * color value is provided, the active request is rejected. If no request
     * is active, this function has no effect.
     *
     * @param color
     *     The color value to resolve the active request with.
     */
    completeActiveRequest(color?: string): void {
        if (this.activeRequest) {

            // Hide color picker, if shown
            this.pickr?.hide();

            // Resolve/reject active request depending on value provided
            if (color)
                this.activeRequestResolve?.(color);
            else
                this.activeRequestReject?.();

            // No active request
            this.activeRequest = undefined;

        }
    }

    createPickr(): void {
        try {

            this.pickr = Pickr.create({

                // Bind color picker to the container element
                el: this.pickerContainer,

                // Wrap color picker dialog in Guacamole-specific class for
                // sake of additional styling
                appClass: 'guac-input-color-picker',

                default: '#000000',

                // Display color details as hex
                defaultRepresentation: 'HEXA',

                // Use "monolith" theme, as a nice balance between "nano" (does
                // not work in Internet Explorer) and "classic" (too big)
                theme: 'monolith',

                // Leverage the container element as the button which shows the
                // picker, relying on our own styling for that button
                useAsButton: true,

                // Do not include opacity controls
                lockOpacity: true,

                // Include a selection of palette entries for convenience and
                // reference
                swatches: [],

                components: {

                    // Include hue and color preview controls
                    preview: true,
                    hue    : true,

                    // Display only a text color input field and the save and
                    // cancel buttons (no clear button)
                    interaction: {
                        input : true,
                        save  : true,
                        cancel: true
                    }

                },

                // Assign translated strings to button text
                i18n: {
                    'btn:save'  : this.saveString,
                    'btn:cancel': this.cancelString
                }

            });

            // Hide color picker after user clicks "cancel"
            this.pickr.on('cancel', () => {
                this.completeActiveRequest();
            });

            // Keep model in sync with changes to the color picker
            this.pickr.on('save', (color: HSVaColor | null) => {
                const colorString = color ? color.toHEXA().toString() : undefined;
                this.completeActiveRequest(colorString);
                this.activeRequest = undefined;
            });

            // Keep color picker in sync with changes to the model
            this.pickr.on('init', () => {
                this.pickrInitComplete = true;
            });
        } catch (e) {
            // If the "Pickr" color picker cannot be loaded (Internet Explorer),
            // the available flag will remain set to false
        }
    }

    /**
     * Promise which is resolved when Pickr initialization has completed
     * and rejected if Pickr cannot be used.
     */
    pickrPromise!: Promise<void>;

    /**
     * Returns whether the underlying color picker (Pickr) can be used by
     * calling selectColor(). If the browser cannot support the color
     * picker, false is returned.
     *
     * @returns
     *     true if the underlying color picker can be used by calling
     *     selectColor(), false otherwise.
     */
    isAvailable(): boolean {
        return this.pickrInitComplete;
    }

    /**
     * Prompts the user to choose a color, returning the color chosen via a
     * Promise.
     *
     * @param element
     *     The element that the user interacted with to indicate their
     *     desire to choose a color.
     *
     * @param current
     *     The color that should be selected by default, in standard
     *     6-digit hexadecimal RGB format, including "#" prefix.
     *
     * @param palette
     *     An array of color choices which should be exposed to the user
     *     within the color chooser for convenience. Each color must be in
     *     standard 6-digit hexadecimal RGB format, including "#" prefix.
     *
     * @returns
     *     A Promise which is resolved with the color chosen by the user,
     *     in standard 6-digit hexadecimal RGB format with "#" prefix, and
     *     rejected if the selection operation was cancelled or the color
     *     picker cannot be used.
     */
    selectColor(element: Element, current: string | null, palette: string[] = []): Promise<string> {

        const newRequest = new Promise<string>((resolve, reject) => {
            this.activeRequestResolve = resolve;
            this.activeRequestReject = reject;
        });

        // Show picker once Pickr is ready for use
        return this.pickrPromise.then(() => {
            if (!this.pickr) {
                return Promise.reject();
            }

            // Cancel any active request
            this.completeActiveRequest();

            // Reset state of color picker to provided parameters. Use silent=true
            // to avoid triggering the "save" event prematurely.
            this.pickr.setColor(current, true);
            this.pickr.applyColor(true);
            element.appendChild(this.pickerContainer);

            // Replace all color swatches with the palette of colors given
            while (this.pickr.removeSwatch(0)) {
            }
            for (const color of palette) {
                this.pickr.addSwatch(color);
            }

            // Show color picker and wait for user to complete selection
            this.activeRequest = newRequest;
            this.pickr.show();

            return newRequest;
        });
    }
}
