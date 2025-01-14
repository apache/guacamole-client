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

import { AfterViewInit, Component, DestroyRef, ElementRef, ViewChild, ViewEncapsulation } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { GuacEventService } from 'guacamole-frontend-lib';
import { GuacFrontendEventArguments } from '../../events/types/GuacFrontendEventArguments';

/**
 * The number of characters to include on either side of text input
 * content, to allow the user room to use backspace and delete.
 */
const TEXT_INPUT_PADDING = 4;

/**
 * The Unicode codepoint of the character to use for padding on
 * either side of text input content.
 */
const TEXT_INPUT_PADDING_CODEPOINT = 0x200B;

/**
 * Keys which should be allowed through to the client when in text
 * input mode, providing corresponding key events are received.
 * Keys in this set will be allowed through to the server.
 */
const ALLOWED_KEYS: Record<number, boolean> = {
    0xFE03: true, /* AltGr */
    0xFF08: true, /* Backspace */
    0xFF09: true, /* Tab */
    0xFF0D: true, /* Enter */
    0xFF1B: true, /* Escape */
    0xFF50: true, /* Home */
    0xFF51: true, /* Left */
    0xFF52: true, /* Up */
    0xFF53: true, /* Right */
    0xFF54: true, /* Down */
    0xFF57: true, /* End */
    0xFF64: true, /* Insert */
    0xFFBE: true, /* F1 */
    0xFFBF: true, /* F2 */
    0xFFC0: true, /* F3 */
    0xFFC1: true, /* F4 */
    0xFFC2: true, /* F5 */
    0xFFC3: true, /* F6 */
    0xFFC4: true, /* F7 */
    0xFFC5: true, /* F8 */
    0xFFC6: true, /* F9 */
    0xFFC7: true, /* F10 */
    0xFFC8: true, /* F11 */
    0xFFC9: true, /* F12 */
    0xFFE1: true, /* Left shift */
    0xFFE2: true, /* Right shift */
    0xFFE3: true, /* Left ctrl */
    0xFFE4: true, /* Right ctrl */
    0xFFE9: true, /* Left alt */
    0xFFEA: true, /* Right alt */
    0xFFFF: true  /* Delete */
};

/**
 * A component which displays the Guacamole text input method.
 */
@Component({
    selector     : 'guac-text-input',
    templateUrl  : './text-input.component.html',
    encapsulation: ViewEncapsulation.None
})
export class TextInputComponent implements AfterViewInit {

    /**
     * Recently-sent text, ordered from oldest to most recent.
     */
    sentText: string[] = [];

    /**
     * Whether the "Alt" key is currently pressed within the text input
     * interface.
     */
    altPressed = false;

    /**
     * Whether the "Ctrl" key is currently pressed within the text
     * input interface.
     */
    ctrlPressed = false;

    /**
     * ElementRef to the text area input target.
     */
    @ViewChild('target') targetRef!: ElementRef<HTMLTextAreaElement>;

    /**
     * The text area input target.
     */
    target!: HTMLTextAreaElement;

    /**
     * Whether the text input target currently has focus. Setting this
     * attribute has no effect, but any bound property will be updated
     * as focus is gained or lost.
     */
    hasFocus = false;

    /**
     * Whether composition is currently active within the text input
     * target element, such as when an IME is in use.
     */
    composingText = false;

    /**
     * Inject required services.
     */
    constructor(private guacEventService: GuacEventService<GuacFrontendEventArguments>,
                private destroyRef: DestroyRef) {
    }

    ngAfterViewInit(): void {
        this.target = this.targetRef.nativeElement;

        this.target.onfocus = () => {
            this.hasFocus = true;
            this.resetTextInputTarget(TEXT_INPUT_PADDING);
        };
        this.target.onblur = () => {
            this.hasFocus = false;
        };

        this.target.addEventListener('input', (e: Event) => {
            // Ignore input events during text composition
            if (this.composingText)
                return;

            let i;
            const content = this.target.value;
            const expectedLength = TEXT_INPUT_PADDING * 2;

            // If content removed, update
            if (content.length < expectedLength) {

                // Calculate number of backspaces and send
                const backspaceCount = TEXT_INPUT_PADDING - this.target.selectionStart;
                for (i = 0; i < backspaceCount; i++)
                    this.sendKeysym(0xFF08);

                // Calculate number of deletes and send
                const deleteCount = expectedLength - content.length - backspaceCount;
                for (i = 0; i < deleteCount; i++)
                    this.sendKeysym(0xFFFF);

            } else
                this.sendString(content);

            // Reset content
            this.resetTextInputTarget(TEXT_INPUT_PADDING);
            e.preventDefault();

        }, false);

        // Do not allow event target contents to be selected during input
        this.target.addEventListener('selectstart', function (e) {
            e.preventDefault();
        }, false);

        // If the text input UI has focus, prevent keydown events
        this.guacEventService.on('guacBeforeKeydown')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(({ event, keysym }) => {
                // filterKeydown
                if (this.hasFocus && !ALLOWED_KEYS[keysym])
                    event.preventDefault();
            });

        // If the text input UI has focus, prevent keyup events
        this.guacEventService.on('guacBeforeKeyup')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(({ event, keysym }) => {
                // filterKeyup
                if (this.hasFocus && !ALLOWED_KEYS[keysym])
                    event.preventDefault();
            });

        // Attempt to focus initially
        this.target.focus();
    }

    /**
     * Translates a given Unicode codepoint into the corresponding X11
     * keysym.
     *
     * @param codepoint
     *     The Unicode codepoint to translate.
     *
     * @returns
     *     The X11 keysym that corresponds to the given Unicode
     *     codepoint, or null if no such keysym exists.
     */
    keysymFromCodepoint(codepoint: number): number | null {

        // Keysyms for control characters
        if (codepoint <= 0x1F || (codepoint >= 0x7F && codepoint <= 0x9F))
            return 0xFF00 | codepoint;

        // Keysyms for ASCII chars
        if (codepoint >= 0x0000 && codepoint <= 0x00FF)
            return codepoint;

        // Keysyms for Unicode
        if (codepoint >= 0x0100 && codepoint <= 0x10FFFF)
            return 0x01000000 | codepoint;

        return null;

    }

    /**
     * Presses and releases the key corresponding to the given keysym,
     * as if typed by the user.
     *
     * @param keysym The keysym of the key to send.
     */
    sendKeysym(keysym: number): void {
        this.guacEventService.broadcast('guacSyntheticKeydown', { keysym });
        this.guacEventService.broadcast('guacSyntheticKeyup', { keysym });
    }

    /**
     * Presses and releases the key having the keysym corresponding to
     * the Unicode codepoint given, as if typed by the user.
     *
     * @param codepoint
     *     The Unicode codepoint of the key to send.
     */
    sendCodepoint(codepoint: number): void {

        if (codepoint === 10) {
            this.sendKeysym(0xFF0D);
            this.releaseStickyKeys();
            return;
        }

        const keysym = this.keysymFromCodepoint(codepoint);
        if (keysym) {
            this.sendKeysym(keysym);
            this.releaseStickyKeys();
        }
    }

    /**
     * Translates each character within the given string to keysyms and
     * sends each, in order, as if typed by the user.
     *
     * @param content
     *     The string to send.
     */
    sendString(content: string): void {

        let sentText = '';

        // Send each codepoint within the string
        for (let i = 0; i < content.length; i++) {
            const codepoint = content.charCodeAt(i);
            if (codepoint !== TEXT_INPUT_PADDING_CODEPOINT) {
                sentText += String.fromCharCode(codepoint);
                this.sendCodepoint(codepoint);
            }
        }

        // Display the text that was sent
        this.sentText.push(sentText);

        // Remove text after one second
        setTimeout(() => {
            this.sentText.shift();
        }, 1000);

    }

    /**
     * Releases all currently-held sticky keys within the text input UI.
     */
    releaseStickyKeys(): void {

        // Reset all sticky keys
        this.altPressed = false;
        this.ctrlPressed = false;

    }

    /**
     * Removes all content from the text input target, replacing it
     * with the given number of padding characters. Padding of the
     * requested size is added on both sides of the cursor, thus the
     * overall number of characters added will be twice the number
     * specified.
     *
     * @param padding
     *     The number of characters to pad the text area with.
     */
    resetTextInputTarget(padding: number): void {

        const paddingChar = String.fromCharCode(TEXT_INPUT_PADDING_CODEPOINT);

        // Pad text area with an arbitrary, non-typable character (so there is something
        // to delete with backspace or del), and position cursor in middle.
        this.target.value = new Array(padding * 2 + 1).join(paddingChar);
        this.target.setSelectionRange(padding, padding);

    }

    trackByIndex(index: number): number {
        return index;
    }
}
