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

import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { of } from 'rxjs';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import { EscapePipe } from '../../../index/pipes/escape.pipe';
import { GuacFilterComponent } from '../../../list/components/guac-filter/guac-filter.component';
import { GuacPagerComponent } from '../../../list/components/guac-pager/guac-pager.component';
import { PermissionService } from '../../../rest/service/permission.service';
import { UserGroupService } from '../../../rest/service/user-group.service';
import { PermissionSet } from '../../../rest/types/PermissionSet';
import { UserGroup } from '../../../rest/types/UserGroup';
import { GuacSettingsUserGroupsComponent } from './guac-settings-user-groups.component';

/**
 * The user groups of each data source, by data source identifier. The group
 * "staff" exists in both data sources.
 */
const userGroups: Record<string, Record<string, UserGroup>> = {
    'postgresql'       : { staff: new UserGroup({ identifier: 'staff' }), admins: new UserGroup({ identifier: 'admins' }) },
    'postgresql-shared': { staff: new UserGroup({ identifier: 'staff' }), guests: new UserGroup({ identifier: 'guests' }) }
};

describe('GuacSettingsUserGroupsComponent', () => {

    it('should list a user group which exists in two data sources once and keep the other groups of the second data source', async () => {

        // GIVEN an administrator of both data sources, and "staff" in both
        const administrator = new PermissionSet({ systemPermissions: [PermissionSet.SystemPermissionType.ADMINISTER] });
        TestBed.configureTestingModule({
            declarations: [GuacSettingsUserGroupsComponent, GuacFilterComponent, GuacPagerComponent, EscapePipe],
            schemas: [NO_ERRORS_SCHEMA],
            imports: [
                TranslocoTestingModule.forRoot({
                    langs: { en: {} },
                    translocoConfig: { availableLangs: ['en'], defaultLang: 'en' }
                })
            ],
            providers: [
                provideRouter([]),
                {
                    provide: AuthenticationService,
                    useValue: {
                        getAvailableDataSources: () => ['postgresql', 'postgresql-shared'],
                        getCurrentUsername: () => 'dave'
                    }
                },
                { provide: PermissionService, useValue: { getEffectivePermissions: () => of(administrator) } },
                {
                    provide: UserGroupService,
                    useValue: { getUserGroups: (dataSource: string) => of(userGroups[dataSource]) }
                }
            ]
        });

        // WHEN the user groups are listed
        const fixture = TestBed.createComponent(GuacSettingsUserGroupsComponent);
        fixture.detectChanges();
        await fixture.whenStable();
        const listed = (fixture.componentInstance.manageableUserGroups ?? [])
            .map(entry => `${entry.dataSource}:${entry.userGroup.identifier}`);

        // THEN "staff" appears once, and "guests" is not lost behind the duplicate
        expect(listed).toEqual(['postgresql:staff', 'postgresql:admins', 'postgresql-shared:guests']);

    });

});
