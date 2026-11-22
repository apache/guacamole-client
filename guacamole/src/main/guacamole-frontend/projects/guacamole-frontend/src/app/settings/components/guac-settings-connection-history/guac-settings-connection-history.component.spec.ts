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

import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { Subject } from 'rxjs';
import { GuacPagerComponent } from '../../../list/components/guac-pager/guac-pager.component';
import { HistoryService } from '../../../rest/service/history.service';
import { RequestService } from '../../../rest/service/request.service';
import { ConnectionHistoryEntry } from '../../../rest/types/ConnectionHistoryEntry';
import { GuacSettingsConnectionHistoryComponent } from './guac-settings-connection-history.component';

describe('GuacSettingsConnectionHistoryComponent', () => {
    let fixture: ComponentFixture<GuacSettingsConnectionHistoryComponent>;

    /**
     * The pending response of each request for history records, in the
     * order of the requests.
     */
    let responses: Subject<ConnectionHistoryEntry[]>[];

    /**
     * Creates the given number of history records, the newest first, each
     * with a connection name that tells its position.
     *
     * @param count
     *     The number of records to create.
     *
     * @returns
     *     The history records.
     */
    const createEntries = (count: number): ConnectionHistoryEntry[] => Array.from({ length: count }, (_, i) =>
        new ConnectionHistoryEntry({
            username      : 'guacadmin',
            connectionName: `connection-${i + 1}`,
            startDate     : Date.UTC(2026, 10, 1) - i * 60000,
            endDate       : Date.UTC(2026, 10, 1) - i * 60000 + 1000
        }));

    /**
     * Returns the pager of the history list.
     *
     * @returns
     *     The instance of GuacPagerComponent within the component.
     */
    const getPager = (): GuacPagerComponent => fixture.componentInstance.pager;

    /**
     * Returns the connection names of the records currently listed.
     *
     * @returns
     *     The text of the connection name column of each listed record.
     */
    const getListedConnections = (): string[] =>
        [...fixture.nativeElement.querySelectorAll('tr.history')].map((row: HTMLTableRowElement) => row.cells[3].textContent!.trim());

    beforeEach(() => {
        responses = [];
        TestBed.configureTestingModule({
            declarations: [GuacSettingsConnectionHistoryComponent, GuacPagerComponent],
            schemas: [NO_ERRORS_SCHEMA],
            imports: [
                TranslocoTestingModule.forRoot({
                    langs: { en: { SETTINGS_CONNECTION_HISTORY: { FORMAT_DATE: 'yyyy-MM-dd HH:mm:ss' } } },
                    translocoConfig: { availableLangs: ['en'], defaultLang: 'en' }
                })
            ],
            providers: [
                provideRouter([]),
                {
                    provide : HistoryService,
                    useValue: {
                        getConnectionHistory: () => {
                            const response = new Subject<ConnectionHistoryEntry[]>();
                            responses.push(response);
                            return response;
                        }
                    }
                },
                { provide: RequestService, useValue: { DIE: vi.fn() } }
            ]
        });

        fixture = TestBed.createComponent(GuacSettingsConnectionHistoryComponent);
        fixture.componentRef.setInput('dataSource', 'postgresql');
        fixture.detectChanges();
    });

    it('should show the first page of the new results after searching again', async () => {

        // GIVEN the second of three pages of records
        responses[0].next(createEntries(60));
        fixture.detectChanges();
        await fixture.whenStable();
        getPager().selectPage(2);
        fixture.detectChanges();
        expect(getListedConnections()[0]).toBe('connection-26');

        // WHEN the user searches again, and the records arrive only after the
        // results were cleared, like any response of the REST API
        fixture.componentInstance.search();
        fixture.detectChanges();
        responses[1].next(createEntries(60));
        fixture.detectChanges();
        await fixture.whenStable();
        fixture.detectChanges();

        // THEN the list starts again with the first page, like the AngularJS
        // list, which clears its results while it searches
        expect(getPager().currentPage).toBe(1);
        expect(getListedConnections()[0]).toBe('connection-1');

    });

});
