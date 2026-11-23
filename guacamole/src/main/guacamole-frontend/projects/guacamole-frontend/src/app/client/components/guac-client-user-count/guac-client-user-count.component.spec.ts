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
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { provideTranslocoMessageformat } from '@jsverse/transloco-messageformat';
import { ClientIdentifierService } from '../../../navigation/service/client-identifier.service';
import { ClientIdentifier } from '../../../navigation/types/ClientIdentifier';
import { OrderByPipe } from '../../../util/order-by.pipe';
import { ManagedClientService } from '../../services/managed-client.service';
import { ManagedClient } from '../../types/ManagedClient';
import { GuacClientUserCountComponent } from './guac-client-user-count.component';

/**
 * The English translations of the strings which the component displays.
 */
const TRANSLATIONS = {
    CLIENT: {
        INFO_ANONYMOUS_USER_COUNT : 'Anonymous{COUNT, plural, one{} other{ (#)}}',
        INFO_USER_COUNT           : '{USERNAME}{COUNT, plural, one{} other{ (#)}}',
        TEXT_ANONYMOUS_USER_JOINED: 'An anonymous user has joined the connection.',
        TEXT_ANONYMOUS_USER_LEFT  : 'An anonymous user has left the connection.',
        TEXT_USER_JOINED          : '{USERNAME} has joined the connection.',
        TEXT_USER_LEFT            : '{USERNAME} has left the connection.'
    }
};

/**
 * The name under which guacd reports a user who joined through a share link
 * without logging in.
 */
const ANONYMOUS = '';

describe('GuacClientUserCountComponent', () => {
    let fixture: ComponentFixture<GuacClientUserCountComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [GuacClientUserCountComponent],
            imports: [
                OrderByPipe,
                TranslocoTestingModule.forRoot({
                    langs: { en: TRANSLATIONS },
                    translocoConfig: { availableLangs: ['en'], defaultLang: 'en' },
                    preloadLangs: true
                })
            ],
            providers: [
                provideTranslocoMessageformat(),
                provideHttpClient(),
                provideHttpClientTesting()
            ]
        });

        // Guacamole.Display draws into canvas elements, which jsdom does not
        // implement; the display is not under test
        vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockImplementation(
            () => new Proxy({}, { get: () => () => undefined }) as CanvasRenderingContext2D);
    });

    afterEach(() => {
        fixture?.destroy();
        vi.restoreAllMocks();
    });

    /**
     * Creates a ManagedClient for a connection through ManagedClientService,
     * which installs the handlers for users joining and leaving on its
     * Guacamole.Client, as when the user opens the connection.
     *
     * @returns
     *     A new ManagedClient, not connected.
     */
    const createManagedClient = (): ManagedClient => {
        const id = TestBed.inject(ClientIdentifierService).getString({
            dataSource: 'postgresql',
            type      : ClientIdentifier.Types.CONNECTION,
            id        : '1'
        });
        return TestBed.inject(ManagedClientService).getInstance(id);
    };

    /**
     * Notifies the given client that a user joined its connection, as
     * guacamole-common-js does when guacd reports the join.
     *
     * @param managedClient
     *     The client whose connection the user joined.
     *
     * @param userID
     *     The unique ID of the connection of the user.
     *
     * @param username
     *     The name of the user.
     */
    const join = (managedClient: ManagedClient, userID: string, username: string): void => {
        managedClient.client.onjoin!(userID, username);
    };

    /**
     * Notifies the given client that a user left its connection, as
     * guacamole-common-js does when guacd reports the leave.
     *
     * @param managedClient
     *     The client whose connection the user left.
     *
     * @param userID
     *     The unique ID of the connection of the user.
     *
     * @param username
     *     The name of the user.
     */
    const leave = (managedClient: ManagedClient, userID: string, username: string): void => {
        managedClient.client.onleave!(userID, username);
    };

    /**
     * Creates the component for the given client and runs the first change
     * detection.
     *
     * @param managedClient
     *     The client whose users the component displays.
     */
    const render = (managedClient: ManagedClient): void => {
        fixture = TestBed.createComponent(GuacClientUserCountComponent);
        fixture.componentRef.setInput('client', managedClient);
        fixture.detectChanges();
    };

    /**
     * Returns the texts of all join and leave messages currently displayed,
     * newest first, as the component inserts each message at the top.
     */
    const messages = (): string[] => Array.from(
        fixture.nativeElement.querySelectorAll('.client-user-count-message') as NodeListOf<HTMLElement>,
        message => message.textContent!);

    /**
     * Returns the texts of the list of users, as shown in the tooltip. The
     * transloco directive sets innerText, which jsdom stores without
     * rendering it as text content.
     */
    const users = (): string[] => Array.from(
        fixture.nativeElement.querySelectorAll('.client-user-count-user') as NodeListOf<HTMLElement>,
        user => user.innerText);

    it('should show no messages for the users who are present when the component is created', () => {
        const managedClient = createManagedClient();
        join(managedClient, '@1', ANONYMOUS);
        join(managedClient, '@2', 'alice');
        render(managedClient);

        expect(messages()).toEqual([]);
        expect(users()).toEqual(['Anonymous', 'alice']);
    });

    it('should show a message when an anonymous user joins and when that user leaves', () => {
        const managedClient = createManagedClient();
        render(managedClient);

        join(managedClient, '@1', ANONYMOUS);
        fixture.detectChanges();

        expect(messages()).toEqual(['An anonymous user has joined the connection.']);
        expect(users()).toEqual(['Anonymous']);

        leave(managedClient, '@1', ANONYMOUS);
        fixture.detectChanges();

        expect(messages()).toEqual([
            'An anonymous user has left the connection.',
            'An anonymous user has joined the connection.'
        ]);
        expect(users()).toEqual([]);
    });

    it('should name a logged-in user in the messages', () => {
        const managedClient = createManagedClient();
        render(managedClient);

        join(managedClient, '@1', 'alice');
        fixture.detectChanges();
        leave(managedClient, '@1', 'alice');
        fixture.detectChanges();

        expect(messages()).toEqual([
            'alice has left the connection.',
            'alice has joined the connection.'
        ]);
    });

    it('should show a message for each connection of users with the same name', () => {
        const managedClient = createManagedClient();
        render(managedClient);

        join(managedClient, '@1', ANONYMOUS);
        fixture.detectChanges();
        join(managedClient, '@2', ANONYMOUS);
        fixture.detectChanges();

        expect(messages()).toEqual([
            'An anonymous user has joined the connection.',
            'An anonymous user has joined the connection.'
        ]);
        expect(users()).toEqual(['Anonymous (2)']);

        leave(managedClient, '@1', ANONYMOUS);
        fixture.detectChanges();

        expect(messages()[0]).toBe('An anonymous user has left the connection.');
        expect(users()).toEqual(['Anonymous']);
    });

    it('should show the users of a newly attached client without messages, and only its joins and leaves', () => {
        const first = createManagedClient();
        render(first);

        const second = createManagedClient();
        join(second, '@1', ANONYMOUS);
        join(second, '@2', 'alice');
        fixture.componentRef.setInput('client', second);
        fixture.detectChanges();

        expect(messages()).toEqual([]);
        expect(users()).toEqual(['Anonymous', 'alice']);

        join(first, '@3', 'bob');
        fixture.detectChanges();

        expect(messages()).toEqual([]);

        leave(second, '@2', 'alice');
        fixture.detectChanges();

        expect(messages()).toEqual(['alice has left the connection.']);
        expect(users()).toEqual(['Anonymous']);
    });

    it('should stop listening to the joins and leaves of a client once another client is attached', () => {
        const first = createManagedClient();
        render(first);

        const second = createManagedClient();
        fixture.componentRef.setInput('client', second);
        fixture.detectChanges();

        expect(first.usersChanged.observed).toBe(false);
        expect(second.usersChanged.observed).toBe(true);
    });

    it('should stop listening to the joins and leaves of the client when destroyed', () => {
        const managedClient = createManagedClient();
        render(managedClient);

        fixture.destroy();

        expect(managedClient.usersChanged.observed).toBe(false);
    });

    it('should show both messages if a user leaves and joins again before the next change detection', () => {
        const managedClient = createManagedClient();
        join(managedClient, '@1', ANONYMOUS);
        render(managedClient);

        // Reloading the page of the user ends their connection and opens a
        // new one; both arrive in the same message of the tunnel
        leave(managedClient, '@1', ANONYMOUS);
        join(managedClient, '@2', ANONYMOUS);
        fixture.detectChanges();

        expect(messages()).toEqual([
            'An anonymous user has joined the connection.',
            'An anonymous user has left the connection.'
        ]);
        expect(users()).toEqual(['Anonymous']);
    });

    it('should show both messages if a user joins and leaves again before the next change detection', () => {
        const managedClient = createManagedClient();
        render(managedClient);

        join(managedClient, '@1', ANONYMOUS);
        leave(managedClient, '@1', ANONYMOUS);
        fixture.detectChanges();

        expect(messages()).toEqual([
            'An anonymous user has left the connection.',
            'An anonymous user has joined the connection.'
        ]);
        expect(users()).toEqual([]);
    });

});
