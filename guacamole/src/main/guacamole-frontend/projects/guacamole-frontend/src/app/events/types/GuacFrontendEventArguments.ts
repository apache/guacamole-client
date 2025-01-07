

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
    guacFatalPageError: { error: any };

    // Auth events
    guacLogin: { authToken: string; };
    guacLoginPending: { parameters: Promise<any> };
    guacLoginFailed: { parameters: Promise<any>; error: Error; };
    guacInvalidCredentials: { parameters: Promise<any>; error: any; };
    guacInsufficientCredentials: { parameters: Promise<any>; error: any; };
    guacLogout: { token: string | null; };

    // Keyboard events
    guacBeforeKeydown: { keysym: number; keyboard: Guacamole.Keyboard; };
    guacBeforeKeyup: { keysym: number; keyboard: Guacamole.Keyboard; };

    // File browser events
    guacUploadComplete: { filename: string; };

    // Client events
    guacClientFocused: { newFocusedClient: ManagedClient | null; };
    guacMenuShown: { menuShown: boolean };
    guacClientArgumentsUpdated: { focusedClient: ManagedClient | null; };

    // Clipboard events
    guacClipboard: { data: ClipboardData };

    // Player events
    guacPlayerLoading: {};
    guacPlayerLoaded: {};
    guacPlayerError: { message: string; };
    guacPlayerProgress: { duration: number; current: number; };
    guacPlayerPlay: {};
    guacPlayerPause: {};
    guacPlayerSeek: { position: number; };
}
