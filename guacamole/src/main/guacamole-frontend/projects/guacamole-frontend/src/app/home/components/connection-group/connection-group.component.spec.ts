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
import { RouterModule } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { provideTranslocoMessageformat } from '@jsverse/transloco-messageformat';
import { GroupListItem } from '../../../group-list/types/GroupListItem';
import { Connection } from '../../../rest/types/Connection';
import { ConnectionGroup } from '../../../rest/types/ConnectionGroup';
import { ConnectionGroupComponent } from './connection-group.component';

describe('ConnectionGroupComponent', () => {

    let fixture: ComponentFixture<ConnectionGroupComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [ConnectionGroupComponent],
            imports     : [
                RouterModule.forRoot([]),
                TranslocoTestingModule.forRoot({
                    langs          : {
                        en: {
                            HOME: {
                                INFO_AVAILABLE_CONNECTIONS: '{AVAILABLE} of {TOTAL} {TOTAL, plural, one{connection} other{connections}} available'
                            }
                        }
                    },
                    translocoConfig: { availableLangs: ['en'], defaultLang: 'en' },
                    preloadLangs   : true
                })
            ],
            providers   : [provideTranslocoMessageformat()]
        });

        fixture = TestBed.createComponent(ConnectionGroupComponent);
    });

    /**
     * Displays a connection group of the given type with two connections,
     * one of which is in use by another user.
     *
     * @param type
     *     The type of the connection group, ConnectionGroup.Type.BALANCING or
     *     ConnectionGroup.Type.ORGANIZATIONAL.
     *
     * @param limit
     *     The value of the attribute "max-connections" of both connections.
     *
     * @returns
     *     The text of the element displaying the number of available
     *     connections, or null if there is no such element.
     */
    function displayedAvailableConnections(type: string, limit: string): string | null {
        const connectionGroup = new ConnectionGroup({
            identifier       : 'group',
            name             : 'group',
            type             : type,
            activeConnections: 0,
            childConnections : [
                new Connection({ identifier: 'a', name: 'a', protocol: 'ssh', attributes: { 'max-connections': limit } }),
                new Connection({ identifier: 'b', name: 'b', protocol: 'ssh', attributes: { 'max-connections': limit } })
            ]
        });

        fixture.componentRef.setInput('item', GroupListItem.fromConnectionGroup('test', connectionGroup, true, true,
            (dataSource, connection) => connection.identifier === 'a' ? 1 : 0, undefined, () => 0));
        fixture.detectChanges();

        // The transloco directive sets innerText, which jsdom stores without
        // rendering it as text content
        const element: HTMLElement | null = fixture.nativeElement.querySelector('.activeUserCount');
        return element && element.innerText;
    }

    it('should display the number of available connections of a balancing group', () => {
        expect(displayedAvailableConnections(ConnectionGroup.Type.BALANCING, '2')).toBe('3 of 4 connections available');
    });

    it('should not display a number for a balancing group without limits', () => {
        expect(displayedAvailableConnections(ConnectionGroup.Type.BALANCING, '')).toBeNull();
    });

    it('should not display a number for an organizational group', () => {
        expect(displayedAvailableConnections(ConnectionGroup.Type.ORGANIZATIONAL, '2')).toBeNull();
    });

});
