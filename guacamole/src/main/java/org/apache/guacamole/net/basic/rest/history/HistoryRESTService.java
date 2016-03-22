/*
 * Copyright (C) 2015 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.apache.guacamole.net.basic.rest.history;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.ConnectionRecord;
import org.apache.guacamole.net.auth.ConnectionRecordSet;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.net.basic.GuacamoleSession;
import org.apache.guacamole.net.basic.rest.ObjectRetrievalService;
import org.apache.guacamole.net.basic.rest.auth.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A REST Service for retrieving and managing the history records of Guacamole
 * objects.
 *
 * @author Michael Jumper
 */
@Path("/data/{dataSource}/history")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HistoryRESTService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(HistoryRESTService.class);

    /**
     * The maximum number of history records to return in any one response.
     */
    private static final int MAXIMUM_HISTORY_SIZE = 1000;

    /**
     * A service for authenticating users from auth tokens.
     */
    @Inject
    private AuthenticationService authenticationService;

    /**
     * Service for convenient retrieval of objects.
     */
    @Inject
    private ObjectRetrievalService retrievalService;

    /**
     * Retrieves the usage history for all connections, restricted by optional
     * filter parameters.
     *
     * @param authToken
     *     The authentication token that is used to authenticate the user
     *     performing the operation.
     *
     * @param authProviderIdentifier
     *     The unique identifier of the AuthenticationProvider associated with
     *     the UserContext containing the connection whose history is to be
     *     retrieved.
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
    @Path("/connections")
    public List<APIConnectionRecord> getConnectionHistory(@QueryParam("token") String authToken,
            @PathParam("dataSource") String authProviderIdentifier,
            @QueryParam("contains") List<String> requiredContents,
            @QueryParam("order") List<APIConnectionRecordSortPredicate> sortPredicates)
            throws GuacamoleException {

        GuacamoleSession session = authenticationService.getGuacamoleSession(authToken);
        UserContext userContext = retrievalService.retrieveUserContext(session, authProviderIdentifier);

        // Retrieve overall connection history
        ConnectionRecordSet history = userContext.getConnectionHistory();

        // Restrict to records which contain the specified strings
        for (String required : requiredContents) {
            if (!required.isEmpty())
                history = history.contains(required);
        }

        // Sort according to specified ordering
        for (APIConnectionRecordSortPredicate predicate : sortPredicates)
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

}
