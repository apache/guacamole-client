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
 * associated with a user group.
 */
export class UserGroup {

    /**
     * The name which uniquely identifies this user group.
     */
    identifier?: string;

    /**
     * Arbitrary name/value pairs which further describe this user group.
     * The semantics and validity of these attributes are dictated by the
     * extension which defines them.
     *
     * @default {}
     */
    attributes: Record<string, string>;

    /**
     * Creates a new UserGroup object.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     UserGroup.
     */
    constructor(template: Partial<UserGroup> = {}) {
        this.identifier = template.identifier;
        this.attributes = template.attributes || {};
    }
}
