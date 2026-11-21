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

import { LocalStorageService } from './local-storage.service';

describe('LocalStorageService', () => {
    let service: LocalStorageService;
    let consoleError: ReturnType<typeof vi.spyOn>;

    beforeEach(() => {
        consoleError = vi.spyOn(console, 'error').mockReturnValue(undefined);
        TestBed.configureTestingModule({});
        service = TestBed.inject(LocalStorageService);
    });

    afterEach(() => {
        localStorage.clear();
        vi.restoreAllMocks();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should return null for a key that is not set', () => {
        expect(service.getItem('missing')).toBeNull();
        expect(consoleError).not.toHaveBeenCalled();
    });

    it('should return the deserialized value of a valid JSON entry', () => {
        localStorage.setItem('valid', '{"name":"value"}');

        expect(service.getItem('valid')).toEqual({ name: 'value' });
        expect(consoleError).not.toHaveBeenCalled();
    });

    it('should treat invalid JSON in localStorage as missing', () => {
        localStorage.setItem('invalid', '{not valid json');

        expect(service.getItem('invalid')).toBeNull();
    });

    it('should log the key and the raw value of invalid JSON in localStorage', () => {
        localStorage.setItem('invalid', '{not valid json');

        service.getItem('invalid');

        expect(consoleError).toHaveBeenCalledWith(
            'Ignoring invalid JSON from localStorage key "invalid":', '{not valid json');
    });
});
