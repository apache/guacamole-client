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

import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import { ConnectionHistoryPlayerComponent } from './connection-history-player.component';

describe('ConnectionHistoryPlayerComponent', () => {
    let fetchMock: ReturnType<typeof vi.fn>;

    beforeEach(() => {
        // The static HTTP tunnel of guacamole-common-js downloads via fetch(); a
        // response that never arrives keeps the test independent of its parsing
        fetchMock = vi.fn().mockReturnValue(new Promise<Response>(() => undefined));
        vi.stubGlobal('fetch', fetchMock);

        TestBed.configureTestingModule({
            declarations: [ConnectionHistoryPlayerComponent],
            providers: [
                // Same route and input binding as in the app routing module
                provideRouter([{
                    path     : 'settings/:dataSource/recording/:identifier/:name',
                    component: ConnectionHistoryPlayerComponent
                }], withComponentInputBinding()),
                { provide: AuthenticationService, useValue: { getCurrentToken: () => 'test-token' } }
            ],
            schemas  : [NO_ERRORS_SCHEMA]
        });
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('should download the recording identified by the route parameters', async () => {
        const harness = await RouterTestingHarness.create();
        const component = await harness.navigateByUrl(
            '/settings/postgresql/recording/42/2026-11-21%20session%2Fa',
            ConnectionHistoryPlayerComponent
        );

        component.tunnel.connect();

        expect(fetchMock).toHaveBeenCalledTimes(1);
        expect(fetchMock.mock.calls[0][0]).toBe(
            'api/session/data/postgresql/history/connections/42/logs/2026-11-21%20session%2Fa'
        );
        expect(fetchMock.mock.calls[0][1].headers).toEqual({ 'Guacamole-Token': 'test-token' });

        component.tunnel.disconnect();
    });

});
