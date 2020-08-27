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

package org.apache.guacamole.auth.cas.conf;

import com.google.inject.Inject;
import java.net.URI;
import java.security.PrivateKey;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;

/**
 * Service for retrieving configuration information regarding the CAS service.
 */
public class ConfigurationService {

    /**
     * The Guacamole server environment.
     */
    @Inject
    private Environment environment;

    /**
     * Returns the authorization endpoint (URI) of the CAS service as
     * configured with guacamole.properties.
     *
     * @return
     *     The authorization endpoint of the CAS service, as configured with
     *     guacamole.properties.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed, or if the authorization
     *     endpoint property is missing.
     */
    public URI getAuthorizationEndpoint() throws GuacamoleException {
        return environment.getRequiredProperty(CASGuacamoleProperties.CAS_AUTHORIZATION_ENDPOINT);
    }

    /**
     * Returns the URI that the CAS service should redirect to after
     * the authentication process is complete, as configured with
     * guacamole.properties. This must be the full URL that a user would enter
     * into their browser to access Guacamole.
     *
     * @return
     *     The URI to redirect the client back to after authentication
     *     is completed, as configured in guacamole.properties.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed, or if the redirect URI
     *     property is missing.
     */
    public URI getRedirectURI() throws GuacamoleException {
        return environment.getRequiredProperty(CASGuacamoleProperties.CAS_REDIRECT_URI);
    }

    /**
     * Returns the PrivateKey used to decrypt the credential object
     * sent encrypted by CAS, or null if no key is defined.
     *
     * @return
     *     The PrivateKey used to decrypt the ClearPass
     *     credential returned by CAS.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public PrivateKey getClearpassKey() throws GuacamoleException {
        return environment.getProperty(CASGuacamoleProperties.CAS_CLEARPASS_KEY);
    }

    /**
     * Returns the attribute used to determine group memberships
     * in CAS, or null if not defined.
     *
     * @return
     *     The attribute name used to determine group memberships in CAS,
     *     null if not defined.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public String getGroupAttribute() throws GuacamoleException {
        return environment.getProperty(CASGuacamoleProperties.CAS_GROUP_ATTRIBUTE);
    }

    /**
     * Returns the attribute used to format group DN's
     * in CAS, or null if not defined.  If CAS is backed by LDAP, it will
     * return an LDAP DN, such as CN=foo,OU=bar,DC=example,DC=com.
     * This attribute may be set to CN=%s,OU=bar,DC=example,DC=com and given
     * the example above, would result in a group called "foo."  CAS backed
     * by something other than LDAP (such as a database) would likely not 
     * need this.
     *
     * @return
     *     The attribute name
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public String getGroupDnFormat() throws GuacamoleException {
        return environment.getProperty(CASGuacamoleProperties.CAS_GROUP_DN_FORMAT);
    }



}
