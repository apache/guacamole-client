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
import { ConnectionGroup } from '../../../rest/types/ConnectionGroup';

/**
 * A component which displays a single connection group within the menu of
 * guac-location-chooser and chooses that group when clicked.
 */
@Component({
    selector: 'guac-location-chooser-connection-group',
    templateUrl: './location-chooser-connection-group.component.html',
    encapsulation: ViewEncapsulation.None,
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class LocationChooserConnectionGroupComponent {

    /**
     * The context provided by guac-group-list, exposing the function of
     * guac-location-chooser which chooses the given connection group.
     */
    @Input({ required: true }) context!: { chooseGroup: (item: ConnectionGroup) => void };

    /**
     * The GroupListItem representing the connection group to display, as
     * provided by guac-group-list for items of this type.
     */
    @Input({ required: true }) item!: GroupListItem;
}
