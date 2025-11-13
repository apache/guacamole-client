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

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.openid.user.OpenIDUserContext;
import org.apache.guacamole.auth.sso.SSOAuthenticationProvider;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.UserContext;

/**
 * Guacamole authentication backend which authenticates users using an
 * arbitrary external system implementing OpenID. Connections are provided
 * from user-mapping.xml and placed in the ROOT group.
 */
public class OpenIDAuthenticationProvider extends SSOAuthenticationProvider {

    /**
     * Creates a new OpenIDAuthenticationProvider that authenticates users
     * against an OpenID service.
     */
    public OpenIDAuthenticationProvider() {
        super(AuthenticationProviderService.class, OpenIDResource.class,
                new OpenIDAuthenticationProviderModule());
    }

    @Override
    public String getIdentifier() {
        return "openid";
    }

    @Override
    public UserContext getUserContext(AuthenticatedUser authenticatedUser)
            throws GuacamoleException {

        // Only provide UserContext for users authenticated by this extension
        if (authenticatedUser.getAuthenticationProvider() != this)
            return null;

        // Ensure we have an SSOAuthenticatedUser to access role information
        if (!(authenticatedUser instanceof org.apache.guacamole.auth.sso.user.SSOAuthenticatedUser)) {
            return null;
        }

        org.apache.guacamole.auth.sso.user.SSOAuthenticatedUser ssoUser = 
                (org.apache.guacamole.auth.sso.user.SSOAuthenticatedUser) authenticatedUser;

        // Create UserContext with connections from user-mapping.xml
        // Pass the authenticated user so we can check roles
        OpenIDUserContext userContext = new OpenIDUserContext(
                this, authenticatedUser.getIdentifier(), ssoUser);
        
        // Inject dependencies into the user context
        getInjector().injectMembers(userContext);
        
        // Initialize the user context (reads connections from user-mapping.xml if user has required role)
        userContext.init();
        
        return userContext;

    }

}
