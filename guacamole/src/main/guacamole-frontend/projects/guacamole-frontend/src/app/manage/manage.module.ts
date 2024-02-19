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
import { ManageUserComponent } from './components/manage-user/manage-user.component';
import { FormModule } from '../form/form.module';
import { TranslocoModule } from '@ngneat/transloco';
import { FormsModule } from '@angular/forms';
import { NavigationModule } from '../navigation/navigation.module';
import { DataSourceTabsComponent } from './components/data-source-tabs/data-source-tabs.component';
import {
    SystemPermissionEditorComponent
} from './components/system-permission-editor/system-permission-editor.component';
import { IdentifierSetEditorComponent } from './components/identifier-set-editor/identifier-set-editor.component';
import { ListModule } from '../list/list.module';
import { ManagementButtonsComponent } from './components/management-buttons/management-buttons.component';
import { ManageConnectionComponent } from './components/manage-connection/manage-connection.component';
import { LocationChooserComponent } from './components/location-chooser/location-chooser.component';
import { GroupListModule } from '../group-list/group-list.module';
import {
    LocationChooserConnectionGroupComponent
} from './components/location-chooser-connection-group/location-chooser-connection-group.component';
import { ManageUserGroupComponent } from './components/manage-user-group/manage-user-group.component';
import { ManageConnectionGroupComponent } from './components/manage-connection-group/manage-connection-group.component';
import { ConnectionPermissionEditorComponent } from './components/connection-permission-editor/connection-permission-editor.component';
import { ConnectionPermissionComponent } from './components/connection-permission/connection-permission.component';
import { SharingProfilePermissionComponent } from './components/sharing-profile-permission/sharing-profile-permission.component';
import { ConnectionGroupPermissionComponent } from './components/connection-group-permission/connection-group-permission.component';

/**
 * The module for the administration functionality.
 */
@NgModule({
    declarations: [
        ManageUserComponent,
        DataSourceTabsComponent,
        SystemPermissionEditorComponent,
        IdentifierSetEditorComponent,
        ManagementButtonsComponent,
        ManageConnectionComponent,
        LocationChooserComponent,
        LocationChooserConnectionGroupComponent,
        ManageUserGroupComponent,
        ManageConnectionGroupComponent,
        ConnectionPermissionEditorComponent,
        ConnectionPermissionComponent,
        SharingProfilePermissionComponent,
        ConnectionGroupPermissionComponent
    ],
    imports: [
        CommonModule,
        FormModule,
        TranslocoModule,
        FormsModule,
        NavigationModule,
        ListModule,
        NgOptimizedImage,
        GroupListModule
    ],
    exports: [
        ManageUserComponent,
        DataSourceTabsComponent,
        SystemPermissionEditorComponent,
        IdentifierSetEditorComponent,
        ManagementButtonsComponent,
        ManageConnectionComponent,
        LocationChooserComponent,
        LocationChooserConnectionGroupComponent,
        ManageConnectionGroupComponent
    ]
})
export class ManageModule {
}
