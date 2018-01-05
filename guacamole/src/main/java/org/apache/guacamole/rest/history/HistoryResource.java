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

package org.apache.guacamole.rest.history;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.ActivityRecord;
import org.apache.guacamole.net.auth.ActivityRecordSet;
import org.apache.guacamole.net.auth.ConnectionRecord;
import org.apache.guacamole.net.auth.UserContext;

/**
 * A REST resource for retrieving and managing the history records of Guacamole
 * objects.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HistoryResource {

    /**
     * The maximum number of history records to return in any one response.
     */
    private static final int MAXIMUM_HISTORY_SIZE = 1000;

    /**
     * The UserContext whose associated connection history is being exposed.
     */
    private final UserContext userContext;

    /**
     * Creates a new HistoryResource which exposes the connection history
     * associated with the given UserContext.
     *
     * @param userContext
     *     The UserContext whose connection history should be exposed.
     */
    public HistoryResource(UserContext userContext) {
        this.userContext = userContext;
    }

    /**
     * Retrieves the usage history for all connections, restricted by optional
     * filter parameters.
     *
     * @param requiredContents
     *     The set of strings that each must occur somewhere within the
     *     returned connection records, whether within the associated username,
     *     the name of the associated connection, or any associated date. If
     *     non-empty, any connection record not matching each of the strings
     *     within the collection will be excluded from the results.
     *
     * @param sortPredicates
     *     A list of predicates to apply while sorting the resulting connection
     *     records, describing the properties involved and the sort order for
     *     those properties.
     *
     * @return
     *     A list of connection records, describing the start and end times of
     *     various usages of this connection.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the connection history.
     */
    @GET
    @Path("connections")
    public List<APIConnectionRecord> getConnectionHistory(
            @QueryParam("contains") List<String> requiredContents,
            @QueryParam("order") List<APISortPredicate> sortPredicates)
            throws GuacamoleException {

        // Retrieve overall connection history
        ActivityRecordSet<ConnectionRecord> history = userContext.getConnectionHistory();

        // Restrict to records which contain the specified strings
        for (String required : requiredContents) {
            if (!required.isEmpty())
                history = history.contains(required);
        }

        // Sort according to specified ordering
        for (APISortPredicate predicate : sortPredicates)
            history = history.sort(predicate.getProperty(), predicate.isDescending());

        // Limit to maximum result size
        history = history.limit(MAXIMUM_HISTORY_SIZE);

        // Convert record set to collection of API connection records
        List<APIConnectionRecord> apiRecords = new ArrayList<APIConnectionRecord>();
        for (ConnectionRecord record : history.asCollection())
            apiRecords.add(new APIConnectionRecord(record));

        // Return the converted history
        return apiRecords;

    }

    /**
     * Retrieves the login history for all users, restricted by optional filter
     * parameters.
     *
     * @param requiredContents
     *     The set of strings that each must occur somewhere within the
     *     returned user records, whether within the associated username or any
     *     associated date. If non-empty, any user record not matching each of
     *     the strings within the collection will be excluded from the results.
     *
     * @param sortPredicates
     *     A list of predicates to apply while sorting the resulting user
     *     records, describing the properties involved and the sort order for
     *     those properties.
     *
     * @return
     *     A list of user records, describing the start and end times of user
     *     sessions.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the user history.
     */
    @GET
    @Path("users")
    public List<APIActivityRecord> getUserHistory(
            @QueryParam("contains") List<String> requiredContents,
            @QueryParam("order") List<APISortPredicate> sortPredicates)
            throws GuacamoleException {

        // Retrieve overall user history
        ActivityRecordSet<ActivityRecord> history = userContext.getUserHistory();

        // Restrict to records which contain the specified strings
        for (String required : requiredContents) {
            if (!required.isEmpty())
                history = history.contains(required);
        }

        // Sort according to specified ordering
        for (APISortPredicate predicate : sortPredicates)
            history = history.sort(predicate.getProperty(), predicate.isDescending());

        // Limit to maximum result size
        history = history.limit(MAXIMUM_HISTORY_SIZE);

        // Convert record set to collection of API user records
        List<APIActivityRecord> apiRecords = new ArrayList<APIActivityRecord>();
        for (ActivityRecord record : history.asCollection())
            apiRecords.add(new APIActivityRecord(record));

        // Return the converted history
        return apiRecords;

    }

}
