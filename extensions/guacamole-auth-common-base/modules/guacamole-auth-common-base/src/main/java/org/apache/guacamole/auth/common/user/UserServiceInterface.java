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

package org.apache.guacamole.auth.common.user;

import java.util.Collection;
import java.util.List;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.common.base.ActivityRecordSearchTerm;
import org.apache.guacamole.auth.common.base.ActivityRecordSortPredicate;
import org.apache.guacamole.net.auth.ActivityRecord;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating users.
 */
public interface UserServiceInterface {

    /**
     * Retrieves the user corresponding to the given credentials from the
     * database. Note that this function will not enforce any additional account
     * restrictions, including explicitly disabled accounts, scheduling, and
     * password expiration. It is the responsibility of the caller to enforce
     * such restrictions, if desired.
     *
     * @param authenticationProvider
     *            The AuthenticationProvider on behalf of which the user is
     *            being retrieved.
     *
     * @param credentials
     *            The credentials to use when locating the user.
     *
     * @return An AuthenticatedUser containing the existing ModeledUser object
     *         if the credentials given are valid, null otherwise.
     *
     * @throws GuacamoleException
     *             If the provided credentials to not conform to expectations.
     */
    public ModeledAuthenticatedUser retrieveAuthenticatedUser(
            AuthenticationProvider authenticationProvider,
            Credentials credentials) throws GuacamoleException;

    /**
     * Retrieves the user corresponding to the given AuthenticatedUser from the
     * database.
     *
     * @param authenticationProvider
     *            The AuthenticationProvider on behalf of which the user is
     *            being retrieved.
     *
     * @param authenticatedUser
     *            The AuthenticatedUser to retrieve the corresponding
     *            ModeledUser of.
     *
     * @return The ModeledUser which corresponds to the given AuthenticatedUser,
     *         or null if no such user exists.
     *
     * @throws GuacamoleException
     *             If a ModeledUser object for the user corresponding to the
     *             given AuthenticatedUser cannot be created.
     */
    public ModeledUserAbstract retrieveUser(
            AuthenticationProvider authenticationProvider,
            AuthenticatedUser authenticatedUser) throws GuacamoleException;

    /**
     * Resets the password of the given user to the new password specified via
     * the "new-password" and "confirm-new-password" parameters from the
     * provided credentials. If these parameters are missing or invalid,
     * additional credentials will be requested.
     *
     * @param user
     *            The user whose password should be reset.
     *
     * @param credentials
     *            The credentials from which the parameters required for
     *            password reset should be retrieved.
     *
     * @throws GuacamoleException
     *             If the password reset parameters within the given credentials
     *             are invalid or missing.
     */
    public void resetExpiredPassword(ModeledUserAbstract user,
            Credentials credentials) throws GuacamoleException;

    /**
     * Retrieves the login history of the given user, including any active
     * sessions.
     *
     * @param authenticatedUser
     *            The user retrieving the login history.
     *
     * @param user
     *            The user whose history is being retrieved.
     *
     * @return The login history of the given user, including any active
     *         sessions.
     *
     * @throws GuacamoleException
     *             If permission to read the login history is denied.
     */
    public List<ActivityRecord> retrieveHistory(
            ModeledAuthenticatedUser authenticatedUser,
            ModeledUserAbstract user) throws GuacamoleException;

    /**
     * Retrieves user login history records matching the given criteria.
     * Retrieves up to <code>limit</code> user history records matching the
     * given terms and sorted by the given predicates. Only history records
     * associated with data that the given user can read are returned.
     *
     * @param user
     *            The user retrieving the login history.
     *
     * @param requiredContents
     *            The search terms that must be contained somewhere within each
     *            of the returned records.
     *
     * @param sortPredicates
     *            A list of predicates to sort the returned records by, in order
     *            of priority.
     *
     * @param limit
     *            The maximum number of records that should be returned.
     *
     * @return The login history of the given user, including any active
     *         sessions.
     *
     * @throws GuacamoleException
     *             If permission to read the user login history is denied.
     */
    public List<ActivityRecord> retrieveHistory(ModeledAuthenticatedUser user,
            Collection<ActivityRecordSearchTerm> requiredContents,
            List<ActivityRecordSortPredicate> sortPredicates, int limit)
            throws GuacamoleException;

}
