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
import { filter, Observable, Subject } from "rxjs";
import { GuacEvent, GuacEventName } from "../types/GuacEvent";
import { GuacEventArguments } from "../types/GuacEventArguments";

/**
 * Service for broadcasting and subscribing to Guacamole frontend events.
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
     * TODO: Document
     * @param eventName
     * @param args
     */
    broadcast<T extends GuacEventName<Args>>(eventName: T, args?: Args[T]): GuacEvent<Args> {
        const guacEvent = {event: new GuacEvent<Args>(eventName)};
        const eventArgs = args ? {...args, ...guacEvent} : guacEvent;
        this.events.next(eventArgs);
        return guacEvent.event;
    }

    /**
     * TODO: Document
     * @param eventName
     * @returns
     */
    on<T extends GuacEventName<Args>>(eventName: T): Observable<{
        event: GuacEvent<Args>
    } & Args[T]> {
        return this.events.pipe(
            filter(({event}) => eventName === event.name)
        );
    }
}
