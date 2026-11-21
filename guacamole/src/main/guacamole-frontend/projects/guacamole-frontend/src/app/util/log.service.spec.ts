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

import { LogService } from './log.service';

describe('LogService', () => {
    let service: LogService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(LogService);
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it.each(['info', 'error', 'warn', 'debug'] as const)(
        'should pass the message and each further value of %s to the console as separate arguments', (level) => {
            const write = vi.spyOn(console, level).mockReturnValue(undefined);

            service[level]('NOT_FOUND', 'Not found', 404);

            expect(write).toHaveBeenCalledExactlyOnceWith('NOT_FOUND', 'Not found', 404);
        });

    it.each(['info', 'error', 'warn', 'debug'] as const)(
        'should pass only the message of %s to the console if there are no further values', (level) => {
            const write = vi.spyOn(console, level).mockReturnValue(undefined);

            service[level]('Extensions could not be loaded');

            expect(write).toHaveBeenCalledExactlyOnceWith('Extensions could not be loaded');
        });

    it('should write informational messages at the info level of the console', () => {
        const info = vi.spyOn(console, 'info').mockReturnValue(undefined);
        const log = vi.spyOn(console, 'log').mockReturnValue(undefined);

        service.info('Connected');

        expect(info).toHaveBeenCalledOnce();
        expect(log).not.toHaveBeenCalled();
    });
});
