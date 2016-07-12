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

package org.apache.guacamole.rest.session;

import org.apache.guacamole.rest.directory.DirectoryResource;
import org.apache.guacamole.rest.directory.DirectoryResourceFactory;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.rest.connection.APIConnection;

/**
 * A REST resource which exposes the contents of a particular UserContext.
 *
 * @author Michael Jumper
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserContextResource {

    /**
     * The UserContext being exposed through this resource.
     */
    private final UserContext userContext;

    /**
     * Factory for creating DirectoryResources which expose a given
     * Connection Directory.
     */
    @Inject
    private DirectoryResourceFactory<Connection, APIConnection> connectionDirectoryResourceFactory;

    /**
     * Creates a new UserContextResource which exposes the data within the
     * given UserContext.
     *
     * @param userContext
     *     The UserContext which should be exposed through this
     *     UserContextResource.
     */
    @AssistedInject
    public UserContextResource(@Assisted UserContext userContext) {
        this.userContext = userContext;
    }

    /**
     * Returns a new resource which represents the Connection Directory
     * contained within the UserContext exposed by this UserContextResource.
     *
     * @return
     *     A new resource which represents the Connection Directory contained
     *     within the UserContext exposed by this UserContextResource.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the Connection Directory.
     */
    @Path("connections")
    public DirectoryResource<Connection, APIConnection> getConnectionDirectoryResource()
            throws GuacamoleException {
        return connectionDirectoryResourceFactory.create(userContext,
                userContext.getConnectionDirectory());
    }

}
