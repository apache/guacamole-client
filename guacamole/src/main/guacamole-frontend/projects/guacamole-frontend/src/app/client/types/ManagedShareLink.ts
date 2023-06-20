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

import { UserCredentials } from '../../rest/types/UserCredentials';
import { SharingProfile } from '../../rest/types/SharingProfile';

/**
 * Represents a link which can be used to gain access to an
 * active Guacamole connection.
 *
 * This class is used by ManagedClient to represent
 * generated connection sharing links.
 */
export class ManagedShareLink {

    /**
     * The human-readable display name of this share link.
     */
    name?: string;

    /**
     * The actual URL of the link which can be used to access the shared
     * connection.
     */
    href: string;

    /**
     * The sharing profile which was used to generate the share link.
     */
    sharingProfile: SharingProfile;

    /**
     * The credentials from which the share link was derived.
     */
    sharingCredentials: UserCredentials;

    /**
     * Create a new ManagedShareLink object.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     ManagedShareLink.
     */
    constructor(template: ManagedShareLink) {
        this.name = template.name;
        this.href = template.href;
        this.sharingProfile = template.sharingProfile;
        this.sharingCredentials = template.sharingCredentials;
    }

    /**
     * Creates a new ManagedShareLink from a set of UserCredentials and the
     * SharingProfile which was used to generate those UserCredentials.
     *
     * @param sharingProfile
     *     The SharingProfile which was used, via the REST API, to generate the
     *     given UserCredentials.
     *
     * @param sharingCredentials
     *     The UserCredentials object returned by the REST API in response to a
     *     request to share a connection using the given SharingProfile.
     *
     * @return
     *     A new ManagedShareLink object can be used to access the connection
     *     shared via the given SharingProfile and resulting UserCredentials.
     */
    static getInstance(sharingProfile: SharingProfile, sharingCredentials: UserCredentials): ManagedShareLink {

        // Generate new share link using the given profile and credentials
        return new ManagedShareLink({
            name: sharingProfile.name,
            href: UserCredentials.getLink(sharingCredentials),
            sharingProfile: sharingProfile,
            sharingCredentials: sharingCredentials
        });

    }
}
