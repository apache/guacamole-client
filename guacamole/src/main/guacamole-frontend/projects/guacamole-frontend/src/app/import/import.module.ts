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
import { ElementModule } from 'guacamole-frontend-lib';
import { ListModule } from '../list/list.module';
import { NavigationModule } from '../navigation/navigation.module';
import {
    ConnectionImportErrorsComponent
} from './components/connection-import-errors/connection-import-errors.component';
import {
    ConnectionImportFileHelpComponent
} from './components/connection-import-file-help/connection-import-file-help.component';
import { ImportConnectionsComponent } from './components/import-connections/import-connections.component';

/**
 * The module for code supporting importing user-supplied files. Currently, only
 * connection import is supported.
 */
@NgModule({
    declarations: [
        ImportConnectionsComponent,
        ConnectionImportFileHelpComponent,
        ConnectionImportErrorsComponent
    ],
    imports     : [
        CommonModule,
        TranslocoModule,
        RouterLink,
        ElementModule,
        NavigationModule,
        FormsModule,
        ListModule
    ],
    exports     : [
        ImportConnectionsComponent,
        ConnectionImportFileHelpComponent
    ]
})
export class ImportModule {
}
