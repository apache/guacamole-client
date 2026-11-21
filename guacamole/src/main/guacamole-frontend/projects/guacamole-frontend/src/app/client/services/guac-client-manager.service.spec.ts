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
import { AuthenticationService } from '../../auth/service/authentication.service';
import { ManagedClient } from '../types/ManagedClient';
import { GuacClientManagerService } from './guac-client-manager.service';
import { ManagedClientService } from './managed-client.service';

describe('GuacClientManagerService', () => {
    let service: GuacClientManagerService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                {
                    // Session storage only keeps values while logged in
                    provide : AuthenticationService,
                    useValue: { getCurrentToken: () => 'token' }
                },
                {
                    provide : ManagedClientService,
                    useValue: {
                        getInstance: (id: string) => ({
                            id,
                            client          : { disconnect: vi.fn() },
                            clientProperties: { focused: false }
                        }) as unknown as ManagedClient
                    }
                }
            ]
        });
        service = TestBed.inject(GuacClientManagerService);
    });

    afterEach(() => {

        // The service stays registered as a listener of the window, so make
        // sure that it no longer manages any client for subsequent tests
        service.clear();
    });

    describe('beforeunload', () => {

        /**
         * Dispatches a cancelable beforeunload event on the window.
         *
         * @returns
         *     The dispatched event.
         */
        const dispatchBeforeUnload = (): Event => {
            const event = new Event('beforeunload', { cancelable: true });
            window.dispatchEvent(event);
            return event;
        };

        it('should not ask for confirmation if there is no managed client', () => {
            const event = dispatchBeforeUnload();

            expect(event.defaultPrevented).toBe(false);
        });

        it('should ask for confirmation if there is at least one managed client', () => {
            service.getManagedClient('1');

            const event = dispatchBeforeUnload();

            expect(event.defaultPrevented).toBe(true);
        });

        it('should no longer ask for confirmation once all managed clients are removed', () => {
            service.getManagedClient('1');
            service.removeManagedClient('1');

            const event = dispatchBeforeUnload();

            expect(event.defaultPrevented).toBe(false);
        });
    });
});
