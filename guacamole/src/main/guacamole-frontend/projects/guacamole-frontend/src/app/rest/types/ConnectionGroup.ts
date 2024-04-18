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

import { Optional } from '../../util/utility-types';
import { Connection } from './Connection';

/**
 * Returned by REST API calls when representing the data
 * associated with a connection group.
 */
export class ConnectionGroup {

    /**
     * The unique identifier associated with this connection group.
     */
    identifier?: string;

    /**
     * The unique identifier of the connection group that contains this
     * connection group.
     *
     * @default ConnectionGroup.ROOT_IDENTIFIER
     */
    parentIdentifier: string;

    /**
     * The human-readable name of this connection group, which is not
     * necessarily unique.
     */
    name: string;

    /**
     * The type of this connection group, which may be either
     * ConnectionGroup.Type.ORGANIZATIONAL or
     * ConnectionGroup.Type.BALANCING.
     *
     * @default ConnectionGroup.Type.ORGANIZATIONAL
     */
    type: string;

    /**
     * An array of all child connections, if known. This property may be
     * null or undefined if children have not been queried, and thus the
     * child connections are unknown.
     */
    childConnections: Connection[] | null | undefined;

    /**
     * An array of all child connection groups, if known. This property may
     * be null or undefined if children have not been queried, and thus the
     * child connection groups are unknown.
     */
    childConnectionGroups: ConnectionGroup[] | null | undefined;

    /**
     * Arbitrary name/value pairs which further describe this connection
     * group. The semantics and validity of these attributes are dictated
     * by the extension which defines them.
     */
    attributes: Record<string, string>;

    /**
     * The count of currently active connections using this connection
     * group. This field will be returned from the REST API during a get
     * operation, but manually setting this field will have no effect.
     */
    activeConnections: number;

    /**
     * Creates a new ConnectionGroup object.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     ConnectionGroup.
     */
    constructor(template: Optional<ConnectionGroup, 'parentIdentifier' | 'type' | 'childConnections'
        | 'childConnectionGroups' | 'attributes'>) {
        this.identifier = template.identifier;
        this.parentIdentifier = template.parentIdentifier || ConnectionGroup.ROOT_IDENTIFIER;
        this.name = template.name;
        this.type = template.type || ConnectionGroup.Type.ORGANIZATIONAL;
        this.childConnections = template.childConnections;
        this.childConnectionGroups = template.childConnectionGroups;
        this.attributes = template.attributes || {};
        this.activeConnections = template.activeConnections;
    }

    /**
     * The reserved identifier which always represents the root connection
     * group.
     */
    static ROOT_IDENTIFIER = 'ROOT';

    /**
     * All valid connection group types.
     */
    static Type = {

        /**
         * The type string associated with balancing connection groups.
         */
        BALANCING: 'BALANCING',

        /**
         * The type string associated with organizational connection groups.
         */
        ORGANIZATIONAL: 'ORGANIZATIONAL'

    };
}
