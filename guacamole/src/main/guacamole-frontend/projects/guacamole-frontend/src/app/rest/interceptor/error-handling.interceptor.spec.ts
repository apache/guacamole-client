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

import { HTTP_INTERCEPTORS, HttpClient, provideHttpClient, withInterceptorsFromDi, withXhr } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { Error } from '../types/Error';
import { ErrorHandlingInterceptor } from './error-handling.interceptor';

describe('ErrorHandlingInterceptor', () => {

    beforeEach(() => {
            TestBed.configureTestingModule({
                providers: [
                    ErrorHandlingInterceptor,
                    { provide: HTTP_INTERCEPTORS, useExisting: ErrorHandlingInterceptor, multi: true },
                    provideHttpClient(withXhr(), withInterceptorsFromDi()),
                    provideHttpClientTesting()
                ],
            });


        }
    );

    it('should be created', () => {
        const interceptor: ErrorHandlingInterceptor = TestBed.inject(ErrorHandlingInterceptor);
        expect(interceptor).toBeTruthy();
    });

    it('should reject with the generic error if an error response has no body', async () => {
        const response = firstValueFrom(TestBed.inject(HttpClient).get('api/languages'));
        TestBed.inject(HttpTestingController).expectOne('api/languages')
            .flush(null, { status: 502, statusText: 'Bad Gateway' });

        await expect(response).rejects.toBeInstanceOf(Error);
        await expect(response).rejects.toMatchObject({
            message: 'Unknown failure sending HTTP request',
            type: Error.Type.INTERNAL_ERROR
        });
    });

});
