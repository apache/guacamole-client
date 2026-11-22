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

import { TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { provideRouter } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { Observable, of } from 'rxjs';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import { GuacFilterComponent } from '../../../list/components/guac-filter/guac-filter.component';
import { GuacPagerComponent } from '../../../list/components/guac-pager/guac-pager.component';
import { PermissionService } from '../../../rest/service/permission.service';
import { UserService } from '../../../rest/service/user.service';
import { PermissionSet } from '../../../rest/types/PermissionSet';
import { User } from '../../../rest/types/User';
import { GuacSettingsUsersComponent } from './guac-settings-users.component';

/**
 * Stand-in for UserService which, like the real service, reads its state
 * through "this" and therefore fails if one of its methods is called
 * unbound.
 */
class FakeUserService {

    /**
     * The users of each data source, by data source identifier.
     */
    private readonly users: Record<string, Record<string, User>> = {
        'postgresql'       : { alice: new User({ username: 'alice' }), bob: new User({ username: 'bob' }) },
        'postgresql-shared': { alice: new User({ username: 'alice' }), carol: new User({ username: 'carol' }) }
    };

    /**
     * The permission types requested by each call of getUsers().
     */
    readonly requestedPermissionTypes: (string[] | undefined)[] = [];

    /**
     * Returns the users of the given data source.
     *
     * @param dataSource
     *     The identifier of the data source.
     *
     * @param permissionTypes
     *     The permission types the users must be granted, if any.
     *
     * @returns
     *     The users of the data source.
     */
    getUsers(dataSource: string, permissionTypes?: string[]): Observable<Record<string, User>> {
        this.requestedPermissionTypes.push(permissionTypes);
        return of(this.users[dataSource]);
    }

}

/**
 * Creates the component with the given permissions of the current user in
 * the data sources "postgresql" and "postgresql-shared".
 *
 * @param permissions
 *     The permissions of the current user, by data source identifier.
 *
 * @returns
 *     The component, once its users have been loaded.
 */
const createComponent = async (permissions: Record<string, PermissionSet>): Promise<GuacSettingsUsersComponent> => {
    TestBed.configureTestingModule({
        declarations: [GuacSettingsUsersComponent, GuacFilterComponent, GuacPagerComponent],
        schemas: [NO_ERRORS_SCHEMA],
        imports: [
            TranslocoTestingModule.forRoot({
                langs: { en: {} },
                translocoConfig: { availableLangs: ['en'], defaultLang: 'en' }
            })
        ],
        providers: [
            provideRouter([]),
            FakeUserService,
            { provide: UserService, useExisting: FakeUserService },
            {
                provide: AuthenticationService,
                useValue: {
                    getAvailableDataSources: () => ['postgresql', 'postgresql-shared'],
                    getCurrentUsername: () => 'dave'
                }
            },
            {
                provide: PermissionService,
                useValue: { getEffectivePermissions: (dataSource: string) => of(permissions[dataSource]) }
            }
        ]
    });

    const fixture = TestBed.createComponent(GuacSettingsUsersComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    return fixture.componentInstance;
};

/**
 * Returns the data source and name of each user listed by the component.
 *
 * @param component
 *     The component listing the users.
 *
 * @returns
 *     One string of the form "dataSource:username" for each listed user.
 */
const listed = (component: GuacSettingsUsersComponent): string[] =>
    (component.manageableUsers ?? []).map(entry => `${entry.dataSource}:${entry.user.username}`);

describe('GuacSettingsUsersComponent', () => {

    it('should list a user which exists in two data sources once and keep the other users of the second data source', async () => {

        // GIVEN an administrator of both data sources, and "alice" in both
        const administrator = new PermissionSet({ systemPermissions: [PermissionSet.SystemPermissionType.ADMINISTER] });

        // WHEN the users are listed
        const component = await createComponent({ 'postgresql': administrator, 'postgresql-shared': administrator });

        // THEN "alice" appears once, and "carol" is not lost behind the duplicate
        expect(listed(component)).toEqual(['postgresql:alice', 'postgresql:bob', 'postgresql-shared:carol']);

    });

    it('should list the users which can be updated if the current user cannot create users', async () => {

        // GIVEN a user who may update "bob" and "carol" and delete "alice", but may not create users
        const permissions = new PermissionSet({
            userPermissions: {
                bob  : [PermissionSet.ObjectPermissionType.UPDATE],
                alice: [PermissionSet.ObjectPermissionType.DELETE]
            }
        });
        const sharedPermissions = new PermissionSet({
            userPermissions: { carol: [PermissionSet.ObjectPermissionType.UPDATE] }
        });

        // WHEN the users are listed
        const component = await createComponent({ 'postgresql': permissions, 'postgresql-shared': sharedPermissions });
        const userService = TestBed.inject(FakeUserService);

        // THEN only users with UPDATE or DELETE permission are requested, and the users are listed
        expect(userService.requestedPermissionTypes).toEqual([
            [PermissionSet.ObjectPermissionType.UPDATE, PermissionSet.ObjectPermissionType.DELETE],
            [PermissionSet.ObjectPermissionType.UPDATE, PermissionSet.ObjectPermissionType.DELETE]
        ]);
        expect(listed(component)).toEqual(['postgresql:alice', 'postgresql:bob', 'postgresql-shared:carol']);

    });

});
