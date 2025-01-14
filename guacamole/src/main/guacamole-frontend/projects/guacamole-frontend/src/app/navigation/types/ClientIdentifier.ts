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
 * Uniquely identifies a particular connection or connection
 * group within Guacamole. This object can be converted to/from a string to
 * generate a guaranteed-unique, deterministic identifier for client URLs.
 */
export class ClientIdentifier {

    /**
     * The identifier of the data source associated with the object to
     * which the client will connect. This identifier will be the
     * identifier of an AuthenticationProvider within the Guacamole web
     * application.
     */
    dataSource: string;

    /**
     * The type of object to which the client will connect. Possible values
     * are defined within ClientIdentifier.Types.
     */
    type: string;

    /**
     * The unique identifier of the object to which the client will
     * connect.
     */
    id?: string;

    /**
     * Creates a new ClientIdentifier. This constructor initializes the properties of the
     * new ClientIdentifier with the corresponding properties of the given template.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     ClientIdentifier.
     */
    constructor(template: ClientIdentifier) {
        this.dataSource = template.dataSource;
        this.type = template.type;
        this.id = template.id;
    }

}

export namespace ClientIdentifier {

    /**
     * All possible ClientIdentifier types.
     */
    export enum Types {
        /**
         * The type string for a Guacamole connection.
         */
        CONNECTION        = 'c',

        /**
         * The type string for a Guacamole connection group.
         */
        CONNECTION_GROUP  = 'g',

        /**
         * The type string for an active Guacamole connection.
         */
        ACTIVE_CONNECTION = 'a'
    }
}

