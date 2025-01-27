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

import { GuacEventArguments } from './GuacEventArguments';

/**
 * Type which represents the names of all possible Guacamole events.
 *
 * @template T
 *     Type which specifies the set of all possible events and their
 *     arguments.
 */
export type GuacEventName<T extends GuacEventArguments> = keyof T;

/**
 * An event which is emitted by the GuacEventService.
 *
 * @template T
 *     Type which specifies the set of all possible events and their
 *     arguments.
 */
export class GuacEvent<T extends GuacEventArguments> {
    name: GuacEventName<T>;

    /**
     * Creates a new GuacEvent having the given name.
     *
     * @param name
     *     The name of the event.
     */
    constructor(name: GuacEventName<T>) {
        this.name = name;
    }

    /**
     * Flag which is true if preventDefault() was called on this event, and
     * false otherwise.
     */
    defaultPrevented = false;

    /**
     * Sets the defaultPrevented flag to true.
     */
    preventDefault() {
        this.defaultPrevented = true;
    }
}
