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

import {
    Component,
    DoCheck,
    ElementRef,
    Input,
    KeyValueDiffer,
    KeyValueDiffers,
    OnChanges,
    Renderer2,
    SimpleChanges,
    ViewChild,
    ViewEncapsulation
} from '@angular/core';
import { ManagedClient } from '../../types/ManagedClient';

/**
 * A component for displaying a Guacamole client as a non-interactive
 * thumbnail.
 */
@Component({
    selector: 'guac-thumbnail',
    templateUrl: './guac-thumbnail.component.html',
    encapsulation: ViewEncapsulation.None
})
export class GuacThumbnailComponent implements OnChanges, DoCheck {

    /**
     * The client to display within this guacThumbnail directive.
     */
    @Input() client?: ManagedClient;

    /**
     * The display of the current Guacamole client instance.
     */
    private display: Guacamole.Display | null = null;

    /**
     * The element associated with the display of the current
     * Guacamole client instance.
     */
    private displayElement: Element | null = null;

    /**
     * The element which must contain the Guacamole display element.
     */
    @ViewChild('display')
    private displayContainer!: ElementRef<HTMLDivElement>;

    /**
     * TODO
     */
    private clientDiffer?: KeyValueDiffer<string, any>;

    /**
     *
     * @param main
     *     The main containing element for the entire directive.
     */
    constructor(private main: ElementRef, private renderer: Renderer2, private differs: KeyValueDiffers) {
    }

    /**
     * Updates the scale of the attached Guacamole.Client based on current window
     * size and "auto-fit" setting.
     */
    updateDisplayScale(): void {

        if (!this.display) return;

        // Fit within available area
        this.display.scale(Math.min(
            this.main.nativeElement.offsetWidth / Math.max(this.display.getWidth(), 1),
            this.main.nativeElement.offsetHeight / Math.max(this.display.getHeight(), 1)
        ));

    }

    ngOnChanges(changes: SimpleChanges): void {

        // Attach any given managed client
        if (changes['client']) {

            const managedClient = changes['client'].currentValue as ManagedClient | undefined;

            this.clientDiffer = this.differs.find(managedClient).create();

            // Remove any existing display
            this.renderer.setProperty(this.displayContainer.nativeElement, 'innerHTML', '')

            // Only proceed if a client is given
            if (!managedClient)
                return;

            // Get Guacamole client instance
            const client: Guacamole.Client = managedClient.client;

            // Attach possibly new display
            this.display = client.getDisplay();

            // Add display element
            this.displayElement = this.display.getElement();
            this.renderer.appendChild(this.displayContainer.nativeElement, this.displayElement);
        }

    }

    ngDoCheck(): void {

        if (!this.clientDiffer || !this.client) return;
        const changes = this.clientDiffer.diff(this.client);
        if (changes) {
            console.log('TODO: Update scale when display is resized', changes);
            // TODO: Update scale when display is resized
            // $scope.$watch('client.managedDisplay.size', function setDisplaySize(size) {
            //     $scope.$evalAsync($scope.updateDisplayScale);
            // });
        }

    }


}
