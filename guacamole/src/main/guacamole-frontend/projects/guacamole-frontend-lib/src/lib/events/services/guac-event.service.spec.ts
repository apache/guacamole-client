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
import { TestScheduler } from 'rxjs/testing';
import { getTestScheduler } from '../../../test-utils/test-scheduler';
import { GuacEvent } from '../types/GuacEvent';
import { GuacEventArguments } from '../types/GuacEventArguments';
import { GuacEventService } from './guac-event.service';

interface TestEventArgs extends GuacEventArguments {
    test: { age: number; }
}

describe('GuacEventService', () => {
    let service: GuacEventService<TestEventArgs>;
    let testScheduler: TestScheduler;

    beforeEach(() => {
        testScheduler = getTestScheduler()
        TestBed.configureTestingModule({
            providers: [GuacEventService<TestEventArgs>],
        });
        service = TestBed.inject(GuacEventService<TestEventArgs>);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should emit test event', () => {
        testScheduler.run(({expectObservable, cold}) => {

            const age = 25;
            const expected = {
                a: {
                    age: age,
                    event: new GuacEvent<TestEventArgs>('test')
                }
            };
            const events = service.on('test');

            // Delay the broadcast for one time unit to allow expectObservable to subscribe to the subject
            cold('-a').subscribe(() => service.broadcast('test', {age}));

            expectObservable(events).toBe('-a', expected);

        });

    });

    it('handles multiple subscribers to the same event', () => {
        testScheduler.run(({expectObservable, cold}) => {
            const age = 25;
            const expected = {
                a: {
                    age: age,
                    event: new GuacEvent<TestEventArgs>('test')
                }
            };

            const events1 = service.on('test');
            const events2 = service.on('test');

            // Delay the broadcast for one time unit to allow expectObservable to subscribe to the subject
            cold('-a').subscribe(() => service.broadcast('test', {age}));

            expectObservable(events1).toBe('-a', expected);
            expectObservable(events2).toBe('-a', expected);

        });
    });

});
