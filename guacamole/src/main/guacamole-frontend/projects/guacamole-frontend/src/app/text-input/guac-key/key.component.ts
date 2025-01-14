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

import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, ViewEncapsulation } from '@angular/core';
import { GuacEventService } from 'guacamole-frontend-lib';
import { GuacFrontendEventArguments } from '../../events/types/GuacFrontendEventArguments';

/**
 * A component which displays a button that controls the pressed state of a
 * single keyboard key.
 */
@Component({
    selector     : 'guac-key',
    templateUrl  : './key.component.html',
    encapsulation: ViewEncapsulation.None
})
export class KeyComponent implements OnChanges {

    /**
     * The text to display within the key. This will be run through the
     * translation filter prior to display.
     */
    @Input({ required: true }) text!: string;

    /**
     * The keysym to send within keyup and keydown events when this key
     * is pressed or released.
     */
    @Input({ required: true }) keysym!: number;

    /**
     * Whether this key is sticky. Sticky keys toggle their pressed
     * state with each click.
     *
     * @default false
     */
    @Input() sticky = false;

    /**
     * Whether this key is currently pressed.
     *
     * @default false
     */
    @Input() pressed = false;
    @Output() pressedChange = new EventEmitter<boolean>();

    constructor(private guacEventService: GuacEventService<GuacFrontendEventArguments>) {
    }

    /**
     * Presses and releases this key, sending the corresponding keydown
     * and keyup events. In the case of sticky keys, the pressed state
     * is toggled, and only a single keydown/keyup event will be sent,
     * depending on the current state.
     *
     * @param event
     *     The mouse event which resulted in this function being
     *     invoked.
     */
    updateKey(event: MouseEvent) {
        // If sticky, toggle pressed state
        if (this.sticky) {
            this.pressed = !this.pressed;
            this.pressedChange.emit(this.pressed);
        }

        // For all non-sticky keys, press and release key immediately
        else {
            this.guacEventService.broadcast('guacSyntheticKeydown', { keysym: this.keysym });
            this.guacEventService.broadcast('guacSyntheticKeyup', { keysym: this.keysym });
        }

        // Prevent loss of focus due to interaction with buttons
        event.preventDefault();
    }

    ngOnChanges(changes: SimpleChanges): void {
        // Send keyup/keydown when pressed state is altered
        if (changes['pressed']) {

            const isPressed = changes['pressed'].currentValue;
            const wasPressed = changes['pressed'].previousValue;

            // If the key is pressed now, send keydown
            if (isPressed)
                this.guacEventService.broadcast('guacSyntheticKeydown', { keysym: this.keysym });

            // If the key was pressed, but is not pressed any longer, send keyup
            else if (wasPressed)
                this.guacEventService.broadcast('guacSyntheticKeyup', { keysym: this.keysym });
        }
    }
}
