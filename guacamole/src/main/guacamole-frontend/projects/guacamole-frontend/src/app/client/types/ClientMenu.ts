

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

import { ScrollState } from 'guacamole-frontend-lib';
import { WritableSignal } from '@angular/core';

/**
 * Properties of the client menu.
 */
export interface ClientMenu {

    /**
     * Whether the menu is currently shown.
     */
    shown: WritableSignal<boolean>;

    /**
     * The currently selected input method. This may be any of the values
     * defined within preferenceService.inputMethods.
     */
    inputMethod: WritableSignal<string>;

    /**
     * Whether translation of touch to mouse events should emulate an
     * absolute pointer device, or a relative pointer device.
     */
    emulateAbsoluteMouse: WritableSignal<boolean>;

    /**
     * The current scroll state of the menu.
     */
    scrollState: WritableSignal<ScrollState>;

    /**
     * The current desired values of all editable connection parameters as
     * a set of name/value pairs, including any changes made by the user.
     */
    connectionParameters: Record<string, string>;

}
