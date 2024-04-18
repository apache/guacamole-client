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

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { catchError, throwError } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';
import { getTestScheduler } from '../../util/test-helper';
import { Error } from '../types/Error';
import { RequestService } from './request.service';
import Type = Error.Type;

describe('RequestService', () => {
    let service: RequestService;
    let testScheduler: TestScheduler;

    beforeEach(() => {
        testScheduler = getTestScheduler();
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
        });
        service = TestBed.inject(RequestService);
    });

    describe('createErrorCallback', () => {
        it('should invoke the given callback if the error is a REST Error', () => {
            testScheduler.run(({expectObservable}) => {

                const restError = new Error({type: Type.NOT_FOUND});
                const defaultValue = 'foo';

                const observable = throwError(() => restError).pipe(
                    catchError(service.defaultValue(defaultValue))
                );

                expectObservable(observable).toBe('(a|)', {a: defaultValue});
            });
        });

        it('should rethrow all other errors', () => {
            testScheduler.run(({expectObservable}) => {
                const otherError = 'Other Error';
                const defaultValue = 'foo';

                const observable = throwError(() => otherError).pipe(
                    catchError(service.defaultValue(defaultValue))
                );

                expectObservable(observable).toBe('#', undefined, otherError);

            });
        });
    });
});
