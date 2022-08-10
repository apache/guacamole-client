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

package org.apache.guacamole.vault.ksm.secret;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A class intended for use as a key in KSM user record client cache. This
 * class contains both a username and a password. When identifying a KSM
 * record using token syntax like "KEEPER_USER_*", the user record will
 * actually be identified by both the user and domain, if the appropriate
 * settings are enabled.
 */
class UserDomain {

    /**
     * The username associated with the user record.
     * This field should never be null.
     */
    private final String username;

    /**
     * The domain associated with the user record.
     * This field can be null.
     */
    private final String domain;

    /**
     * Create a new UserDomain instance with the provided username and
     * domain. The domain may be null, but the username should never be.
     *
     * @param username
     *    The username to create the UserDomain instance with. This should
     *    never be null.
     *
     * @param domain
     *    The domain to create the UserDomain instance with. This can be null.
     */
    UserDomain(@Nonnull String username, @Nullable String domain) {
        this.username = username;
        this.domain = domain;
    }

    @Override
    public int hashCode() {

        final int prime = 31;

        int result = 1;
        result = prime * result + ((domain == null) ? 0 : domain.hashCode());
        result = prime * result + ((username == null) ? 0 : username.hashCode());

        return result;

    }

    @Override
    public boolean equals(Object obj) {

        // Check if the other object is this exact object
        if (this == obj)
            return true;

        // Check if the other object is null
        if (obj == null)
            return false;

        // Check if the other object is also a UserDomain
        if (getClass() != obj.getClass())
            return false;

        // If it is a UserDomain, it must have the same username...
        UserDomain other = (UserDomain) obj;
        if (username == null) {
            if (other.username != null)
                return false;
        } else if (!username.equals(other.username))
            return false;

        // .. and the same domain
        if (domain == null) {
            if (other.domain != null)
                return false;
        } else if (!domain.equals(other.domain))
            return false;

        return true;
    }

    /**
     * Get the username associated with this UserDomain.
     *
     * @return
     *     The username associated with this UserDomain.
     */
    public String getUsername() {
        return username;
    }


    /**
     * Get the domain associated with this UserDomain.
     *
     * @return
     *     The domain associated with this UserDomain.
     */
    public String getDomain() {
        return domain;
    }

}