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

import { HttpClient } from '@angular/common/http';
import { Component, Input, NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormGroup, FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { provideRouter, Router, RouterLink, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { Observable, of, throwError } from 'rxjs';
import type { Mock } from 'vitest';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import { GuacPageListComponent } from '../../../navigation/components/guac-page-list/guac-page-list.component';
import { GuacNotificationService } from '../../../notification/services/guac-notification.service';
import { MembershipService } from '../../../rest/service/membership.service';
import { PermissionService } from '../../../rest/service/permission.service';
import { SchemaService } from '../../../rest/service/schema.service';
import { UserGroupService } from '../../../rest/service/user-group.service';
import { UserService } from '../../../rest/service/user.service';
import { Error } from '../../../rest/types/Error';
import { Field } from '../../../rest/types/Field';
import { Form } from '../../../rest/types/Form';
import { PermissionSet } from '../../../rest/types/PermissionSet';
import { User } from '../../../rest/types/User';
import { DataSourceTabsComponent } from '../data-source-tabs/data-source-tabs.component';
import { ManagementButtonsComponent } from '../management-buttons/management-buttons.component';
import { ManageUserComponent } from './manage-user.component';

/**
 * Stand-in for the guac-form component which exposes the inputs the template
 * of ManageUserComponent binds.
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
     * The form definitions passed by the template.
     */
    @Input() content?: Form[];

    /**
     * The form group passed by the template.
     */
    @Input() modelFormGroup?: FormGroup;

    /**
     * Whether only model attributes are passed by the template.
     */
    @Input() modelOnly = false;
}

/**
 * Attribute definitions with a checkbox and a text field, for the specs
 * which exercise the attributes of a user.
 */
const ATTRIBUTES: Form[] = [new Form({
    name: 'user-attributes',
    fields: [
        new Field({ name: 'flag', type: Field.Type.BOOLEAN, options: ['true'] }),
        new Field({ name: 'note', type: Field.Type.TEXT })
    ]
})];

/**
 * The user service of the editor; only "postgresql" contains a user, which
 * the REST API returns without a password.
 */
let userService: {
    getUser: (dataSource: string, username: string) => Observable<User>,
    createUser: Mock<(dataSource: string, user: User) => Observable<User>>,
    saveUser: Mock<(dataSource: string, user: User) => Observable<User>>
};

/**
 * Receives each error which the editor displays after a failed save
 * operation.
 */
let showRequestError: Mock<(error: unknown) => void>;

/**
 * Opens the user editor at the given URL as the administrator of the data
 * sources "postgresql" and "postgresql-shared". The user "t92user" exists
 * only within "postgresql".
 *
 * @param url
 *     The URL of the editor, as the router would receive it.
 *
 * @param attributes
 *     The attribute definitions which the data source offers for users.
 *
 * @param storedAttributes
 *     The attributes stored for the existing user. A value of null stands for
 *     an attribute which is defined but not set, as JDBC returns it.
 *
 * @returns
 *     The harness of the router, showing the editor once all requested data
 *     has been loaded.
 */
const openEditor = async (url: string, attributes: Form[] = [],
                          storedAttributes: Record<string, string | null> = {}): Promise<RouterTestingHarness> => {
    userService = {
        getUser: (dataSource: string, username: string) => dataSource === 'postgresql'
            ? of({ username, disabled: false, attributes: { ...storedAttributes } } as User)
            : throwError(() => new Error({ type: Error.Type.NOT_FOUND })),
        createUser: vi.fn((dataSource: string, user: User) => of(user)),
        saveUser: vi.fn((dataSource: string, user: User) => of(user))
    };
    showRequestError = vi.fn();
    TestBed.configureTestingModule({
        declarations: [ManageUserComponent, DataSourceTabsComponent, GuacPageListComponent, ManagementButtonsComponent,
            GuacFormStubComponent],
        // Ignores the user menu and the editors below the password section, which this spec does not exercise
        schemas: [NO_ERRORS_SCHEMA],
        imports: [
            FormsModule,
            RouterLink,
            TranslocoTestingModule.forRoot({
                langs: { en: {} },
                translocoConfig: { availableLangs: ['en'], defaultLang: 'en' }
            })
        ],
        providers: [
            provideRouter([
                { path: 'manage/:dataSource/users/:id', component: ManageUserComponent },
                { path: 'manage/:dataSource/users', component: ManageUserComponent },
                { path: 'settings/users', children: [] }
            ], withComponentInputBinding()),
            { provide: HttpClient, useValue: {} },
            {
                provide: AuthenticationService,
                useValue: {
                    getAvailableDataSources: () => ['postgresql', 'postgresql-shared'],
                    getCurrentUsername: () => 'guacadmin'
                }
            },
            { provide: UserService, useValue: userService },
            {
                provide: PermissionService,
                useValue: {
                    getEffectivePermissions: () => of(new PermissionSet({
                        systemPermissions: [PermissionSet.SystemPermissionType.ADMINISTER]
                    })),
                    getPermissions: () => of(new PermissionSet()),
                    patchPermissions: () => of(void 0)
                }
            },
            { provide: MembershipService, useValue: { getUserGroups: () => of([]), patchUserGroups: () => of(void 0) } },
            { provide: UserGroupService, useValue: { getUserGroups: () => of({}) } },
            { provide: SchemaService, useValue: { getUserAttributes: () => of(attributes) } },
            {
                provide: GuacNotificationService,
                useValue: { showStatus: vi.fn(), SHOW_REQUEST_ERROR: (error: unknown) => showRequestError(error) }
            }
        ]
    });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(url, ManageUserComponent);
    await harness.fixture.whenStable();
    harness.detectChanges();
    return harness;
};

/**
 * Returns the data source tabs of the editor.
 *
 * @param harness
 *     The harness showing the editor.
 *
 * @returns
 *     The links of all data source tabs.
 */
const getTabs = (harness: RouterTestingHarness): HTMLAnchorElement[] =>
    Array.from(harness.routeNativeElement!.querySelectorAll('.page-tabs a'));

/**
 * Types the given values into the username, password and password
 * confirmation fields of the editor, as the user would. Fields without a
 * value remain untouched.
 *
 * @param harness
 *     The harness showing the editor.
 *
 * @param values
 *     The values to type into each field.
 */
const enter = (harness: RouterTestingHarness, values: { username?: string, password?: string, passwordMatch?: string }): void => {
    const element = harness.routeNativeElement!;
    const [password, passwordMatch] = Array.from(element.querySelectorAll<HTMLInputElement>('.properties input[type="password"]'));
    const fields: [HTMLInputElement, string | undefined][] = [
        [element.querySelector<HTMLInputElement>('.properties input[type="text"]')!, values.username],
        [password, values.password],
        [passwordMatch, values.passwordMatch]
    ];
    for (const [input, value] of fields) {
        if (value !== undefined) {
            input.value = value;
            input.dispatchEvent(new Event('input'));
        }
    }
    harness.detectChanges();
};

/**
 * Clicks the save button of the editor and waits until saving has finished.
 *
 * @param harness
 *     The harness showing the editor.
 */
const save = async (harness: RouterTestingHarness): Promise<void> => {
    harness.routeNativeElement!.querySelector<HTMLButtonElement>('management-buttons button.save')!.click();
    await harness.fixture.whenStable();
};

/**
 * Returns the given user as the JSON which HttpClient sends to the REST API.
 *
 * @param user
 *     The user passed to the user service.
 *
 * @returns
 *     The user as parsed from its JSON.
 */
const asJson = (user: User): object => JSON.parse(JSON.stringify(user));

/**
 * Returns the form which the editor shows for the attributes of the user.
 *
 * @param harness
 *     The harness showing the editor.
 *
 * @returns
 *     The attribute form.
 */
const getAttributeForm = (harness: RouterTestingHarness): GuacFormStubComponent =>
    harness.routeDebugElement!.query(By.directive(GuacFormStubComponent)).componentInstance;

describe('ManageUserComponent', () => {

    it('should link each data source tab to the same user within that data source', async () => {
        const tabs = getTabs(await openEditor('/manage/postgresql/users/t92user'));

        expect(tabs.map(tab => tab.getAttribute('href'))).toEqual([
            '/manage/postgresql/users/t92user',
            '/manage/postgresql-shared/users/t92user'
        ]);
    });

    it('should mark the tab of the current data source while creating a new user', async () => {
        const tabs = getTabs(await openEditor('/manage/postgresql/users'));

        expect(tabs.map(tab => tab.classList.contains('current'))).toEqual([true, false]);
    });

    it('should create a new user without a password if no password is entered', async () => {
        const harness = await openEditor('/manage/postgresql/users');
        enter(harness, { username: 'alice' });
        await save(harness);

        expect(showRequestError).not.toHaveBeenCalled();
        expect(userService.createUser).toHaveBeenCalledOnce();
        expect(asJson(userService.createUser.mock.calls[0][1])).toEqual({ username: 'alice', attributes: {} });
        expect(TestBed.inject(Router).url).toBe('/settings/users');
    });

    it('should create a new user with the entered password if its confirmation matches', async () => {
        const harness = await openEditor('/manage/postgresql/users');
        enter(harness, { username: 'alice', password: 'secret', passwordMatch: 'secret' });
        await save(harness);

        expect(showRequestError).not.toHaveBeenCalled();
        expect(asJson(userService.createUser.mock.calls[0][1]))
            .toEqual({ username: 'alice', password: 'secret', attributes: {} });
    });

    it('should not create a new user if the password confirmation differs', async () => {
        const harness = await openEditor('/manage/postgresql/users');
        enter(harness, { username: 'alice', password: 'secret', passwordMatch: 'other' });
        await save(harness);

        expect(showRequestError).toHaveBeenCalledWith(expect.objectContaining({
            translatableMessage: { key: 'MANAGE_USER.ERROR_PASSWORD_MISMATCH' }
        }));
        expect(userService.createUser).not.toHaveBeenCalled();
    });

    it('should save an existing user without a password if no password is entered', async () => {
        const harness = await openEditor('/manage/postgresql/users/t92user');
        await save(harness);

        expect(showRequestError).not.toHaveBeenCalled();
        expect(asJson(userService.saveUser.mock.calls[0][1]))
            .toEqual({ username: 't92user', disabled: false, attributes: {} });
    });

    describe('attributes', () => {

        it('should provide a control for each attribute while creating a new user', async () => {
            const form = getAttributeForm(await openEditor('/manage/postgresql/users', ATTRIBUTES));

            expect(form.modelOnly).toBe(false);
            expect(Object.keys(form.modelFormGroup?.controls ?? {})).toEqual(['flag', 'note']);
        });

        it('should provide controls only for the attributes of an existing user, even those without a value', async () => {
            const form = getAttributeForm(await openEditor('/manage/postgresql/users/t92user', ATTRIBUTES, { flag: null }));

            expect(form.modelOnly).toBe(true);
            expect(Object.keys(form.modelFormGroup?.controls ?? {})).toEqual(['flag']);
        });

        it('should save only the attributes of an existing user', async () => {
            const harness = await openEditor('/manage/postgresql/users/t92user', ATTRIBUTES, { flag: null });
            getAttributeForm(harness).modelFormGroup?.get('flag')?.setValue('true');
            await save(harness);

            expect(asJson(userService.saveUser.mock.calls[0][1]))
                .toEqual({ username: 't92user', disabled: false, attributes: { flag: 'true' } });
        });

    });

});
