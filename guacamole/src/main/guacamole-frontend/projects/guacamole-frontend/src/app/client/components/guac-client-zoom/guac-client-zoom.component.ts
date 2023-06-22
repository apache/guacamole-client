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

/**
 * A component for controlling the zoom level and scale-to-fit behavior of
 * a single Guacamole client.
 */
@Component({
    selector: 'guac-client-zoom',
    templateUrl: './guac-client-zoom.component.html',
    encapsulation: ViewEncapsulation.None
})
export class GuacClientZoomComponent {

    /**
     * The client to control the zoom/autofit of.
     */
    @Input({required: true}) client!: ManagedClient;

    /**
     * Zooms in by 10%, automatically disabling autofit.
     */
    zoomIn(): void {
        this.client.clientProperties.autoFit = false;
        this.client.clientProperties.scale += 0.1;
    }

    /**
     * Zooms out by 10%, automatically disabling autofit.
     */
    zoomOut(): void {
        this.client.clientProperties.autoFit = false;
        this.client.clientProperties.scale -= 0.1;
    }

    /**
     * TODO: Got removed in commit b0febd340226c8d21de7ba4df2807149c8489c10
     * Either remove this method and its call in the template or reimplement it.
     */
    zoomSet(): void {
        // this.menu.autoFit = false;
        // this.client.clientProperties.autoFit = false;
    }

    /**
     * Resets the client autofit setting to false.
     */
    clearAutoFit(): void {
        this.client.clientProperties.autoFit = false;
    }

    /**
     * Notifies that the autofit setting has been manually changed by the
     * user.
     */
    autoFitChanged(): void {

        // Reset to 100% scale when autofit is first disabled
        if (!this.client.clientProperties.autoFit)
            this.client.clientProperties.scale = 1;

    }

    /**
     * TODO: Got removed in commit b0febd340226c8d21de7ba4df2807149c8489c10
     * Either remove this method and its call in the template or reimplement it.
     */
    changeAutoFit() {

    }

    /**
     * TODO: Got removed in commit b0febd340226c8d21de7ba4df2807149c8489c10
     * Either remove this method and its call in the template or reimplement it.
     */
    autoFitDisabled() {
        // return this.client.clientProperties.minZoom >= 1;
        return false;
    }
}
