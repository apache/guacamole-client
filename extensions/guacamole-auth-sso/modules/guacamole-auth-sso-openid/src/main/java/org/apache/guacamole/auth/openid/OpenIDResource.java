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
package org.apache.guacamole.auth.openid;

import com.google.inject.Inject;
import javax.ws.rs.core.Response;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.sso.SSOResource;

/**
 * REST API resource that automatically redirects users to the OpenID identity
 * provider.
 */
public class OpenIDResource implements SSOResource {

    /**
     * Service for authenticating users using OpenID.
     */
    @Inject
    private AuthenticationProviderService authService;

    @Override
    public Response redirectToIdentityProvider() throws GuacamoleException {
        return Response.seeOther(authService.getLoginURI()).build();
    }

}
