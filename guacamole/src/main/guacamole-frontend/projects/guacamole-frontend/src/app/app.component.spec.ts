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

import { Component, Input, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormGroup } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { provideRouter, Router } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { GuacEventService } from 'guacamole-frontend-lib';
import { AppComponent } from './app.component';
import { AuthenticationService } from './auth/service/authentication.service';
import { GuacClientManagerService } from './client/services/guac-client-manager.service';
import { ClipboardService } from './clipboard/services/clipboard.service';
import { GuacFrontendEventArguments } from './events/types/GuacFrontendEventArguments';
import { ApplyPatchesService } from './index/services/apply-patches.service';
import { StyleLoaderService } from './index/services/style-loader.service';
import { LoginComponent } from './login/components/login/login.component';
import { GuacNotificationService } from './notification/services/guac-notification.service';
import { RequestService } from './rest/service/request.service';
import { Error } from './rest/types/Error';
import { Field } from './rest/types/Field';

/**
 * Stand-in for the guac-form component which exposes the inputs the template
 * of LoginComponent binds.
 */
@Component({
    selector: 'guac-form',
    template: '',
    standalone: false
})
class GuacFormStubComponent {

    /**
     * The translation namespace passed by the template.
     */
    @Input() namespace?: string;

    /**
     * The fields passed by the template.
     */
    @Input() content?: Field[];

    /**
     * The form group passed by the template.
     */
    @Input() modelFormGroup?: FormGroup;

    /**
     * The name of the focused field passed by the template.
     */
    @Input() focusedField?: string;

}

/**
 * Stand-in for the component of a route.
 */
@Component({
    template: '',
    standalone: false
})
class RouteStubComponent {
}

describe('AppComponent', () => {
    let fixture: ComponentFixture<AppComponent>;
    let eventService: GuacEventService<GuacFrontendEventArguments>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [AppComponent, LoginComponent, GuacFormStubComponent, RouteStubComponent],
            // Ignores the components of the other application states, which
            // this spec does not exercise
            schemas: [NO_ERRORS_SCHEMA],
            imports: [
                TranslocoTestingModule.forRoot({
                    langs: { en: { APP: { NAME: 'Guacamole' } } },
                    translocoConfig: { availableLangs: ['en'], defaultLang: 'en' }
                })
            ],
            providers: [
                {
                    provide: AuthenticationService,
                    useValue: { getCurrentToken: () => null }
                },
                { provide: GuacNotificationService, useValue: { getStatus: () => false } },
                { provide: GuacClientManagerService, useValue: { getManagedClients: () => ({}) } },
                { provide: ClipboardService, useValue: { resyncClipboard: vi.fn() } },
                { provide: ApplyPatchesService, useValue: { applyPatches: vi.fn() } },
                { provide: StyleLoaderService, useValue: { loadStyle: vi.fn() } },
                { provide: RequestService, useValue: {} },
                provideRouter([
                    { path: 'client/:id', component: RouteStubComponent, data: { bodyClassName: 'client' } }
                ])
            ]
        });

        eventService = TestBed.inject(GuacEventService<GuacFrontendEventArguments>);
        fixture = TestBed.createComponent(AppComponent);
        fixture.detectChanges();
    });

    afterEach(() => {
        fixture.destroy();
        document.body.className = '';
    });

    /**
     * Waits until all pending promise callbacks have run. The session check
     * interval of AppComponent keeps fixture.whenStable() from resolving.
     *
     * @returns
     *     A promise which resolves after all pending promise callbacks.
     */
    const settle = (): Promise<void> => new Promise(resolve => setTimeout(resolve));

    /**
     * Returns the classes of the login dialog currently displayed.
     *
     * @returns
     *     The classes of the element with the class "login-ui".
     */
    const getLoginClasses = (): DOMTokenList => {
        const loginUi = fixture.nativeElement.querySelector('.login-ui') as HTMLElement;
        return loginUi.classList;
    };

    it('should continue the login attempt if the submitted credentials were not sufficient', async () => {
        const username = new Field({ name: 'username', type: Field.Type.USERNAME });
        const password = new Field({ name: 'password', type: Field.Type.PASSWORD });

        // First step: the user is asked for username and password
        eventService.broadcast('guacInvalidCredentials', {
            parameters: Promise.resolve({}),
            error: new Error({ type: Error.Type.INVALID_CREDENTIALS, expected: [username, password] })
        });
        fixture.detectChanges();
        expect(getLoginClasses()).toContain('initial');

        // Second step: the password has expired and must be changed
        eventService.broadcast('guacInsufficientCredentials', {
            parameters: Promise.resolve({ username: 'alice', password: 'secret' }),
            error: new Error({
                type: Error.Type.INSUFFICIENT_CREDENTIALS,
                translatableMessage: { key: 'LOGIN.INFO_PASSWORD_EXPIRED' },
                expected: [username, password, new Field({ name: 'new-password', type: Field.Type.PASSWORD })]
            })
        });
        await settle();
        fixture.detectChanges();

        expect(getLoginClasses()).toContain('continuation');
        expect(getLoginClasses()).not.toContain('initial');
    });

    it('should reset the title and the body class when the login screen is shown', async () => {
        const title = TestBed.inject(Title);

        await TestBed.inject(Router).navigateByUrl('/client/c2lnbmFs');
        title.setTitle('ssh-target');
        expect(document.body.classList).toContain('client');

        // The session has expired while a connection was open
        eventService.broadcast('guacInvalidCredentials', {
            parameters: Promise.resolve({}),
            error: new Error({ type: Error.Type.INVALID_CREDENTIALS, expected: [] })
        });
        await settle();
        fixture.detectChanges();

        expect(document.body.classList).not.toContain('client');
        expect(title.getTitle()).toBe('Guacamole');
    });

    it('should apply the body class of the route again after logging in again', async () => {
        const router = TestBed.inject(Router);

        await router.navigateByUrl('/client/c2lnbmFs');
        eventService.broadcast('guacInvalidCredentials', {
            parameters: Promise.resolve({}),
            error: new Error({ type: Error.Type.INVALID_CREDENTIALS, expected: [] })
        });

        // A successful login reloads the same route
        await router.navigateByUrl('/client/c2lnbmFs', { onSameUrlNavigation: 'reload' });

        expect(document.body.classList).toContain('client');
    });

    it('should keep the title and the body class while the logged-out notice is shown', async () => {
        const title = TestBed.inject(Title);

        await TestBed.inject(Router).navigateByUrl('/client/c2lnbmFs');
        title.setTitle('ssh-target');

        eventService.broadcast('guacLogout', { token: 'TOKEN' });
        await settle();
        fixture.detectChanges();

        expect(fixture.nativeElement.querySelector('.logged-out-modal')).not.toBeNull();
        expect(document.body.classList).toContain('client');
        expect(title.getTitle()).toBe('ssh-target');
    });

});
