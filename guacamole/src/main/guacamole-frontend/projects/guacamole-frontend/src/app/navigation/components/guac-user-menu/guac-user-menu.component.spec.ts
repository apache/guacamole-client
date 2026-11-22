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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { of } from 'rxjs';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import { UserPageService } from '../../../manage/services/user-page.service';
import { RequestService } from '../../../rest/service/request.service';
import { UserService } from '../../../rest/service/user.service';
import { User } from '../../../rest/types/User';
import { GuacUserMenuComponent } from './guac-user-menu.component';

describe('GuacUserMenuComponent', () => {
    let fixture: ComponentFixture<GuacUserMenuComponent>;

    /**
     * Renders the user menu of guacadmin with the given user attributes, as
     * UserService returns them.
     *
     * @param attributes
     *     The attributes of the user.
     */
    const render = (attributes: Record<string, string>): void => {
        TestBed.configureTestingModule({
            declarations: [GuacUserMenuComponent],
            imports: [TranslocoTestingModule.forRoot({})],
            // Ignores guac-menu and guac-page-list, which this spec does not
            // exercise
            schemas: [NO_ERRORS_SCHEMA],
            providers: [
                {
                    provide : AuthenticationService,
                    useValue: {
                        getCurrentUsername: () => 'guacadmin',
                        getDataSource     : () => 'postgresql',
                        isAnonymous       : () => false
                    }
                },
                { provide: RequestService, useValue: { IGNORE: vi.fn() } },
                { provide: UserService, useValue: { getUser: () => of(new User({ username: 'guacadmin', attributes })) } },
                { provide: UserPageService, useValue: { getMainPages: () => of([]) } }
            ]
        });

        fixture = TestBed.createComponent(GuacUserMenuComponent);
        fixture.detectChanges();
    };

    afterEach(() => {
        fixture.destroy();
    });

    it('should link the full name of the user to the email address of the user', () => {
        render({
            [User.Attributes.FULL_NAME]    : 'Guacamole Administrator',
            [User.Attributes.EMAIL_ADDRESS]: 'guacadmin@example.com'
        });

        const link: HTMLAnchorElement = fixture.nativeElement.querySelector('.profile .full-name a');
        expect(link.textContent?.trim()).toBe('Guacamole Administrator');
        expect(link.getAttribute('href')).toBe('mailto:guacadmin@example.com');
    });

    it('should not link the full name of a user without an email address', () => {
        render({ [User.Attributes.FULL_NAME]: 'Guacamole Administrator' });

        const link: HTMLAnchorElement = fixture.nativeElement.querySelector('.profile .full-name a');
        expect(link.textContent?.trim()).toBe('Guacamole Administrator');
        expect(link.hasAttribute('href')).toBe(false);
    });

});
