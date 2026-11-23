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
import { PlayerTimeService } from './player-time.service';

describe('PlayerTimeService', () => {

    let service: PlayerTimeService;

    beforeEach(() => {
        service = TestBed.inject(PlayerTimeService);
    });

    it('should format milliseconds as minutes and seconds', () => {
        expect(service.formatTime(1000)).toBe('0:01');
        expect(service.formatTime(61000)).toBe('1:01');
    });

    it('should include hours only if needed', () => {
        expect(service.formatTime(3661000)).toBe('1:01:01');
    });

    it('should round down to whole seconds and treat a missing value as zero', () => {
        expect(service.formatTime(1999)).toBe('0:01');
        expect(service.formatTime(undefined as unknown as number)).toBe('0:00');
    });

    // Components bind the method to a field and call it from their template
    it('should format time when the method is called detached from the service', () => {
        const { formatTime } = service;
        expect(formatTime(3000)).toBe('0:03');
    });

});
