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

import { TestBed } from '@angular/core/testing';
import { TranslocoService } from '@jsverse/transloco';
import { Observable, of, Subject } from 'rxjs';
import { ColorPickerService } from './color-picker.service';

describe('ColorPickerService', () => {

    /**
     * Elements the color picker is requested for, which are removed again
     * after each test.
     */
    let elements: HTMLElement[];

    beforeEach(() => {
        elements = [];

        // Pickr completes its initialization only once its dialog has a
        // width, which jsdom never computes
        vi.spyOn(HTMLElement.prototype, 'offsetWidth', 'get').mockReturnValue(100);
    });

    afterEach(() => {
        elements.forEach(element => element.remove());
        vi.restoreAllMocks();
    });

    /**
     * Creates the service with a TranslocoService that provides the given
     * translated strings.
     *
     * @param selectTranslate
     *     The implementation of TranslocoService.selectTranslate().
     *
     * @returns
     *     The new ColorPickerService.
     */
    function createService(selectTranslate: (key: string) => Observable<string>): ColorPickerService {
        TestBed.configureTestingModule({
            providers: [{ provide: TranslocoService, useValue: { selectTranslate } }]
        });
        return TestBed.inject(ColorPickerService);
    }

    /**
     * Creates an element attached to the document, as the element a user
     * interacts with to request a color.
     *
     * @returns
     *     The new element.
     */
    function createElement(): HTMLElement {
        const element = document.createElement('div');
        document.body.appendChild(element);
        elements.push(element);
        return element;
    }

    /**
     * Waits until the shared Pickr container has been moved into the given
     * element, which selectColor() does once it has made the request active.
     *
     * @param service
     *     The service whose Pickr container is waited for.
     *
     * @param element
     *     The element which has to contain the Pickr container.
     */
    async function waitForPicker(service: ColorPickerService, element: HTMLElement): Promise<void> {
        await vi.waitFor(() => expect(element.contains(service.pickerContainer)).toBe(true));
    }

    /**
     * Saves the given color in the color picker, as if the user had entered
     * it and clicked "Save".
     *
     * @param service
     *     The service whose color picker is used.
     *
     * @param color
     *     The color to save, in 6-digit hexadecimal RGB format with "#"
     *     prefix.
     */
    function save(service: ColorPickerService, color: string): void {
        service.pickr!.setColor(color, true);
        service.pickr!.applyColor();
    }

    it('should resolve a request with the saved color', async () => {
        const service = createService(key => of(key));
        const element = createElement();

        const request = service.selectColor(element, '#112233');
        await waitForPicker(service, element);
        save(service, '#445566');

        expect(await request).toBe('#445566');
    });

    it('should reject a request if the color picker is canceled', async () => {
        const service = createService(key => of(key));
        const element = createElement();

        const request = service.selectColor(element, '#112233')
            .then(() => 'resolved', () => 'rejected');
        await waitForPicker(service, element);
        const root = service.pickr!.getRoot() as unknown as { interaction: { cancel: HTMLElement } };
        root.interaction.cancel.click();

        expect(await request).toBe('rejected');
    });

    it('should resolve a second request while the first request is still active', async () => {
        const service = createService(key => of(key));
        const first = createElement();
        const second = createElement();

        const firstRequest = service.selectColor(first, '#112233')
            .then(color => color, () => 'rejected');
        await waitForPicker(service, first);

        const secondRequest = service.selectColor(second, '#445566')
            .then(color => color, () => 'rejected');
        await waitForPicker(service, second);
        save(service, '#778899');

        expect(await secondRequest).toBe('#778899');
        expect(await firstRequest).toBe('rejected');
    });

    it('should wait for the translations before showing the color picker', async () => {
        const translations = new Subject<string>();
        const service = createService(() => translations);
        const element = createElement();

        // Request a color before the labels of the buttons have been loaded
        let request!: Promise<string>;
        expect(() => request = service.selectColor(element, '#112233')).not.toThrow();
        expect(element.contains(service.pickerContainer)).toBe(false);

        translations.next('translated');
        await waitForPicker(service, element);
        save(service, '#445566');

        expect(await request).toBe('#445566');
    });

});
