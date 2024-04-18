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

import { Component, Input, ViewEncapsulation } from '@angular/core';
import { GroupListItem } from '../../../group-list/types/GroupListItem';
import { ConnectionListContext } from '../../types/ConnectionListContext';

/**
 * A component which displays a sharing profile for a specific
 * connection and allows manipulation of the sharing profile permissions.
 */
@Component({
    selector     : 'guac-sharing-profile-permission',
    templateUrl  : './sharing-profile-permission.component.html',
    encapsulation: ViewEncapsulation.None
})
export class SharingProfilePermissionComponent {

    /**
     * TODO
     */
    @Input({ required: true }) context!: ConnectionListContext;

    /**
     * TODO
     */
    @Input({ required: true }) item!: GroupListItem;

}
