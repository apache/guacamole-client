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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { LocaleModule } from '../../locale/locale.module';
import { TextBatch } from '../services/key-event-display.service';
import { PlayerTextViewComponent } from './player-text-view.component';

describe('PlayerTextViewComponent', () => {

    let fixture: ComponentFixture<PlayerTextViewComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [PlayerTextViewComponent],
            imports: [FormsModule, LocaleModule],
            providers: [provideHttpClient(withXhr(), withInterceptorsFromDi()), provideHttpClientTesting()]
        });

        fixture = TestBed.createComponent(PlayerTextViewComponent);
        fixture.componentRef.setInput('textBatches', [
            new TextBatch({ events: [{ text: 'ls', typed: true, timestamp: 1000 } as TextBatch['events'][number]], simpleValue: 'ls' }),
            new TextBatch({ events: [{ text: 'pwd', typed: true, timestamp: 61000 } as TextBatch['events'][number]], simpleValue: 'pwd' })
        ]);
        fixture.componentRef.setInput('seek', () => undefined);
        fixture.componentRef.setInput('currentPosition', 0);
    });

    it('should show the formatted timestamp of each text batch', () => {
        fixture.detectChanges();

        const timestamps = Array.from<Element>(fixture.nativeElement.querySelectorAll('.timestamp'))
            .map(element => element.textContent);

        expect(timestamps).toEqual(['0:01', '1:01']);
    });

});
