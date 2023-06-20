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

import { NgModule } from '@angular/core';
import { CommonModule, NgOptimizedImage } from '@angular/common';
import { TranslocoModule } from '@ngneat/transloco';
import { ListModule } from '../list/list.module';
import { RouterLink, RouterOutlet } from '@angular/router';
import { SettingsComponent } from './components/settings/settings.component';
import { NavigationModule } from '../navigation/navigation.module';
import { GuacSettingsUsersComponent } from './components/guac-settings-users/guac-settings-users.component';
import {
    GuacSettingsPreferencesComponent
} from './components/guac-settings-preferences/guac-settings-preferences.component';
import { FormModule } from '../form/form.module';
import { FormsModule } from '@angular/forms';
import {
    GuacSettingsUserGroupsComponent
} from './components/guac-settings-user-groups/guac-settings-user-groups.component';
import { IndexModule } from '../index/index.module';
import {
    GuacSettingsConnectionsComponent
} from './components/guac-settings-connections/guac-settings-connections.component';
import { GroupListModule } from '../group-list/group-list.module';
import { ConnectionComponent } from './components/connection/connection.component';
import { ConnectionGroupComponent } from './components/connection-group/connection-group.component';
import { NewConnectionComponent } from './components/new-connection/new-connection.component';
import { NewConnectionGroupComponent } from './components/new-connection-group/new-connection-group.component';
import { NewSharingProfileComponent } from './components/new-sharing-profile/new-sharing-profile.component';
import { GuacSettingsConnectionHistoryComponent } from './components/guac-settings-connection-history/guac-settings-connection-history.component';
import { GuacSettingsSessionsComponent } from './components/guac-settings-sessions/guac-settings-sessions.component';

/**
 * The module for manipulation of general settings. This is distinct from the
 * "manage" module, which deals only with administrator-level system management.
 */
@NgModule({
    declarations: [
        GuacSettingsUsersComponent,
        SettingsComponent,
        GuacSettingsPreferencesComponent,
        GuacSettingsUserGroupsComponent,
        GuacSettingsConnectionsComponent,
        ConnectionComponent,
        ConnectionGroupComponent,
        NewConnectionComponent,
        NewConnectionGroupComponent,
        NewSharingProfileComponent,
        GuacSettingsConnectionHistoryComponent,
        GuacSettingsSessionsComponent
    ],
    imports: [
        CommonModule,
        TranslocoModule,
        ListModule,
        RouterLink,
        NavigationModule,
        RouterOutlet,
        FormModule,
        NgOptimizedImage,
        FormsModule,
        IndexModule,
        GroupListModule,
    ],
    exports: [
        GuacSettingsUsersComponent,
        SettingsComponent,
        GuacSettingsPreferencesComponent,
        GuacSettingsUserGroupsComponent,
        GuacSettingsConnectionsComponent,
        GuacSettingsConnectionHistoryComponent,
        GuacSettingsSessionsComponent
    ]
})
export class SettingsModule {
}
