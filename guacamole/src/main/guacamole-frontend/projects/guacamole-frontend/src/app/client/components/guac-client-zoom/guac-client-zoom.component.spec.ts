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
import { FormsModule } from '@angular/forms';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { GuacZoomCtrlDirective } from '../../directives/guac-zoom-ctrl.directive';
import { ClientProperties } from '../../types/ClientProperties';
import { ManagedClient } from '../../types/ManagedClient';
import { GuacClientZoomComponent } from './guac-client-zoom.component';

describe('GuacClientZoomComponent', () => {
    let fixture: ComponentFixture<GuacClientZoomComponent>;
    let client: ManagedClient;
    let input: HTMLInputElement;

    beforeEach(async () => {
        TestBed.configureTestingModule({
            declarations: [GuacClientZoomComponent, GuacZoomCtrlDirective],
            imports: [
                FormsModule,
                TranslocoTestingModule.forRoot({
                    langs: { en: { CLIENT: { TEXT_ZOOM_AUTO_FIT: 'Automatically fit to browser window' } } },
                    translocoConfig: { availableLangs: ['en'], defaultLang: 'en' }
                })
            ]
        });

        client = new ManagedClient({
            id              : 'c2lnbmFs',
            client          : {} as Guacamole.Client,
            tunnel          : {} as Guacamole.Tunnel,
            clientProperties: new ClientProperties({ scale: 1, minScale: 0.5, maxScale: 3 })
        });

        fixture = TestBed.createComponent(GuacClientZoomComponent);
        fixture.componentRef.setInput('client', client);
        fixture.detectChanges();
        await fixture.whenStable();
        fixture.detectChanges();

        input = fixture.nativeElement.querySelector('.client-zoom-state input');
    });

    afterEach(() => {
        fixture.destroy();
    });

    /**
     * Types the given text into the zoom field, one input event per
     * character, as a user does.
     *
     * @param text
     *     The text which replaces the content of the zoom field.
     */
    const type = (text: string): void => {
        input.value = '';
        input.dispatchEvent(new Event('input'));
        for (const character of text) {
            input.value += character;
            input.dispatchEvent(new Event('input'));
        }
        fixture.detectChanges();
    };

    it('should show the current scale as a percentage', () => {
        expect(input.value).toBe('100');
    });

    it('should not change the scale while the user is typing a new zoom level', () => {
        type('75');

        expect(client.clientProperties.scale).toBe(1);
        expect(input.value).toBe('75');
    });

    it('should change the scale once the user leaves the zoom field', () => {
        type('75');
        input.dispatchEvent(new Event('blur'));
        fixture.detectChanges();

        expect(client.clientProperties.scale).toBe(0.75);
    });

});
