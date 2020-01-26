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

package org.apache.guacamole.auth.wol.rest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.Map;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.wol.connection.WOLConnection;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST API endpoints for the Wake-on-LAN module
 */
@Produces(MediaType.APPLICATION_JSON)
public class WOLUserContextResource {

    /**
     * The logger for this class.
     */
    private static final Logger logger =
            LoggerFactory.getLogger(WOLUserContextResource.class);
    
    /**
     * The directory associated with this REST endpoint resource that
     * will be queried to trigger WOL packets.
     */
    private final Directory<Connection> directory;
    
    /**
     * Default timeout for attempting to connect to a host, in milliseconds.
     */
    public static final int DEFAULT_TIMEOUT = 5000;

    /**
     * Set up a new REST resource that will be use to trigger
     * WOL packets.
     * 
     * @param directory
     *     The directory to use for this REST endpoint.
     */
    public WOLUserContextResource(Directory<Connection> directory) {
        this.directory = directory;
    }

    /**
     * A method which takes a connection identifier and triggers the wake-up
     * of the host, returning the connection identifier and true if the wake-up
     * was sent successfully, and throwing an exception if an error occurs.
     * 
     * @param connectionIdentifier
     *     The identifier of the connection to trigger the wake-up for.
     * 
     * @return
     *     A map containing the connection identifier and a Boolean true if 
     *     the wake-up packet was triggered successfully.
     * 
     * @throws GuacamoleException
     *     If an error occurs triggering the host wake-up.
     */
    @POST
    @Path("wake")
    public Map<String, Boolean> wakeUpHost(
            @FormParam("connectionIdentifier") String connectionIdentifier)
            throws GuacamoleException {

        // Retrieve the connection
        WOLConnection connection = (WOLConnection) directory.get(connectionIdentifier);

        // Send the wake-up packet
        connection.wakeUpHost();

        // If we don't get an exception, we assume the packet is sent.
        return Collections.<String, Boolean>singletonMap(connectionIdentifier,
                true);
    }
    
    /**
     * Have Java attempting to connect to the given hostname on the given
     * port number, returning true if the connection is successful, otherwise
     * false.
     * 
     * @param hostname
     *     The hostname or IP of the system to connect to.
     * 
     * @param port
     *     The port number on which to connect to the system.
     * 
     * @return
     *     True if the connection is established successfully, otherwise false.
     */
    @GET
    @Path("check/{hostname}/{port}")
    public Boolean checkHost(@PathParam("hostname") String hostname,
            @PathParam("port") int port) {
        
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(hostname, port), DEFAULT_TIMEOUT);
            return true;
        }
        catch (IOException | IllegalArgumentException | SecurityException e) {
            logger.debug("Received an exception while trying to connection: {}",
                    e);
            return false;
        }
    }

}
