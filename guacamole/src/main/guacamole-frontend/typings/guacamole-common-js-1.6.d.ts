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

/*
 * Declares the parts of the guacamole-common-js 1.6 API that the frontend
 * uses but @types/guacamole-common-js 1.5.3 lacks. Only APIs the frontend
 * actually uses belong here. Remove each declaration once a published
 * @types/guacamole-common-js covers it.
 *
 * New names must augment the module that declares a related class, not the
 * package root: names added to the package root are invisible as types
 * through the global Guacamole namespace.
 */

import type { Mouse } from 'guacamole-common-js/lib/Mouse';
import type { Touch } from 'guacamole-common-js/lib/Touch';

declare module 'guacamole-common-js/lib/SessionRecording' {

    // The interpreters themselves are internal to SessionRecording; the
    // frontend only receives their events.

    export namespace KeyEventInterpreter {

        /**
         * The definition of a single key: its keysym, its human-readable
         * name and, for printable keys, the character it produces.
         */
        export class KeyDefinition {
            constructor(template?: Partial<KeyDefinition>);
            keysym: number;
            name: string;
            value?: string;
        }

        /**
         * A single press or release of a key within a session recording.
         */
        export class KeyEvent {
            constructor(template?: Partial<KeyEvent>);
            definition: KeyDefinition;
            pressed: boolean;

            /**
             * The time of the event in milliseconds, relative to the start
             * of the recording.
             */
            timestamp: number;
        }

    }

    export namespace ClipboardEventInterpreter {

        /**
         * A single clipboard change within a session recording.
         */
        export class ClipboardEvent {
            constructor(template?: Partial<ClipboardEvent>);
            mimetype: string;
            data: string;

            /**
             * The time of the event in milliseconds, relative to the start
             * of the recording.
             */
            timestamp: number;
        }

    }

    interface SessionRecording {

        /**
         * Fired once with all key events of the recording after it has been
         * fully loaded. The list is empty if the recording contains no key
         * events.
         */
        onkeyevents: null | ((events: KeyEventInterpreter.KeyEvent[]) => void);

        /**
         * Fired once with all clipboard events of the recording after it has
         * been fully loaded.
         */
        onclipboardevents: null | ((events: ClipboardEventInterpreter.ClipboardEvent[]) => void);

    }

}

declare module 'guacamole-common-js/lib/Keyboard' {

    interface Keyboard {

        /**
         * Resynchronizes the modifier and lock keys with the modifier flags
         * of the given mouse or touch event, pressing or releasing keys as
         * needed.
         *
         * @param event
         *     The mouse or touch event that occurred.
         */
        updateModifiers(event: Mouse.Event | Touch.Event): void;

    }

    namespace Keyboard {

        /**
         * Named keysyms of modifier and lock keys, plus functions that
         * classify keysyms.
         */
        export const Keysym: {

            /**
             * Returns whether the given keysym is a modifier key such as
             * Shift or Ctrl. Lock keys like Caps Lock do not count.
             */
            isModifier(keysym: number): boolean;

        };

    }

}
