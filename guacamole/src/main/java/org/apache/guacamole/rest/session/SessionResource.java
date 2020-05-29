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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleResourceNotFoundException;
import org.apache.guacamole.GuacamoleSession;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.rest.tunnel.TunnelCollectionResource;
import org.apache.guacamole.rest.tunnel.TunnelCollectionResourceFactory;

/**
 * A REST resource which exposes all data associated with a Guacamole user's
 * session via the underlying UserContexts.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SessionResource {

    /**
     * The GuacamoleSession being exposed by this SessionResource.
     */
    private final GuacamoleSession session;

    /**
     * Factory for creating UserContextResources which expose a given
     * UserContext.
     */
    @Inject
    private UserContextResourceFactory userContextResourceFactory;

    /**
     * Factory for creating instances of resources which represent the
     * collection of tunnels within a GuacamoleSession.
     */
    @Inject
    private TunnelCollectionResourceFactory tunnelCollectionResourceFactory;

    /**
     * Creates a new SessionResource which exposes the data within the given
     * GuacamoleSession.
     *
     * @param session
     *     The GuacamoleSession which should be exposed through this
     *     SessionResource.
     */
    @AssistedInject
    public SessionResource(@Assisted GuacamoleSession session) {
        this.session = session;
    }

    /**
     * Retrieves a resource representing the UserContext associated with the
     * AuthenticationProvider having the given identifier.
     *
     * @param authProviderIdentifier
     *     The unique identifier of the AuthenticationProvider associated with
     *     the UserContext being retrieved.
     *
     * @return
     *     A resource representing the UserContext associated with the
     *     AuthenticationProvider having the given identifier.
     *
     * @throws GuacamoleException
     *     If the AuthenticationProvider identifier is invalid.
     */
    @Path("data/{dataSource}")
    public UserContextResource getUserContextResource(
            @PathParam("dataSource") String authProviderIdentifier)
            throws GuacamoleException {

        // Pull UserContext defined by the given auth provider identifier
        UserContext userContext = session.getUserContext(authProviderIdentifier);

        // Return a resource exposing the retrieved UserContext
        return userContextResourceFactory.create(userContext);

    }

    /**
     * Returns the arbitrary REST resource exposed by the UserContext
     * associated with the AuthenticationProvider having the given identifier.
     *
     * @param authProviderIdentifier
     *     The unique identifier of the AuthenticationProvider associated with
     *     the UserContext whose arbitrary REST service is being retrieved.
     *
     * @return
     *     The arbitrary REST resource exposed by the UserContext exposed by
     *     this UserContextresource.
     *
     * @throws GuacamoleException
     *     If no such resource could be found, or if an error occurs while
     *     retrieving that resource.
     */
    @Path("ext/{dataSource}")
    public Object getExtensionResource(
            @PathParam("dataSource") String authProviderIdentifier)
            throws GuacamoleException {

        // Pull UserContext defined by the given auth provider identifier
        UserContext userContext = session.getUserContext(authProviderIdentifier);

        // Pull resource from user context
        Object resource = userContext.getResource();
        if (resource != null)
            return resource;

        // UserContext-specific resource could not be found
        throw new GuacamoleResourceNotFoundException("No such resource.");

    }

    /**
     * Retrieves a resource representing all tunnels associated with session
     * exposed by this SessionResource.
     *
     * @return
     *     A resource representing all tunnels associated with the
     *     AuthenticationProvider having the given identifier.
     */
    @Path("tunnels")
    public TunnelCollectionResource getTunnelCollectionResource() {
        return tunnelCollectionResourceFactory.create(session);
    }

}
