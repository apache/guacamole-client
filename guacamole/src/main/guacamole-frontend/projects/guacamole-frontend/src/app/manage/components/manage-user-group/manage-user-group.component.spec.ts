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
import { TestBed } from '@angular/core/testing';
import { FormGroup, FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { provideRouter, RouterLink, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { firstValueFrom, of, throwError } from 'rxjs';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import { GuacPageListComponent } from '../../../navigation/components/guac-page-list/guac-page-list.component';
import { MembershipService } from '../../../rest/service/membership.service';
import { PermissionService } from '../../../rest/service/permission.service';
import { SchemaService } from '../../../rest/service/schema.service';
import { UserGroupService } from '../../../rest/service/user-group.service';
import { UserService } from '../../../rest/service/user.service';
import { Error } from '../../../rest/types/Error';
import { Field } from '../../../rest/types/Field';
import { Form } from '../../../rest/types/Form';
import { PermissionSet } from '../../../rest/types/PermissionSet';
import { UserGroup } from '../../../rest/types/UserGroup';
import { DataSourceTabsComponent } from '../data-source-tabs/data-source-tabs.component';
import { ManageUserGroupComponent } from './manage-user-group.component';

/**
 * Stand-in for the guac-form component which exposes the inputs the template
 * of ManageUserGroupComponent binds.
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
 * The attribute definitions which the data source offers for user groups in
 * this spec: a checkbox and a text field.
 */
const ATTRIBUTES: Form[] = [new Form({
    name: 'group-attributes',
    fields: [
        new Field({ name: 'flag', type: Field.Type.BOOLEAN, options: ['true'] }),
        new Field({ name: 'note', type: Field.Type.TEXT })
    ]
})];

/**
 * The attributes stored for every existing user group in this spec.
 */
const STORED_ATTRIBUTES: Record<string, string> = { flag: 'true', note: 'hello' };

/**
 * The user group editor opened by {@link openEditor}, with the service of
 * which the editor saves the user group.
 */
interface OpenedEditor {

    /**
     * The element of the editor.
     */
    element: HTMLElement;

    /**
     * The editor component.
     */
    component: ManageUserGroupComponent;

    /**
     * The form which the editor shows for the attributes of the user group.
     */
    attributeForm: GuacFormStubComponent;

    /**
     * The service which saves the user group.
     */
    userGroupService: {
        saveUserGroup: ReturnType<typeof vi.fn>;
        createUserGroup: ReturnType<typeof vi.fn>;
    };
}

/**
 * Effective permissions of an administrator.
 */
const ADMINISTRATOR = new PermissionSet({
    systemPermissions: [PermissionSet.SystemPermissionType.ADMINISTER]
});

/**
 * Opens the user group editor at the given URL within the data sources
 * "postgresql" and "postgresql-shared". The user group "t92group" exists only
 * within "postgresql".
 *
 * @param url
 *     The URL of the editor, as the router would receive it.
 *
 * @param permissions
 *     The effective permissions of the current user within each data source.
 *
 * @returns
 *     The editor once all requested data has been loaded.
 */
const openEditor = async (url: string, permissions: PermissionSet): Promise<OpenedEditor> => {
    const userGroupService = {
        getUserGroup: (dataSource: string, identifier: string) => dataSource === 'postgresql'
            ? of(new UserGroup({ identifier, attributes: { ...STORED_ATTRIBUTES } }))
            : throwError(() => new Error({ type: Error.Type.NOT_FOUND })),
        getUserGroups: () => of({}),
        saveUserGroup: vi.fn(() => of(undefined)),
        createUserGroup: vi.fn(() => of(undefined))
    };
    const patched = { patchPermissions: vi.fn(() => of(undefined)) };
    const patchedMembership = {
        patchUserGroups: vi.fn(() => of(undefined)),
        patchMemberUserGroups: vi.fn(() => of(undefined)),
        patchMemberUsers: vi.fn(() => of(undefined))
    };
    TestBed.configureTestingModule({
        declarations: [ManageUserGroupComponent, DataSourceTabsComponent, GuacPageListComponent, GuacFormStubComponent],
        // Ignores the user menu and the editors below the tabs, which this spec does not exercise
        schemas: [NO_ERRORS_SCHEMA],
        imports: [
            FormsModule,
            RouterLink,
            // Without translations, the notice shows its translation key
            TranslocoTestingModule.forRoot({
                langs: { en: {} },
                translocoConfig: { availableLangs: ['en'], defaultLang: 'en' }
            })
        ],
        providers: [
            provideRouter([
                { path: 'manage/:dataSource/userGroups/:id', component: ManageUserGroupComponent },
                { path: 'manage/:dataSource/userGroups', component: ManageUserGroupComponent }
            ], withComponentInputBinding()),
            {
                provide: AuthenticationService,
                useValue: {
                    getAvailableDataSources: () => ['postgresql', 'postgresql-shared'],
                    getCurrentUsername: () => 'guacadmin'
                }
            },
            { provide: UserGroupService, useValue: userGroupService },
            {
                provide: PermissionService,
                useValue: {
                    getEffectivePermissions: () => of(permissions),
                    getPermissions: () => of(new PermissionSet()),
                    ...patched
                }
            },
            {
                provide: MembershipService,
                useValue: {
                    getUserGroups: () => of([]),
                    getMemberUserGroups: () => of([]),
                    getMemberUsers: () => of([]),
                    ...patchedMembership
                }
            },
            { provide: UserService, useValue: { getUsers: () => of({}) } },
            { provide: SchemaService, useValue: { getUserGroupAttributes: () => of(ATTRIBUTES) } }
        ]
    });
    const harness = await RouterTestingHarness.create();
    const component = await harness.navigateByUrl(url, ManageUserGroupComponent);
    await harness.fixture.whenStable();
    harness.detectChanges();
    return {
        element: harness.routeNativeElement!,
        component,
        attributeForm: harness.routeDebugElement!.query(By.directive(GuacFormStubComponent)).componentInstance,
        userGroupService
    };
};

/**
 * Returns the data source tabs which the given editor displays.
 *
 * @param editor
 *     The element of the user group editor.
 *
 * @returns
 *     The links of all data source tabs.
 */
const getTabs = (editor: HTMLElement): HTMLAnchorElement[] =>
    Array.from(editor.querySelectorAll('.page-tabs a'));

/**
 * Returns the section which warns that the user group is read-only.
 *
 * @param editor
 *     The element of the user group editor.
 *
 * @returns
 *     The section containing the read-only notice, or null if there is none.
 */
const getReadOnlySection = (editor: HTMLElement): HTMLElement | null =>
    editor.querySelector('.notice.read-only')?.closest('.section') ?? null;

describe('ManageUserGroupComponent', () => {

    it('should link each data source tab to the same user group within that data source', async () => {
        const { element: editor } = await openEditor('/manage/postgresql/userGroups/t92group', ADMINISTRATOR);

        expect(getTabs(editor).map(tab => tab.getAttribute('href'))).toEqual([
            '/manage/postgresql/userGroups/t92group',
            '/manage/postgresql-shared/userGroups/t92group'
        ]);
    });

    it('should mark the tab of the current data source while creating a new user group', async () => {
        const { element: editor } = await openEditor('/manage/postgresql/userGroups', ADMINISTRATOR);

        expect(getTabs(editor).map(tab => tab.classList.contains('current'))).toEqual([true, false]);
    });

    it('should warn that the user group is read-only if the current user cannot save it', async () => {
        const { element: editor } = await openEditor('/manage/postgresql/userGroups/t92group', new PermissionSet({
            userGroupPermissions: { t92group: [PermissionSet.ObjectPermissionType.READ] }
        }));
        const section = getReadOnlySection(editor);

        expect(section?.textContent).toContain('MANAGE_USER_GROUP.INFO_READ_ONLY');
        expect(section?.hidden).toBe(false);
    });

    it('should not warn that the user group is read-only if the current user can save it', async () => {
        const { element: editor } = await openEditor('/manage/postgresql/userGroups/t92group', ADMINISTRATOR);

        expect(getReadOnlySection(editor)?.hidden).toBe(true);
    });

    describe('attributes', () => {

        it('should bind the attribute form to a form group with a control for each attribute', async () => {
            const { attributeForm } = await openEditor('/manage/postgresql/userGroups/t92group', ADMINISTRATOR);

            expect(attributeForm.namespace).toBe('USER_GROUP_ATTRIBUTES');
            expect(attributeForm.content).toBe(ATTRIBUTES);
            expect(Object.keys(attributeForm.modelFormGroup?.controls ?? {})).toEqual(['flag', 'note']);
        });

        it('should show the attributes of the loaded user group in the attribute form', async () => {
            const { attributeForm } = await openEditor('/manage/postgresql/userGroups/t92group', ADMINISTRATOR);

            expect(attributeForm.modelFormGroup?.value).toEqual(STORED_ATTRIBUTES);
        });

        it('should save the changed value of an attribute', async () => {
            const { component, attributeForm, userGroupService } =
                await openEditor('/manage/postgresql/userGroups/t92group', ADMINISTRATOR);

            attributeForm.modelFormGroup?.get('note')?.setValue('changed');
            await firstValueFrom(component.saveUserGroup());

            expect(userGroupService.saveUserGroup).toHaveBeenCalledWith('postgresql', expect.objectContaining({
                identifier: 't92group',
                attributes: { flag: 'true', note: 'changed' }
            }));
        });

        it('should save the attributes of a user group which was not changed', async () => {
            const { component, userGroupService } =
                await openEditor('/manage/postgresql/userGroups/t92group', ADMINISTRATOR);

            await firstValueFrom(component.saveUserGroup());

            expect(userGroupService.saveUserGroup).toHaveBeenCalledWith('postgresql', expect.objectContaining({
                attributes: STORED_ATTRIBUTES
            }));
        });

        it('should show the attributes of the cloned user group in the attribute form', async () => {
            const { attributeForm } = await openEditor('/manage/postgresql/userGroups?clone=t92group', ADMINISTRATOR);

            expect(attributeForm.modelFormGroup?.value).toEqual(STORED_ATTRIBUTES);
        });

        it('should create the clone with the attributes of the source, including changes', async () => {
            const { component, attributeForm, userGroupService } =
                await openEditor('/manage/postgresql/userGroups?clone=t92group', ADMINISTRATOR);

            attributeForm.modelFormGroup?.get('flag')?.setValue('');
            await firstValueFrom(component.saveUserGroup());

            expect(userGroupService.createUserGroup).toHaveBeenCalledWith('postgresql', expect.objectContaining({
                attributes: { flag: '', note: 'hello' }
            }));
        });

    });

});
