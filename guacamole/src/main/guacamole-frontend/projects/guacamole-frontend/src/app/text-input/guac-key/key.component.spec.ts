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

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslocoTestingModule } from '@ngneat/transloco';
import { GuacEventService } from 'guacamole-frontend-lib';
import { GuacFrontendEventArguments } from '../../events/types/GuacFrontendEventArguments';
import { KeyComponent } from './key.component';

describe('KeyComponent', () => {
    let component: KeyComponent;
    let fixture: ComponentFixture<KeyComponent>;
    let guacEventService: GuacEventService<GuacFrontendEventArguments>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [KeyComponent],
            imports     : [
                TranslocoTestingModule.forRoot({})
            ],
        });
        fixture = TestBed.createComponent(KeyComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
        guacEventService = TestBed.inject(GuacEventService<GuacFrontendEventArguments>);
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should toggle pressed state for sticky keys', () => {
        spyOn(component.pressedChange, 'emit');

        component.sticky = true;
        component.pressed = false;
        fixture.detectChanges();

        component.updateKey(new MouseEvent('click'));

        expect(component.pressed).toBeTrue();
        expect(component.pressedChange.emit).toHaveBeenCalledWith(true);
    });

    it('should not toggle pressed state for non-sticky keys', () => {
        spyOn(component.pressedChange, 'emit');

        component.sticky = false;
        fixture.detectChanges();

        component.updateKey(new MouseEvent('click'));

        expect(component.pressed).toBeFalse();
        expect(component.pressedChange.emit).not.toHaveBeenCalled();
    });

    it('should broadcast keydown and keyup events for non-sticky keys', () => {
        spyOn(guacEventService, 'broadcast');

        component.sticky = false;
        component.keysym = 65; // Example keysym
        fixture.detectChanges();

        component.updateKey(new MouseEvent('click'));

        expect(guacEventService.broadcast).toHaveBeenCalledWith('guacSyntheticKeydown', { keysym: 65 });
        expect(guacEventService.broadcast).toHaveBeenCalledWith('guacSyntheticKeyup', { keysym: 65 });
    });
});
