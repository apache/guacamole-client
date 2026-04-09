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

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.Map;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.quickconnect.QuickConnectDirectory;
import org.apache.guacamole.auth.quickconnect.utility.QCParser;

/**
 * A class that implements REST endpoints for the QuickConnect
 * extension.
 */
@Produces(MediaType.APPLICATION_JSON)
public class QuickConnectREST {
    
    /**
     * The connection directory for this REST endpoint.
     */
    private final QuickConnectDirectory directory;

    /**
     * Construct a new QuickConnectREST class, taking in a
     * QuickConnectDirectory for use with this class. 
     *
     * @param directory
     *     The QuickConnectDirectory object to associate with this
     *     REST endpoint class.
     */
    public QuickConnectREST(QuickConnectDirectory directory) {
        this.directory = directory;
    }

    /**
     * Parse the URI read from the POST input, add the connection
     * to the directory, and return a Map containing a single key,
     * identifier, and the identifier of the new connection.
     *
     * @param uri
     *     The URI to parse into a connection.
     *
     * @return
     *     A Map containing a single key, identifier, and the
     *     identifier of the new connection.
     *
     * @throws GuacamoleException
     *     If an error is encountered parsing the URI.
     */
    @POST
    @Path("create")
    public Map<String, String> create(@FormParam("uri") String uri) 
            throws GuacamoleException {

        return Collections.singletonMap("identifier", directory.create(uri));
 
    }

}
