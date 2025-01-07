

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
import { ManagedClient } from '../../types/ManagedClient';
import { ManagedClientGroup } from '../../types/ManagedClientGroup';

/**
 * A component for displaying a group of Guacamole clients as a non-interactive
 * thumbnail of tiled client displays.
 */
@Component({
    selector     : 'guac-tiled-thumbnails',
    templateUrl  : './guac-tiled-thumbnails.component.html',
    encapsulation: ViewEncapsulation.None
})
export class GuacTiledThumbnailsComponent {

    /**
     * The group of clients to display as a thumbnail of tiled client
     * displays.
     */
    @Input({ required: true }) clientGroup!: ManagedClientGroup;

    /**
     * The overall height of the thumbnail view of the tiled grid of
     * clients within the client group, in pixels. This value is
     * intentionally based off a snapshot of the current browser size at
     * the time the directive comes into existence to ensure the contents
     * of the thumbnail are familiar in appearance and aspect ratio.
     */
    height = Math.min(window.innerHeight, 128);

    /**
     * The overall width of the thumbnail view of the tiled grid of
     * clients within the client group, in pixels. This value is
     * intentionally based off a snapshot of the current browser size at
     * the time the directive comes into existence to ensure the contents
     * of the thumbnail are familiar in appearance and aspect ratio.
     */
    width = window.innerWidth / window.innerHeight * this.height;

    /**
     * @borrows ManagedClientGroup.getClientGrid
     */
    getClientGrid(group: ManagedClientGroup): ManagedClient[][] {
        return ManagedClientGroup.getClientGrid(group);
    }

}
