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
import { provideRouter } from '@angular/router';

import { LocalStorageService } from '../../storage/local-storage.service';
import { PreferenceService } from './preference.service';

describe('PreferenceService', () => {
    let service: PreferenceService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideRouter([])]
        });
        service = TestBed.inject(PreferenceService);
    });

    afterEach(() => {
        localStorage.clear();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should persist the preferences when the window is unloaded', () => {
        service.preferences.showRecentConnections = false;

        window.dispatchEvent(new Event('unload'));

        const stored = TestBed.inject(LocalStorageService).getItem('GUAC_PREFERENCES');
        expect(stored).toEqual(expect.objectContaining({ showRecentConnections: false }));
        expect(JSON.parse(localStorage.getItem('GUAC_PREFERENCES')!)).toEqual(service.preferences);
    });
});
