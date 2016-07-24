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

package org.apache.guacamole.auth.jdbc.sharing;

import org.apache.guacamole.auth.jdbc.sharingprofile.ModeledSharingProfile;
import org.apache.guacamole.auth.jdbc.tunnel.ActiveConnectionRecord;

/**
 * Defines the semantics/restrictions of a shared connection by associating an
 * active connection with a sharing profile. The sharing profile defines the
 * access provided to users of the shared active connection through its
 * connection parameters.
 *
 * @author Michael Jumper
 */
public class SharedConnectionDefinition {

    /**
     * The active connection being shared.
     */
    private final ActiveConnectionRecord activeConnection;

    /**
     * The sharing profile which dictates the level of access provided to a user
     * of the shared connection.
     */
    private final ModeledSharingProfile sharingProfile;

    /**
     * The unique key with which a user may access the shared connection.
     */
    private final String shareKey;

    /**
     * Creates a new SharedConnectionDefinition which describes an active
     * connection that can be joined, including the restrictions dictated by a
     * given sharing profile.
     *
     * @param activeConnection
     *     The active connection being shared.
     *
     * @param sharingProfile
     *     A sharing profile whose associated parameters dictate the level of
     *     access provided to the shared connection.
     *
     * @param shareKey
     *     The unique key with which a user may access the shared connection.
     */
    public SharedConnectionDefinition(ActiveConnectionRecord activeConnection,
            ModeledSharingProfile sharingProfile, String shareKey) {
        this.activeConnection = activeConnection;
        this.sharingProfile = sharingProfile;
        this.shareKey = shareKey;
    }

    /**
     * Returns the ActiveConnectionRecord of the actual in-progress connection
     * being shared.
     *
     * @return
     *     The ActiveConnectionRecord being shared.
     */
    public ActiveConnectionRecord getActiveConnection() {
        return activeConnection;
    }

    /**
     * Returns the ModeledSharingProfile whose associated parameters dictate the
     * level of access granted to users of the shared connection.
     *
     * @return
     *     A ModeledSharingProfile whose associated parameters dictate the
     *     level of access granted to users of the shared connection.
     */
    public ModeledSharingProfile getSharingProfile() {
        return sharingProfile;
    }

    /**
     * Returns the unique key with which a user may access the shared
     * connection.
     *
     * @return
     *     The unique key with which a user may access the shared connection.
     */
    public String getShareKey() {
        return shareKey;
    }

}
