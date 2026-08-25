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

package org.apache.guacamole.templates;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.AbstractAuthenticationProvider;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.templates.user.TemplateUserContext;

/**
 * An implementation of an Authentication Provider which allows users to
 * associate a connection with another connection as a template, pulling
 * the values of parameters specified in the selected "template connection"
 * into the connection.
 */
public class TemplateAuthenticationProvider extends AbstractAuthenticationProvider {
    
    @Override
    public String getIdentifier() {
        return "template-ext";
    }
    
    @Override
    public UserContext decorate(UserContext context, AuthenticatedUser authenticatedUser, Credentials credentials) throws GuacamoleException {
        if (context instanceof TemplateUserContext)
            return context;
        
        return new TemplateUserContext(context);
    }
    
}
