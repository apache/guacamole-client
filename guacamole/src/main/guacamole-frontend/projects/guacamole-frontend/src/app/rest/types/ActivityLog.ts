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

import { TranslatableMessage } from './TranslatableMessage';

/**
 * Returned by REST API calls when representing a log or
 * recording associated with a connection's usage history, such as a
 * session recording or typescript.
 */
export class ActivityLog {

    /**
     * The type of this ActivityLog.
     */
    type?: string;

    /**
     * A human-readable description of this log.
     */
    description?: TranslatableMessage;

    /**
     * Creates a new ActivityLog object.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     ActivityLog.
     */
    constructor(template: Partial<ActivityLog> = {}) {
        this.type = template.type;
        this.description = template.description;
    }

    /**
     * All possible types of ActivityLog.
     */
    static Type = {
        /**
         * A Guacamole session recording in the form of a Guacamole protocol
         * dump.
         */
        GUACAMOLE_SESSION_RECORDING: 'GUACAMOLE_SESSION_RECORDING',

        /**
         * A text log from a server-side process, such as the Guacamole web
         * application or guacd.
         */
        SERVER_LOG: 'SERVER_LOG',

        /**
         * A text session recording in the form of a standard typescript.
         */
        TYPESCRIPT: 'TYPESCRIPT',

        /**
         * The timing file related to a typescript.
         */
        TYPESCRIPT_TIMING: 'TYPESCRIPT_TIMING'
    };

}
