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

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { GuacEventService } from 'guacamole-frontend-lib';
import { Observable, of, throwError } from 'rxjs';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import { ClipboardService } from '../../../clipboard/services/clipboard.service';
import { GuacFrontendEventArguments } from '../../../events/types/GuacFrontendEventArguments';
import { DataSourceService } from '../../../rest/service/data-source-service.service';
import { TunnelService } from '../../../rest/service/tunnel.service';
import { Error } from '../../../rest/types/Error';
import { SharingProfile } from '../../../rest/types/SharingProfile';
import { GuacClientManagerService } from '../../services/guac-client-manager.service';
import { ManagedClient } from '../../types/ManagedClient';
import { ManagedClientGroup } from '../../types/ManagedClientGroup';
import { ClientPageComponent } from './client-page.component';

/**
 * The X11 keysym of the left Shift key.
 */
const SHIFT_LEFT = 0xFFE1;

/**
 * The X11 keysym of the left Control key.
 */
const CONTROL_LEFT = 0xFFE3;

/**
 * The X11 keysym of the End key.
 */
const END = 0xFF57;

/**
 * The X11 keysym of the Delete key.
 */
const DELETE = 0xFFFF;

/**
 * The keysyms which Guacamole.Keyboard reports for the Alt key: Alt on
 * Linux and Windows, and the left and right Option key on a Mac.
 */
const ALT_KEYSYMS: [string, number][] = [
    ['Alt', 0xFFE9],
    ['the left Option key of a Mac', 0xFFED],
    ['the right Option key of a Mac', 0xFFEE]
];

describe('ClientPageComponent', () => {
    let fixture: ComponentFixture<ClientPageComponent>;
    let eventService: GuacEventService<GuacFrontendEventArguments>;

    /**
     * The local keyboard whose state accompanies the key events, as the
     * keyboard of AppComponent.
     */
    let keyboard: Guacamole.Keyboard;

    /**
     * Stub of ClipboardService.resyncClipboard(), which reparseRoute() calls
     * once per run.
     */
    let resyncClipboard: ReturnType<typeof vi.fn>;

    /**
     * The responses of the stub of TunnelService.getSharingProfiles(), by
     * the UUID of the tunnel.
     */
    let sharingProfileResponses: Record<string, Observable<Record<string, SharingProfile>>>;

    beforeEach(() => {
        resyncClipboard = vi.fn();
        sharingProfileResponses = {};

        TestBed.configureTestingModule({
            declarations: [ClientPageComponent],
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                provideRouter([]),
                { provide: AuthenticationService, useValue: { getAvailableDataSources: () => [] } },
                { provide: DataSourceService, useValue: { apply: () => new Promise(() => undefined) } },
                {
                    provide: GuacClientManagerService,
                    useValue: { getManagedClientGroup: () => new ManagedClientGroup() }
                },
                { provide: ClipboardService, useValue: { resyncClipboard } },
                {
                    provide : TunnelService,
                    useValue: { getSharingProfiles: (tunnel: string) => sharingProfileResponses[tunnel] }
                }
            ]
        });

        // Only the handling of key events is under test, not the clients and
        // the menu of the template
        TestBed.overrideTemplate(ClientPageComponent, '');

        eventService = TestBed.inject(GuacEventService<GuacFrontendEventArguments>);
        fixture = TestBed.createComponent(ClientPageComponent);
        fixture.componentRef.setInput('id', 'c2lnbmFs');
        fixture.detectChanges();

        keyboard = new Guacamole.Keyboard(document.createElement('div'));
    });

    afterEach(() => {
        fixture.destroy();
        vi.restoreAllMocks();
    });

    /**
     * Focuses a new client of a connection whose tunnel has the given UUID,
     * as the tiled clients do when the user clicks a client, and runs the
     * change detection of the page.
     *
     * @param uuid
     *     The UUID of the tunnel of the new client.
     */
    const focusClient = (uuid: string): void => {
        const newFocusedClient = new ManagedClient({
            id    : uuid,
            client: {} as Guacamole.Client,
            tunnel: { uuid } as Guacamole.Tunnel
        });
        eventService.broadcast('guacClientFocused', { newFocusedClient });
        fixture.detectChanges();
    };

    it('should reparse the route once when the page is opened', () => {
        expect(resyncClipboard).toHaveBeenCalledTimes(1);
    });

    it('should reparse the route again when the group id changes without a new route', () => {
        fixture.componentRef.setInput('id', 'c2lnbmFs.c2lnbmFs2');
        fixture.detectChanges();

        expect(resyncClipboard).toHaveBeenCalledTimes(2);
    });

    it.each(ALT_KEYSYMS)('should open the menu when Ctrl, Shift and %s are pressed', (name, altKeysym) => {
        const menuShown: boolean[] = [];
        eventService.on('guacMenuShown').subscribe(event => menuShown.push(event.menuShown));

        keyboard.press(CONTROL_LEFT);
        keyboard.press(SHIFT_LEFT);
        keyboard.press(altKeysym);
        const keydown = eventService.broadcast('guacBeforeKeydown', { keysym: altKeysym, keyboard });
        fixture.detectChanges();

        expect(keydown.defaultPrevented).toBe(true);
        expect(menuShown).toEqual([true]);
    });

    it.each(ALT_KEYSYMS)('should send Ctrl+Alt+Delete when Ctrl, %s and End are pressed', (name, altKeysym) => {
        const sentKeysyms: number[] = [];
        eventService.on('guacSyntheticKeydown').subscribe(({ keysym }) => sentKeysyms.push(keysym));

        keyboard.press(CONTROL_LEFT);
        keyboard.press(altKeysym);
        keyboard.press(END);
        const keydown = eventService.broadcast('guacKeydown', { keysym: END, keyboard });

        expect(keydown.defaultPrevented).toBe(true);
        expect(sentKeysyms).toEqual([DELETE]);
    });

    it('should keep the sharing profiles if those of the newly focused connection cannot be retrieved', () => {
        vi.spyOn(console, 'warn').mockImplementation(() => undefined);
        sharingProfileResponses = {
            'tunnel-1': of({ 42: new SharingProfile({ identifier: '42', name: 'Watch' }) }),
            'tunnel-2': throwError(() => new Error({ type: Error.Type.INTERNAL_ERROR, message: 'Simulated failure' }))
        };

        focusClient('tunnel-1');
        expect(fixture.componentInstance.canShareConnection()).toBe(true);

        focusClient('tunnel-2');
        expect(fixture.componentInstance.canShareConnection()).toBe(true);
    });

});
