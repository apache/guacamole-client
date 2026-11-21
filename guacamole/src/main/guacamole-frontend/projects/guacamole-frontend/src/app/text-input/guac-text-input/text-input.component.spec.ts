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

describe('TextInputComponent', () => {
    let fixture: ComponentFixture<TextInputComponent>;
    let component: TextInputComponent;

    /**
     * The keysyms of all synthetic key presses broadcast by the component,
     * in the order of their broadcast.
     */
    let pressedKeysyms: number[];

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
        TestBed.inject(GuacEventService<GuacFrontendEventArguments>)
            .on('guacSyntheticKeydown')
            .subscribe(({ keysym }) => pressedKeysyms.push(keysym));

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
     * event.
     *
     * @param text
     *     The text typed by the user.
     */
    function type(text: string): void {
        const target = component.target;
        target.value = PADDING.repeat(4) + text + PADDING.repeat(4);
        target.dispatchEvent(new Event('input'));
    }

    it('should label the key buttons with the translated key names', () => {
        const buttons: HTMLElement[] = Array.from(fixture.nativeElement.querySelectorAll('guac-key button'));

        expect(buttons.map(button => button.textContent!.trim())).toEqual(['Ctrl', 'Alt', 'Esc', 'Tab']);
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
