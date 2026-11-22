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
import { NavigationError, provideRouter, Router, RouterOutlet, Routes, withHashLocation } from '@angular/router';
import { provideTransloco, TRANSLOCO_LOADER, Translation, TranslocoTestingModule } from '@jsverse/transloco';
import { GuacEventService } from 'guacamole-frontend-lib';
import { catchError, defer, Observable, of, Subject, throwError } from 'rxjs';
import { AppComponent } from './app.component';
import { appRoutes } from './app-routing.module';
import { AuthenticationService } from './auth/service/authentication.service';
import { GuacClientManagerService } from './client/services/guac-client-manager.service';
import { ManagedClient } from './client/types/ManagedClient';
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

    /**
     * Whether the template disabled the fields of the form.
     */
    @Input() disabled = false;

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

    /**
     * The keyboard created by AppComponent, whose updateModifiers() is
     * replaced by a mock.
     */
    let keyboard: Guacamole.Keyboard;

    beforeEach(() => {

        // Capture the keyboard that AppComponent creates for itself
        const RealKeyboard = Guacamole.Keyboard;
        vi.spyOn(Guacamole, 'Keyboard').mockImplementation(function (element: HTMLElement | Document) {
            keyboard = new RealKeyboard(element);
            vi.spyOn(keyboard, 'updateModifiers').mockImplementation(() => undefined);
            return keyboard;
        } as unknown as typeof Guacamole.Keyboard);

        TestBed.configureTestingModule({
            declarations: [AppComponent, LoginComponent, GuacFormStubComponent, RouteStubComponent],
            // Ignores the components of the other application states, which
            // this spec does not exercise
            schemas: [NO_ERRORS_SCHEMA],
            imports: [
                TranslocoTestingModule.forRoot({
                    langs: { en: { APP: { NAME: 'Guacamole', TEXT_UNTRANSLATED: '{{MESSAGE}}' } } },
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
        vi.restoreAllMocks();
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

    it('should show all expected fields, filled with the submitted values, when the login continues', async () => {
        const username = new Field({ name: 'username', type: Field.Type.USERNAME });
        const password = new Field({ name: 'password', type: Field.Type.PASSWORD });
        const newPassword = new Field({ name: 'new-password', type: Field.Type.PASSWORD });

        // The password has expired: the server expects the submitted
        // credentials again, together with a new password
        eventService.broadcast('guacInsufficientCredentials', {
            parameters: Promise.resolve({ username: 'alice', password: 'secret' }),
            error: new Error({
                type: Error.Type.INSUFFICIENT_CREDENTIALS,
                translatableMessage: { key: 'LOGIN.INFO_PASSWORD_EXPIRED' },
                expected: [username, password, newPassword]
            })
        });
        await settle();
        fixture.detectChanges();

        const form = fixture.debugElement.query(By.directive(GuacFormStubComponent)).componentInstance as GuacFormStubComponent;
        expect(form.content).toEqual([username, password, newPassword]);
        expect(form.modelFormGroup?.value).toEqual({ username: 'alice', password: 'secret', 'new-password': '' });
    });

    /**
     * Returns the stand-in of the guac-form within the login dialog.
     *
     * @returns
     *     The GuacFormStubComponent rendered by LoginComponent.
     */
    const getLoginForm = (): GuacFormStubComponent =>
        fixture.debugElement.query(By.directive(GuacFormStubComponent)).componentInstance as GuacFormStubComponent;

    it('should focus the first field of a new login form', () => {
        eventService.broadcast('guacInvalidCredentials', {
            parameters: Promise.resolve({}),
            error: new Error({
                type    : Error.Type.INVALID_CREDENTIALS,
                expected: [
                    new Field({ name: 'username', type: Field.Type.USERNAME }),
                    new Field({ name: 'password', type: Field.Type.PASSWORD })
                ]
            })
        });
        fixture.detectChanges();

        expect(getLoginForm().focusedField).toBe('username');
    });

    it('should focus the first empty field when the login continues', async () => {

        // The password has expired: username and password are filled with
        // the submitted values, the new password is still empty
        eventService.broadcast('guacInsufficientCredentials', {
            parameters: Promise.resolve({ username: 'alice', password: 'secret' }),
            error: new Error({
                type               : Error.Type.INSUFFICIENT_CREDENTIALS,
                translatableMessage: { key: 'LOGIN.INFO_PASSWORD_EXPIRED' },
                expected           : [
                    new Field({ name: 'username', type: Field.Type.USERNAME }),
                    new Field({ name: 'password', type: Field.Type.PASSWORD }),
                    new Field({ name: 'new-password', type: Field.Type.PASSWORD }),
                    new Field({ name: 'confirm-new-password', type: Field.Type.PASSWORD })
                ]
            })
        });
        await settle();
        fixture.detectChanges();

        expect(getLoginForm().focusedField).toBe('new-password');
    });

    it('should focus the password field after the credentials were rejected', () => {
        const expected = (): Field[] => [
            new Field({ name: 'username', type: Field.Type.USERNAME }),
            new Field({ name: 'password', type: Field.Type.PASSWORD })
        ];
        const invalid = new Error({ type: Error.Type.INVALID_CREDENTIALS, expected: expected() });

        eventService.broadcast('guacInvalidCredentials', { parameters: Promise.resolve({}), error: invalid });
        fixture.detectChanges();

        // The user enters a wrong password
        getLoginForm().modelFormGroup!.setValue({ username: 'alice', password: 'wrong' });

        // The server rejects the credentials and asks for them again
        const rejected = new Error({ type: Error.Type.INVALID_CREDENTIALS, expected: expected() });
        eventService.broadcast('guacLoginFailed', { parameters: Promise.resolve({}), error: rejected });
        eventService.broadcast('guacInvalidCredentials', { parameters: Promise.resolve({}), error: rejected });
        fixture.detectChanges();

        expect(getLoginForm().modelFormGroup?.value).toEqual({ username: 'alice', password: '' });
        expect(getLoginForm().focusedField).toBe('password');
    });

    it('should disable the fields of the login form while the login attempt is pending', () => {
        const rejected = new Error({
            type    : Error.Type.INVALID_CREDENTIALS,
            expected: [
                new Field({ name: 'username', type: Field.Type.USERNAME }),
                new Field({ name: 'password', type: Field.Type.PASSWORD })
            ]
        });

        eventService.broadcast('guacInvalidCredentials', { parameters: Promise.resolve({}), error: rejected });
        fixture.detectChanges();
        expect(getLoginForm().disabled).toBe(false);

        // The attempt is submitted: the fields and the buttons are disabled
        eventService.broadcast('guacLoginPending', { parameters: Promise.resolve({}) });
        fixture.detectChanges();
        expect(getLoginForm().disabled).toBe(true);
        expect(fixture.nativeElement.querySelector('input.login').disabled).toBe(true);

        // The credentials are rejected: the fields and the buttons are enabled again
        eventService.broadcast('guacLoginFailed', { parameters: Promise.resolve({}), error: rejected });
        fixture.detectChanges();
        expect(getLoginForm().disabled).toBe(false);
        expect(fixture.nativeElement.querySelector('input.login').disabled).toBe(false);
    });

    /**
     * A message of the server which spans two lines.
     */
    const MULTI_LINE_MESSAGE = 'First line of the message\nSecond line of the message';

    /**
     * Creates an error of the REST API with the given type and a message
     * which spans two lines.
     *
     * @param type
     *     The type of the error.
     *
     * @param expected
     *     The fields the server expects, if any.
     *
     * @returns
     *     The error.
     */
    const createMultiLineError = (type: Error.Type, expected?: Field[]): Error => new Error({
        type,
        expected,
        translatableMessage: { key: 'APP.TEXT_UNTRANSLATED', variables: { MESSAGE: MULTI_LINE_MESSAGE } }
    });

    /**
     * Verifies that the given paragraph holds the multi-line message as
     * plain text, without line break elements, so that the line breaks
     * render as spaces like in the AngularJS frontend.
     *
     * @param paragraph
     *     The paragraph which displays the message.
     */
    const expectPlainMessage = (paragraph: HTMLElement): void => {
        expect(paragraph.querySelector('br')).toBeNull();
        expect(paragraph.textContent!.trim()).toBe(MULTI_LINE_MESSAGE);
    };

    it('should show a login error with line breaks as one paragraph of plain text', () => {
        const expected = [new Field({ name: 'username', type: Field.Type.USERNAME })];
        eventService.broadcast('guacInvalidCredentials', {
            parameters: Promise.resolve({}),
            error: new Error({ type: Error.Type.INVALID_CREDENTIALS, expected })
        });
        fixture.detectChanges();

        eventService.broadcast('guacLoginFailed', {
            parameters: Promise.resolve({}),
            error: createMultiLineError(Error.Type.PERMISSION_DENIED, expected)
        });
        fixture.detectChanges();

        expectPlainMessage(fixture.nativeElement.querySelector('.login-ui .login-error'));
    });

    it('should show a help text with line breaks as one paragraph of plain text', async () => {
        eventService.broadcast('guacInsufficientCredentials', {
            parameters: Promise.resolve({ username: 'alice' }),
            error: createMultiLineError(Error.Type.INSUFFICIENT_CREDENTIALS, [
                new Field({ name: 'username', type: Field.Type.USERNAME }),
                new Field({ name: 'code', type: Field.Type.TEXT })
            ])
        });
        await settle();
        fixture.detectChanges();

        expectPlainMessage(fixture.nativeElement.querySelector('.login-ui .login-form > p'));
    });

    it('should show the rejection of an automatic login with line breaks as one paragraph of plain text', () => {
        eventService.broadcast('guacLoginFailed', {
            parameters: Promise.resolve({}),
            error: createMultiLineError(Error.Type.PERMISSION_DENIED)
        });
        fixture.detectChanges();

        expectPlainMessage(fixture.nativeElement.querySelector('.automatic-login-rejected-modal .notification > p'));
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

    it.each(['guacBeforeClientMouseDown', 'guacClientMouseUp'] as const)(
        'should update the keyboard modifiers from the mouse event of %s', (eventName) => {
            const inputEvent = { type: 'mouse' } as unknown as Guacamole.Mouse.Event;

            eventService.broadcast(eventName, { inputEvent, client: {} as ManagedClient });

            expect(keyboard.updateModifiers).toHaveBeenCalledExactlyOnceWith(inputEvent);
        });

    it('should not update the keyboard modifiers after the mouse button has been pressed', () => {
        eventService.broadcast('guacClientMouseDown', {
            inputEvent: { type: 'mousedown' } as unknown as Guacamole.Mouse.Event,
            client: {} as ManagedClient
        });

        expect(keyboard.updateModifiers).not.toHaveBeenCalled();
    });

    it.each(['guacClientTouchStart', 'guacClientTouchEnd'] as const)(
        'should update the keyboard modifiers from the touch event of %s', (eventName) => {
            const inputEvent = { type: 'touch' } as unknown as Guacamole.Touch.Event;

            eventService.broadcast(eventName, { inputEvent, client: {} as ManagedClient });

            expect(keyboard.updateModifiers).toHaveBeenCalledExactlyOnceWith(inputEvent);
        });

    it('should not update the keyboard modifiers when the mouse moves', () => {
        eventService.broadcast('guacClientMouseMove', {
            inputEvent: { type: 'mousemove' } as unknown as Guacamole.Mouse.Event,
            client: {} as ManagedClient
        });

        expect(keyboard.updateModifiers).not.toHaveBeenCalled();
    });

    it('should not update the keyboard modifiers when a touch moves', () => {
        eventService.broadcast('guacClientTouchMove', {
            inputEvent: { type: 'touchmove' } as unknown as Guacamole.Touch.Event,
            client: {} as ManagedClient
        });

        expect(keyboard.updateModifiers).not.toHaveBeenCalled();
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
     * The observable which the stub of UserPageService.getHomePage()
     * returns.
     */
    let homePage: Observable<PageDefinition>;

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
        homePage = of(new PageDefinition({ name: 'APP.NAME', url: '/' }));

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
                    useValue: { getHomePage: () => homePage }
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

    it('should keep the error of the page instead of opening the home page if the home page cannot be determined', async () => {
        vi.spyOn(console, 'error').mockImplementation(() => undefined);
        const navigationErrors: NavigationError[] = [];
        router.events.subscribe(event => event instanceof NavigationError && navigationErrors.push(event));

        // UserPageService handles a failed request with RequestService.DIE
        const requestService = TestBed.inject(RequestService);
        homePage = defer(() => throwError(() => new Error({ type: Error.Type.INTERNAL_ERROR, message: 'Simulated failure' })))
            .pipe(catchError(requestService.DIE));

        open('/');
        await respondToTokenRequest(session);

        expect(fixture.nativeElement.querySelector('.fatal-page-error')).not.toBeNull();
        expect(isPageShown()).toBe(false);
        expect(navigationErrors).toEqual([]);
    });

    it('should open the home page if it is the page at the root path', async () => {
        homePage = of(new PageDefinition({ name: 'APP.NAME', url: '/' }));

        open('/');
        await respondToTokenRequest(session);

        expect(isPageShown()).toBe(true);
        expect(router.url).toBe('/');
    });

    it('should open the home page at the root path although the URL has query parameters', async () => {
        homePage = of(new PageDefinition({ name: 'APP.NAME', url: '/' }));

        open('/?foo=bar');
        const request = await respondToTokenRequest(session);

        expect(request.get('foo')).toBe('bar');
        expect(isPageShown()).toBe(true);
        expect(router.url).toBe('/?foo=bar');
    });

    it('should open the only connection of the user instead of the root path', async () => {
        homePage = of(new PageDefinition({ name: 'connection', url: '/client/c2lnbmFs' }));

        open('/');
        await respondToTokenRequest(session);
        await respondToTokenRequest(session);

        expect(isPageShown()).toBe(true);
        expect(router.url).toBe('/client/c2lnbmFs');
    });

    it('should open the only connection of the user instead of the root path with query parameters', async () => {
        homePage = of(new PageDefinition({ name: 'connection', url: '/client/c2lnbmFs' }));

        open('/?foo=bar');
        await respondToTokenRequest(session);
        const request = await respondToTokenRequest(session);

        expect(request.get('foo')).toBe('bar');
        expect(isPageShown()).toBe(true);
        expect(router.url).toBe('/client/c2lnbmFs?foo=bar');
    });

});

describe('AppComponent with a translation which arrives late', () => {
    let fixture: ComponentFixture<AppComponent>;
    let router: Router;
    let title: Title;

    /**
     * Emits the translation of the active language once the test lets the
     * translation file "arrive".
     */
    let translationFile: Subject<Translation>;

    /**
     * Lets the translation file arrive and waits until the app has handled it.
     *
     * @returns
     *     A promise which resolves once the translation has been handled.
     */
    const receiveTranslation = async (): Promise<void> => {
        translationFile.next({ APP: { NAME: 'Guacamole' } });
        translationFile.complete();
        await settle();
    };

    beforeEach(() => {
        translationFile = new Subject<Translation>();

        TestBed.configureTestingModule({
            declarations: [AppComponent, RouteStubComponent],
            // Ignores the components of the other application states, which
            // this spec does not exercise
            schemas: [NO_ERRORS_SCHEMA],
            providers: [
                provideTransloco({ config: { availableLangs: ['en'], defaultLang: 'en' } }),
                { provide: TRANSLOCO_LOADER, useValue: { getTranslation: () => translationFile } },
                { provide: AuthenticationService, useValue: { getCurrentToken: () => null } },
                { provide: GuacNotificationService, useValue: { getStatus: () => false } },
                { provide: GuacClientManagerService, useValue: { getManagedClients: () => ({}) } },
                { provide: ClipboardService, useValue: { resyncClipboard: vi.fn() } },
                { provide: ApplyPatchesService, useValue: { applyPatches: vi.fn() } },
                { provide: StyleLoaderService, useValue: { loadStyle: vi.fn() } },
                { provide: RequestService, useValue: {} },
                // The page of an extension, which sets its title without
                // waiting for the translation
                provideRouter([
                    { path: 'extension', component: RouteStubComponent, title: 'Angular Extension' }
                ])
            ]
        });

        router = TestBed.inject(Router);
        title = TestBed.inject(Title);
        title.setTitle('');
        fixture = TestBed.createComponent(AppComponent);
        fixture.detectChanges();
    });

    afterEach(() => {
        fixture.destroy();
        document.body.className = '';
    });

    it('should keep the title of the route if the translation arrives after the route was opened', async () => {
        await router.navigateByUrl('/extension');
        expect(title.getTitle()).toBe('Angular Extension');

        await receiveTranslation();

        expect(title.getTitle()).toBe('Angular Extension');
    });

    it('should show the title of the route if the translation arrives before the route is opened', async () => {
        await receiveTranslation();
        expect(title.getTitle()).toBe('Guacamole');

        await router.navigateByUrl('/extension');

        expect(title.getTitle()).toBe('Angular Extension');
    });

    it('should show the title of the app if no route has been opened when the translation arrives', async () => {
        await receiveTranslation();

        expect(title.getTitle()).toBe('Guacamole');
    });

});
