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

import { inject, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterModule, Routes } from '@angular/router';
import { ManageUserComponent } from './manage/components/manage-user/manage-user.component';
import { ManageConnectionComponent } from './manage/components/manage-connection/manage-connection.component';
import { SettingsComponent } from './settings/components/settings/settings.component';
import { GuacSettingsUsersComponent } from './settings/components/guac-settings-users/guac-settings-users.component';
import {
    GuacSettingsPreferencesComponent
} from './settings/components/guac-settings-preferences/guac-settings-preferences.component';
import {
    GuacSettingsUserGroupsComponent
} from './settings/components/guac-settings-user-groups/guac-settings-user-groups.component';
import {
    GuacSettingsConnectionsComponent
} from './settings/components/guac-settings-connections/guac-settings-connections.component';
import {
    GuacSettingsConnectionHistoryComponent
} from './settings/components/guac-settings-connection-history/guac-settings-connection-history.component';
import {
    GuacSettingsSessionsComponent
} from './settings/components/guac-settings-sessions/guac-settings-sessions.component';
import { HomeComponent } from './home/components/home/home.component';
import { UserPageService } from './manage/services/user-page.service';
import { AuthenticationService } from './auth/service/authentication.service';
import { catchError, map, Observable, of, switchMap } from 'rxjs';
import { AuthenticationResult } from './auth/types/AuthenticationResult';
import { ClientPageComponent } from './client/components/client-page/client-page.component';
import { ManageUserGroupComponent } from './manage/components/manage-user-group/manage-user-group.component';


/**
 * Redirects the user to their home page. This necessarily requires
 * attempting to re-authenticate with the Guacamole server, as the user's
 * credentials may have changed, and thus their most-appropriate home page
 * may have changed as well.
 *
 * @param route
 *     The route which was requested.
 *
 * @returns {Promise}
 *     A promise which resolves successfully only after an attempt to
 *     re-authenticate and determine the user's proper home page has been
 *     made.
 */
const routeToUserHomePage: CanActivateFn = (route: ActivatedRouteSnapshot) => {

    // Required services
    const router = inject(Router);
    const userPageService = inject(UserPageService)


    // Re-authenticate including any parameters in URL
    return updateCurrentToken(route)
        .pipe(
            switchMap(() =>

                // Redirect to home page
                userPageService.getHomePage()
                    .pipe(
                        map(homePage => {
                                // If home page is the requested location, allow through
                                if (route.root.url.join('/') || '/' === homePage.url)
                                    return true;

                                // Otherwise, reject and reroute
                                else {
                                    return router.parseUrl(homePage.url);
                                }
                            }
                        ),

                        // If retrieval of home page fails, assume requested page is OK
                        catchError(() => of(true))
                    )
            ),
            catchError(() => of(false)),
        );

}


/**
 * Attempts to re-authenticate with the Guacamole server, sending any
 * query parameters in the URL, along with the current auth token, and
 * updating locally stored token if necessary.
 *
 * @param route
 *     The route which was requested.
 *
 * @returns
 *     An observable which completes only after an attempt to
 *     re-authenticate has been made. If the authentication attempt fails,
 *     the observable will emit an error.
 */
const updateCurrentToken = (route: ActivatedRouteSnapshot): Observable<AuthenticationResult> => {

    // Required services
    const authenticationService = inject(AuthenticationService);

    // Re-authenticate including any parameters in URL
    return authenticationService.updateCurrentToken(route.queryParams);

};

/**
 * Guard which prevents access to routes which require authentication if
 * the user is not authenticated.
 *
 * @param route
 *     The route which was requested.
 *
 * @returns
 *     An observable which emits true if the user is authenticated, or false
 *     if the user is not authenticated.
 */
const authGuard: CanActivateFn = (route: ActivatedRouteSnapshot): Observable<boolean> => {

    return updateCurrentToken(route)
        .pipe(
            map(() => true),
            catchError(() => of(false))
        );
};

/**
 * Configure each possible route.
 */
export const appRoutes: Routes = [

    // Home screen
    {
        path: '',
        title: 'APP.NAME',
        component: HomeComponent,
        data: {bodyClassName: 'home'},
        // Run the canActivate guard on every navigation, even if the route hasn't changed
        runGuardsAndResolvers: 'always',
        canActivate: [routeToUserHomePage]
    },

    // User editor
    {
        path: 'manage/:dataSource/users/:id',
        title: 'APP.NAME',
        component: ManageUserComponent,
        data: {bodyClassName: 'manage'},
        canActivate: [authGuard]
    },

    // User editor for creating a new user
    {
        path: 'manage/:dataSource/users',
        title: 'APP.NAME',
        component: ManageUserComponent,
        data: {bodyClassName: 'manage'},
        canActivate: [authGuard]
    },

    // Management screen
    {
        path: 'settings',
        title: 'APP.NAME',
        data: {bodyClassName: 'settings'},
        component: SettingsComponent,
        canActivate: [authGuard],
        children: [
            {path: 'users', component: GuacSettingsUsersComponent},
            {path: 'userGroups', component: GuacSettingsUserGroupsComponent},
            {path: ':dataSource/connections', component: GuacSettingsConnectionsComponent},
            {path: ':dataSource/history', component: GuacSettingsConnectionHistoryComponent},
            {path: 'sessions', component: GuacSettingsSessionsComponent},
            {path: 'preferences', component: GuacSettingsPreferencesComponent}
        ]
    },

    // Connection editor
    {
        path: 'manage/:dataSource/connections/:id',
        title: 'APP.NAME',
        component: ManageConnectionComponent,
        data: {bodyClassName: 'manage'},
        canActivate: [authGuard]
    },

    // Connection editor for creating a new connection
    {
        path: 'manage/:dataSource/connections',
        title: 'APP.NAME',
        component: ManageConnectionComponent,
        data: {bodyClassName: 'manage'},
        canActivate: [authGuard]
    },

    // User group editor
    {
        path: 'manage/:dataSource/userGroups/:id',
        title: 'APP.NAME',
        component: ManageUserGroupComponent,
        data: {bodyClassName: 'manage'},
        canActivate: [authGuard]
    },

    // User group editor for creating a new user group
    {
        path: 'manage/:dataSource/userGroups',
        title: 'APP.NAME',
        component: ManageUserGroupComponent,
        data: {bodyClassName: 'manage'},
        canActivate: [authGuard]
    },

    // Client view
    {
        path: 'client/:id',
        component: ClientPageComponent,
        // TODO: reloadOnUrl: false,
        // f the option is set to false and the URL in the browser changes,
        // but the new URL maps to the same route, then a $routeUpdate event is broadcasted
        // on the root scope (without reloading the route).
        canActivate: [authGuard],
        data: {bodyClassName: 'client'},

    },

    {path: '**', redirectTo: ''}
];

@NgModule({
    imports: [RouterModule.forRoot(appRoutes, {bindToComponentInputs: true, useHash: true})],
    exports: [RouterModule]
})
export class AppRoutingModule {
}
