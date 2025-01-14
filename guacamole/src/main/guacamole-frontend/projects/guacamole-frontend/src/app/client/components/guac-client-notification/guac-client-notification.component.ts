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

import { Component, DestroyRef, DoCheck, Input, OnInit, ViewEncapsulation } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';

import { GuacEvent, GuacEventService } from 'guacamole-frontend-lib';
import { finalize } from 'rxjs';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import { GuacFrontendEventArguments } from '../../../events/types/GuacFrontendEventArguments';
import { UserPageService } from '../../../manage/services/user-page.service';
import { Notification } from '../../../notification/types/Notification';
import { NotificationAction } from '../../../notification/types/NotificationAction';
import { NotificationCountdown } from '../../../notification/types/NotificationCountdown';
import { RequestService } from '../../../rest/service/request.service';
import { Form } from '../../../rest/types/Form';
import { Protocol } from '../../../rest/types/Protocol';
import { GuacClientManagerService } from '../../services/guac-client-manager.service';
import { GuacTranslateService } from '../../services/guac-translate.service';
import { ManagedClientService } from '../../services/managed-client.service';
import { ManagedClient } from '../../types/ManagedClient';
import { ManagedClientState } from '../../types/ManagedClientState';


/**
 * All error codes for which automatic reconnection is appropriate when a
 * client error occurs.
 */
const CLIENT_AUTO_RECONNECT = {
    0x0200: true,
    0x0202: true,
    0x0203: true,
    0x0207: true,
    0x0208: true,
    0x0301: true,
    0x0308: true
};

/**
 * All error codes for which automatic reconnection is appropriate when a
 * tunnel error occurs.
 */
const TUNNEL_AUTO_RECONNECT = {
    0x0200: true,
    0x0202: true,
    0x0203: true,
    0x0207: true,
    0x0208: true,
    0x0308: true
};

/**
 * Connection status of specific Guacamole client.
 */
type ClientState = [string, Record<string, string> | null, string | null, Form[]];


/**
 * A Component for displaying a non-global notification describing the status
 * of a specific Guacamole client, including prompts for any information
 * necessary to continue the connection.
 */
@Component({
    selector     : 'guac-client-notification',
    templateUrl  : './guac-client-notification.component.html',
    encapsulation: ViewEncapsulation.None
})
export class GuacClientNotificationComponent implements OnInit, DoCheck {

    /**
     * The client whose status should be displayed.
     */
    @Input({ required: true }) client!: ManagedClient;

    /**
     * A Notification object describing the client status to display as a
     * dialog or prompt, as would be accepted by guacNotification.showStatus(),
     * or false if no status should be shown.
     */
    status: Notification | object | boolean = false;

    /**
     * Action which logs out from Guacamole entirely.
     */
    private readonly LOGOUT_ACTION: NotificationAction = {
        name     : 'CLIENT.ACTION_LOGOUT',
        className: 'logout button',
        callback : () => this.logout()
    };

    /**
     * Action which returns the user to the home screen. If the home page has
     * not yet been determined, this will be null.
     */
    private NAVIGATE_HOME_ACTION: NotificationAction | null = null;

    /**
     * Action which replaces the current client with a newly-connected client.
     */
    private readonly RECONNECT_ACTION: NotificationAction = {
        name     : 'CLIENT.ACTION_RECONNECT',
        className: 'reconnect button',
        callback : () => {
            this.client = this.guacClientManager.replaceManagedClient(this.client.id);
            this.status = false;
        }
    };

    /**
     * The reconnect countdown to display if an error or status warrants an
     * automatic, timed reconnect.
     */
    private readonly RECONNECT_COUNTDOWN: NotificationCountdown = {
        text     : 'CLIENT.TEXT_RECONNECT_COUNTDOWN',
        callback : this.RECONNECT_ACTION.callback,
        remaining: 15
    };

    /**
     * The client state to react to changes.
     */
    private lastClientState: ClientState = ['', null, null, []];

    /**
     * Inject required services.
     */
    constructor(private authenticationService: AuthenticationService,
                private guacClientManager: GuacClientManagerService,
                private guacTranslate: GuacTranslateService,
                private requestService: RequestService,
                private userPageService: UserPageService,
                private managedClientService: ManagedClientService,
                private guacEventService: GuacEventService<GuacFrontendEventArguments>,
                private router: Router,
                private route: ActivatedRoute,
                private destroyRef: DestroyRef) {
    }

    ngOnInit(): void {

        // Assign home page action once user's home page has been determined
        this.userPageService.getHomePage()
            .subscribe({
                next    : homePage => {

                    // Define home action only if different from current location
                    if (this.route.snapshot.root.url.join('/') || '/' === homePage.url) {
                        this.NAVIGATE_HOME_ACTION = {
                            name     : 'CLIENT.ACTION_NAVIGATE_HOME',
                            className: 'home button',
                            callback : () => {
                                this.router.navigate([homePage.url]);
                            }
                        };
                    }

                }, error: this.requestService.WARN
            });

        // Block internal handling of key events (by the client) if a
        // notification is visible
        this.guacEventService.on('guacBeforeKeydown')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(({ event }) => this.preventDefaultDuringNotification(event));
        this.guacEventService.on('guacBeforeKeyup')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(({ event }) => this.preventDefaultDuringNotification(event));
    }

    /**
     * Custom change detection logic for the component.
     */
    ngDoCheck(): void {

        const currentClientState: ClientState = [
            this.client.clientState.connectionState,
            this.client.requiredParameters,
            this.client.protocol,
            this.client.forms
        ];

        // Compare the current state with the last known state
        if (!this.compareClientState(this.lastClientState, currentClientState)) {

            this.lastClientState = [...currentClientState];
            this.clientStateChanged(currentClientState);

        }

    }

    /**
     * Compares two client states to determine if they are identical.
     *
     * @param prevValues - The previous client state.
     * @param currValues - The current client state.
     * @returns true if the states are identical, false otherwise.
     */
    private compareClientState(prevValues: ClientState, currValues: ClientState): boolean {
        if (prevValues.length !== currValues.length) return false;
        return prevValues.every((value, index) => value === currValues[index]);
    }

    /**
     * Show status dialog when connection status changes
     */
    private clientStateChanged(newValues: ClientState): void {
        const connectionState = newValues[0];
        const requiredParameters = newValues[1];

        // Prompt for parameters only if parameters can actually be submitted
        if (requiredParameters && this.canSubmitParameters(connectionState))
            this.notifyParametersRequired(requiredParameters);

        // Otherwise, just show general connection state
        else
            this.notifyConnectionState(connectionState);
    }

    /**
     * Displays a notification at the end of a Guacamole connection, whether
     * that connection is ending normally or due to an error. As the end of
     * a Guacamole connection may be due to changes in authentication status,
     * this will also implicitly perform a re-authentication attempt to check
     * for such changes, possibly resulting in auth-related events like
     * guacInvalidCredentials.
     *
     * @param status
     *     The status notification to show, as would be accepted by
     *     guacNotification.showStatus().
     */
    private notifyConnectionClosed(status: Notification | object | boolean): void {

        // Re-authenticate to verify auth status at end of connection
        this.authenticationService.updateCurrentToken(this.route.snapshot.queryParams)
            .pipe(
                // Show the requested status once the authentication check has finished
                finalize(() => {
                    this.status = status;
                })
            )
            .subscribe({
                error: this.requestService.IGNORE
            });
    }

    /**
     * Notifies the user that the connection state has changed.
     *
     * @param connectionState
     *     The current connection state, as defined by
     *     ManagedClientState.ConnectionState.
     */
    private notifyConnectionState(connectionState: string): void {

        // Hide any existing status
        this.status = false;

        // Do not display status if status not known
        if (!connectionState)
            return;

        // Build array of available actions
        let actions: NotificationAction[];
        if (this.NAVIGATE_HOME_ACTION)
            actions = [this.NAVIGATE_HOME_ACTION, this.RECONNECT_ACTION, this.LOGOUT_ACTION];
        else
            actions = [this.RECONNECT_ACTION, this.LOGOUT_ACTION];

        // Get any associated status code
        const status = this.client.clientState.statusCode;

        // Connecting
        if (connectionState === ManagedClientState.ConnectionState.CONNECTING
            || connectionState === ManagedClientState.ConnectionState.WAITING) {
            this.status = {
                className: 'connecting',
                title    : 'CLIENT.DIALOG_HEADER_CONNECTING',
                text     : {
                    key: 'CLIENT.TEXT_CLIENT_STATUS_' + connectionState.toUpperCase()
                }
            };
        }

        // Client error
        else if (connectionState === ManagedClientState.ConnectionState.CLIENT_ERROR) {

            // Translation IDs for this error code
            const errorPrefix = 'CLIENT.ERROR_CLIENT_';
            const errorId = errorPrefix + status.toString(16).toUpperCase();
            const defaultErrorId = errorPrefix + 'DEFAULT';

            // Determine whether the reconnect countdown applies
            const countdown = (status in CLIENT_AUTO_RECONNECT) ? this.RECONNECT_COUNTDOWN : null;

            // Use the guacTranslate service to determine if there is a translation for
            // this error code; if not, use the default
            this.guacTranslate.translateWithFallback(errorId, defaultErrorId).subscribe(
                // Show error status
                translationResult => this.notifyConnectionClosed({
                    className: 'error',
                    title    : 'CLIENT.DIALOG_HEADER_CONNECTION_ERROR',
                    text     : {
                        key: translationResult.id
                    },
                    countdown: countdown,
                    actions  : actions
                })
            );

        }

        // Tunnel error
        else if (connectionState === ManagedClientState.ConnectionState.TUNNEL_ERROR) {

            // Translation IDs for this error code
            const errorPrefix = 'CLIENT.ERROR_TUNNEL_';
            const errorId = errorPrefix + status.toString(16).toUpperCase();
            const defaultErrorId = errorPrefix + 'DEFAULT';

            // Determine whether the reconnect countdown applies
            const countdown = (status in TUNNEL_AUTO_RECONNECT) ? this.RECONNECT_COUNTDOWN : null;

            // Use the guacTranslate service to determine if there is a translation for
            // this error code; if not, use the default
            this.guacTranslate.translateWithFallback(errorId, defaultErrorId).subscribe(
                // Show error status
                translationResult => this.notifyConnectionClosed({
                    className: 'error',
                    title    : 'CLIENT.DIALOG_HEADER_CONNECTION_ERROR',
                    text     : {
                        key: translationResult.id
                    },
                    countdown: countdown,
                    actions  : actions
                })
            );

        }

        // Disconnected
        else if (connectionState === ManagedClientState.ConnectionState.DISCONNECTED) {
            this.notifyConnectionClosed({
                title  : 'CLIENT.DIALOG_HEADER_DISCONNECTED',
                text   : {
                    key: 'CLIENT.TEXT_CLIENT_STATUS_' + connectionState.toUpperCase()
                },
                actions: actions
            });
        }

        // Hide status for all other states
        else
            this.status = false;

    }

    /**
     * Prompts the user to enter additional connection parameters. If the
     * protocol and associated parameters of the underlying connection are not
     * yet known, this function has no effect and should be re-invoked once
     * the parameters are known.
     *
     * @param requiredParameters
     *     The set of all parameters requested by the server via "required"
     *     instructions, where each object key is the name of a requested
     *     parameter and each value is the current value entered by the user.
     */
    private notifyParametersRequired(requiredParameters: Record<string, string>): void {

        /**
         * Action which submits the current set of parameter values, requesting
         * that the connection continue.
         */
        const SUBMIT_PARAMETERS = {
            name     : 'CLIENT.ACTION_CONTINUE',
            className: 'button',
            callback : () => {
                if (this.client) {
                    const params = this.client.requiredParameters;
                    this.client.requiredParameters = null;
                    this.managedClientService.sendArguments(this.client, params);
                }
            }
        };

        /**
         * Action which cancels submission of additional parameters and
         * disconnects from the current connection.
         */
        const CANCEL_PARAMETER_SUBMISSION = {
            name     : 'CLIENT.ACTION_CANCEL',
            className: 'button',
            callback : () => {
                this.client.requiredParameters = null;
                this.client.client.disconnect();
            }
        };

        // Attempt to prompt for parameters only if the parameters that apply
        // to the underlying connection are known
        if (!this.client.protocol || !this.client.forms)
            return;

        // Prompt for parameters
        this.status = {
            className         : 'parameters-required',
            formNamespace     : Protocol.getNamespace(this.client.protocol),
            forms             : this.client.forms,
            formModel         : requiredParameters,
            formSubmitCallback: SUBMIT_PARAMETERS.callback,
            actions           : [SUBMIT_PARAMETERS, CANCEL_PARAMETER_SUBMISSION]
        };

    }

    /**
     * Returns whether the given connection state allows for submission of
     * connection parameters via "argv" instructions.
     *
     * @param connectionState
     *     The connection state to test, as defined by
     *     ManagedClientState.ConnectionState.
     *
     * @returns
     *     true if the given connection state allows submission of connection
     *     parameters via "argv" instructions, false otherwise.
     */
    private canSubmitParameters(connectionState: string): boolean {
        return (connectionState === ManagedClientState.ConnectionState.WAITING ||
            connectionState === ManagedClientState.ConnectionState.CONNECTED);
    }

    /**
     * Prevents the default behavior of the given AngularJS event if a
     * notification is currently shown and the client is focused.
     *
     * @param e
     *     The GuacEvent to selectively prevent.
     */
    private preventDefaultDuringNotification(e: GuacEvent<GuacFrontendEventArguments>): void {
        if (this.status && this.client.clientProperties.focused)
            e.preventDefault();
    }

    /**
     * Logs out the current user, redirecting them to back to the root
     * after logout completes.
     */
    logout(): void {
        this.authenticationService.logout().subscribe({
            error: this.requestService.IGNORE
        });
    }

}
