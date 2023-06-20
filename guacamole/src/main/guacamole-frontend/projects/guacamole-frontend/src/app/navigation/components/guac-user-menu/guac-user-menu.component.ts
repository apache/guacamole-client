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

import { Component, Input, OnInit, ViewEncapsulation } from '@angular/core';
import { MenuAction } from '../../types/MenuAction';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import { RequestService } from '../../../rest/service/request.service';
import { UserService } from '../../../rest/service/user.service';
import { UserPageService } from '../../../manage/services/user-page.service';
import { PageDefinition } from '../../types/PageDefinition';
import { User } from '../../../rest/types/User';

/**
 * A directive which provides a user-oriented menu containing options for
 * navigation and configuration.
 */
@Component({
    selector: 'guac-user-menu',
    templateUrl: './guac-user-menu.component.html',
    encapsulation: ViewEncapsulation.None
})
export class GuacUserMenuComponent implements OnInit {

    /**
     * Optional array of actions which are specific to this particular
     * location, as these actions may not be appropriate for other
     * locations which contain the user menu.
     */
    @Input() localActions?: MenuAction[];

    /**
     * The username of the current user.
     */
    username: string | null = this.authenticationService.getCurrentUsername();

    /**
     * The user's full name. If not yet available, or if not defined,
     * this will be null.
     */
    fullName: string | null = null;

    /**
     * A URL pointing to relevant user information such as the user's
     * email address. If not yet available, or if no such URL can be
     * determined, this will be null.
     */
    userURL: string | null = null;

    /**
     * The organization, company, group, etc. that the user belongs to.
     * If not yet available, or if not defined, this will be null.
     */
    organization: string | null = null;

    /**
     * The role that the user has at the organization, company, group,
     * etc. they belong to. If not yet available, or if not defined,
     * this will be null.
     */
    role: string | null = null;

    /**
     * The available main pages for the current user.
     */
    pages: PageDefinition[] = [];

    /**
     * Action which logs out the current user, redirecting them to back
     * to the login screen after logout completes.
     */
    private readonly LOGOUT_ACTION: MenuAction = {
        name: 'USER_MENU.ACTION_LOGOUT',
        className: 'logout',
        callback: () => this.logout()
    };

    /**
     * All available actions for the current user.
     */
    actions: MenuAction[] = [this.LOGOUT_ACTION];

    /**
     * Inject required services.
     */
    constructor(private authenticationService: AuthenticationService,
                private requestService: RequestService,
                private userService: UserService,
                private userPageService: UserPageService
    ) {
    }

    ngOnInit(): void {
        // Display user profile attributes if available
        this.userService.getUser(this.authenticationService.getDataSource()!, this.username!)
            .subscribe({
                next: user => {
                    // Pull basic profile information
                    this.fullName = user.attributes[User.Attributes.FULL_NAME];
                    this.organization = user.attributes[User.Attributes.ORGANIZATION];
                    this.role = user.attributes[User.Attributes.ORGANIZATIONAL_ROLE];

                    // Link to email address if available
                    const email = user.attributes[User.Attributes.EMAIL_ADDRESS];
                    this.userURL = email ? 'mailto:' + email : null;
                },

                error: this.requestService.IGNORE
            });

        // Retrieve the main pages from the user page service
        this.userPageService.getMainPages()
            .subscribe(pages => {
                this.pages = pages;
            });
    }

    /**
     * Returns whether the current user has authenticated anonymously.
     *
     * @returns
     *     true if the current user has authenticated anonymously, false
     *     otherwise.
     */
    isAnonymous(): boolean {
        return this.authenticationService.isAnonymous();
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
