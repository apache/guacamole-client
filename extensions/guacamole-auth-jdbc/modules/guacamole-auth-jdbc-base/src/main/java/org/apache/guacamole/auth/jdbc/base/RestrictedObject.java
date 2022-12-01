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

package org.apache.guacamole.auth.jdbc.base;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleUnauthorizedException;
import org.apache.guacamole.auth.jdbc.JDBCEnvironment;
import org.apache.guacamole.auth.jdbc.user.ModeledAuthenticatedUser;
import org.apache.guacamole.auth.jdbc.user.ModeledUser;

import com.google.inject.Inject;

/**
 * Common base class for objects that are associated with the users that
 * obtain them.
 */
public abstract class RestrictedObject {

    /**
     * The user this object belongs to. Access is based on his/her permission
     * settings.
     */
    private ModeledAuthenticatedUser currentUser;

    /**
     * The environment of the Guacamole server.
     */
    @Inject
    private JDBCEnvironment environment;

    /**
     * Initializes this object, associating it with the current authenticated
     * user and populating it with data from the given model object
     *
     * @param currentUser
     *     The user that created or retrieved this object.
     */
    public void init(ModeledAuthenticatedUser currentUser) {
        setCurrentUser(currentUser);
    }

    /**
     * Returns the user that created or queried this object. This user's
     * permissions dictate what operations can be performed on or through this
     * object.
     *
     * @return
     *     The user that created or queried this object.
     */
    public ModeledAuthenticatedUser getCurrentUser() {
        return currentUser;
    }

    /**
     * Sets the user that created or queried this object. This user's
     * permissions dictate what operations can be performed on or through this
     * object.
     *
     * @param currentUser 
     *     The user that created or queried this object.
     */
    public void setCurrentUser(ModeledAuthenticatedUser currentUser) {
        this.currentUser = currentUser;
    }

    /**
     * Validate that the current user is within a valid access time window
     * and not disabled. If the user account is disabled or not within a
     * valid access window, a GuacamoleUnauthorizedException will be thrown.
     *
     * This method can be called by RestrictedObject implementations before
     * any operation that's specific to the current logged in user, to make
     * sure that their access is still valid and enabled.
     *
     * If accessWindowCheckEnabled is set to false, the check will be skipped,
     * and GuacamoleUnauthorizedException will never be thrown.
     *
     * @throws GuacamoleException
     *     If the user is outside of a valid access window, the user is
     *     disabled, or an error occurs while trying to determine access time
     *     restriction configuration.
     */
    protected void validateUserAccess() throws GuacamoleException {

        // If access windows shouldn't be checked for active sessions, skip
        // this check entirely
        if (!environment.enforceAccessWindowsForActiveSessions())
            return;

        // If the user is outside of a valid access time window or disabled,
        // throw an exception to immediately log them out
        ModeledUser modeledUser = getCurrentUser().getUser();
        if (
                !modeledUser.isAccountAccessible()
                || !modeledUser.isAccountValid()
                || modeledUser.isDisabled()
        )
            throw new GuacamoleUnauthorizedException("Permission Denied.");
    }

}
