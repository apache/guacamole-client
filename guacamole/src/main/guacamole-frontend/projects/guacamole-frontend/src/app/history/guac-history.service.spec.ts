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
import { LocalStorageService } from '../storage/local-storage.service';

import { GuacHistoryService } from './guac-history.service';

describe('GuacHistoryService Unit-Test', () => {
    let service: GuacHistoryService;

    const fakeLocalStorageService = jasmine.createSpyObj<LocalStorageService>(
        'LocalStorageService',
        {
            getItem   : null,
            setItem   : undefined,
            removeItem: undefined,
        }
    );

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [{ provide: LocalStorageService, useValue: fakeLocalStorageService }]
        });
        service = TestBed.inject(GuacHistoryService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should update thumbnail', () => {
        service.updateThumbnail('id', 'thumbnail');
        expect(service.recentConnections.length).toBe(1);
        expect(service.recentConnections[0].id).toBe('id');
        expect(service.recentConnections[0].thumbnail).toBe('thumbnail');
    });

    it('should replace existing HistoryEntry', () => {
        service.updateThumbnail('1', 'thumbnail1');
        service.updateThumbnail('2', 'thumbnail2');
        service.updateThumbnail('3', 'thumbnail3');

        service.updateThumbnail('2', 'thumbnail22');

        expect(service.recentConnections.length).toBe(3);
        expect(service.recentConnections[0].id).toBe('2');
        expect(service.recentConnections[0].thumbnail).toBe('thumbnail22');
    });


});

describe('GuacHistoryService Integration-Test', () => {

    let service: GuacHistoryService;

    beforeAll(() => {
        localStorage.clear();
    });

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(GuacHistoryService);
    });

    it('adds history entries', () => {
        service.updateThumbnail('1', 'thumbnail1');
        service.updateThumbnail('2', 'thumbnail2');
        service.updateThumbnail('3', 'thumbnail3');

        expect(service.recentConnections.length).toBe(3);
    });

    it('loads history entries from local storage', () => {
        expect(service.recentConnections.length).toBe(3);
    });

});
