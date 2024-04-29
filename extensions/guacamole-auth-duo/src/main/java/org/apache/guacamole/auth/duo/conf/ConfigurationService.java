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

package org.apache.guacamole.auth.duo.conf;

import com.google.inject.Inject;
import java.net.URI;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.properties.IntegerGuacamoleProperty;
import org.apache.guacamole.properties.StringGuacamoleProperty;
import org.apache.guacamole.properties.URIGuacamoleProperty;

/**
 * Service for retrieving configuration information regarding the Duo
 * authentication extension.
 */
public class ConfigurationService {

    /**
     * The Guacamole server environment.
     */
    @Inject
    private Environment environment;

    /**
     * The property within guacamole.properties which defines the hostname
     * of the Duo API endpoint to be used to verify user identities. This will
     * usually be in the form "api-XXXXXXXX.duosecurity.com", where "XXXXXXXX"
     * is some arbitrary alphanumeric value assigned by Duo and specific to
     * your organization.
     */
    private static final StringGuacamoleProperty DUO_API_HOSTNAME =
            new StringGuacamoleProperty() {

        @Override
        public String getName() { return "duo-api-hostname"; }

    };

    /**
     * The property within guacamole.properties which defines the client id
     * received from Duo for verifying Guacamole users. This value MUST be
     * exactly 20 characters.
     */
    private static final StringGuacamoleProperty DUO_CLIENT_ID =
            new StringGuacamoleProperty() {

        @Override
        public String getName() { return "duo-client-id"; }

    };

    /**
     * The property within guacamole.properties which defines the secret key
     * received from Duo for verifying Guacamole users. This value MUST be
     * exactly 40 characters.
     */
    private static final StringGuacamoleProperty DUO_CLIENT_SECRET =
            new StringGuacamoleProperty() {

        @Override
        public String getName() { return "duo-client-secret"; }

    };

    /**
     * The property within guacamole.properties which defines the redirect URI
     * that Duo will call after the second factor has been completed. This
     * should be the URI used to access Guacamole.
     */
    private static final URIGuacamoleProperty DUO_REDIRECT_URI =
            new URIGuacamoleProperty() {

        @Override
        public String getName() { return "duo-redirect-uri"; }
                
    };
    
    /**
     * The property that configures the timeout, in minutes, of in-progress
     * Duo authentication attempts. Authentication attempts that take longer
     * than this period of time will be invalidated.
     */
    private static final IntegerGuacamoleProperty DUO_AUTH_TIMEOUT =
            new IntegerGuacamoleProperty() {
                
        @Override
        public String getName() { return "duo-auth-timeout"; }
                
    };

    /**
     * Returns the hostname of the Duo API endpoint to be used to verify user
     * identities, as defined in guacamole.properties by the "duo-api-hostname"
     * property. This will usually be in the form
     * "api-XXXXXXXX.duosecurity.com", where "XXXXXXXX" is some arbitrary
     * alphanumeric value assigned by Duo and specific to your organization.
     *
     * @return
     *     The hostname of the Duo API endpoint to be used to verify user
     *     identities.
     *
     * @throws GuacamoleException
     *     If the associated property within guacamole.properties is missing.
     */
    public String getAPIHostname() throws GuacamoleException {
        return environment.getRequiredProperty(DUO_API_HOSTNAME);
    }

    /**
     * Returns the Duo client id received from Duo for verifying Guacamole
     * users, as defined in guacamole.properties by the "duo-client-id"
     * property. This value MUST be exactly 20 characters.
     *
     * @return
     *     The client id received from Duo for verifying Guacamole users.
     *
     * @throws GuacamoleException
     *     If the associated property within guacamole.properties is missing.
     */
    public String getClientId() throws GuacamoleException {
        return environment.getRequiredProperty(DUO_CLIENT_ID);
    }

    /**
     * Returns the client secret received from Duo for verifying Guacamole users,
     * as defined in guacamole.properties by the "duo-client-secret" property.
     * This value MUST be exactly 20 characters.
     *
     * @return
     *     The client secret received from Duo for verifying Guacamole users.
     *
     * @throws GuacamoleException
     *     If the associated property within guacamole.properties is missing.
     */
    public String getClientSecret() throws GuacamoleException {
        return environment.getRequiredProperty(DUO_CLIENT_SECRET);
    }

    /**
     * Return the callback URI that will be called by Duo after authentication
     * with Duo has been completed. This should be the URI to return the user
     * to the Guacamole interface, and will be a full URI.
     * 
     * @return
     *     The URL for Duo to use to callback to the Guacamole interface after
     *     authentication has been completed.
     * 
     * @throws GuacamoleException 
     *     If guacamole.properties cannot be read, or if the property is not
     *     defined.
     */
    public URI getRedirectUri() throws GuacamoleException {
        return environment.getRequiredProperty(DUO_REDIRECT_URI);
    }


    /**
     * Returns the maximum amount of time to allow for an in-progress Duo
     * authentication attempt to be completed, in minutes. A user that takes
     * longer than this amount of time to complete authentication with Duo
     * will need to try again.
     *
     * @return
     *     The maximum amount of time to allow for an in-progress Duo
     *     authentication attempt to be completed, in minutes.
     *
     * @throws GuacamoleException
     *     If the authentication timeout cannot be parsed.
     */
    public int getAuthenticationTimeout() throws GuacamoleException {
        return environment.getProperty(DUO_AUTH_TIMEOUT, 5);
    }

}
