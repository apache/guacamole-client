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
 * associated with a sharing profile.
 */
export class SharingProfile {

    /**
     * The unique identifier associated with this sharing profile.
     */
    identifier: string;

    /**
     * The unique identifier of the connection that this sharing profile
     * can be used to share.
     */
    primaryConnectionIdentifier?: string;

    /**
     * The human-readable name of this sharing profile, which is not
     * necessarily unique.
     */
    name?: string;

    /**
     * Connection configuration parameters, as dictated by the protocol in
     * use by the primary connection, arranged as name/value pairs. This
     * information may not be available until directly queried. If this
     * information is unavailable, this property will be null or undefined.
     */
    parameters?: Record<string, string>;

    /**
     * Arbitrary name/value pairs which further describe this sharing
     * profile. The semantics and validity of these attributes are dictated
     * by the extension which defines them.
     */
    attributes: Record<string, string>;

    /**
     * Creates a new SharingProfile object.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     SharingProfile.
     */
    constructor(template: Partial<SharingProfile> = {}) {
        this.identifier = template.identifier || '';
        this.primaryConnectionIdentifier = template.primaryConnectionIdentifier;
        this.name = template.name;
        this.parameters = template.parameters;
        this.attributes = template.attributes || {};
    }
}
