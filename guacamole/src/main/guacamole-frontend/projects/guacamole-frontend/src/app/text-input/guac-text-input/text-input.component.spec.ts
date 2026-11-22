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


import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { GuacEventService } from 'guacamole-frontend-lib';
import { GuacFrontendEventArguments } from '../../events/types/GuacFrontendEventArguments';
import { KeyComponent } from '../guac-key/key.component';
import { TextInputComponent } from './text-input.component';

/**
 * The Unicode codepoint of the zero-width space which the component uses
 * as padding around the content of its text area.
 */
const PADDING = '\u200B';

/**
 * The keysym of the sticky Ctrl key of the text input.
 */
const CTRL = 0xFFE3;

/**
 * The keysym of the sticky Alt key of the text input.
 */
const ALT = 0xFFE9;

describe('TextInputComponent', () => {
    let fixture: ComponentFixture<TextInputComponent>;
    let component: TextInputComponent;

    /**
     * The keysyms of all synthetic key presses broadcast by the component,
     * in the order of their broadcast.
     */
    let pressedKeysyms: number[];

    /**
     * All synthetic key presses and releases broadcast by the component, in
     * the order of their broadcast.
     */
    let keyEvents: ['keydown' | 'keyup', number][];

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [TextInputComponent, KeyComponent],
            imports: [
                TranslocoTestingModule.forRoot({
                    langs: {
                        en: {
                            CLIENT: {
                                NAME_KEY_CTRL: 'Ctrl',
                                NAME_KEY_ALT: 'Alt',
                                NAME_KEY_ESC: 'Esc',
                                NAME_KEY_TAB: 'Tab'
                            }
                        }
                    },
                    translocoConfig: {
                        availableLangs: ['en'],
                        defaultLang: 'en'
                    }
                })
            ]
        });

        pressedKeysyms = [];
        keyEvents = [];
        const guacEventService = TestBed.inject(GuacEventService<GuacFrontendEventArguments>);
        guacEventService.on('guacSyntheticKeydown').subscribe(({ keysym }) => {
            pressedKeysyms.push(keysym);
            keyEvents.push(['keydown', keysym]);
        });
        guacEventService.on('guacSyntheticKeyup')
            .subscribe(({ keysym }) => keyEvents.push(['keyup', keysym]));

        fixture = TestBed.createComponent(TextInputComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    afterEach(() => {
        fixture.destroy();
    });

    /**
     * Types the given text into the text area as the browser would, by
     * placing it between the padding characters and dispatching an input
     * event. Like Zone.js in the application, the change detection runs
     * after the event.
     *
     * @param text
     *     The text typed by the user.
     */
    function type(text: string): void {
        const target = component.target;
        target.value = PADDING.repeat(4) + text + PADDING.repeat(4);
        target.dispatchEvent(new Event('input'));
        fixture.detectChanges();
    }

    /**
     * Returns the button of the key with the given keysym.
     *
     * @param keysym
     *     The keysym of the key.
     *
     * @returns
     *     The button which presses or releases the key.
     */
    function keyButton(keysym: number): HTMLButtonElement {
        return fixture.debugElement.queryAll(By.directive(KeyComponent))
            .find(key => key.componentInstance.keysym === keysym)!
            .query(By.css('button')).nativeElement;
    }

    /**
     * Clicks the button of the key with the given keysym as the user would.
     * Like Zone.js in the application, the change detection runs after the
     * event.
     *
     * @param keysym
     *     The keysym of the key.
     */
    function clickKey(keysym: number): void {
        keyButton(keysym).dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true }));
        fixture.detectChanges();
    }

    /**
     * Broadcasts the given key event as AppComponent does before passing a
     * key of the local keyboard to the remote desktop.
     *
     * @param eventName
     *     The name of the event, for a key press or a key release.
     *
     * @param keysym
     *     The X11 keysym of the key.
     *
     * @returns
     *     true if a listener prevented the key from reaching the remote
     *     desktop, false otherwise.
     */
    function isKeyBlocked(eventName: 'guacBeforeKeydown' | 'guacBeforeKeyup', keysym: number): boolean {
        return TestBed.inject(GuacEventService<GuacFrontendEventArguments>)
            .broadcast(eventName, { keysym, keyboard: {} as Guacamole.Keyboard })
            .defaultPrevented;
    }

    it('should block a printable key of the local keyboard while the text input has focus', () => {
        expect(isKeyBlocked('guacBeforeKeydown', 0x61)).toBe(true);
        expect(isKeyBlocked('guacBeforeKeyup', 0x61)).toBe(true);
    });

    it.each([
        ['left', 0xFFED],
        ['right', 0xFFEE]
    ])('should let the %s Option key of a Mac through while the text input has focus', (side, keysym) => {
        expect(isKeyBlocked('guacBeforeKeydown', keysym)).toBe(false);
        expect(isKeyBlocked('guacBeforeKeyup', keysym)).toBe(false);
    });

    it('should label the key buttons with the translated key names', () => {
        const buttons: HTMLElement[] = Array.from(fixture.nativeElement.querySelectorAll('guac-key button'));

        expect(buttons.map(button => button.textContent!.trim())).toEqual(['Ctrl', 'Alt', 'Esc', 'Tab']);
    });

    it.each([
        ['Ctrl', CTRL],
        ['Alt', ALT]
    ])('should press the sticky %s key when its button is clicked', (name, keysym) => {
        clickKey(keysym);

        expect(keyEvents).toEqual([['keydown', keysym]]);
        expect(keyButton(keysym).classList).toContain('pressed');
    });

    it.each([
        ['Ctrl', CTRL],
        ['Alt', ALT]
    ])('should release the sticky %s key when its button is clicked again', (name, keysym) => {
        clickKey(keysym);
        clickKey(keysym);

        expect(keyEvents).toEqual([['keydown', keysym], ['keyup', keysym]]);
        expect(keyButton(keysym).classList).not.toContain('pressed');
    });

    it.each([
        ['Ctrl', CTRL],
        ['Alt', ALT]
    ])('should release the sticky %s key after the next typed character', (name, keysym) => {
        clickKey(keysym);
        type('c');

        expect(keyEvents).toEqual([['keydown', keysym], ['keydown', 0x63], ['keyup', 0x63], ['keyup', keysym]]);
        expect(keyButton(keysym).classList).not.toContain('pressed');
    });

    it('should release the sticky Ctrl key before the second of two characters typed at once', () => {
        clickKey(CTRL);
        type('xy');

        expect(keyEvents).toEqual([
            ['keydown', CTRL], ['keydown', 0x78], ['keyup', 0x78], ['keyup', CTRL],
            ['keydown', 0x79], ['keyup', 0x79]
        ]);
    });

    it('should not send keys while text is being composed', () => {
        component.target.dispatchEvent(new CompositionEvent('compositionstart'));
        type('k');

        expect(keyEvents).toEqual([]);
        expect(component.target.value).toBe(PADDING.repeat(4) + 'k' + PADDING.repeat(4));
    });

    it('should send the composed text with the next input after the composition has ended', () => {
        component.target.dispatchEvent(new CompositionEvent('compositionstart'));
        type('に');
        component.target.dispatchEvent(new CompositionEvent('compositionend', { data: 'に' }));
        type('にz');

        expect(pressedKeysyms).toEqual([0x0100306B, 0x7A]);
        expect(component.target.value).toBe(PADDING.repeat(8));
    });

    it('should send a character outside the Basic Multilingual Plane as a single key', () => {
        type('\u{1F600}');

        // 0x01000000 | U+1F600, instead of one key for each surrogate
        expect(pressedKeysyms).toEqual([0x0101F600]);
    });

    it('should send the keys of a surrogate pair between other characters in order', () => {
        type('a\u{1F600}b');

        expect(pressedKeysyms).toEqual([0x61, 0x0101F600, 0x62]);
    });

    it('should display a character outside the Basic Multilingual Plane as it was typed', () => {
        type('a\u{1F600}b');

        expect(component.sentText).toEqual(['a\u{1F600}b']);
    });
});
