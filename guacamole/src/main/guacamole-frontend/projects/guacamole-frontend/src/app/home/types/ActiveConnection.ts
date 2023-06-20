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

import { ManagedClient } from '../../client/types/ManagedClient';

/**
 * A recently-user connection, visible to the current user, with an
 * associated history entry.
 *
 * Used by the guacRecentConnections directive.
 */
export class ActiveConnection {

    /**
     * Create a new ActiveConnection object.
     *
     * @param name
     *     The human-readable name of this connection.
     *
     * @param client
     *     The client associated with this active connection.
     */
    constructor(public name: string, public client: ManagedClient) {
    }

}
