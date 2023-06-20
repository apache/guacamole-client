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

import { SharingProfile } from './SharingProfile';
import { Optional } from '../../util/utility-types';

/**
 * Returned by REST API calls when representing the data
 * associated with a connection.
 */
export class Connection {

    /**
     * The unique identifier associated with this connection.
     */
    identifier?: string;

    /**
     * The unique identifier of the connection group that contains this
     * connection.
     */
    parentIdentifier?: string;

    /**
     * The human-readable name of this connection, which is not necessarily
     * unique.
     */
    name?: string;

    /**
     * The name of the protocol associated with this connection, such as
     * "vnc" or "rdp".
     */
    protocol: string;

    /**
     * Connection configuration parameters, as dictated by the protocol in
     * use, arranged as name/value pairs. This information may not be
     * available until directly queried. If this information is
     * unavailable, this property will be null or undefined.
     */
    parameters?: Record<string, string>;

    /**
     * Arbitrary name/value pairs which further describe this connection.
     * The semantics and validity of these attributes are dictated by the
     * extension which defines them.
     */
    attributes: Record<string, string>;

    /**
     * The count of currently active connections using this connection.
     * This field will be returned from the REST API during a get
     * operation, but manually setting this field will have no effect.
     */
    activeConnections?: number;

    /**
     * An array of all associated sharing profiles, if known. This property
     * may be null or undefined if sharing profiles have not been queried,
     * and thus the sharing profiles are unknown.
     */
    sharingProfiles?: SharingProfile[];

    /**
     * The time that this connection was last used, in milliseconds since
     * 1970-01-01 00:00:00 UTC. If this information is unknown or
     * unavailable, this will be null.
     */
    lastActive?: number;

    /**
     * Creates a new Connection object.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     Connection.
     */
    constructor(template: Optional<Connection, 'attributes'>) {
        this.identifier = template.identifier;
        this.parentIdentifier = template.parentIdentifier;
        this.name = template.name;
        this.protocol = template.protocol;
        this.parameters = template.parameters;
        this.attributes = template.attributes || {};
        this.activeConnections = template.activeConnections;
        this.sharingProfiles = template.sharingProfiles;
        this.lastActive = template.lastActive;
    }

}
