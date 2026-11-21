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
import { TranslocoService, TranslocoTestingModule } from '@jsverse/transloco';
import { ActivityLog } from '../../rest/types/ActivityLog';
import { ConnectionHistoryEntry } from '../../rest/types/ConnectionHistoryEntry';
import { ConnectionHistoryEntryWrapper } from './ConnectionHistoryEntryWrapper';

describe('ConnectionHistoryEntryWrapper', () => {

    let translocoService: TranslocoService;

    beforeEach(() => {

        // Translations are not preloaded, as in the app where the translation
        // file may still be in flight when history entries arrive
        TestBed.configureTestingModule({
            imports: [
                TranslocoTestingModule.forRoot({
                    langs: { en: { 'RECORDING_STORAGE': { 'INFO_GUACAMOLE_SESSION_RECORDING': 'Graphical recording of remote desktop session' } } },
                    translocoConfig: { availableLangs: ['en'], defaultLang: 'en' },
                    preloadLangs: false
                })
            ]
        });

        translocoService = TestBed.inject(TranslocoService);
    });

    /**
     * Creates a history entry with a graphical session recording.
     */
    function entryWithRecording(): ConnectionHistoryEntry {
        return new ConnectionHistoryEntry({
            identifier: '42',
            logs: {
                'a b/c': new ActivityLog({
                    type: ActivityLog.Type.GUACAMOLE_SESSION_RECORDING,
                    description: { key: 'RECORDING_STORAGE.INFO_GUACAMOLE_SESSION_RECORDING' }
                })
            }
        });
    }

    it('should link to the player for the session recording', () => {
        const wrapper = new ConnectionHistoryEntryWrapper('postgresql', entryWithRecording(), translocoService);

        expect(wrapper.sessionRecording?.url).toBe('/settings/postgresql/recording/42/a%20b%2Fc');
    });

    it('should resolve the description with the translation of the log description', async () => {
        const wrapper = new ConnectionHistoryEntryWrapper('postgresql', entryWithRecording(), translocoService);

        await expect(wrapper.sessionRecording?.description)
            .resolves.toBe('Graphical recording of remote desktop session');
    });

    it('should not have a session recording if the entry has no such log', () => {
        const wrapper = new ConnectionHistoryEntryWrapper('postgresql', new ConnectionHistoryEntry({ identifier: '42' }), translocoService);

        expect(wrapper.sessionRecording).toBeNull();
    });

});
