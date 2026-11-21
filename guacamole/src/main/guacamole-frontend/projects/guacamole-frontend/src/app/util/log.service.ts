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

import { Injectable } from '@angular/core';

/**
 * A service for writing messages to the browser console at the levels info,
 * error, warn and debug.
 */
@Injectable({
    providedIn: 'root'
})
export class LogService {

    /**
     * Writes an informational message to the browser console.
     *
     * @param message
     *     The message to write.
     *
     * @param optionalParams
     *     Further values to write after the message.
     */
    info(message: any, ...optionalParams: any[]) {
        console.info(message, ...optionalParams);
    }

    /**
     * Writes an error message to the browser console.
     *
     * @param message
     *     The message to write.
     *
     * @param optionalParams
     *     Further values to write after the message.
     */
    error(message: any, ...optionalParams: any[]) {
        console.error(message, ...optionalParams);
    }

    /**
     * Writes a warning to the browser console.
     *
     * @param message
     *     The message to write.
     *
     * @param optionalParams
     *     Further values to write after the message.
     */
    warn(message: any, ...optionalParams: any[]) {
        console.warn(message, ...optionalParams);
    }

    /**
     * Writes a debug message to the browser console.
     *
     * @param message
     *     The message to write.
     *
     * @param optionalParams
     *     Further values to write after the message.
     */
    debug(message: any, ...optionalParams: any[]) {
        console.debug(message, ...optionalParams);
    }
}
