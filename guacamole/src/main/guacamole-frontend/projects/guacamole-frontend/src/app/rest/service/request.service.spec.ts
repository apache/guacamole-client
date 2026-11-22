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

import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { GuacEventService } from 'guacamole-frontend-lib';
import { catchError, firstValueFrom, throwError } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';
import { GuacFrontendEventArguments } from '../../events/types/GuacFrontendEventArguments';
import { getTestScheduler } from '../../util/test-helper';
import { Error } from '../types/Error';
import { RequestService } from './request.service';
import { provideHttpClient, withInterceptorsFromDi, withXhr } from '@angular/common/http';
import Type = Error.Type;

describe('RequestService', () => {
    let service: RequestService;
    let testScheduler: TestScheduler;

    beforeEach(() => {
        testScheduler = getTestScheduler();
        TestBed.configureTestingModule({
    imports: [],
    providers: [provideHttpClient(withXhr(), withInterceptorsFromDi()), provideHttpClientTesting()]
});
        service = TestBed.inject(RequestService);
    });

    describe('createErrorCallback', () => {
        it('should invoke the given callback if the error is a REST Error', () => {
            testScheduler.run(({ expectObservable }) => {

                const restError = new Error({ type: Type.NOT_FOUND });
                const defaultValue = 'foo';

                const observable = throwError(() => restError).pipe(
                    catchError(service.defaultValue(defaultValue))
                );

                expectObservable(observable).toBe('(a|)', { a: defaultValue });
            });
        });

        it('should rethrow all other errors', () => {
            testScheduler.run(({ expectObservable }) => {
                const otherError = 'Other Error';
                const defaultValue = 'foo';

                const observable = throwError(() => otherError).pipe(
                    catchError(service.defaultValue(defaultValue))
                );

                expectObservable(observable).toBe('#', undefined, otherError);

            });
        });
    });

    describe('IGNORE, WARN and DIE', () => {

        beforeEach(() => {
            vi.spyOn(console, 'warn').mockImplementation(() => undefined);
            vi.spyOn(console, 'error').mockImplementation(() => undefined);
        });

        afterEach(() => {
            vi.restoreAllMocks();
        });

        it.each(['IGNORE', 'WARN', 'DIE'] as const)('should complete without a value when %s handles a REST Error', (callback) => {
            testScheduler.run(({ expectObservable }) => {
                const restError = new Error({ type: Type.INTERNAL_ERROR, message: 'Failure' });

                const observable = throwError(() => restError).pipe(
                    catchError(service[callback])
                );

                expectObservable(observable).toBe('|');
            });
        });

        it.each(['IGNORE', 'WARN', 'DIE'] as const)('should rethrow other errors when %s handles them', (callback) => {
            testScheduler.run(({ expectObservable }) => {
                const otherError = new TypeError('Failure');

                const observable = throwError(() => otherError).pipe(
                    catchError(service[callback])
                );

                expectObservable(observable).toBe('#', undefined, otherError);
            });
        });
    });

    describe('DIE and PROMISE_DIE', () => {
        let guacEventService: GuacEventService<GuacFrontendEventArguments>;

        beforeEach(() => {
            guacEventService = TestBed.inject(GuacEventService<GuacFrontendEventArguments>);
        });

        it('should pass the REST Error to subscribers of guacFatalPageError when DIE handles it', async () => {
            const restError = new Error({ type: Type.INTERNAL_ERROR, message: 'Failure' });
            const fatalPageError = firstValueFrom(guacEventService.on('guacFatalPageError'));

            throwError(() => restError).pipe(catchError(service.DIE)).subscribe();

            expect((await fatalPageError).error).toBe(restError);
        });

        it('should pass the REST Error to subscribers of guacFatalPageError when PROMISE_DIE handles it', async () => {
            const restError = new Error({ type: Type.INTERNAL_ERROR, message: 'Failure' });
            const fatalPageError = firstValueFrom(guacEventService.on('guacFatalPageError'));

            service.PROMISE_DIE(restError);

            expect((await fatalPageError).error).toBe(restError);
        });
    });
});
