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
import { KeyComponent } from '../guac-key/key.component';
import { TextInputComponent } from './text-input.component';

describe('TextInputComponent', () => {
    let fixture: ComponentFixture<TextInputComponent>;

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
        fixture = TestBed.createComponent(TextInputComponent);
        fixture.detectChanges();
    });

    it('should label the key buttons with the translated key names', () => {
        const buttons: HTMLElement[] = Array.from(fixture.nativeElement.querySelectorAll('guac-key button'));

        expect(buttons.map(button => button.textContent!.trim())).toEqual(['Ctrl', 'Alt', 'Esc', 'Tab']);
    });
});
