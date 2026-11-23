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
 * Returned by REST API calls when representing the data
 * associated with an active connection. Each active connection is
 * effectively a pairing of a connection and the user currently using it,
 * along with other information.
 */
export class ActiveConnection {

    /**
     * The identifier which uniquely identifies this specific active
     * connection.
     */
    identifier?: string;

    /**
     * The identifier of the connection associated with this active
     * connection.
     */
    connectionIdentifier?: string;

    /**
     * The time that the connection began, in seconds since
     * 1970-01-01 00:00:00 UTC, if known.
     */
    startDate?: number;

    /**
     * The remote host that initiated the connection, if known.
     */
    remoteHost?: string;

    /**
     * The username of the user associated with the connection, if known.
     */
    username?: string;

    /**
     * Whether this active connection may be connected to, just as a
     * normal connection.
     */
    connectable?: boolean;

    /**
     * Creates a new ActiveConnection object.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     ActiveConnection.
     */
    constructor(template: Partial<ActiveConnection> = {}) {
        this.identifier = template.identifier;
        this.connectionIdentifier = template.connectionIdentifier;
        this.startDate = template.startDate;
        this.remoteHost = template.remoteHost;
        this.username = template.username;
        this.connectable = template.connectable;
    }
}
