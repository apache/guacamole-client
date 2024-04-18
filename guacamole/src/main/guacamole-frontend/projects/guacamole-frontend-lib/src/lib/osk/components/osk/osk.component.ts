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

import { HttpClient } from '@angular/common/http';
import { Component, ElementRef, Input, OnChanges, SimpleChanges, ViewChild, ViewEncapsulation } from '@angular/core';
import { GuacEventService } from '../../../events/services/guac-event.service';
import { GuacEventArguments } from '../../../events/types/GuacEventArguments';


/**
 * A component which displays the Guacamole on-screen keyboard.
 */
@Component({
    selector     : 'guac-osk',
    templateUrl  : './osk.component.html',
    encapsulation: ViewEncapsulation.None
})
export class OskComponent implements OnChanges {

    /**
     * The URL for the Guacamole on-screen keyboard layout to use.
     */
    @Input({ required: true }) layout!: string;

    /**
     * The current on-screen keyboard, if any.
     */
    private keyboard: Guacamole.OnScreenKeyboard | null = null;

    /**
     * The main containing element for the entire directive.
     */
    @ViewChild('osk') main!: ElementRef<HTMLDivElement>;

    /**
     * Inject required services.
     */
    constructor(private http: HttpClient,
                private guacEventService: GuacEventService<GuacEventArguments>) {
    }

    /**
     * Load the new keyboard when the layout changes.
     */
    ngOnChanges(changes: SimpleChanges): void {

        if (changes['layout']) {
            this.onLayoutChange(changes['layout'].currentValue);
        }

    }

    // Size keyboard to same size as main element
    keyboardResized(): void {

        // Resize keyboard, if defined
        if (this.keyboard)
            this.keyboard.resize(this.main.nativeElement.offsetWidth);

    }

    /**
     * Load the new keyboard.
     *
     * @param url
     *     The URL for the Guacamole on-screen keyboard layout to use.
     */
    private onLayoutChange(url: string): void {

        // Remove current keyboard
        if (this.keyboard) {
            this.main.nativeElement.removeChild(this.keyboard.getElement());
            this.keyboard = null;
        }

        // Load new keyboard
        if (url) {

            // Retrieve layout JSON
            this.http.get<Guacamole.OnScreenKeyboard.Layout>(url).subscribe(
                // Build OSK with retrieved layout
                (layout) => {

                    // Abort if the layout changed while we were waiting for a response
                    if (this.layout !== url)
                        return;

                    // Add OSK element
                    this.keyboard = new Guacamole.OnScreenKeyboard(layout);
                    this.main.nativeElement.appendChild(this.keyboard.getElement());

                    // Init size
                    this.keyboard.resize(this.main.nativeElement.offsetWidth);

                    // Broadcast keydown for each key pressed
                    this.keyboard.onkeydown = (keysym: number) => {
                        this.guacEventService.broadcast('guacSyntheticKeydown', { keysym });
                    };

                    // Broadcast keydown for each key released
                    this.keyboard.onkeyup = (keysym: number) => {
                        this.guacEventService.broadcast('guacSyntheticKeyup', { keysym });
                    };
                });
        }
    }

}
