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
import { GuacClickDirective } from './directives/guac-click.directive';
import { GuacDropDirective } from './directives/guac-drop.directive';
import { GuacFocusDirective } from './directives/guac-focus.directive';
import { GuacResizeDirective } from './directives/guac-resize.directive';
import { GuacScrollDirective } from './directives/guac-scroll.directive';
import { GuacUploadDirective } from './directives/guac-upload.directive';

/**
 * Module for manipulating element state, such as focus or scroll position, as
 * well as handling browser events.
 */
@NgModule({
    declarations: [
        GuacClickDirective,
        GuacFocusDirective,
        GuacResizeDirective,
        GuacScrollDirective,
        GuacUploadDirective,
        GuacDropDirective
    ],
    imports     : [
        CommonModule
    ],
    exports     : [
        GuacClickDirective,
        GuacFocusDirective,
        GuacResizeDirective,
        GuacScrollDirective,
        GuacUploadDirective,
        GuacDropDirective
    ]
})
export class ElementModule {
}
