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

import { GuacEventArguments } from 'guacamole-frontend-lib';
import { ManagedClient } from '../../client/types/ManagedClient';
import { ClipboardData } from '../../clipboard/types/ClipboardData';
import { Error } from '../../rest/types/Error';

/**
 * Defines all possible guacamole events and their payloads that
 * can be emitted by the frontend.
 */
export interface GuacFrontendEventArguments extends GuacEventArguments {

    // Global events

    /**
     * The page cannot be displayed due to an error, and AppComponent replaces
     * all content with an error message. Broadcast by RequestService.DIE and
     * RequestService.PROMISE_DIE, and by AuthenticationService if
     * authentication fails with an internal error. error is the Error received
     * from the REST API.
     */
    guacFatalPageError: { error: Error };

    // Auth events

    /**
     * Authentication was successful and AuthenticationService received a new
     * token. authToken is the new auth token.
     */
    guacLogin: { authToken: string; };

    /**
     * AuthenticationService is submitting an authentication request and awaits
     * the result. The request may not yet have been submitted if the parameters
     * for that request are not ready. parameters is a promise that resolves
     * with the HTTP parameters that are ultimately submitted.
     */
    guacLoginPending: { parameters: Promise<Record<string, string>> };

    /**
     * An authentication request of AuthenticationService has failed with an
     * error from the REST API, of whatever type. This event is broadcast before
     * any other events that are specific to the nature of the failure, and may
     * be used to detect login failures in lieu of those events. parameters is
     * the promise of the HTTP parameters submitted, and error is the Error
     * received from the REST endpoint.
     */
    guacLoginFailed: { parameters: Promise<Record<string, string>>; error: Error; };

    /**
     * An authentication request failed because the credentials provided are
     * invalid. parameters is the promise of the HTTP parameters submitted, and
     * error is the Error received from the REST endpoint.
     */
    guacInvalidCredentials: { parameters: Promise<Record<string, string>>; error: Error; };

    /**
     * An authentication request failed because additional credentials are
     * needed before the request can be processed. parameters is the promise of
     * the HTTP parameters submitted, and error is the Error received from the
     * REST endpoint.
     */
    guacInsufficientCredentials: { parameters: Promise<Record<string, string>>; error: Error; };

    /**
     * An existing token is being destroyed by AuthenticationService.logout().
     * token is the auth token being destroyed, or null if there was none. If
     * the existing token for the current session is being replaced without
     * destroying that session, this event is not broadcast.
     */
    guacLogout: { token: string | null; };

    // Keyboard events

    /**
     * A key has been pressed while the application state is READY. AppComponent
     * broadcasts this event before guacKeydown; calling preventDefault() on it
     * suppresses guacKeydown. keysym is the keysym of the key, and keyboard the
     * Guacamole.Keyboard which received the key event.
     */
    guacBeforeKeydown: { keysym: number; keyboard: Guacamole.Keyboard; };

    /**
     * A key has been released while the application state is READY.
     * AppComponent broadcasts this event before guacKeyup; calling
     * preventDefault() on it suppresses guacKeyup. keysym is the keysym of the
     * key, and keyboard the Guacamole.Keyboard which received the key event.
     */
    guacBeforeKeyup: { keysym: number; keyboard: Guacamole.Keyboard; };

    // Mouse events

    /**
     * Broadcast when a mouse button is pressed, before the mouse state is
     * sent, so that modifiers inferred from the flags of the mouse event
     * (especially Cmd+Click) are processed before the click that depends on
     * them.
     */
    guacBeforeClientMouseDown: { inputEvent: Guacamole.Mouse.Event, client: ManagedClient };

    /**
     * Broadcast by guac-client after it has sent the state of a pressed mouse
     * button to the server. inputEvent is the mouse event, and client the
     * ManagedClient which received it.
     */
    guacClientMouseDown: { inputEvent: Guacamole.Mouse.Event, client: ManagedClient };

    /**
     * Broadcast by guac-client after it has sent the state of a released mouse
     * button to the server. inputEvent is the mouse event, and client the
     * ManagedClient which received it.
     */
    guacClientMouseUp: { inputEvent: Guacamole.Mouse.Event, client: ManagedClient };

    /**
     * Broadcast by guac-client after it has sent the state of a moved mouse to
     * the server. inputEvent is the mouse event, and client the ManagedClient
     * which received it.
     */
    guacClientMouseMove: { inputEvent: Guacamole.Mouse.Event, client: ManagedClient };

    // Touch events

    /**
     * Broadcast by guac-client after it has sent the state of a new touch to
     * the server. inputEvent is the touch event, and client the ManagedClient
     * which received it.
     */
    guacClientTouchStart: { inputEvent: Guacamole.Touch.Event, client: ManagedClient };

    /**
     * Broadcast by guac-client after it has sent the state of an ended touch to
     * the server. inputEvent is the touch event, and client the ManagedClient
     * which received it.
     */
    guacClientTouchEnd: { inputEvent: Guacamole.Touch.Event, client: ManagedClient };

    /**
     * Broadcast by guac-client after it has sent the state of a moved touch to
     * the server. inputEvent is the touch event, and client the ManagedClient
     * which received it.
     */
    guacClientTouchMove: { inputEvent: Guacamole.Touch.Event, client: ManagedClient };

    // File browser events

    /**
     * A file upload has completed. Broadcast by ManagedFileUploadService once
     * the whole file has been sent; filename is the name of the uploaded file.
     */
    guacUploadComplete: { filename: string; };

    // Client events

    /**
     * Broadcast by guac-tiled-clients whenever the focused client changes.
     * newFocusedClient is the only focused ManagedClient of the group, or null
     * if no client or several clients are focused.
     */
    guacClientFocused: { newFocusedClient: ManagedClient | null; };

    /**
     * Broadcast by the client page as the visibility of the Guacamole menu
     * changes. menuShown is whether the menu is shown.
     */
    guacMenuShown: { menuShown: boolean };

    /**
     * Broadcast by guac-tiled-clients whenever the arguments of the focused
     * client change, that is, the connection parameters which the server allows
     * to be changed during the connection. focusedClient is the focused
     * ManagedClient, or null if no client or several clients are focused.
     */
    guacClientArgumentsUpdated: { focusedClient: ManagedClient | null; };

    /**
     * Broadcast by guac-tiled-clients whenever the protocol of the focused
     * client changes, including when focus moves to a client with another
     * protocol. focusedClient is the focused ManagedClient, or null if no
     * client or several clients are focused.
     */
    guacClientProtocolUpdated: { focusedClient: ManagedClient | null; };

    // Clipboard events

    /**
     * The contents of the clipboard have changed. Broadcast by ClipboardService
     * once it has set the clipboard; data is the new ClipboardData.
     */
    guacClipboard: { data: ClipboardData };

    /**
     * Broadcast by ClipboardService with status true when it begins to
     * synchronize the internal clipboard with the local clipboard, and with
     * status false once the clipboard is set or the local clipboard cannot be
     * read. guac-client holds back mouse events of the real mouse while a
     * synchronization is in progress.
     */
    clipboardSyncInProgress: { status: boolean };

    // Player events

    /**
     * A new recording has been selected in PlayerComponent and is now loading.
     */
    guacPlayerLoading: Record<string, never>;

    /**
     * The current recording of PlayerComponent has finished loading.
     */
    guacPlayerLoaded: Record<string, never>;

    /**
     * The current recording of PlayerComponent cannot be loaded or played due
     * to an error. The recording may be unreadable (lack of permissions) or
     * corrupt (protocol error). message is the error message given by
     * guacamole-common-js.
     */
    guacPlayerError: { message: string; };

    /**
     * Additional data has been loaded for the current recording of
     * PlayerComponent and the duration of the recording has changed. duration
     * is the new duration in milliseconds, and current the number of bytes
     * loaded so far.
     */
    guacPlayerProgress: { duration: number; current: number; };

    /**
     * Playback of the current recording of PlayerComponent has started or has
     * been resumed.
     */
    guacPlayerPlay: Record<string, never>;

    /**
     * Playback of the current recording of PlayerComponent has been paused.
     */
    guacPlayerPause: Record<string, never>;

    /**
     * The playback position of the current recording of PlayerComponent has
     * changed. position is the new position as the number of milliseconds since
     * the start of the recording.
     */
    guacPlayerSeek: { position: number; };
}

/**
 * The names of the events which guac-client broadcasts for mouse events.
 */
export type MouseEventName = 'guacClientMouseDown' | 'guacClientMouseUp' | 'guacClientMouseMove';

/**
 * The names of the events which guac-client broadcasts for touch events.
 */
export type TouchEventName = 'guacClientTouchStart' | 'guacClientTouchEnd' | 'guacClientTouchMove';
