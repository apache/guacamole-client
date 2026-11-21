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
import { KeyEventDisplayService } from './key-event-display.service';

describe('KeyEventDisplayService', () => {

    let service: KeyEventDisplayService;

    /**
     * The X11 keysym of the left Shift key, a modifier that produces no text.
     */
    const SHIFT_LEFT = 0xFFE1;

    /**
     * The X11 keysym of the left Control key, a modifier commonly used in
     * shortcuts.
     */
    const CONTROL_LEFT = 0xFFE3;

    /**
     * Returns a raw key event as produced by KeyEventInterpreter.
     *
     * @param keysym
     *     The X11 keysym of the key.
     *
     * @param name
     *     The name of the key.
     *
     * @param pressed
     *     Whether the key was pressed (true) or released (false).
     *
     * @param timestamp
     *     The time of the event in milliseconds.
     *
     * @param [value]
     *     The character the key produces, if it is printable.
     *
     * @returns
     *     The raw key event.
     */
    const keyEvent = (keysym: number, name: string, pressed: boolean, timestamp: number, value?: string): Guacamole.KeyEventInterpreter.KeyEvent =>
        ({ definition: { keysym, name, value }, pressed, timestamp });

    /**
     * Returns the raw events of a single keystroke, i.e. pressing and
     * releasing the given printable key.
     *
     * @param character
     *     The character produced by the key.
     *
     * @param timestamp
     *     The time of the key press in milliseconds.
     *
     * @returns
     *     The raw events of the key press and the key release.
     */
    const typeKey = (character: string, timestamp: number): Guacamole.KeyEventInterpreter.KeyEvent[] => [
        keyEvent(character.charCodeAt(0), character, true, timestamp, character),
        keyEvent(character.charCodeAt(0), character, false, timestamp + 10, character)
    ];

    beforeEach(() => {
        service = TestBed.inject(KeyEventDisplayService);
    });

    it('should return no batches if there are no key events', () => {
        expect(service.parseEvents([])).toEqual([]);
    });

    it('should return no batches if the key events produce no text', () => {
        const events = [keyEvent(SHIFT_LEFT, 'Shift_L', true, 0), keyEvent(SHIFT_LEFT, 'Shift_L', false, 100)];

        expect(service.parseEvents(events)).toEqual([]);
    });

    it('should collect the typed text of close key events in a single batch', () => {
        const events = [...typeKey('a', 0), ...typeKey('b', 1000)];

        const batches = service.parseEvents(events);

        expect(batches.length).toBe(1);
        expect(batches[0].simpleValue).toBe('ab');
        expect(batches[0].events.map(event => event.text)).toEqual(['a', 'b']);
    });

    it('should split the key events into batches separated by the batch separation', () => {
        const events = [...typeKey('a', 0), ...typeKey('b', 6000)];

        const batches = service.parseEvents(events, 5000);

        expect(batches.map(batch => batch.simpleValue)).toEqual(['a', 'b']);
    });

    it('should omit a batch that ended up without any events', () => {
        // Pressing Shift after the pause starts a new batch, but produces no text
        const events = [...typeKey('a', 0), keyEvent(SHIFT_LEFT, 'Shift_L', true, 10000)];

        const batches = service.parseEvents(events, 5000);

        expect(batches.map(batch => batch.simpleValue)).toEqual(['a']);
    });

    it('should consolidate consecutive typed events if requested', () => {
        const events = [...typeKey('a', 0), ...typeKey('b', 1000)];

        const batches = service.parseEvents(events, 5000, true);

        expect(batches[0].events.length).toBe(1);
        expect(batches[0].events[0].text).toBe('ab');
    });

    it('should print keys in the order they were pressed, not released', () => {
        // "b" is pressed while "a" is still held, and released first
        const events = [
            keyEvent(0x61, 'a', true, 0, 'a'),
            keyEvent(0x62, 'b', true, 50, 'b'),
            keyEvent(0x62, 'b', false, 100, 'b'),
            keyEvent(0x61, 'a', false, 150, 'a')
        ];

        const batches = service.parseEvents(events);

        expect(batches.map(batch => batch.simpleValue)).toEqual(['ab']);
    });

    it('should print a shortcut only once, without the key itself', () => {
        const events = [
            ...typeKey('a', 0),
            keyEvent(CONTROL_LEFT, 'Ctrl', true, 1000),
            ...typeKey('c', 1100),
            keyEvent(CONTROL_LEFT, 'Ctrl', false, 1300)
        ];

        const batches = service.parseEvents(events);

        expect(batches.map(batch => batch.simpleValue)).toEqual(['a<Ctrl+c>']);
    });

    it('should use the time a key was pressed as the time of its event', () => {
        const events = [keyEvent(0x61, 'a', true, 1000, 'a'), keyEvent(0x61, 'a', false, 1500, 'a')];

        const batches = service.parseEvents(events);

        expect(batches[0].events.map(event => event.timestamp)).toEqual([1000]);
    });

});
