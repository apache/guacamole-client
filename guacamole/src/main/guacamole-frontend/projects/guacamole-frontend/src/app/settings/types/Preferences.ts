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
 * Preferences, as name/value pairs. Each property name
 * corresponds to the name of a preference.
 */
export interface Preferences {
    /**
     * Whether translation of touch to mouse events should emulate an
     * absolute pointer device, or a relative pointer device.
     */
    emulateAbsoluteMouse: boolean;

    /**
     * The default input method. This may be any of the values defined
     * within preferenceService.inputMethods.
     */
    inputMethod: string;

    /**
     * The key of the desired display language.
     */
    language: string;

    /**
     * The timezone set by the user, in IANA zone key format (Olson time
     * zone database).
     */
    timezone: string
}
