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

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { HistoryService } from './history.service';

describe('HistoryService', () => {

    const HISTORY_URL = 'api/session/data/postgresql/history/connections';

    let service: HistoryService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(),
                provideHttpClientTesting()
            ]
        });
        service = TestBed.inject(HistoryService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    describe('getConnectionHistory', () => {

        it('should send neither contents nor sort predicates if none are given', async () => {
            const result = firstValueFrom(service.getConnectionHistory('postgresql'));

            httpMock.expectOne(HISTORY_URL).flush([]);
            await result;
        });

        it('should send each required content as a contains parameter', async () => {
            const result = firstValueFrom(service.getConnectionHistory('postgresql', ['guacadmin', 'Zulu']));

            httpMock.expectOne(`${HISTORY_URL}?contains=guacadmin&contains=Zulu`).flush([]);
            await result;
        });

        it('should send the sort predicates as the order parameter which the REST API reads', async () => {
            const result = firstValueFrom(service.getConnectionHistory('postgresql', ['Zulu'], ['startDate']));

            httpMock.expectOne(`${HISTORY_URL}?contains=Zulu&order=startDate`).flush([]);
            await result;
        });

    });

});
