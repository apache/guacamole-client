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

/**
 * Defines the events broadcast by guacamole-frontend-lib, mapping the name of
 * each event to the type of its arguments. Events without arguments map to an
 * empty object. Applications extend this interface with their own events. No
 * argument may be named "event", as subscribers receive the GuacEvent under
 * that name.
 */
export interface GuacEventArguments {

    // Keyboard events

    /**
     * Broadcast by the application for each key pressed on the local
     * keyboard while a user is logged in. The event receives the keysym of
     * the key as keysym and the Guacamole.Keyboard which tracks the local
     * keyboard as keyboard. Calling preventDefault() on the event keeps the
     * key event from reaching the browser.
     */
    guacKeydown: { keysym: number; keyboard: Guacamole.Keyboard; };

    /**
     * Broadcast by the application for each key released on the local
     * keyboard while a user is logged in. The event receives the keysym of
     * the key as keysym and the Guacamole.Keyboard which tracks the local
     * keyboard as keyboard.
     */
    guacKeyup: { keysym: number; keyboard: Guacamole.Keyboard; };

    // OSK events

    /**
     * Broadcast for each key pressed through a source other than the local
     * keyboard, such as the on-screen keyboard or the text input. The event
     * receives the keysym of the key as keysym.
     */
    guacSyntheticKeydown: { keysym: number; };

    /**
     * Broadcast for each key released through a source other than the local
     * keyboard, such as the on-screen keyboard or the text input. The event
     * receives the keysym of the key as keysym.
     */
    guacSyntheticKeyup: { keysym: number; };

    // Menu events

    /**
     * Broadcast to toggle the Guacamole menu of the client page. The client
     * page listens for this event; the frontend itself does not broadcast
     * it.
     */
    guacToggleMenu: Record<string, never>;

    /**
     * Broadcast to show the Guacamole menu of the client page. The client
     * page listens for this event; the frontend itself does not broadcast
     * it.
     */
    guacShowMenu: Record<string, never>;

    /**
     * Broadcast to hide the Guacamole menu of the client page. The client
     * page listens for this event; the frontend itself does not broadcast
     * it.
     */
    guacHideMenu: Record<string, never>;

}
