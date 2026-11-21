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

import { PlatformLocation } from '@angular/common';
import { HTTP_INTERCEPTORS, HttpParams, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, Input, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormGroup, FormsModule } from '@angular/forms';
import { By, Title } from '@angular/platform-browser';
import { provideRouter, Router, RouterOutlet, Routes, withHashLocation } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { GuacEventService } from 'guacamole-frontend-lib';
import { of } from 'rxjs';
import { AppComponent } from './app.component';
import { appRoutes } from './app-routing.module';
import { AuthenticationService } from './auth/service/authentication.service';
import { GuacClientManagerService } from './client/services/guac-client-manager.service';
import { ClipboardService } from './clipboard/services/clipboard.service';
import { GuacFrontendEventArguments } from './events/types/GuacFrontendEventArguments';
import { ApplyPatchesService } from './index/services/apply-patches.service';
import { StyleLoaderService } from './index/services/style-loader.service';
import { TranslationLoaderService } from './locale/service/translation-loader.service';
import { LoginComponent } from './login/components/login/login.component';
import { UserPageService } from './manage/services/user-page.service';
import { PageDefinition } from './navigation/types/PageDefinition';
import { GuacNotificationService } from './notification/services/guac-notification.service';
import { ErrorHandlingInterceptor } from './rest/interceptor/error-handling.interceptor';
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

/**
 * Waits until all pending promise callbacks have run. The session check
 * interval of AppComponent keeps fixture.whenStable() from resolving.
 *
 * @returns
 *     A promise which resolves after all pending promise callbacks.
 */
const settle = (): Promise<void> => new Promise(resolve => setTimeout(resolve));

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

/**
 * Copies the routes of the app, keeping paths, data, guards and children but
 * replacing each component with RouteStubComponent and dropping titles.
 *
 * @param routes
 *     The routes to copy.
 *
 * @returns
 *     The copied routes.
 */
const stubRoutes = (routes: Routes): Routes => routes.map(route => ({
    path                 : route.path,
    data                 : route.data,
    component            : RouteStubComponent,
    canActivate          : route.canActivate,
    runGuardsAndResolvers: route.runGuardsAndResolvers,
    children             : route.children && stubRoutes(route.children)
}));

describe('AppComponent with the routes of the app', () => {
    let fixture: ComponentFixture<AppComponent>;
    let router: Router;
    let platformLocation: PlatformLocation;
    let httpMock: HttpTestingController;

    /**
     * The response of the REST API to a successful authentication.
     */
    const session = {
        authToken           : 'TOKEN',
        username            : 'guacadmin',
        dataSource          : 'postgresql',
        availableDataSources: ['postgresql']
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [AppComponent, LoginComponent, GuacFormStubComponent, RouteStubComponent],
            // Ignores the components of the other application states, which
            // this spec does not exercise
            schemas: [NO_ERRORS_SCHEMA],
            imports: [
                FormsModule,
                RouterOutlet,
                TranslocoTestingModule.forRoot({
                    langs: { en: { APP: { NAME: 'Guacamole' } } },
                    translocoConfig: { availableLangs: ['en'], defaultLang: 'en' }
                })
            ],
            providers: [
                // The real AuthenticationService against a fake REST API
                { provide: HTTP_INTERCEPTORS, useClass: ErrorHandlingInterceptor, multi: true },
                provideHttpClient(withInterceptorsFromDi()),
                provideHttpClientTesting(),
                {
                    provide : UserPageService,
                    useValue: { getHomePage: () => of(new PageDefinition({ name: 'APP.NAME', url: '/' })) }
                },
                { provide: GuacNotificationService, useValue: { getStatus: () => false } },
                {
                    provide : GuacClientManagerService,
                    useValue: { getManagedClients: () => ({}), getManagedClientGroups: () => [] }
                },
                { provide: ClipboardService, useValue: { resyncClipboard: vi.fn() } },
                { provide: ApplyPatchesService, useValue: { applyPatches: vi.fn() } },
                { provide: StyleLoaderService, useValue: { loadStyle: vi.fn() } },
                {
                    provide : TranslationLoaderService,
                    useValue: { getTranslation: () => Promise.resolve({ APP: { NAME: 'Guacamole' } }) }
                },
                // Routes and hash routing of the app routing module
                provideRouter(stubRoutes(appRoutes), withHashLocation())
            ]
        });

        router = TestBed.inject(Router);
        platformLocation = TestBed.inject(PlatformLocation);
        httpMock = TestBed.inject(HttpTestingController);
        fixture = TestBed.createComponent(AppComponent);
        fixture.detectChanges();
    });

    afterEach(() => {
        fixture.destroy();
        localStorage.removeItem('GUAC_AUTH_TOKEN');
        document.body.className = '';
        httpMock.verify();
    });

    /**
     * Opens the given URL of the app as if it had been entered in the
     * address bar of a new browser tab.
     *
     * @param url
     *     The URL to open, relative to the app, such as "/settings/users".
     */
    const open = (url: string): void => {
        platformLocation.replaceState(null, '', '#' + url);
        router.initialNavigation();
    };

    /**
     * Answers the pending request to the token REST API endpoint and waits
     * until the app has handled the response.
     *
     * @param body
     *     The body of the response.
     *
     * @param status
     *     The HTTP status of the response.
     *
     * @returns
     *     A promise which resolves with the parameters of the answered request.
     */
    const respondToTokenRequest = async (body: object, status = 200): Promise<HttpParams> => {
        await settle();
        const request = httpMock.expectOne('api/tokens');
        request.flush(body, { status, statusText: status === 200 ? 'OK' : 'Forbidden' });
        await settle();
        fixture.detectChanges();
        return request.request.body;
    };

    /**
     * Rejects the pending authentication request as the REST API does if no
     * valid session exists, asking for username and password.
     *
     * @returns
     *     A promise which resolves once the app has handled the response.
     */
    const rejectCredentials = async (): Promise<void> => {
        await respondToTokenRequest({
            type    : Error.Type.INVALID_CREDENTIALS,
            message : 'Permission Denied.',
            expected: [
                { name: 'username', type: Field.Type.USERNAME },
                { name: 'password', type: Field.Type.PASSWORD }
            ]
        }, 403);
    };

    /**
     * Enters username and password into the login form, submits it and
     * accepts the login, as well as the re-authentication of the route
     * which the app opens after the login.
     *
     * @returns
     *     A promise which resolves with the parameters of the login request
     *     and of the subsequent re-authentication, in that order.
     */
    const logIn = async (): Promise<HttpParams[]> => {
        const form = fixture.debugElement.query(By.directive(GuacFormStubComponent))
            .componentInstance as GuacFormStubComponent;
        form.modelFormGroup!.patchValue({ username: 'guacadmin', password: 'guacadmin' });
        fixture.nativeElement.querySelector('input.login').click();

        const login = await respondToTokenRequest(session);
        const reauthentication = await respondToTokenRequest(session);
        return [login, reauthentication];
    };

    /**
     * Returns whether the login form is displayed.
     *
     * @returns
     *     true if the login form is displayed, false otherwise.
     */
    const isLoginShown = (): boolean => fixture.nativeElement.querySelector('.login-ui') !== null;

    /**
     * Returns whether the component of a route is displayed.
     *
     * @returns
     *     true if the component of a route is displayed, false otherwise.
     */
    const isPageShown = (): boolean => fixture.debugElement.query(By.directive(RouteStubComponent)) !== null;

    it('should keep the requested URL while logging in and open it after the login', async () => {
        open('/manage/postgresql/users/t54user');
        await rejectCredentials();

        expect(isLoginShown()).toBe(true);
        expect(platformLocation.hash).toBe('#/manage/postgresql/users/t54user');

        await logIn();

        expect(isLoginShown()).toBe(false);
        expect(isPageShown()).toBe(true);
        expect(router.url).toBe('/manage/postgresql/users/t54user');
        expect(platformLocation.hash).toBe('#/manage/postgresql/users/t54user');
    });

    it('should open the home page after logging out and logging in again', async () => {
        open('/client/c2lnbmFs');
        await respondToTokenRequest(session);
        expect(isPageShown()).toBe(true);

        // Log out, then choose to log in again
        TestBed.inject(AuthenticationService).logout().subscribe();
        httpMock.expectOne('api/session').flush(null);
        fixture.detectChanges();
        fixture.nativeElement.querySelector('.logged-out-modal button').click();
        await rejectCredentials();

        expect(isLoginShown()).toBe(true);
        expect(platformLocation.hash).toBe('#/');

        await logIn();

        expect(isPageShown()).toBe(true);
        expect(router.url).toBe('/');

        // As in AngularJS, the page of the connection remains in the history
        platformLocation.back();
        await respondToTokenRequest(session);

        expect(router.url).toBe('/client/c2lnbmFs');
    });

    it('should keep the query parameters of the requested URL while logging in and send them', async () => {
        open('/?foo=bar');
        await rejectCredentials();

        expect(isLoginShown()).toBe(true);
        expect(platformLocation.hash).toBe('#/?foo=bar');

        const [login, reauthentication] = await logIn();

        expect(login.get('foo')).toBe('bar');
        expect(login.get('username')).toBe('guacadmin');
        expect(reauthentication.get('foo')).toBe('bar');
        expect(reauthentication.get('token')).toBe('TOKEN');
        expect(isPageShown()).toBe(true);
        expect(router.url).toBe('/?foo=bar');
    });

});
