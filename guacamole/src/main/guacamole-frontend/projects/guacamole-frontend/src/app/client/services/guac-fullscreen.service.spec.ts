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
import { GuacFullscreenService } from './guac-fullscreen.service';

describe('GuacFullscreenService', () => {

    /**
     * Mock of the Keyboard Lock API that records calls to lock() and unlock().
     */
    let keyboard: { lock: ReturnType<typeof vi.fn>, unlock: ReturnType<typeof vi.fn> };

    /**
     * The "fullscreenchange" listeners registered on the document while a
     * test runs. The document is shared by all tests, so they are removed
     * again afterwards.
     */
    let listeners: EventListenerOrEventListenerObject[];

    /**
     * Mock of Element.requestFullscreen() of the document element.
     */
    let requestFullscreen: ReturnType<typeof vi.fn>;

    /**
     * Defines a property which the DOM environment of the specs lacks.
     *
     * @param target
     *     The object to define the property on.
     *
     * @param name
     *     The name of the property.
     *
     * @param value
     *     The value of the property.
     */
    function define(target: object, name: string, value: unknown): void {
        Object.defineProperty(target, name, { configurable: true, value });
    }

    /**
     * Sets document.fullscreenElement and dispatches the "fullscreenchange"
     * event like the browser does after entering or leaving fullscreen mode.
     *
     * @param element
     *     The element that is now shown fullscreen, or null if the document
     *     left fullscreen mode.
     */
    function changeFullscreen(element: Element | null): void {
        define(document, 'fullscreenElement', element);
        document.dispatchEvent(new Event('fullscreenchange'));
    }

    beforeEach(() => {
        keyboard = { lock: vi.fn().mockResolvedValue(undefined), unlock: vi.fn() };
        requestFullscreen = vi.fn().mockResolvedValue(undefined);
        listeners = [];

        const addEventListener = document.addEventListener.bind(document);
        vi.spyOn(document, 'addEventListener').mockImplementation((type, listener, options) => {
            if (type === 'fullscreenchange')
                listeners.push(listener);
            addEventListener(type, listener, options);
        });

        define(navigator, 'keyboard', keyboard);
        define(document, 'fullscreenEnabled', true);
        define(document, 'fullscreenElement', null);
        define(document.documentElement, 'requestFullscreen', requestFullscreen);
        define(document, 'exitFullscreen', vi.fn().mockResolvedValue(undefined));
    });

    afterEach(() => {
        vi.restoreAllMocks();
        for (const listener of listeners)
            document.removeEventListener('fullscreenchange', listener);

        Reflect.deleteProperty(navigator, 'keyboard');
        Reflect.deleteProperty(document, 'fullscreenEnabled');
        Reflect.deleteProperty(document, 'fullscreenElement');
        Reflect.deleteProperty(document, 'exitFullscreen');
        Reflect.deleteProperty(document.documentElement, 'requestFullscreen');
    });

    it('should not lock the keyboard when fullscreen mode is only requested', () => {
        const service = TestBed.inject(GuacFullscreenService);

        service.setFullscreenMode(true);

        expect(requestFullscreen).toHaveBeenCalledTimes(1);
        expect(keyboard.lock).not.toHaveBeenCalled();
    });

    it('should lock the keyboard once the document has entered fullscreen mode', () => {
        TestBed.inject(GuacFullscreenService);

        changeFullscreen(document.documentElement);

        expect(keyboard.lock).toHaveBeenCalledTimes(1);
        expect(keyboard.unlock).not.toHaveBeenCalled();
    });

    it('should unlock the keyboard once the document has left fullscreen mode', () => {
        TestBed.inject(GuacFullscreenService);

        changeFullscreen(null);

        expect(keyboard.unlock).toHaveBeenCalledTimes(1);
        expect(keyboard.lock).not.toHaveBeenCalled();
    });

    it('should not unlock the keyboard when fullscreen mode is only left', () => {
        const service = TestBed.inject(GuacFullscreenService);
        define(document, 'fullscreenElement', document.documentElement);

        service.setFullscreenMode(false);

        expect(document.exitFullscreen).toHaveBeenCalledTimes(1);
        expect(keyboard.unlock).not.toHaveBeenCalled();
    });

    it('should not fail on fullscreen changes if the browser lacks keyboard lock', () => {
        define(navigator, 'keyboard', undefined);
        const errors: ErrorEvent[] = [];
        const onError = (event: ErrorEvent) => errors.push(event);
        window.addEventListener('error', onError);

        try {
            TestBed.inject(GuacFullscreenService);
            changeFullscreen(document.documentElement);
            changeFullscreen(null);
        } finally {
            window.removeEventListener('error', onError);
        }

        expect(errors).toEqual([]);
        expect(listeners).toEqual([]);
    });

});
