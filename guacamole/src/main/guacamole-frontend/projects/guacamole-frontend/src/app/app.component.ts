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

import { AfterViewChecked, Component, DestroyRef, Inject, OnInit, Renderer2, ViewEncapsulation, DOCUMENT, ChangeDetectionStrategy } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { NavigationEnd, NavigationStart, Router } from '@angular/router';
import { TranslocoService } from '@jsverse/transloco';
import { GuacEventService } from 'guacamole-frontend-lib';
import _ from 'lodash';
import { filter, take } from 'rxjs';
import { AuthenticationService } from './auth/service/authentication.service';
import { GuacClientManagerService } from './client/services/guac-client-manager.service';
import { ManagedClientGroup } from './client/types/ManagedClientGroup';
import { ManagedClientState } from './client/types/ManagedClientState';
import { ClipboardService } from './clipboard/services/clipboard.service';
import { GuacFrontendEventArguments } from './events/types/GuacFrontendEventArguments';
import { ApplyPatchesService } from './index/services/apply-patches.service';
import { StyleLoaderService } from './index/services/style-loader.service';
import { GuacNotificationService } from './notification/services/guac-notification.service';
import { Error } from './rest/types/Error';
import { Field } from './rest/types/Field';
import { TranslatableMessage } from './rest/types/TranslatableMessage';
import { ApplicationState } from './util/ApplicationState';

/**
 * The number of milliseconds that should elapse between client-side
 * session checks. This DOES NOT impact whether a session expires at all;
 * such checks will always be server-side. This only affects how quickly
 * the client-side view can recognize that a user's session has expired
 * absent any action taken by the user.
 */
const SESSION_VALIDITY_RECHECK_INTERVAL = 15000;

/**
 * Name of the file that contains additional styles provided by extensions.
 */
const EXTENSION_STYLES_FILENAME = 'app.css';

/**
 * The Component for the root of the application.
 */
@Component({
    selector: 'guac-root',
    templateUrl: './app.component.html',
    encapsulation: ViewEncapsulation.None,
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class AppComponent implements OnInit, AfterViewChecked {

    /**
     * The error that prevents the current page from rendering at all. If no
     * such error has occurred, this will be null.
     */
    fatalError: Error | null = null;

    /**
     * The message to display to the user as instructions for the login
     * process.
     */
    loginHelpText: TranslatableMessage | null = null;

    /**
     * Whether the user has selected to log back in after having logged out.
     */
    reAuthenticating = false;

    /**
     * The credentials that the authentication service is has already accepted,
     * pending additional credentials, if any. If credentials have
     * been accepted, this will contain a form control for each of
     * the parameters submitted in a previous authentication attempt.
     */
    acceptedCredentials: FormGroup = new FormGroup<any>({});

    /**
     * The credentials that the authentication service is currently expecting,
     * if any. If the user is logged in, this will be null.
     */
    expectedCredentials: Field[] | null = null;

    /**
     * The current overall state of the client side of the application.
     * Possible values are defined by {@link ApplicationState}.
     */
    applicationState: ApplicationState = ApplicationState.LOADING;

    /**
     * Provide access to the ApplicationState enum within the template.
     */
    readonly ApplicationState = ApplicationState;

    /**
     * The name of the CSS class which is applied to the body element for the
     * current page, if any.
     */
    private bodyClassName: string | null = null;

    /**
     * Inject required services.
     */
    constructor(private authenticationService: AuthenticationService,
                protected notificationService: GuacNotificationService,
                private guacEventService: GuacEventService<GuacFrontendEventArguments>,
                private guacClientManager: GuacClientManagerService,
                private clipboardService: ClipboardService,
                private applyPatchesService: ApplyPatchesService,
                private styleLoaderService: StyleLoaderService,
                private translocoService: TranslocoService,
                private router: Router,
                private title: Title,
                @Inject(DOCUMENT) private document: Document,
                private renderer: Renderer2,
                private destroyRef: DestroyRef) {
    }

    ngOnInit(): void {

        // Load extension styles
        this.styleLoaderService.loadStyle(EXTENSION_STYLES_FILENAME);

        // Add default destination for input events
        const sink = new Guacamole.InputSink();
        this.document.body.appendChild(sink.getElement());

        // Create event listeners at the global level
        const keyboard = new Guacamole.Keyboard(this.document);
        keyboard.listenTo(sink.getElement());

        // Broadcast keydown events
        keyboard.onkeydown = keysym => {

            // Do not handle key events if not logged in
            if (this.applicationState !== ApplicationState.READY)
                return true;

            // Warn of pending keydown
            const guacBeforeKeydownEvent = this.guacEventService.broadcast('guacBeforeKeydown',
                { keysym, keyboard }
            );
            if (guacBeforeKeydownEvent.defaultPrevented)
                return true;

            // If not prevented via guacBeforeKeydown, fire corresponding keydown event
            const guacKeydownEvent = this.guacEventService.broadcast('guacKeydown', {
                keysym,
                keyboard
            });
            return !guacKeydownEvent.defaultPrevented;

        };

        // Broadcast keyup events
        keyboard.onkeyup = keysym => {

            // Do not handle key events if not logged in or if a notification is
            // shown
            if (this.applicationState !== ApplicationState.READY)
                return;

            // Warn of pending keyup
            const guacBeforeKeydownEvent = this.guacEventService.broadcast('guacBeforeKeyup', {
                keysym,
                keyboard
            });
            if (guacBeforeKeydownEvent.defaultPrevented)
                return;

            // If not prevented via guacBeforeKeyup, fire corresponding keydown event
            this.guacEventService.broadcast('guacKeyup', { keysym, keyboard });

        };

        // Release all keys when window loses focus
        window.onblur = () => {
            keyboard.reset();
        };

        // If we're logged in and not connected to anything, periodically check
        // whether the current session is still valid. If the session has expired,
        // refresh the auth state to reshow the login screen (rather than wait for
        // the user to take some action and discover that they are not logged in
        // after all). There is no need to do this if a connection is active as
        // that connection activity will already automatically check session
        // validity.
        window.setInterval(() => {
            if (!!this.authenticationService.getCurrentToken() && !this.hasActiveTunnel()) {
                this.authenticationService.getValidity().subscribe((valid) => {
                    if (!valid)
                        this.reAuthenticate();
                });
            }
        }, SESSION_VALIDITY_RECHECK_INTERVAL);

        // Release all keys upon form submission (there may not be corresponding
        // keyup events for key presses involved in submitting a form)
        this.document.addEventListener('submit', () => {
            keyboard.reset();
        });

        // Attempt to read the clipboard if it may have changed
        window.addEventListener('load', this.clipboardService.resyncClipboard, true);
        window.addEventListener('copy', this.clipboardService.resyncClipboard);
        window.addEventListener('cut', this.clipboardService.resyncClipboard);
        window.addEventListener('focus', (e: FocusEvent) => {

            // Only recheck clipboard if it's the window itself that gained focus
            if (e.target === window)
                this.clipboardService.resyncClipboard();

        }, true);


        // Display login screen if a whole new set of credentials is needed
        this.guacEventService
            .on('guacInvalidCredentials')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(({ error }) => {

                this.setApplicationState(ApplicationState.AWAITING_CREDENTIALS);

                this.loginHelpText = null;
                this.acceptedCredentials = new FormGroup<any>({});
                this.expectedCredentials = error.expected;

            });

        // Prompt for remaining credentials if provided credentials were not enough
        this.guacEventService
            .on('guacInsufficientCredentials')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(async ({ parameters, error }) => {

                // The parameters of the previous attempt are provided as a promise
                const submitted: Record<string, string> = await parameters;

                this.setApplicationState(ApplicationState.AWAITING_CREDENTIALS);

                this.loginHelpText = error.translatableMessage;
                this.acceptedCredentials = new FormGroup(_.mapValues(submitted, value => new FormControl(value)));
                this.expectedCredentials = error.expected;

            });

        // Alert user to authentication errors that occur in the absence of an
        // interactive login form
        this.guacEventService.on('guacLoginFailed')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(({ error }) => {

                // All errors related to an interactive login form are handled elsewhere
                if (this.applicationState === ApplicationState.AWAITING_CREDENTIALS
                    || error.type === Error.Type.INSUFFICIENT_CREDENTIALS
                    || error.type === Error.Type.INVALID_CREDENTIALS)
                    return;

                this.setApplicationState(ApplicationState.AUTOMATIC_LOGIN_REJECTED);
                this.reAuthenticating = false;
                this.fatalError = error;

            });

        // Replace absolutely all content with an error message if the page itself
        // cannot be displayed due to an error
        this.guacEventService.on('guacFatalPageError')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(({ error }) => {
                this.setApplicationState(ApplicationState.FATAL_ERROR);
                this.fatalError = error;
            });

        // Replace the overall user interface with an informational message if the
        // user has manually logged out
        this.guacEventService.on('guacLogout')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(() => {
                this.applicationState = ApplicationState.LOGGED_OUT;
                this.reAuthenticating = false;
            });

        // Set the initial title manually when the application starts
        this.translocoService.selectTranslate('APP.NAME')
            .pipe(take(1))
            .subscribe((title) => this.title.setTitle(title));

        // Ensure new pages always start with clear keyboard state
        this.router.events
            .pipe(filter(event => event instanceof NavigationStart))
            .subscribe(() => keyboard.reset());


        // Update CSS class upon navigation
        this.router.events
            .pipe(filter(event => event instanceof NavigationEnd))
            .subscribe(() => {

                // Clear login screen if route change was successful (and thus
                // login was either successful or not required)
                this.applicationState = ApplicationState.READY;

                // Set body CSS class
                const route = this.router.routerState.snapshot.root.firstChild;
                this.setBodyClass(route?.data['bodyClassName'] || null);

            });

    }


    /**
     * Returns whether the current user has at least one active connection
     * running within the current tab.
     *
     * @returns
     *     true if the current user has at least one active connection running
     *     in the current browser tab, false otherwise.
     */
    private hasActiveTunnel(): boolean {

        const clients = this.guacClientManager.getManagedClients();
        for (const id in clients) {

            switch (clients[id].clientState.connectionState) {
                case ManagedClientState.ConnectionState.CONNECTING:
                case ManagedClientState.ConnectionState.WAITING:
                case ManagedClientState.ConnectionState.CONNECTED:
                    return true;
            }

        }

        return false;

    }

    /**
     * Sets the current overall state of the client side of the
     * application to the given value. Possible values are defined by
     * {@link ApplicationState}. The title and class associated with the
     * current page are automatically reset to the standard values applicable
     * to the application as a whole (rather than any specific page).
     *
     * @param state
     *     The state to assign, as defined by {@link ApplicationState}.
     */
    private setApplicationState(state: ApplicationState): void {
        this.applicationState = state;
        this.translocoService.selectTranslate('APP.NAME')
            .pipe(take(1))
            .subscribe((title) => this.title.setTitle(title));
        this.setBodyClass(null);
    }

    /**
     * Navigates the user back to the root of the application (or reloads the
     * current route and controller if the user is already there), effectively
     * forcing reauthentication. If the user is not logged in, this will result
     * in the login screen appearing.
     */
    reAuthenticate(): void {

        this.reAuthenticating = true;

        // Clear out URL state to conveniently bring user back to home screen
        // upon relogin
        this.router.navigate(['/'], { onSameUrlNavigation: 'reload' });
    }

    /**
     * Replaces the CSS class of the body element which is associated with the
     * current page.
     *
     * @param className
     *     The class to add to the body element, or null if the body element
     *     should have no such class.
     */
    private setBodyClass(className: string | null): void {

        // Remove previous class
        if (this.bodyClassName)
            this.renderer.removeClass(this.document.body, this.bodyClassName);

        // Add the new class
        if (className)
            this.renderer.addClass(this.document.body, className);

        this.bodyClassName = className;

    }

    /**
     * @borrows GuacClientManagerService.getManagedClientGroups
     */
    getManagedClientGroups(): ManagedClientGroup[] {
        return this.guacClientManager.getManagedClientGroups();
    }

    /**
     * Apply HTML patches each time the DOM could be updated.
     */
    ngAfterViewChecked(): void {
        this.applyPatchesService.applyPatches();
    }

}


