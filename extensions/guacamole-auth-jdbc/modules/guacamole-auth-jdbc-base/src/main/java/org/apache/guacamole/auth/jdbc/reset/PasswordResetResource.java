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


package org.apache.guacamole.auth.jdbc.reset;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.jdbc.user.ModeledAuthenticatedUser;

/**
 * REST resource exposed at /api/session/ext/{provider}/ through the
 * UserContext of an authenticated user.
 */
@Produces(MediaType.APPLICATION_JSON)
public class PasswordResetResource {

    /**
     * The user whose session this resource belongs to.
     */
    private final ModeledAuthenticatedUser caller;

    /**
     * The service which sends the link.
     */
    private final PasswordResetService service;

    /**
     * Creates a resource acting on behalf of the given user.
     *
     * @param caller
     *     The user whose session this resource belongs to.
     *
     * @param service
     *     The service which sends the link.
     */
    public PasswordResetResource(ModeledAuthenticatedUser caller,
            PasswordResetService service) {
        this.caller = caller;
        this.service = service;
    }

    /**
     * Mails a password reset link to the given user's stored e-mail address.
     * Requires system ADMINISTER or UPDATE permission on that user.
     *
     * @param username
     *     The user whose password should be reset.
     *
     * @throws GuacamoleException
     *     If the caller lacks permission (403), the user does not exist
     *     (404), has no e-mail address (400), has been sent too many links
     *     (429), or the mail cannot be sent.
     */
    @POST
    @Path("password-reset")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void sendResetLink(@FormParam("username") String username)
            throws GuacamoleException {
        service.sendResetLink(caller, username);
    }

}
