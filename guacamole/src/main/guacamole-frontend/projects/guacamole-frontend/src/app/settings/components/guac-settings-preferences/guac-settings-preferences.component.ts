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

import { Component, DestroyRef, OnInit, ViewEncapsulation } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormGroup } from '@angular/forms';
import { TranslocoService } from '@ngneat/transloco';
import { EMPTY, forkJoin } from 'rxjs';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import { FormService } from '../../../form/service/form.service';
import { GuacNotificationService } from '../../../notification/services/guac-notification.service';
import { NotificationAction } from '../../../notification/types/NotificationAction';
import { PermissionService } from '../../../rest/service/permission.service';
import { RequestService } from '../../../rest/service/request.service';
import { SchemaService } from '../../../rest/service/schema.service';
import { UserService } from '../../../rest/service/user.service';
import { Field } from '../../../rest/types/Field';
import { Form } from '../../../rest/types/Form';
import { PermissionSet } from '../../../rest/types/PermissionSet';
import { User } from '../../../rest/types/User';
import { PreferenceService } from '../../services/preference.service';

/**
 * A component for managing preferences local to the current user.
 */
@Component({
    selector: 'guac-settings-preferences',
    templateUrl: './guac-settings-preferences.component.html',
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class GuacSettingsPreferencesComponent implements OnInit {

    /**
     * The user being modified.
     */
    user: User | null = null;

    /**
     * The username of the current user.
     */
    private username: string | null = this.authenticationService.getCurrentUsername();

    /**
     * The identifier of the data source which authenticated the
     * current user.
     */
    private dataSource: string | null = this.authenticationService.getDataSource();

    /**
     * All available user attributes. This is only the set of attribute
     * definitions, organized as logical groupings of attributes, not attribute
     * values.
     */
    attributes: Form[] | null = null;

    /**
     * The fields which should be displayed for choosing locale
     * preferences. Each field name must be a property on
     * $scope.preferences.
     */
    localeFields: Field[] = [
        { type: Field.Type.LANGUAGE, name: 'language' },
        { type: Field.Type.TIMEZONE, name: 'timezone' }
    ];

    /**
     * The new password for the user.
     */
    newPassword: string | null = null;

    /**
     * The password match for the user. The update password action will
     * fail if $scope.newPassword !== $scope.passwordMatch.
     */
    newPasswordMatch: string | null = null;

    /**
     * The old password of the user.
     */
    oldPassword: string | null = null;

    /**
     * Whether the current user can edit themselves - i.e. update their
     * password or change user preference attributes, or null if this
     * is not yet known.
     */
    canUpdateSelf: boolean | null = null;

    /**
     * Form group for editing locale settings.
     */
    preferencesFormGroup: FormGroup = new FormGroup({});

    /**
     * Form group for editing user attributes.
     */
    userAttributesFormGroup: FormGroup = new FormGroup({});

    /**
     * An action to be provided along with the object sent to
     * showStatus which closes the currently-shown status dialog.
     */
    private readonly ACKNOWLEDGE_ACTION: NotificationAction = {
        name: 'SETTINGS_PREFERENCES.ACTION_ACKNOWLEDGE',
        // Handle action
        callback: () => {
            this.guacNotification.showStatus(false);
        }
    };

    /**
     * An action which closes the current dialog, and refreshes
     * the user data on dialog close.
     */
    private readonly ACKNOWLEDGE_ACTION_RELOAD = {
        name: 'SETTINGS_PREFERENCES.ACTION_ACKNOWLEDGE',
        // Handle action
        callback: () => {
            this.userService.getUser(this.dataSource!, this.username!)
                .subscribe(user => {
                    this.user = user;
                    this.guacNotification.showStatus(false);
                });
        }
    };

    /**
     * Inject required services.
     */
    constructor(private authenticationService: AuthenticationService,
                private guacNotification: GuacNotificationService,
                private permissionService: PermissionService,
                protected preferenceService: PreferenceService,
                private requestService: RequestService,
                private schemaService: SchemaService,
                private userService: UserService,
                private formService: FormService,
                private translocoService: TranslocoService,
                private destroyRef: DestroyRef) {
    }

    ngOnInit(): void {

        // Retrieve current permissions
        this.permissionService.getEffectivePermissions(this.dataSource!, this.username!)
            .subscribe({
                next : permissions => {

                    // Add action for updating password or user preferences if permission is granted
                    this.canUpdateSelf = (

                        // If permission is explicitly granted
                        PermissionSet.hasUserPermission(permissions,
                            PermissionSet.ObjectPermissionType.UPDATE, this.username!)

                        // Or if implicitly granted through being an administrator
                        || PermissionSet.hasSystemPermission(permissions,
                            PermissionSet.SystemPermissionType.ADMINISTER));

                },
                error: this.requestService.createErrorCallback(error => {
                    this.canUpdateSelf = false;
                    return EMPTY;
                })
            });

        // Fetch the user record
        const user = this.userService.getUser(this.dataSource!, this.username!);

        // Fetch all user preference attribute forms defined
        const attributes = this.schemaService.getUserPreferenceAttributes(this.dataSource!);

        forkJoin([user, attributes])
            .subscribe(([user, attributes]) => {
                // Store the fetched data
                this.attributes = attributes;
                this.user = user;

                // Create form group for editing user attributes
                this.userAttributesFormGroup = this.formService.getFormGroup(attributes);
                this.userAttributesFormGroup.patchValue(user.attributes);
                this.userAttributesFormGroup.valueChanges
                    .pipe(takeUntilDestroyed(this.destroyRef))
                    .subscribe(value => {
                        if (this.user)
                            this.user.attributes = value;
                    });
            });

        // Create form group for editing locale settings
        const localeForm = this.formService.asFormArray(this.localeFields);
        this.preferencesFormGroup = this.formService.getFormGroup(localeForm);
        this.preferencesFormGroup.patchValue(this.preferenceService.preferences);
        this.preferencesFormGroup.valueChanges
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(value => {
                //Automatically update applied translation when language preference is changed
                if (this.preferenceService.preferences.language !== value.language) {
                    this.translocoService.setActiveLang(value.language);
                }

                const newPreferences = { ...this.preferenceService.preferences, ...value };
                this.preferenceService.preferences = newPreferences;
            });

    }

    /**
     * Update the current user's password to the password currently set within
     * the password change dialog.
     */
    updatePassword(): void {

        // Verify passwords match
        if (this.newPasswordMatch !== this.newPassword) {
            this.guacNotification.showStatus({
                className: 'error',
                title    : 'SETTINGS_PREFERENCES.DIALOG_HEADER_ERROR',
                text     : {
                    key: 'SETTINGS_PREFERENCES.ERROR_PASSWORD_MISMATCH'
                },
                actions  : [this.ACKNOWLEDGE_ACTION]
            });
            return;
        }

        // Verify that the new password is not blank
        if (!this.newPassword) {
            this.guacNotification.showStatus({
                className: 'error',
                title    : 'SETTINGS_PREFERENCES.DIALOG_HEADER_ERROR',
                text     : {
                    key: 'SETTINGS_PREFERENCES.ERROR_PASSWORD_BLANK'
                },
                actions  : [this.ACKNOWLEDGE_ACTION]
            });
            return;
        }

        // Save the user with the new password
        this.userService.updateUserPassword(this.dataSource!, this.username!, this.oldPassword!, this.newPassword)
            .subscribe({
                next : () => {

                    // Clear the password fields
                    this.oldPassword = null;
                    this.newPassword = null;
                    this.newPasswordMatch = null;

                    // Indicate that the password has been changed
                    this.guacNotification.showStatus({
                        text   : {
                            key: 'SETTINGS_PREFERENCES.INFO_PASSWORD_CHANGED'
                        },
                        actions: [this.ACKNOWLEDGE_ACTION]
                    });
                },
                error: this.guacNotification.SHOW_REQUEST_ERROR
            });

    }

    /**
     * Returns whether critical data has completed being loaded.
     *
     * @returns
     *     true if enough data has been loaded for the user interface to be
     *     useful, false otherwise.
     */
    isLoaded(): boolean {

        return this.canUpdateSelf !== null;

    }

    /**
     * Saves the current user, displaying an acknowledgement message if
     * saving was successful, or an error if the save failed.
     */
    saveUser(): void {
        this.userService.saveUser(this.dataSource!, this.user!)
            .subscribe({
                next : () => this.guacNotification.showStatus({
                    text: {
                        key: 'SETTINGS_PREFERENCES.INFO_PREFERENCE_ATTRIBUTES_CHANGED'
                    },

                    // Reload the user on successful save in case any attributes changed
                    actions: [this.ACKNOWLEDGE_ACTION_RELOAD]
                }),
                error: this.guacNotification.SHOW_REQUEST_ERROR
            });
    }


}
