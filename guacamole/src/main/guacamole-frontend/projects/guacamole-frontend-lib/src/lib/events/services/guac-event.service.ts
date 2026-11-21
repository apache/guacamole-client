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
import { filter, Observable, Subject } from 'rxjs';
import { GuacEvent, GuacEventName } from '../types/GuacEvent';
import { GuacEventArguments } from '../types/GuacEventArguments';

/**
 * Service for broadcasting and subscribing to Guacamole frontend events.
 *
 * @template Args
 *     Type which specifies the set of all possible events and their
 *     arguments.
 */
@Injectable({
    providedIn: 'root'
})
export class GuacEventService<Args extends GuacEventArguments> {

    /**
     * Subject for all Guacamole frontend events.
     */
    private events = new Subject<any>();

    /**
     * Broadcasts the event with the given name to all subscribers registered
     * via on(). Subscribers receive the given arguments together with the
     * created GuacEvent.
     *
     * @template T
     *     Union type of all possible event names.
     *
     * @param eventName
     *     The name of the event to broadcast.
     *
     * @param args
     *     The arguments of the event, if the event has any. No argument may
     *     be named "event", as subscribers receive the GuacEvent under that
     *     name.
     *
     * @returns
     *     The broadcast event. Subscribers are called synchronously, so the
     *     caller can check whether any of them called preventDefault() on
     *     the event.
     */
    broadcast<T extends GuacEventName<Args>>(eventName: T, args?: Args[T] & { event?: never }): GuacEvent<Args> {
        const guacEvent = { event: new GuacEvent<Args>(eventName) };
        const eventArgs = args ? { ...args, ...guacEvent } : guacEvent;
        this.events.next(eventArgs);
        return guacEvent.event;
    }

    /**
     * Returns an Observable which emits each time an event with the given
     * name is broadcast.
     *
     * @template T
     *     Union type of all possible event names.
     *
     * @param eventName
     *     The name of the event to subscribe to.
     *
     * @returns
     *     An Observable which emits the arguments of each matching event
     *     together with the GuacEvent itself, as the property event.
     */
    on<T extends GuacEventName<Args>>(eventName: T): Observable<{
        event: GuacEvent<Args>
    } & Args[T]> {
        return this.events.pipe(
            filter(({ event }) => eventName === event.name)
        );
    }
}
