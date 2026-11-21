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
import { GuacEventService } from 'guacamole-frontend-lib';

import { GuacFrontendEventArguments } from '../../events/types/GuacFrontendEventArguments';
import { ClipboardData } from '../types/ClipboardData';
import { ClipboardService } from './clipboard.service';

describe('ClipboardService', () => {
    let service: ClipboardService;
    let guacEventService: GuacEventService<GuacFrontendEventArguments>;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(ClipboardService);
        guacEventService = TestBed.inject(GuacEventService<GuacFrontendEventArguments>);

        // The internal clipboard is session storage, which only keeps values
        // while the user is logged in
        guacEventService.broadcast('guacLogin', { authToken: 'token' });
    });

    afterEach(() => {
        vi.restoreAllMocks();
        Reflect.deleteProperty(navigator, 'clipboard');
    });

    /**
     * Replaces navigator.clipboard with the given value. Without a call to
     * this function, navigator.clipboard is undefined, as in a browser
     * outside of a secure context.
     *
     * @param clipboard
     *     The object to expose as navigator.clipboard.
     */
    function stubClipboard(clipboard: Partial<Clipboard>): void {
        Object.defineProperty(navigator, 'clipboard', { configurable: true, value: clipboard });
    }

    /**
     * Returns the private getLocalClipboard() function of the service, bound
     * to the service.
     */
    function getLocalClipboard(): () => Promise<ClipboardData> {
        return (service as unknown as { getLocalClipboard(): Promise<ClipboardData> }).getLocalClipboard.bind(service);
    }

    describe('getLocalClipboard', () => {

        it('should reject without throwing if the Clipboard API is unavailable', async () => {
            const read = getLocalClipboard();

            let result!: Promise<ClipboardData>;
            expect(() => result = read()).not.toThrow();
            await expect(result).rejects.toBeUndefined();
        });

        it('should reject without throwing if the Clipboard API cannot read text', async () => {
            stubClipboard({});
            const read = getLocalClipboard();

            let result!: Promise<ClipboardData>;
            expect(() => result = read()).not.toThrow();
            await expect(result).rejects.toBeUndefined();
        });

    });

    describe('getClipboard', () => {

        it('should resolve with the internal clipboard if the Clipboard API is unavailable', async () => {
            const data = new ClipboardData({ type: 'text/plain', data: 'internal' });
            await service.setClipboard(data);

            await expect(service.getClipboard()).resolves.toEqual(data);
        });

    });

    describe('setClipboard', () => {

        it('should store and broadcast the data if the Clipboard API is unavailable', async () => {
            const broadcast = vi.spyOn(guacEventService, 'broadcast');
            const data = new ClipboardData({ type: 'text/plain', data: 'copied' });

            await expect(service.setClipboard(data)).resolves.toBeUndefined();

            expect(broadcast).toHaveBeenCalledWith('guacClipboard', { data });
            await expect(service.getClipboard()).resolves.toEqual(data);
        });

        it('should store and broadcast the data if writing the local clipboard fails', async () => {
            stubClipboard({ writeText: vi.fn().mockRejectedValue(new DOMException('denied', 'NotAllowedError')) });
            const broadcast = vi.spyOn(guacEventService, 'broadcast');
            const data = new ClipboardData({ type: 'text/plain', data: 'copied' });

            await expect(service.setClipboard(data)).resolves.toBeUndefined();

            expect(broadcast).toHaveBeenCalledWith('guacClipboard', { data });
        });

    });

    describe('resyncClipboard', () => {

        it('should ignore a rejected read of the local clipboard', async () => {

            // Browsers reject the read if the user denies the permission
            const read = Promise.reject(new DOMException('Read permission denied.', 'NotAllowedError'));
            vi.spyOn(service as unknown as { getLocalClipboard(): Promise<ClipboardData> }, 'getLocalClipboard').mockReturnValue(read);
            const setClipboard = vi.spyOn(service, 'setClipboard');

            // resyncClipboard() does not return the promise it chains onto the
            // read, so capture it; if it rejected, nothing would handle that
            // rejection in the running application. The spy itself also calls
            // then() on the mocked return value, so the last call is the
            // one made by resyncClipboard().
            const then = vi.spyOn(read, 'then');

            service.resyncClipboard();

            await expect(then.mock.results.at(-1)!.value).resolves.toBeUndefined();
            expect(setClipboard).not.toHaveBeenCalled();
        });

        it('should not throw if the Clipboard API is unavailable', () => {
            const setClipboard = vi.spyOn(service, 'setClipboard');

            expect(() => service.resyncClipboard()).not.toThrow();
            expect(setClipboard).not.toHaveBeenCalled();
        });

    });
});
