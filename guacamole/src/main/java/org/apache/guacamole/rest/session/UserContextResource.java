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
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.guacamole.net.auth.UserContext;

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

}
