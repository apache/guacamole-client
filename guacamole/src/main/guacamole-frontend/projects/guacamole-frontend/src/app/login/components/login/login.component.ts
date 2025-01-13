

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

import { Component, DestroyRef, Input, OnChanges, OnInit, SimpleChanges, ViewEncapsulation } from '@angular/core';
import { GuacFrontendEventArguments } from '../../../events/types/GuacFrontendEventArguments';
import { TranslatableMessage } from '../../../rest/types/TranslatableMessage';
import { Field } from '../../../rest/types/Field';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import { HttpClient } from '@angular/common/http';
import { RequestService } from '../../../rest/service/request.service';
import { catchError, filter } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup } from '@angular/forms';
import { GuacEventService } from 'guacamole-frontend-lib';
import { Error } from '../../../rest/types/Error';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';

@Component({
    selector     : 'guac-login',
    templateUrl  : './login.component.html',
    encapsulation: ViewEncapsulation.None
})
export class LoginComponent implements OnInit, OnChanges {
    /**
     * An optional instructional message to display within the login
     * dialog.
     */
    @Input() helpText?: TranslatableMessage;

    /**
     * The login form or set of fields. This will be displayed to the user
     * to capture their credentials.
     */
    @Input({ required: true }) form: Field[] | null = null

    /**
     * A form group of all field name/value pairs that have already been provided.
     * If not undefined, the user will be prompted to continue their login
     * attempt using only the fields which remain.
     */
    @Input() values?: FormGroup = new FormGroup({});

    /**
     * The initial value for all login fields. Note that this value must
     * not be null. If null, empty fields may not be submitted back to the
     * server at all, causing the request to misrepresent true login state.
     *
     * For example, if a user receives an insufficient credentials error
     * due to their password expiring, failing to provide that new password
     * should result in the user submitting their username, original
     * password, and empty new password. If only the username and original
     * password are sent, the invalid password reset request will be
     * indistinguishable from a normal login attempt.
     */
    readonly DEFAULT_FIELD_VALUE: string = '';

    /**
     * A description of the error that occurred during login, if any.
     */
    loginError: TranslatableMessage | null = null;

    /**
     * All form values entered by the user, as parameter form group.
     */
    enteredValues: FormGroup = new FormGroup({});

    /**
     * All form fields which have not yet been filled by the user.
     */
    remainingFields: Field[] = [];

    /**
     * Whether an authentication attempt has been submitted. This will be
     * set to true once credentials have been submitted and will only be
     * reset to false once the attempt has been fully processed, including
     * rerouting the user to the requested page if the attempt succeeded.
     */
    submitted = false;

    /**
     * The field that is most relevant to the user.
     */
    relevantField: Field | null = null;

    /**
     * Inject required services.
     */
    constructor(private authenticationService: AuthenticationService,
                private requestService: RequestService,
                private guacEventService: GuacEventService<GuacFrontendEventArguments>,
                private router: Router,
                private route: ActivatedRoute,
                private destroyRef: DestroyRef) {
    }

    /**
     * Returns whether a previous login attempt is continuing.
     *
     * @return
     *     true if a previous login attempt is continuing, false otherwise.
     */
    isContinuation(): boolean {

        // The login is continuing if any parameter values are provided
        for (const name in this.values?.controls)
            return true;

        return false;

    }

    ngOnChanges(changes: SimpleChanges): void {

        // Ensure provided values are included within entered values, even if
        // they have no corresponding input fields
        if (changes['values']) {
            const values = changes['values'].currentValue as FormGroup;
            this.enteredValues = new FormGroup({ ...this.enteredValues.controls, ...values.controls });
            // angular.extend($scope.enteredValues, values || {});
        }

        // Update field information when form is changed
        if (changes['form']) {
            const fields = changes['form'].currentValue as Field[];

            // If no fields are provided, then no fields remain
            if (!fields) {
                this.remainingFields = [];
                return;
            }

            // Filter provided fields against provided values
            this.remainingFields = fields.filter((field) => {
                return this.values && !(field.name in this.values);
            });

            // Set default values for all unset fields
            this.remainingFields.forEach((field) => {
                if (!this.enteredValues.get(field.name))
                    this.enteredValues.addControl(field.name, new FormControl(this.DEFAULT_FIELD_VALUE));
            });

            this.relevantField = this.getRelevantField();
        }


    }

    /**
     * Submits the currently-specified fields to the authentication service,
     * as well as any URL parameters set for the current page, preferring
     * the values from the fields, and redirecting to the main view if
     * successful.
     */
    login(): void {

        // Any values from URL paramters
        const urlValues = this.route.snapshot.queryParams;

        // Values from the fields
        const fieldValues = this.enteredValues.value;

        // All the values to be submitted in the auth attempt, preferring
        // any values from fields over those in the URL
        const authParams = {...urlValues, ...fieldValues};

        this.authenticationService.authenticate(authParams)
            .pipe(
                takeUntilDestroyed(this.destroyRef),
                catchError(this.requestService.IGNORE)
            )
            .subscribe()
    }

    /**
     * Returns the field most relevant to the user given the current state
     * of the login process. This will normally be the first empty field.
     *
     * @return
     *     The field most relevant, null if there is no single most relevant
     *     field.
     */
    private getRelevantField(): Field | null {

        for (let i = 0; i < this.remainingFields.length; i++) {
            const field = this.remainingFields[i];
            if (!this.enteredValues.get(field.name))
                return field;
        }

        return null;

    }

    ngOnInit(): void {
        // Update UI to reflect in-progress auth status (clear any previous
        // errors, flag as pending)
        this.guacEventService.on('guacLoginPending')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(() => {
                this.submitted = true;
                this.loginError = null;
            });

        // Retry route upon success (entered values will be cleared only
        // after route change has succeeded as this can take time)
        this.guacEventService.on('guacLogin')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(() => {
                this.router.navigate([this.router.url], { onSameUrlNavigation: 'reload' });
            });

        // Reset upon failure
        this.guacEventService.on('guacLoginFailed')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(({ error }) => {

                // Initial submission is complete and has failed
                this.submitted = false;

                // Clear out passwords if the credentials were rejected for any reason
                if (error.type !== Error.Type.INSUFFICIENT_CREDENTIALS) {

                    // Flag generic error for invalid login
                    if (error.type === Error.Type.INVALID_CREDENTIALS)
                        this.loginError = {
                            key: 'LOGIN.ERROR_INVALID_LOGIN'
                        };

                    // Display error if anything else goes wrong
                    else
                        this.loginError = error.translatableMessage || null;

                    // Reset all remaining fields to default values, but
                    // preserve any usernames
                    this.remainingFields.forEach((field) => {
                        if (field.type !== Field.Type.USERNAME && this.enteredValues.get(field.name) !== null)
                            this.enteredValues.get(field.name)!.setValue(this.DEFAULT_FIELD_VALUE);
                    });
                }

            });

        // Reset state after authentication and routing have succeeded
        this.router.events.pipe(
            takeUntilDestroyed(this.destroyRef),
            filter(event => event instanceof NavigationEnd)
        ).subscribe(() => {
            this.enteredValues = new FormGroup({});
            this.submitted = false;
        });

    }

}
