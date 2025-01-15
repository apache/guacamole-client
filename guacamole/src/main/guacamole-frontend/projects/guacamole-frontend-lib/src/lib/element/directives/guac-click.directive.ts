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

import { DestroyRef, Directive, ElementRef, HostListener, Input } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { GuacEventService } from '../../events/services/guac-event.service';
import { GuacEventArguments } from '../../events/types/GuacEventArguments';

/**
 * A callback that is invoked by the guacClick directive when a
 * click or click-like event is received.
 *
 * @param shift
 *     Whether Shift was held down at the time the click occurred.
 *
 * @param ctrl
 *     Whether Ctrl or Meta (the Mac "Command" key) was held down
 *     at the time the click occurred.
 */
export type GuacClickCallback = (shift: boolean, ctrl: boolean) => void;

/**
 * A directive which provides handling of click and click-like touch events.
 * The state of Shift and Ctrl modifiers is tracked through these click events
 * to allow for specific handling of Shift+Click and Ctrl+Click.
 */
@Directive({
    selector: '[guacClick]',
    standalone: false
})
export class GuacClickDirective {

    /**
     * A callback that is invoked by the guacClick directive when a
     * click or click-like event is received.
     */
    @Input({ required: true }) guacClick!: GuacClickCallback;

    /**
     * The element which will register the click.
     */
    element: Element;

    /**
     * Whether either Shift key is currently pressed.
     */
    shift = false;

    /**
     * Whether either Ctrl key is currently pressed. To allow the
     * Command key to be used on Mac platforms, this flag also
     * considers the state of either Meta key.
     */
    ctrl = false;

    // Fire provided callback for each mouse-initiated "click" event ...
    @HostListener('click', ['$event']) elementClicked(e: MouseEvent) {
        if (this.element.contains(e.target as Node)) {
            this.guacClick(this.shift, this.ctrl);
        }
    }

    // ... and for touch-initiated click-like events
    @HostListener('touchstart', ['$event']) elementTouched(e: Event) {
        if (this.element.contains(e.target as Node)) {
            this.guacClick(this.shift, this.ctrl);
        }
    }

    constructor(el: ElementRef,
                private guacEventService: GuacEventService<GuacEventArguments>,
                private destroyRef: DestroyRef) {
        this.element = el.nativeElement;

        // Update tracking of modifier states for each key press
        this.guacEventService.on('guacKeydown')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(({ keyboard }) => {
                this.updateModifiers(keyboard);
            });

        // Update tracking of modifier states for each key release
        this.guacEventService.on('guacKeyup')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(({ keyboard }) => {
                this.updateModifiers(keyboard);
            });
    }

    /**
     * Updates the state of the {@link shift} and {@link ctrl} flags
     * based on which keys are currently marked as held down by the
     * given Guacamole.Keyboard.
     *
     * @param keyboard
     *     The Guacamole.Keyboard instance to read key states from.
     */
    updateModifiers(keyboard: Guacamole.Keyboard) {

        this.shift = !!(
            keyboard.pressed[0xFFE1] // Left shift
            || keyboard.pressed[0xFFE2] // Right shift
        );

        this.ctrl = !!(
            keyboard.pressed[0xFFE3] // Left ctrl
            || keyboard.pressed[0xFFE4] // Right ctrl
            || keyboard.pressed[0xFFE7] // Left meta (command)
            || keyboard.pressed[0xFFE8] // Right meta (command)
        );

    }

}
