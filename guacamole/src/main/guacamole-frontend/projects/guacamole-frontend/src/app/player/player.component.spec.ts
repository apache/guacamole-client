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

import { provideHttpClient, withInterceptorsFromDi, withXhr } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { provideRouter, Router } from '@angular/router';
import { LocaleModule } from '../locale/locale.module';
import { PlayerComponent } from './player.component';

/**
 * Stand-in for the connection history page the close button navigates to.
 */
@Component({ template: '', standalone: false })
class HistoryStubComponent {
}

describe('PlayerComponent', () => {

    let fixture: ComponentFixture<PlayerComponent>;
    let router: Router;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [PlayerComponent, HistoryStubComponent],
            imports     : [FormsModule, LocaleModule],
            providers   : [
                provideHttpClient(withXhr(), withInterceptorsFromDi()),
                provideHttpClientTesting(),
                provideRouter([{ path: 'settings/:dataSource/history', component: HistoryStubComponent }])
            ],
            schemas     : [NO_ERRORS_SCHEMA]
        });

        router = TestBed.inject(Router);
        fixture = TestBed.createComponent(PlayerComponent);

        // The controls, and with them the close button, are only rendered
        // once a recording exists
        fixture.componentInstance.recording = {
            isPlaying  : () => false,
            getDuration: () => 0,
            getDisplay : () => undefined,
            pause      : () => undefined,
            abort      : () => undefined
        } as unknown as Guacamole.SessionRecording;
    });

    /**
     * Clicks the close button of the player and waits for the resulting
     * navigation.
     */
    async function clickClose(): Promise<void> {
        fixture.detectChanges();
        fixture.nativeElement.querySelector('.guac-player-close').click();
        await fixture.whenStable();
    }

    it('should return to the history of the data source when the close button is clicked', async () => {
        fixture.componentRef.setInput('dataSource', 'postgresql');

        await clickClose();

        expect(router.url).toBe('/settings/postgresql/history');
    });

    it('should encode the data source in the path of the history', async () => {
        fixture.componentRef.setInput('dataSource', 'a b/c');

        await clickClose();

        expect(router.url).toBe('/settings/a%20b%2Fc/history');
    });

});
