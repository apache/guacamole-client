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

import { Component, Input, ViewEncapsulation, ChangeDetectionStrategy } from '@angular/core';
import { GroupListItem } from '../../../group-list/types/GroupListItem';
import { ConnectionListContext } from '../../types/ConnectionListContext';

/**
 * A component which displays a single connection group within the list
 * of available connections in the Guacamole menu. Balancing groups can
 * be added to or removed from the current view.
 */
@Component({
    selector: 'guac-connection-group',
    templateUrl: './connection-group.component.html',
    styles: [],
    encapsulation: ViewEncapsulation.None,
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class ConnectionGroupComponent {

    /**
     * The context provided by guac-group-list, exposing the set of clients
     * attached to the current view and a function to update it.
     */
    @Input({ required: true }) context!: ConnectionListContext;

    /**
     * The GroupListItem representing the connection group to display, as
     * provided by guac-group-list for items of this type.
     */
    @Input({ required: true }) item!: GroupListItem;
}
