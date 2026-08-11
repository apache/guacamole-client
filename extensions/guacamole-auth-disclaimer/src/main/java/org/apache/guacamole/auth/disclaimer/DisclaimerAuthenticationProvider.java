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

package org.apache.guacamole.auth.disclaimer;

import java.util.Collections;
import java.util.Date;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.disclaimer.conf.ConfigurationService;
import org.apache.guacamole.auth.disclaimer.form.DisclaimerField;
import org.apache.guacamole.language.TranslatableGuacamoleInsufficientCredentialsException;
import org.apache.guacamole.net.auth.AbstractAuthenticationProvider;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;

/**
 * AuthenticationProvider implementation which uses Duo as an additional
 * authentication factor for users which have already been authenticated by
 * some other AuthenticationProvider.
 */
public class DisclaimerAuthenticationProvider extends AbstractAuthenticationProvider {

    /**
     * The unique identifier for this authentication provider. This is used in
     * various parts of the Guacamole client to distinguish this provider from
     * others, particularly when multiple authentication providers are used.
     */
    public static String PROVIDER_IDENTIFER = "disclaimer";
    
    /**
     * The configuration service for this authentication extension.
     */
    private final ConfigurationService confService = new ConfigurationService();

    /**
     * Creates a new DisclaimerAuthenticationProvider that requires users to
     * acknowledge a disclaimer prior to logging in.
     * 
     * @throws GuacamoleException
     *     If a required property is missing, or an error occurs while parsing
     *     a property.
     */
    public DisclaimerAuthenticationProvider() throws GuacamoleException {

    }

    @Override
    public String getIdentifier() {
        return PROVIDER_IDENTIFER;
    }
    
    @Override
    public UserContext decorate(UserContext context,
            AuthenticatedUser authenticatedUser, Credentials credentials) throws GuacamoleException {
        
        // If a token has already been issued, we have been logged in and we can continue.
        String token = credentials.getRequestDetails().getParameter("token");
        if (token != null && !token.isEmpty())
            return context;
        
        // If the parameter for acknowledging the disclaimer is present, check
        // that and continue the login.
        String acknowledgedValue = credentials.getRequestDetails().getParameter(DisclaimerField.PARAMETER_NAME);
        if (acknowledgedValue != null && !acknowledgedValue.isEmpty()) {
            if (DisclaimerField.PARAMETER_TRUTH_VALUE.equals(acknowledgedValue))
                return context;
        }
        
        // If configured, grab the last login.
        Date lastLogin = null;
        if (confService.getLastLogin())
            lastLogin = context.self().getLastActive();
        
        // Throw the exception that will display the disclaimer field.
        throw new TranslatableGuacamoleInsufficientCredentialsException(
                "User must accept disclaimer before continuinging.",
                "LOGIN.INFO_DISCLAIMER_ACKNOWLEDGEMENT_REQUIRED",
                new CredentialsInfo(
                        Collections.singletonList(
                                new DisclaimerField(
                                        confService.getTitle(),
                                        confService.getDisclaimerText(),
                                        lastLogin
                                )
                        )
                )
        );
        
    }

}
