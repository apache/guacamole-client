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
import { GuacEventService } from 'guacamole-frontend-lib';
import { ManagedClientService } from '../../services/managed-client.service';
import { ManagedArgument } from '../../types/ManagedArgument';
import { ManagedClient } from '../../types/ManagedClient';
import { ManagedClientGroup } from '../../types/ManagedClientGroup';
import { GuacTiledClientsComponent } from './guac-tiled-clients.component';

describe('GuacTiledClientsComponent', () => {
    let fixture: ComponentFixture<GuacTiledClientsComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [GuacTiledClientsComponent],
            providers: [{ provide: ManagedClientService, useValue: {} }]
        });

        // Only the change detection of the component is under test, not the
        // tiles of its template
        TestBed.overrideTemplate(GuacTiledClientsComponent, '');
        fixture = TestBed.createComponent(GuacTiledClientsComponent);
    });

    it('should broadcast guacClientArgumentsUpdated when the focused client receives a new argument', () => {
        const client = new ManagedClient({
            id: 'c2lnbmFs',
            client: {} as Guacamole.Client,
            tunnel: {} as Guacamole.Tunnel
        });
        client.clientProperties.focused = true;

        fixture.componentRef.setInput('clientGroup', new ManagedClientGroup({ clients: [client] }));
        fixture.componentRef.setInput('onClose', () => undefined);
        fixture.componentRef.setInput('emulateAbsoluteMouse', false);
        fixture.detectChanges();

        const broadcast = vi.spyOn(TestBed.inject(GuacEventService), 'broadcast');

        // ManagedClientService adds each argument to the existing object
        client.arguments['font-size'] = new ManagedArgument({
            name: 'font-size',
            value: '12',
            stream: {} as Guacamole.OutputStream
        });
        fixture.detectChanges();

        expect(broadcast).toHaveBeenCalledWith('guacClientArgumentsUpdated', { focusedClient: client });
    });

});
