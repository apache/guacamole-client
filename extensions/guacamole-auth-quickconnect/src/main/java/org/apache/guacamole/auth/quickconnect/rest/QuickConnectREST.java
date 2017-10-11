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

package org.apache.guacamole.auth.quickconnect.rest;

import com.google.inject.Inject;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.apache.guacamole.auth.quickconnect.QuickConnection;
import org.apache.guacamole.auth.quickconnect.QuickConnectDirectory;
import org.apache.guacamole.auth.quickconnect.QuickConnectUserContext;
import org.apache.guacamole.auth.quickconnect.utility.QCParser;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.Directory;

/**
 * A class to create and manage REST endpoints for the
 * QuickConnect extension.
 */
@Produces(MediaType.APPLICATION_JSON)
public class QuickConnectREST {

    /**
     * The connection directory for this REST endpoint.
     */
    private QuickConnectDirectory directory;

    /**
     * The UserContext object for this REST endpoint.
     */
    private QuickConnectUserContext userContext;

    /**
     * Construct a new QuickConnectREST class, taking in the UserContext
     * object that calls this constructor.
     *
     * @param userContext
     *     The UserContext object associated with this REST endpoint
     *
     * @throws GuacamoleException
     *     If the UserContext is unavailable or the directory object
     *     cannot be retrieved.
     */
    public QuickConnectREST(QuickConnectUserContext userContext)
            throws GuacamoleException {
        this.userContext = userContext;
        this.directory = (QuickConnectDirectory)this.userContext.getConnectionDirectory();
    }

    /**
     * Parse the URI read from the POST input, add the connection
     * to the directory, and return the ID of the newly-created
     * connection.
     *
     * @param uri
     *     The URI to parse into a connection.
     *
     * @returns
     *     The ID of the connection in the directory.
     *
     * @throws
     *     Throws a GuacamoleException if an error is encountered
     *     parsing the URI.
     */
    @POST
    @Path("create")
    public String create(@FormParam("uri") String uri) 
            throws GuacamoleException {

        if (directory == null)
            throw new GuacamoleServerException("No connection directory available.");

        return directory.create(QCParser.getConfiguration(uri));
 
    }
    

}
