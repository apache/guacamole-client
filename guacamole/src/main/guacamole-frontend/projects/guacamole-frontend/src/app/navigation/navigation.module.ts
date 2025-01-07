

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
import { RouterLink } from '@angular/router';
import { TranslocoModule } from '@ngneat/transloco';
import { GuacMenuComponent } from './components/guac-menu/guac-menu.component';
import { GuacPageListComponent } from './components/guac-page-list/guac-page-list.component';
import { GuacSectionTabsComponent } from './components/guac-section-tabs/guac-section-tabs.component';
import { GuacUserMenuComponent } from './components/guac-user-menu/guac-user-menu.component';

/**
 * Module for generating and implementing user navigation options.
 */
@NgModule({
    declarations: [
        GuacMenuComponent,
        GuacPageListComponent,
        GuacSectionTabsComponent,
        GuacUserMenuComponent
    ],
    exports     : [
        GuacMenuComponent,
        GuacPageListComponent,
        GuacSectionTabsComponent,
        GuacUserMenuComponent
    ],
    imports     : [
        CommonModule,
        TranslocoModule,
        RouterLink,
    ]
})
export class NavigationModule {
}
