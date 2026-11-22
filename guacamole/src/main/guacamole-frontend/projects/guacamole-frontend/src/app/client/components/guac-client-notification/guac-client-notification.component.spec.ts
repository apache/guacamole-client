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

import { Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { of } from 'rxjs';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import { UserPageService } from '../../../manage/services/user-page.service';
import { PageDefinition } from '../../../navigation/types/PageDefinition';
import { NotificationModule } from '../../../notification/notification.module';
import { Notification } from '../../../notification/types/Notification';
import { RequestService } from '../../../rest/service/request.service';
import { Field } from '../../../rest/types/Field';
import { Form } from '../../../rest/types/Form';
import { GuacClientManagerService } from '../../services/guac-client-manager.service';
import { ManagedClientService } from '../../services/managed-client.service';
import { ManagedClient } from '../../types/ManagedClient';
import { ManagedClientState } from '../../types/ManagedClientState';
import { GuacClientNotificationComponent } from './guac-client-notification.component';

/**
 * Placeholder for the pages the router navigates to in this spec.
 */
@Component({ template: '', standalone: false })
class PageStubComponent {
}

describe('GuacClientNotificationComponent', () => {
    let router: Router;
    let fixture: ComponentFixture<GuacClientNotificationComponent>;

    /**
     * The home page of the user, as returned by the UserPageService.
     */
    let homePage: PageDefinition;

    /**
     * Shows the notification of a disconnected client, as the user's home page
     * is the one with the given URL and the app currently shows the given URL.
     *
     * @param homeUrl
     *     The URL of the home page of the user.
     *
     * @param currentUrl
     *     The URL the router currently shows, which may include a query.
     *
     * @returns
     *     The names of the actions of the notification, in order.
     */
    async function getActions(homeUrl: string, currentUrl: string): Promise<string[]> {

        homePage = new PageDefinition({ name: 'HOME', url: homeUrl });

        await router.navigateByUrl(currentUrl);

        fixture = TestBed.createComponent(GuacClientNotificationComponent);
        fixture.componentRef.setInput('client', new ManagedClient({
            id: 'abc',
            client: {} as Guacamole.Client,
            tunnel: {} as Guacamole.Tunnel,
            clientState: new ManagedClientState({ connectionState: ManagedClientState.ConnectionState.DISCONNECTED })
        }));
        fixture.detectChanges();

        return (fixture.componentInstance.status as Notification).actions!.map(action => action.name);
    }

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [GuacClientNotificationComponent, PageStubComponent],
            imports: [
                NotificationModule,
                TranslocoTestingModule.forRoot({
                    langs: { en: {} },
                    translocoConfig: { availableLangs: ['en'], defaultLang: 'en' }
                })
            ],
            providers: [
                provideRouter([
                    { path: '', component: PageStubComponent },
                    { path: 'client/:id', component: PageStubComponent }
                ]),
                { provide: UserPageService, useValue: { getHomePage: () => of(homePage) } },
                { provide: AuthenticationService, useValue: { updateCurrentToken: () => of(undefined), logout: vi.fn() } },
                { provide: GuacClientManagerService, useValue: {} },
                { provide: ManagedClientService, useValue: { sendArguments: vi.fn() } },
                { provide: RequestService, useValue: { WARN: vi.fn(), IGNORE: vi.fn() } }
            ],
            schemas: [NO_ERRORS_SCHEMA]
        });
        router = TestBed.inject(Router);
    });

    it('should offer to go home if the home page is not the page shown', async () => {
        const actions = await getActions('/', '/client/abc');

        expect(actions).toEqual(['CLIENT.ACTION_NAVIGATE_HOME', 'CLIENT.ACTION_RECONNECT', 'CLIENT.ACTION_LOGOUT']);
    });

    it('should not offer to go home if the home page is the page shown', async () => {
        const actions = await getActions('/', '/');

        expect(actions).toEqual(['CLIENT.ACTION_RECONNECT', 'CLIENT.ACTION_LOGOUT']);
    });

    it('should offer to go home if the home page is another connection', async () => {
        const actions = await getActions('/client/abc', '/client/xyz');

        expect(actions).toEqual(['CLIENT.ACTION_NAVIGATE_HOME', 'CLIENT.ACTION_RECONNECT', 'CLIENT.ACTION_LOGOUT']);
    });

    it('should not offer to go home if the home page is the connection shown', async () => {
        const actions = await getActions('/client/abc', '/client/abc');

        expect(actions).toEqual(['CLIENT.ACTION_RECONNECT', 'CLIENT.ACTION_LOGOUT']);
    });

    it('should ignore the query of the URL when comparing it to the home page', async () => {
        const actions = await getActions('/client/abc', '/client/abc?foo=bar');

        expect(actions).toEqual(['CLIENT.ACTION_RECONNECT', 'CLIENT.ACTION_LOGOUT']);
    });

    it('should send the parameters entered by the user when continuing the connection', async () => {
        homePage = new PageDefinition({ name: 'HOME', url: '/' });

        const client = new ManagedClient({
            id: 'abc',
            client: {} as Guacamole.Client,
            tunnel: {} as Guacamole.Tunnel,
            protocol: 'vnc',
            forms: [new Form({
                name: 'authentication',
                fields: [
                    new Field({ name: 'username', type: Field.Type.USERNAME }),
                    new Field({ name: 'password', type: Field.Type.PASSWORD })
                ]
            })],
            clientState: new ManagedClientState({ connectionState: ManagedClientState.ConnectionState.WAITING })
        });

        // The server requests only the password, as by a "required" instruction
        client.requiredParameters = { password: '' };

        fixture = TestBed.createComponent(GuacClientNotificationComponent);
        fixture.componentRef.setInput('client', client);
        fixture.detectChanges();
        await fixture.whenStable();
        fixture.detectChanges();

        const input: HTMLInputElement = fixture.nativeElement.querySelector('input[name="password"]');
        input.value = 'secret';
        input.dispatchEvent(new Event('input'));

        // The first action of the prompt continues the connection
        fixture.nativeElement.querySelector('.parameters-required .buttons button').click();

        expect(TestBed.inject(ManagedClientService).sendArguments).toHaveBeenCalledWith(client, { password: 'secret' });
    });
});
