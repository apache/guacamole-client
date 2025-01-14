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

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslocoModule } from '@ngneat/transloco';
import { ClientLibModule, ElementModule, OskModule, TouchModule } from 'guacamole-frontend-lib';
import { ClipboardModule } from '../clipboard/clipboard.module';
import { FormModule } from '../form/form.module';
import { GroupListModule } from '../group-list/group-list.module';
import { NavigationModule } from '../navigation/navigation.module';
import { NotificationModule } from '../notification/notification.module';
import { TextInputModule } from '../text-input/text-input.module';
import { OrderByPipe } from '../util/order-by.pipe';
import { ClientPageComponent } from './components/client-page/client-page.component';
import { ConnectionGroupComponent } from './components/connection-group/connection-group.component';
import { ConnectionComponent } from './components/connection/connection.component';
import { FileComponent } from './components/file/file.component';
import {
    GuacClientNotificationComponent
} from './components/guac-client-notification/guac-client-notification.component';
import { GuacClientPanelComponent } from './components/guac-client-panel/guac-client-panel.component';
import { GuacClientUserCountComponent } from './components/guac-client-user-count/guac-client-user-count.component';
import { GuacClientZoomComponent } from './components/guac-client-zoom/guac-client-zoom.component';
import { GuacClientComponent } from './components/guac-client/guac-client.component';
import { GuacFileBrowserComponent } from './components/guac-file-browser/guac-file-browser.component';
import {
    GuacFileTransferManagerComponent
} from './components/guac-file-transfer-manager/guac-file-transfer-manager.component';
import { GuacFileTransferComponent } from './components/guac-file-transfer/guac-file-transfer.component';
import { GuacThumbnailComponent } from './components/guac-thumbnail/guac-thumbnail.component';
import { GuacTiledClientsComponent } from './components/guac-tiled-clients/guac-tiled-clients.component';
import { GuacTiledThumbnailsComponent } from './components/guac-tiled-thumbnails/guac-tiled-thumbnails.component';
import { GuacZoomCtrlDirective } from './directives/guac-zoom-ctrl.directive';

/**
 * The module for code used to connect to a connection or balancing group.
 */
@NgModule({
    declarations: [
        GuacZoomCtrlDirective,
        GuacClientZoomComponent,
        GuacThumbnailComponent,
        GuacTiledThumbnailsComponent,
        GuacFileTransferComponent,
        GuacFileTransferManagerComponent,
        ClientPageComponent,
        GuacClientUserCountComponent,
        GuacClientNotificationComponent,
        GuacClientComponent,
        GuacTiledClientsComponent,
        ConnectionComponent,
        ConnectionGroupComponent,
        GuacClientPanelComponent,
        GuacFileBrowserComponent,
        FileComponent
    ],
    imports     : [
        CommonModule,
        TranslocoModule,
        FormsModule,
        ElementModule,
        TextInputModule,
        OskModule,
        TouchModule,
        ClientLibModule,
        NavigationModule,
        RouterLink,
        ClipboardModule,
        FormModule,
        GroupListModule,
        NotificationModule,
        OrderByPipe
    ],
    exports     : [
        GuacClientPanelComponent
    ]
})
export class ClientModule {
}
