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
    AfterViewInit,
    Component,
    effect,
    ElementRef,
    Input,
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
    selector     : 'guac-thumbnail',
    templateUrl  : './guac-thumbnail.component.html',
    encapsulation: ViewEncapsulation.None
})
export class GuacThumbnailComponent implements AfterViewInit, OnChanges {

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
    @ViewChild('display') private displayContainer?: ElementRef<HTMLDivElement>;

    /**
     * Reference to the main containing element for the component.
     */
    @ViewChild('main') private mainRef!: ElementRef<HTMLDivElement>;

    /**
     * The main containing element for the component.
     */
    private main!: HTMLDivElement;

    /**
     * Inject required services.
     */
    constructor(private renderer: Renderer2) {

        // Update scale when display is resized
        effect(() => {
            // Used to trigger this effect when the display size changes
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
            const newSize = this.client?.managedDisplay?.size()
            this.updateDisplayScale();
        });

    }

    /**
     * Attach the given client after the view has been initialized.
     */
    ngAfterViewInit(): void {
        this.main = this.mainRef.nativeElement;
        this.attachClient(this.client);
    }

    /**
     * When the client changes, attach the new client.
     */
    ngOnChanges(changes: SimpleChanges): void {

        if (changes['client']) {
            const managedClient = changes['client'].currentValue as ManagedClient | undefined;
            this.attachClient(managedClient);
        }

    }

    /**
     * Updates the scale of the attached Guacamole.Client based on current window
     * size and "auto-fit" setting.
     */
    updateDisplayScale(): void {

        if (!this.display) return;

        // Fit within available area
        this.display.scale(Math.min(
            this.main.offsetWidth / Math.max(this.display.getWidth(), 1),
            this.main.offsetHeight / Math.max(this.display.getHeight(), 1)
        ));

    }

    /**
     * Attach any given managed client.
     *
     * @param managedClient
     *     The managed client to attach.
     */
    attachClient(managedClient?: ManagedClient): void {

        // Only proceed if a display container present
        if (!this.displayContainer)
            return;

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
